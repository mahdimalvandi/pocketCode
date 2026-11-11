'use strict';

(function attachAgentReliability(root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.PocketAgentReliability = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function createAgentReliability() {
  const UI_MUTATING_TOOLS = new Set([
    'open_app', 'click_element', 'type_text', 'scroll_element', 'phone_back', 'tap_screen'
  ]);
  const OBSERVATION_TOOLS = new Set(['read_screen', 'capture_screen']);
  // These broad-storage calls do not open per-operation approval dialogs.
  const SAFE_READ_TOOLS = new Set(['phone_storage_roots', 'list_phone_storage', 'read_phone_storage']);
  const FILE_MUTATION_TOOLS = new Set(['write_file', 'write_phone_file', 'delete_file']);

  function requiresVerification(toolName) {
    return UI_MUTATING_TOOLS.has(String(toolName || ''));
  }

  function isObservationTool(toolName) {
    return OBSERVATION_TOOLS.has(String(toolName || ''));
  }

  function allowedDuringVerification(pending, toolName) {
    return !pending || isObservationTool(toolName);
  }

  function blockedAfterUnverified(lastVerification, toolName) {
    if (!lastVerification || lastVerification.status !== 'unverified') return false;
    if (lastVerification.tool === toolName) return true;
    return ['user_denied', 'blocked_target'].includes(lastVerification.recovery?.category)
      && UI_MUTATING_TOOLS.has(String(toolName || ''));
  }

  function expectedOutcome(toolName, args = {}) {
    switch (toolName) {
      case 'open_app':
        return 'The app package ' + String(args.package || '(unspecified)') + ' should become the active app.';
      case 'click_element':
        return args.reason
          ? 'The visible screen should reflect this intended control action: ' + String(args.reason).slice(0, 180)
          : 'The visible screen should reflect the selected control action.';
      case 'tap_screen':
        return 'The visible screen should reflect this intended control action: ' + String(args.reason || 'the selected visual control').slice(0, 180);
      case 'type_text':
        return 'The intended editable field should contain the requested text. Do not repeat the text in the verification summary.';
      case 'scroll_element':
        return 'The visible content or scroll position should change in the requested direction; reaching a boundary is possible.';
      case 'phone_back':
        return 'The previous screen or app state should become visible.';
      default:
        return 'The UI should reflect the requested action.';
    }
  }

  function classifyFailure(toolName, rawMessage) {
    const normalized = String(rawMessage || '').toLowerCase();
    const sideEffecting = UI_MUTATING_TOOLS.has(toolName) || FILE_MUTATION_TOOLS.has(toolName);
    let category = 'unknown';
    let nextStep = 'Inspect the current state and explain the failure. Do not repeat the same operation blindly.';
    let userActionRequired = false;

    if (/user denied|denied this|operation cancelled|operation canceled|user cancelled|user canceled|رد شد|لغو شد|لغو کرد/.test(normalized)) {
      category = 'user_denied';
      nextStep = 'Respect the denial. Do not ask for the same approval again unless the user explicitly requests it.';
      userActionRequired = true;
    } else if (/blocked|system, security|financial|sensitive or|screen.*blocked|مسدود|صفحه.*حساس/.test(normalized)) {
      category = 'blocked_target';
      nextStep = 'Stop interaction with this target. Do not try screenshots, coordinate taps or another route around the block.';
    } else if (/permission|accessibility service|grant|مجوز|دسترسی|فعال کن/.test(normalized)) {
      category = 'permission_required';
      nextStep = 'Explain which Android permission or setting is missing and ask the user to enable it manually.';
      userActionRequired = true;
    } else if (/expired|stale|changed since|screen app changed|read the screen again|capture the current screen again|منقضی|صفحه.*تغییر/.test(normalized)) {
      category = 'stale_ui';
      nextStep = 'Read the current screen again and use only fresh element IDs or a new screenshot token. Do not reuse the previous target.';
    } else if (/no clickable|not found|not visible|not scrollable|cannot be found|element.*expired|قابل کلیک|پیدا نشد|قابل اسکرول/.test(normalized)) {
      category = 'target_unavailable';
      nextStep = 'Read the current screen again. If the harmless control is absent from Accessibility, use an approved screenshot; do not repeat the same element ID.';
    } else if (/timeout|timed out|network|connection|temporarily|429|rate limit|app is busy|پاسخی دریافت نشد|اتصال/.test(normalized)) {
      category = 'transient_failure';
      nextStep = sideEffecting
        ? 'The outcome may be ambiguous. Observe the current screen or file state before deciding whether a retry is safe.'
        : 'A single retry may be safe for this read-only operation after a brief delay.';
    }

    const automaticRetryAllowed = category === 'transient_failure' && SAFE_READ_TOOLS.has(toolName);
    return {
      category,
      nextStep,
      userActionRequired,
      automaticRetryAllowed,
      retryPolicy: automaticRetryAllowed ? 'safe_once' : 'never',
      outcomeMayBeUnknown: sideEffecting && (category === 'transient_failure' || category === 'unknown'),
      neverRepeatBlindly: sideEffecting || category === 'user_denied' || category === 'blocked_target'
    };
  }

  function createVerification(actionId, toolName, args = {}, result = {}) {
    return {
      actionId: String(actionId),
      tool: String(toolName),
      expectedOutcome: expectedOutcome(toolName, args),
      apiAccepted: result && typeof result === 'object' && typeof result.ok === 'boolean' ? result.ok : null,
      observationAttempts: 0
    };
  }

  return Object.freeze({requiresVerification, isObservationTool, allowedDuringVerification, blockedAfterUnverified, expectedOutcome, classifyFailure, createVerification});
});
