'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const policy = require('../../main/assets/agent-reliability.js');

test('marks only UI-changing phone actions for readback verification', () => {
  for (const name of ['open_app', 'click_element', 'type_text', 'scroll_element', 'phone_back', 'tap_screen']) {
    assert.equal(policy.requiresVerification(name), true, name);
  }
  for (const name of ['read_screen', 'capture_screen', 'list_files', 'read_phone_storage', 'write_file']) {
    assert.equal(policy.requiresVerification(name), false, name);
  }
});

test('accepts only fresh screen readers as verification observations', () => {
  assert.equal(policy.isObservationTool('read_screen'), true);
  assert.equal(policy.isObservationTool('capture_screen'), true);
  assert.equal(policy.isObservationTool('click_element'), false);
});

test('verification summaries do not copy typed text', () => {
  const checkpoint = policy.createVerification('a-1', 'type_text', {text:'private text'}, {ok:true});
  assert.equal(checkpoint.actionId, 'a-1');
  assert.equal(checkpoint.apiAccepted, true);
  assert.match(checkpoint.expectedOutcome, /contain the requested text/);
  assert.equal(checkpoint.expectedOutcome.includes('private text'), false);
});

test('user denial is terminal and never retryable', () => {
  const recovery = policy.classifyFailure('read_phone_storage', 'User denied this phone action.');
  assert.equal(recovery.category, 'user_denied');
  assert.equal(recovery.automaticRetryAllowed, false);
  assert.equal(recovery.userActionRequired, true);
});

test('stale element IDs require a fresh screen read', () => {
  const recovery = policy.classifyFailure('click_element', 'Element expired. Read the screen again.');
  assert.equal(recovery.category, 'stale_ui');
  assert.equal(recovery.automaticRetryAllowed, false);
  assert.match(recovery.nextStep, /fresh element IDs/);
});

test('ambiguous side-effect timeout is never automatically replayed', () => {
  const recovery = policy.classifyFailure('tap_screen', 'Tap timed out while dispatching.');
  assert.equal(recovery.category, 'transient_failure');
  assert.equal(recovery.automaticRetryAllowed, false);
  assert.equal(recovery.outcomeMayBeUnknown, true);
  assert.equal(recovery.neverRepeatBlindly, true);
});

test('only broad-storage read calls qualify for one safe transient retry', () => {
  const recovery = policy.classifyFailure('read_phone_storage', 'Temporary network connection timeout.');
  assert.equal(recovery.category, 'transient_failure');
  assert.equal(recovery.automaticRetryAllowed, true);
  assert.equal(recovery.retryPolicy, 'safe_once');
  assert.equal(policy.classifyFailure('read_file', 'Temporary connection timeout.').automaticRetryAllowed, false);
});

test('permission and blocked-target errors are never retried', () => {
  assert.equal(policy.classifyFailure('read_screen', 'Accessibility service is not enabled.').automaticRetryAllowed, false);
  assert.equal(policy.classifyFailure('read_screen', 'System, security, financial and Pocket Code screens are blocked.').automaticRetryAllowed, false);
});

test('blocks every non-observation tool while a UI checkpoint is pending', () => {
  const pending = {actionId:'a-2'};
  assert.equal(policy.allowedDuringVerification(pending, 'read_screen'), true);
  assert.equal(policy.allowedDuringVerification(pending, 'capture_screen'), true);
  assert.equal(policy.allowedDuringVerification(pending, 'click_element'), false);
  assert.equal(policy.allowedDuringVerification(null, 'click_element'), true);
});

test('a denied or blocked verification cannot be bypassed with another UI action', () => {
  const denied = {status:'unverified',tool:'tap_screen',recovery:{category:'user_denied'}};
  assert.equal(policy.blockedAfterUnverified(denied, 'tap_screen'), true);
  assert.equal(policy.blockedAfterUnverified(denied, 'click_element'), true);
  assert.equal(policy.blockedAfterUnverified(denied, 'read_screen'), false);
  const stale = {status:'unverified',tool:'click_element',recovery:{category:'stale_ui'}};
  assert.equal(policy.blockedAfterUnverified(stale, 'click_element'), true);
  assert.equal(policy.blockedAfterUnverified(stale, 'phone_back'), false);
});

test('native receipt IDs are required for side-effecting Agent tools, not reads', () => {
  for (const name of ['open_app','click_element','tap_screen','type_text','scroll_element','phone_back','write_file','write_phone_file','delete_file']) {
    assert.equal(policy.requiresOperationReceipt(name), true, name);
  }
  for (const name of ['read_screen','capture_screen','read_file','read_phone_storage','list_files']) {
    assert.equal(policy.requiresOperationReceipt(name), false, name);
  }
});

test('receipt reconciliation permits only the matching read surface', () => {
  assert.equal(policy.isReceiptObservationFor('writeFile','read_file'), true);
  assert.equal(policy.isReceiptObservationFor('writeFile','read_phone_file'), false);
  assert.equal(policy.isReceiptObservationFor('deleteFile','list_files'), true);
  assert.equal(policy.isReceiptObservationFor('writeShared','read_phone_file'), true);
  assert.equal(policy.isReceiptObservationFor('writeShared','read_screen'), false);
  assert.equal(policy.isReceiptObservationFor('click_element','read_screen'), true);
});

test('duplicate native operation receipts are never retryable', () => {
  const recovery = policy.classifyFailure('click_element','Duplicate operation suppressed (receipt state: completed).');
  assert.equal(recovery.category,'outcome_unknown');
  assert.equal(recovery.retryPolicy,'never');
  assert.equal(recovery.automaticRetryAllowed,false);
  assert.equal(recovery.outcomeMayBeUnknown,true);
  assert.equal(recovery.neverRepeatBlindly,true);
});
