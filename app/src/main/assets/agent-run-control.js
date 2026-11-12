'use strict';

(function attachRunControl(root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.PocketRunControl = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function createRunControlModule() {
  const OPERATIONS = new Set(['model_request', 'tool_call']);

  function createRunControl() {
    return { stopRequested: false, stopRequestedAt: null, activeOperation: null };
  }

  function requestStop(control, now = Date.now()) {
    if (!control || typeof control !== 'object') {
      return { accepted: false, changed: false, activeOperation: null };
    }
    if (control.stopRequested) {
      return { accepted: true, changed: false, activeOperation: control.activeOperation || null };
    }
    control.stopRequested = true;
    const timestamp = Number(now);
    control.stopRequestedAt = Number.isFinite(timestamp) ? Math.max(0, timestamp) : Date.now();
    return { accepted: true, changed: true, activeOperation: control.activeOperation || null };
  }

  function canStartNextOperation(control) {
    return Boolean(control && typeof control === 'object' && control.stopRequested !== true);
  }

  function beginOperation(control, operation) {
    if (!canStartNextOperation(control)) return false;
    const name = String(operation || '');
    if (!OPERATIONS.has(name)) return false;
    control.activeOperation = name;
    return true;
  }

  function finishOperation(control, operation) {
    if (!control || typeof control !== 'object') return false;
    if (operation && control.activeOperation !== operation) return false;
    control.activeOperation = null;
    return true;
  }

  function isStopRequested(control) {
    return Boolean(control && control.stopRequested === true);
  }

  return Object.freeze({
    createRunControl, requestStop, canStartNextOperation,
    beginOperation, finishOperation, isStopRequested
  });
});
