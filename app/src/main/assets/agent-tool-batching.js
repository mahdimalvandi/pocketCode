'use strict';

(function attachToolBatching(root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.PocketToolBatching = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function createToolBatching() {
  const BATCHABLE_READ_TOOLS = new Set(['phone_storage_roots', 'read_phone_storage']);
  const DEFAULT_MAX_BATCH_SIZE = 3;

  function toolName(call) {
    return String(call && (call.name || call.function && call.function.name) || '');
  }

  function planToolCalls(calls, options = {}) {
    if (!Array.isArray(calls) || calls.length === 0) {
      return {mode:'none', executeIndices:[], deferredIndices:[], reason:'no_tool_calls'};
    }
    const indices = calls.map((_, index) => index);
    if (calls.length === 1) {
      return {mode:'single', executeIndices:[0], deferredIndices:[], reason:'single_tool'};
    }
    const max = Number.isInteger(options.maxBatchSize) && options.maxBatchSize > 0
      ? Math.min(options.maxBatchSize, 3) : DEFAULT_MAX_BATCH_SIZE;
    let reason = '';
    if (options.automationStopped) reason = 'automation_stopped';
    else if (options.hasPendingVerification) reason = 'verification_checkpoint';
    else if (options.hasTaskPlan) reason = 'task_plan_active';
    else if (!calls.every(call => BATCHABLE_READ_TOOLS.has(toolName(call)))) reason = 'non_batchable_or_dependent_tools';
    if (reason) return {mode:'single',executeIndices:[0],deferredIndices:indices.slice(1),reason};
    return {
      mode:'batch',
      executeIndices:indices.slice(0,max),
      deferredIndices:indices.slice(max),
      reason:calls.length>max?'batch_size_limit':'independent_read_only_calls'
    };
  }

  function isBatchableTool(name) {
    return BATCHABLE_READ_TOOLS.has(String(name || ''));
  }

  return Object.freeze({planToolCalls,isBatchableTool,toolName,DEFAULT_MAX_BATCH_SIZE});
});
