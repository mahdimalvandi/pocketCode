'use strict';

(function attachTaskMemory(root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.PocketTaskMemory = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function createTaskMemory() {
  const STEP_STATUSES = new Set(['pending', 'running', 'completed', 'failed', 'requires_approval']);
  const TASK_STATUSES = new Set(['active', 'completed', 'paused', 'failed']);

  function clean(value, maxLength) {
    return String(value == null ? '' : value).replace(/[\u0000-\u001f\u007f]/g, ' ').replace(/\s+/g, ' ').trim().slice(0, maxLength);
  }

  function createPlan(goal, titles, id, now = Date.now()) {
    const cleanGoal = clean(goal, 240);
    if (!cleanGoal) throw new Error('Task goal is required.');
    if (!Array.isArray(titles) || titles.length < 2 || titles.length > 10) throw new Error('A task plan must contain between 2 and 10 steps.');
    const steps = titles.map((title, index) => {
      const cleanTitle = clean(title, 110);
      if (!cleanTitle) throw new Error('Task step ' + (index + 1) + ' is empty.');
      return {id: 'step-' + (index + 1), title: cleanTitle, status: 'pending', note: '', updatedAt: now};
    });
    return {id: clean(id || ('task-' + Number(now).toString(36)), 80), goal: cleanGoal, status: 'active',
      summary: '', currentStepId: null, createdAt: now, updatedAt: now, steps};
  }

  function deriveStatus(plan) {
    return plan.steps.length && plan.steps.every(step => step.status === 'completed') ? 'completed' : 'active';
  }

  function updateStep(plan, stepId, status, note = '', now = Date.now()) {
    if (!plan || !Array.isArray(plan.steps)) throw new Error('No active task plan.');
    if (!STEP_STATUSES.has(status)) throw new Error('Unsupported task step status.');
    const step = plan.steps.find(item => item.id === String(stepId));
    if (!step) throw new Error('Task step was not found.');
    if (step.status === 'completed' && status !== 'completed') throw new Error('A completed task step cannot be reopened; create a new plan for additional work.');
    step.status = status;
    if (note !== undefined && note !== null && String(note).trim()) step.note = clean(note, 160);
    step.updatedAt = now;
    plan.currentStepId = status === 'completed' ? null : step.id;
    plan.status = deriveStatus(plan);
    plan.updatedAt = now;
    return plan;
  }

  function startNextStep(plan, preferredId, now = Date.now()) {
    if (!plan || plan.status !== 'active') return null;
    let index = preferredId ? plan.steps.findIndex(item => item.id === String(preferredId)) : -1;
    if (preferredId && index < 0) throw new Error('Task step was not found.');
    if (index >= 0 && plan.steps.slice(0, index).some(item => item.status !== 'completed')) {
      throw new Error('Task steps must execute in order; finish earlier steps before starting this one.');
    }
    if (index < 0) index = plan.steps.findIndex(item => item.status !== 'completed');
    if (index < 0) return null;
    let step = plan.steps[index];
    if (!preferredId && (step.status === 'failed' || step.status === 'requires_approval')) {
      throw new Error('The next task step is blocked; inspect it or obtain the required approval before continuing.');
    }
    if (step.status === 'completed' || step.status === 'requires_approval') throw new Error('This task step requires a different state transition before it can run.');
    step.status = 'running';
    step.updatedAt = now;
    plan.currentStepId = step.id;
    plan.updatedAt = now;
    return step;
  }

  function resumePlan(plan, now = Date.now()) {
    if (!plan || plan.status !== 'paused') throw new Error('There is no paused task to resume.');
    plan.status = 'active';
    plan.summary = 'Resumed by the user; inspect the current state before repeating the last operation.';
    plan.updatedAt = now;
    return plan;
  }

  function finishPlan(plan, status, summary = '', now = Date.now()) {
    if (!plan || !Array.isArray(plan.steps)) throw new Error('No active task plan.');
    if (!TASK_STATUSES.has(status) || status === 'active') throw new Error('Task must be completed, paused or failed.');
    if (status === 'completed' && !plan.steps.every(step => step.status === 'completed')) throw new Error('A task cannot be completed while any step is unfinished.');
    plan.status = status;
    plan.summary = clean(summary, 220);
    plan.updatedAt = now;
    return plan;
  }

  function restore(input) {
    if (!input || typeof input !== 'object' || !Array.isArray(input.steps)) return null;
    try {
      const goal = clean(input.goal, 240);
      if (!goal || input.steps.length < 2 || input.steps.length > 10) return null;
      const steps = input.steps.map((step, index) => {
        if (!step || typeof step !== 'object') throw new Error('Invalid step');
        const title = clean(step.title, 110);
        const status = STEP_STATUSES.has(step.status) ? step.status : 'pending';
        if (!title) throw new Error('Empty step');
        return {id: 'step-' + (index + 1), title, status, note: clean(step.note, 160),
          updatedAt: Number.isFinite(step.updatedAt) ? step.updatedAt : 0};
      });
      return {id: clean(input.id, 80) || 'task-restored', goal,
        status: TASK_STATUSES.has(input.status) ? input.status : 'active',
        summary: clean(input.summary, 220),
        currentStepId: steps.some(step => step.id === input.currentStepId && step.status !== 'completed') ? input.currentStepId : null,
        createdAt: Number.isFinite(input.createdAt) ? input.createdAt : 0,
        updatedAt: Number.isFinite(input.updatedAt) ? input.updatedAt : 0, steps};
    } catch (_) { return null; }
  }

  function snapshot(plan) {
    const restored = restore(plan);
    return restored ? JSON.parse(JSON.stringify(restored)) : null;
  }

  function summarize(plan) {
    if (!plan) return null;
    const counts = {completed: 0, total: plan.steps.length, running: 0, pending: 0, failed: 0, requiresApproval: 0};
    for (const step of plan.steps) {
      if (step.status === 'completed') counts.completed++;
      else if (step.status === 'running') counts.running++;
      else if (step.status === 'failed') counts.failed++;
      else if (step.status === 'requires_approval') counts.requiresApproval++;
      else counts.pending++;
    }
    return {id: plan.id, goal: plan.goal, status: plan.status, summary: plan.summary, currentStepId: plan.currentStepId,
      counts, steps: plan.steps.map(step => ({id: step.id, title: step.title, status: step.status, note: step.note}))};
  }

  return Object.freeze({createPlan, updateStep, startNextStep, resumePlan, finishPlan, restore, snapshot, summarize});
});
