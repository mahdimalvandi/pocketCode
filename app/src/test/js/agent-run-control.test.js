'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const control = require('../../main/assets/agent-run-control.js');

test('starts with no stop request and allows new operations', () => {
  const run = control.createRunControl();
  assert.equal(control.isStopRequested(run), false);
  assert.equal(control.canStartNextOperation(run), true);
  assert.equal(control.beginOperation(run, 'model_request'), true);
  assert.equal(run.activeOperation, 'model_request');
});

test('stop request is immediate and prevents the next operation', () => {
  const run = control.createRunControl();
  control.beginOperation(run, 'model_request');
  const result = control.requestStop(run, 1234);
  assert.deepEqual(result, {accepted:true,changed:true,activeOperation:'model_request'});
  assert.equal(run.stopRequestedAt, 1234);
  assert.equal(control.canStartNextOperation(run), false);
  assert.equal(control.beginOperation(run, 'tool_call'), false);
});

test('requesting stop is idempotent', () => {
  const run = control.createRunControl();
  assert.equal(control.requestStop(run, 100).changed, true);
  const second = control.requestStop(run, 200);
  assert.deepEqual(second, {accepted:true,changed:false,activeOperation:null});
  assert.equal(run.stopRequestedAt, 100);
});

test('an already-started operation may finish but is not restarted', () => {
  const run = control.createRunControl();
  assert.equal(control.beginOperation(run, 'tool_call'), true);
  control.requestStop(run, 10);
  assert.equal(run.activeOperation, 'tool_call');
  assert.equal(control.finishOperation(run, 'tool_call'), true);
  assert.equal(run.activeOperation, null);
  assert.equal(control.beginOperation(run, 'tool_call'), false);
});

test('rejects unsupported operation labels and mismatched completion', () => {
  const run = control.createRunControl();
  assert.equal(control.beginOperation(run, 'arbitrary text'), false);
  assert.equal(control.beginOperation(run, 'tool_call'), true);
  assert.equal(control.finishOperation(run, 'model_request'), false);
  assert.equal(run.activeOperation, 'tool_call');
});

test('handles invalid state inputs without throwing', () => {
  assert.equal(control.canStartNextOperation(null), false);
  assert.equal(control.isStopRequested(null), false);
  assert.equal(control.beginOperation(null, 'tool_call'), false);
  assert.equal(control.requestStop(null).accepted, false);
});
