package dev.pocketcode.app;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Durable journal for side-effecting native operations.
 * It stores only operation IDs, action names, state and timestamps; no raw
 * tool arguments, file paths, typed text, screen content or screenshots.
 */
public final class OperationReceiptStore {
    private static final String STORAGE_KEY = "receipts_v1";
    private static final int MAX_RECEIPTS = 256;
    private static final int MAX_PENDING_RETURNED = MAX_RECEIPTS;
    private static final long TERMINAL_RETENTION_MS = 30L * 24L * 60L * 60L * 1000L;

    public enum Decision { STARTED, DUPLICATE, CONFLICT, FULL, STORAGE_ERROR }

    public static final class Receipt {
        public final String operationId;
        public final String action;
        public final String state;
        public final String resolution;
        public final long createdAt;
        public final long updatedAt;

        Receipt(String operationId, JSONObject entry) {
            this.operationId = operationId;
            this.action = entry.optString("action", "");
            this.state = entry.optString("state", "unknown");
            this.resolution = entry.optString("resolution", "");
            this.createdAt = entry.optLong("createdAt", 0L);
            this.updatedAt = entry.optLong("updatedAt", createdAt);
        }

        public JSONObject toJson(boolean replayed) throws JSONException {
            JSONObject json = new JSONObject()
                    .put("operationId", operationId)
                    .put("action", action)
                    .put("state", state)
                    .put("createdAt", createdAt)
                    .put("updatedAt", updatedAt)
                    .put("replayed", replayed);
            if (!resolution.isEmpty()) json.put("resolution", resolution);
            return json;
        }
    }

    public static final class BeginResult {
        public final Decision decision;
        public final Receipt receipt;

        BeginResult(Decision decision, Receipt receipt) {
            this.decision = decision;
            this.receipt = receipt;
        }
    }

    private final SharedPreferences preferences;
    private final String processEpoch;

    public OperationReceiptStore(SharedPreferences preferences, String processEpoch) {
        this.preferences = preferences;
        this.processEpoch = processEpoch == null ? "" : processEpoch;
    }

    public synchronized boolean recoverInterrupted() {
        try {
            JSONObject records = readRecords();
            boolean changed = false;
            long now = System.currentTimeMillis();
            for (String id : keys(records)) {
                JSONObject entry = records.optJSONObject(id);
                if (entry == null) continue;
                if (OperationReceiptPolicy.shouldRecoverInterrupted(
                        entry.optString("state", ""), entry.optString("processEpoch", ""), processEpoch)) {
                    entry.put("state", "unknown");
                    entry.put("updatedAt", now);
                    changed = true;
                }
            }
            return !changed || writeRecords(records);
        } catch (Exception ignored) {
            return false;
        }
    }

    public synchronized BeginResult begin(String operationId, String action) {
        if (!OperationReceiptPolicy.isValidOperationId(operationId)
                || !OperationReceiptPolicy.requiresNativeReceipt(action)) {
            return new BeginResult(Decision.STORAGE_ERROR, null);
        }
        try {
            JSONObject records = readRecords();
            JSONObject existing = records.optJSONObject(operationId);
            if (existing != null) {
                Receipt receipt = new Receipt(operationId, existing);
                return new BeginResult(OperationReceiptPolicy.sameAction(receipt.action, action)
                        ? Decision.DUPLICATE : Decision.CONFLICT, receipt);
            }

            long now = System.currentTimeMillis();
            pruneTerminal(records, now);
            if (records.length() >= MAX_RECEIPTS) return new BeginResult(Decision.FULL, null);

            JSONObject entry = new JSONObject()
                    .put("action", action)
                    .put("state", "started")
                    .put("processEpoch", processEpoch)
                    .put("createdAt", now)
                    .put("updatedAt", now);
            records.put(operationId, entry);
            if (!writeRecords(records)) return new BeginResult(Decision.STORAGE_ERROR, null);
            return new BeginResult(Decision.STARTED, new Receipt(operationId, entry));
        } catch (Exception ignored) {
            return new BeginResult(Decision.STORAGE_ERROR, null);
        }
    }

    public synchronized Receipt finish(String operationId, String error) {
        if (!OperationReceiptPolicy.isValidOperationId(operationId)) return null;
        try {
            JSONObject records = readRecords();
            JSONObject entry = records.optJSONObject(operationId);
            if (entry == null) return null;
            String state = entry.optString("state", "unknown");
            String recordedProcess = entry.optString("processEpoch", "");
            if (!("started".equals(state) || "unknown".equals(state))
                    || !processEpoch.equals(recordedProcess)) return new Receipt(operationId, entry);

            entry.put("state", OperationReceiptPolicy.failureState(error));
            entry.put("updatedAt", System.currentTimeMillis());
            if (!writeRecords(records)) {
                entry.put("state", "unknown");
            }
            return new Receipt(operationId, entry);
        } catch (Exception ignored) {
            return null;
        }
    }

