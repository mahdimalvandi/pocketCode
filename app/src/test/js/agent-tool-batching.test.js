'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const batching = require('../../main/assets/agent-tool-batching.js');
const call = name => ({id:'id-'+name,type:'function',function:{name,arguments:'{}'}});

test('batches independent previously-consented broad-storage reads', () => {
  const plan=batching.planToolCalls([call('phone_storage_roots'),call('read_phone_storage'),call('read_phone_storage')]);
  assert.equal(plan.mode,'batch');
  assert.deepEqual(plan.executeIndices,[0,1,2]);
  assert.deepEqual(plan.deferredIndices,[]);
});

test('keeps one tool on the ordinary path', () => {
  const plan=batching.planToolCalls([call('read_phone_storage')]);
  assert.equal(plan.mode,'single');
  assert.deepEqual(plan.executeIndices,[0]);
});

test('pending verification disables batching', () => {
  const plan=batching.planToolCalls([call('phone_storage_roots'),call('read_phone_storage')],{hasPendingVerification:true});
  assert.equal(plan.reason,'verification_checkpoint');
  assert.deepEqual(plan.executeIndices,[0]);
  assert.deepEqual(plan.deferredIndices,[1]);
});

test('active or paused task plans disable batching', () => {
  const plan=batching.planToolCalls([call('phone_storage_roots'),call('read_phone_storage')],{hasTaskPlan:true});
  assert.equal(plan.reason,'task_plan_active');
});

test('mixed read and UI/action calls fall back to first-only execution', () => {
  const plan=batching.planToolCalls([call('read_phone_storage'),call('click_element')]);
  assert.equal(plan.reason,'non_batchable_or_dependent_tools');
  assert.deepEqual(plan.executeIndices,[0]);
  assert.deepEqual(plan.deferredIndices,[1]);
});

test('approval-gated reads and all mutations are not batchable', () => {
  for(const name of ['write_file','write_phone_file','read_file','read_phone_file','list_files','list_apps','read_screen','tap_screen','click_element']){
    assert.equal(batching.isBatchableTool(name),false,name);
    assert.equal(batching.planToolCalls([call('read_phone_storage'),call(name)]).mode,'single',name);
  }
});

test('limits each batch to three and returns overflow indices', () => {
  const plan=batching.planToolCalls(Array.from({length:5},()=>call('read_phone_storage')));
  assert.equal(plan.mode,'batch');
  assert.deepEqual(plan.executeIndices,[0,1,2]);
  assert.deepEqual(plan.deferredIndices,[3,4]);
  assert.equal(plan.reason,'batch_size_limit');
});

test('stopped automation does not start a batch', () => {
  const plan=batching.planToolCalls([call('phone_storage_roots'),call('read_phone_storage')],{automationStopped:true});
  assert.equal(plan.reason,'automation_stopped');
  assert.equal(plan.mode,'single');
});

test('handles empty calls and both supported call shapes', () => {
  assert.equal(batching.planToolCalls([]).mode,'none');
  assert.equal(batching.toolName(call('read_phone_storage')),'read_phone_storage');
  assert.equal(batching.toolName({name:'phone_storage_roots'}),'phone_storage_roots');
});
