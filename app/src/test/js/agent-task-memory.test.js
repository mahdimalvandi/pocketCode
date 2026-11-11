'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const task = require('../../main/assets/agent-task-memory.js');

test('creates a bounded plan with stable ordered step IDs', () => {
  const plan = task.createPlan('Find a document', ['Open files', 'Search folder', 'Read matching file'], 't1', 10);
  assert.equal(plan.id, 't1');
  assert.equal(plan.status, 'active');
  assert.deepEqual(plan.steps.map(step => step.id), ['step-1', 'step-2', 'step-3']);
  assert.ok(plan.steps.every(step => step.status === 'pending'));
});

test('rejects missing goals and plans outside the 2–10 step range', () => {
  assert.throws(() => task.createPlan('', ['One', 'Two'], 't1'), /goal/);
  assert.throws(() => task.createPlan('Goal', ['Only one'], 't1'), /between 2 and 10/);
  assert.throws(() => task.createPlan('Goal', Array(11).fill('Step'), 't1'), /between 2 and 10/);
});

test('cleans control characters and bounds goal, title and notes', () => {
  const plan = task.createPlan('  Goal\nwith\tcontrols  ', ['  First\nstep  ', 'Second'], 't1', 1);
  assert.equal(plan.goal, 'Goal with controls');
  assert.equal(plan.steps[0].title, 'First step');
  task.updateStep(plan, 'step-1', 'running', 'x'.repeat(200), 2);
  assert.equal(plan.steps[0].note.length, 160);
});

test('starts requested or next pending step while keeping the same running step', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  assert.equal(task.startNextStep(plan, null, 2).id, 'step-1');
  assert.equal(task.startNextStep(plan, null, 3).id, 'step-1');
  task.updateStep(plan, 'step-1', 'completed', '', 4);
  assert.equal(task.startNextStep(plan, null, 5).id, 'step-2');
});

test('requires an explicit transition before running a requires-approval step', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  task.updateStep(plan, 'step-1', 'requires_approval', 'Need permission', 2);
  assert.throws(() => task.startNextStep(plan, 'step-1', 3), /different state transition/);
  task.updateStep(plan, 'step-1', 'running', 'Permission granted', 4);
  assert.equal(task.startNextStep(plan, 'step-1', 5).status, 'running');
});

test('updates step statuses and derives task completion only when all steps complete', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  task.startNextStep(plan, 'step-1', 2);
  task.updateStep(plan, 'step-1', 'completed', 'Verified', 3);
  assert.equal(plan.status, 'active');
  task.startNextStep(plan, 'step-2', 4);
  task.updateStep(plan, 'step-2', 'completed', '', 5);
  assert.equal(plan.status, 'completed');
});

test('does not allow completed steps to be reopened', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  task.updateStep(plan, 'step-1', 'completed', '', 2);
  assert.throws(() => task.updateStep(plan, 'step-1', 'running', '', 3), /cannot be reopened/);
});

test('refuses to finish a plan as completed while steps remain unfinished', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  assert.throws(() => task.finishPlan(plan, 'completed', 'Done', 2), /unfinished/);
  assert.equal(task.finishPlan(plan, 'paused', 'Need user input', 3).status, 'paused');
});

test('paused plans must be explicitly resumed', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  task.startNextStep(plan, 'step-1', 2);
  task.finishPlan(plan, 'paused', 'Interrupted', 3);
  assert.equal(task.startNextStep(plan, null, 4), null);
  assert.equal(task.resumePlan(plan, 5).status, 'active');
  assert.equal(task.startNextStep(plan, null, 6).id, 'step-1');
});

test('restores only bounded sanitized task data', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  plan.steps[0].status = 'running';
  plan.steps[0].note = '<script>do not trust stored content</script>';
  const restored = task.restore(plan);
  assert.equal(restored.steps.length, 2);
  assert.equal(restored.steps[0].id, 'step-1');
  assert.equal(restored.steps[0].title, 'One');
  assert.equal(restored.steps[0].status, 'running');
  assert.equal(restored.steps[0].note.length <= 160, true);
  assert.equal(task.restore({goal:'bad',steps:[]}), null);
});

test('summary is compact and contains no arbitrary tool payloads', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  const summary = task.summarize(plan);
  assert.deepEqual(Object.keys(summary), ['id','goal','status','summary','currentStepId','counts','steps']);
  assert.equal(JSON.stringify(summary).includes('rawScreenshot'), false);
});

test('resuming requires the paused plan and preserves the unfinished running step', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  task.startNextStep(plan, 'step-1', 2);
  task.finishPlan(plan, 'paused', 'Interrupted', 3);
  task.resumePlan(plan, 4);
  assert.equal(plan.status, 'active');
  assert.equal(task.startNextStep(plan, null, 5).id, 'step-1');
});

test('refuses to resume a plan that is not paused', () => {
  const plan = task.createPlan('Goal', ['One', 'Two'], 't1', 1);
  assert.throws(() => task.resumePlan(plan, 2), /paused task/);
});

test('does not silently skip a failed or approval-blocked earlier step', () => {
  const plan = task.createPlan('Goal', ['First', 'Second'], 't1', 1);
  task.updateStep(plan, 'step-1', 'requires_approval', 'Waiting for permission', 2);
  assert.throws(() => task.startNextStep(plan, null, 3), /blocked/);
  task.updateStep(plan, 'step-1', 'running', 'Permission granted', 4);
  assert.equal(task.startNextStep(plan, null, 5).id, 'step-1');
  task.updateStep(plan, 'step-1', 'failed', 'Needs retry', 6);
  assert.throws(() => task.startNextStep(plan, null, 7), /blocked/);
  task.startNextStep(plan, 'step-1', 8);
  assert.equal(plan.steps[0].status, 'running');
});

test('does not allow a caller to skip ahead to a later step ID', () => {
  const plan = task.createPlan('Goal', ['First', 'Second', 'Third'], 't1', 1);
  assert.throws(() => task.startNextStep(plan, 'step-3', 2), /execute in order/);
  assert.equal(task.startNextStep(plan, 'step-1', 3).id, 'step-1');
  task.updateStep(plan, 'step-1', 'completed', '', 4);
  assert.throws(() => task.startNextStep(plan, 'step-3', 5), /execute in order/);
  task.startNextStep(plan, 'step-2', 6);
  task.updateStep(plan, 'step-2', 'completed', '', 7);
  assert.equal(task.startNextStep(plan, 'step-3', 8).id, 'step-3');
});