    public synchronized Receipt get(String operationId) {
        if (!OperationReceiptPolicy.isValidOperationId(operationId)) return null;
        try {
            JSONObject entry = readRecords().optJSONObject(operationId);
            return entry == null ? null : new Receipt(operationId, entry);
        } catch (Exception ignored) {
            return null;
        }
    }

    public synchronized JSONArray listPending() throws JSONException {
        JSONObject records = readRecords();
        List<Receipt> pending = new ArrayList<>();
        for (String id : keys(records)) {
            JSONObject entry = records.optJSONObject(id);
            if (entry == null) continue;
            String state = entry.optString("state", "unknown");
            if ("started".equals(state) || "unknown".equals(state) || "completed".equals(state)) {
                pending.add(new Receipt(id, entry));
            }
        }
        Collections.sort(pending, Comparator.comparingLong(receipt -> receipt.createdAt));
        JSONArray result = new JSONArray();
        int start = Math.max(0, pending.size() - MAX_PENDING_RETURNED);
        for (int i = start; i < pending.size(); i++) result.put(pending.get(i).toJson(false));
        return result;
    }

    public synchronized JSONObject resolve(String operationId, String observedOutcome) throws Exception {
        if (!OperationReceiptPolicy.isValidOperationId(operationId)) throw new Exception("Invalid operation ID.");
        if (!"applied".equals(observedOutcome) && !"not_applied".equals(observedOutcome)
                && !"uncertain".equals(observedOutcome)) throw new Exception("Invalid observed outcome.");
        JSONObject records = readRecords();
        JSONObject entry = records.optJSONObject(operationId);
        if (entry == null) throw new Exception("Operation receipt was not found.");
        String state = entry.optString("state", "unknown");
        if (!"unknown".equals(state) && !"completed".equals(state))
            throw new Exception("Only a completed or unknown operation receipt can be reconciled.");
        if ("uncertain".equals(observedOutcome)) {
            return new JSONObject().put("ok", true).put("operationReceipt", new Receipt(operationId, entry).toJson(false));
        }
        entry.put("state", "resolved");
        entry.put("resolution", observedOutcome);
        entry.put("updatedAt", System.currentTimeMillis());
        if (!writeRecords(records)) throw new Exception("Operation receipt resolution could not be saved.");
        return new JSONObject().put("ok", true).put("operationReceipt", new Receipt(operationId, entry).toJson(false));
    }

    public synchronized void markCurrentProcessStartedUnknown() {
        try {
            JSONObject records = readRecords();
            boolean changed = false;
            long now = System.currentTimeMillis();
            for (String id : keys(records)) {
                JSONObject entry = records.optJSONObject(id);
                if (entry == null) continue;
                if ("started".equals(entry.optString("state", ""))
                        && processEpoch.equals(entry.optString("processEpoch", ""))) {
                    entry.put("state", "unknown");
                    entry.put("updatedAt", now);
                    changed = true;
                }
            }
            if (changed) writeRecords(records);
        } catch (Exception ignored) { }
    }

    private void pruneTerminal(JSONObject records, long now) {
        List<String> remove = new ArrayList<>();
        List<String> retainedTerminal = new ArrayList<>();
        for (String id : keys(records)) {
            JSONObject entry = records.optJSONObject(id);
            if (entry == null) { remove.add(id); continue; }
            String state = entry.optString("state", "unknown");
            long updatedAt = entry.optLong("updatedAt", 0L);
            if ("resolved".equals(state) || "failed".equals(state)) {
                if (updatedAt > 0 && now - updatedAt > TERMINAL_RETENTION_MS) remove.add(id);
                else retainedTerminal.add(id);
            }
        }
        for (String id : remove) records.remove(id);
        // Under capacity pressure, discard the oldest terminal records first.
        // Never evict started, completed or unknown receipts.
        Collections.sort(retainedTerminal, Comparator.comparingLong(id -> {
            JSONObject entry = records.optJSONObject(id);
            return entry == null ? 0L : entry.optLong("updatedAt", 0L);
        }));
        for (String id : retainedTerminal) {
            if (records.length() < MAX_RECEIPTS) break;
            records.remove(id);
        }
    }

    private JSONObject readRecords() throws JSONException {
        return new JSONObject(preferences.getString(STORAGE_KEY, "{}"));
    }

    private boolean writeRecords(JSONObject records) {
        return preferences.edit().putString(STORAGE_KEY, records.toString()).commit();
    }

    private static List<String> keys(JSONObject object) {
        List<String> result = new ArrayList<>();
        java.util.Iterator<String> iterator = object.keys();
        while (iterator.hasNext()) result.add(iterator.next());
        return result;
    }
}
