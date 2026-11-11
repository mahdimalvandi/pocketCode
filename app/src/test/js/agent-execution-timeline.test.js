'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const timeline = require('../../main/assets/agent-execution-timeline.js');

test('creates bounded metadata-only events without arguments or results', () => {
  const created = timeline.createEvent([], 'read_phone_storage', 1000);
  assert.equal(created.event.toolName, 'read_phone_storage');
  assert.equal(created.event.label, 'خواندن فایل از حافظهٔ گوشی');
  assert.equal(created.event.status, 'running');
  assert.equal(created.event.startedAt, 1000);
  assert.equal('args' in created.event, false);
  assert.equal('content' in created.event, false);
});

test('keeps only the newest 100 events', () => {
  let events = [];
  for (let i = 0; i < 105; i++) events = timeline.createEvent(events, 'read_screen', i).events;
  assert.equal(events.length, 100);
  assert.equal(events[0].startedAt, 5);
  assert.equal(events.at(-1).startedAt, 104);
});

test('restores only allowlisted metadata from persisted history', () => {
  const restored = timeline.restore([{
    id:'safe-1',toolName:'read_file',status:'completed',startedAt:10,finishedAt:20,
    noteCode:'fresh_observation',actionId:'act-1',label:'<img src=x>',args:{password:'secret'},content:'private'
  }]);
  assert.equal(restored.length,1);
  assert.equal(restored[0].label,'خواندن فایل فضای کاری');
  assert.equal(restored[0].durationMs,10);
  assert.equal('args' in restored[0],false);
  assert.equal('content' in restored[0],false);
  assert.equal(restored[0].noteCode,'fresh_observation');
});

test('records terminal status and duration without copying error text', () => {
  const created = timeline.createEvent([], 'write_file', 2000);
  const updated = timeline.updateEvent(created.events, created.event.id, 'failed', 2375, {noteCode:'execution_failed'});
  assert.equal(updated[0].status, 'failed');
  assert.equal(updated[0].durationMs, 375);
  assert.equal(updated[0].noteCode, 'execution_failed');
  assert.equal('error' in updated[0], false);
});

test('approval is represented as a pending state until the user decides', () => {
  const created = timeline.createEvent([], 'read_file', 10);
  const waiting = timeline.updateEvent(created.events, created.event.id, 'awaiting_approval', 20, {noteCode:'approval_requested'});
  assert.equal(waiting[0].finishedAt, null);
  assert.equal(waiting[0].durationMs, null);
  assert.equal(timeline.noteLabel(waiting[0].noteCode), 'برای ادامه، تأیید لازم است');
  const denied = timeline.updateEvent(waiting, created.event.id, 'denied', 30, {noteCode:'approval_denied'});
  assert.equal(denied[0].durationMs, 20);
});

test('classifies UI actions as awaiting verification, not completed', () => {
  const direct = timeline.outcome({result:{ok:true},verificationRequired:{actionId:'act-123'}});
  const wrapped = timeline.outcome({result:{ok:true,verificationRequired:{actionId:'act-456'}}});
  assert.equal(direct.status, 'awaiting_verification');
  assert.equal(direct.actionId, 'act-123');
  assert.equal(wrapped.status, 'awaiting_verification');
  assert.equal(wrapped.actionId, 'act-456');
  const ambiguous = timeline.outcome({error:'native operation returned an uncertain result',recovery:{category:'unknown'},verificationRequired:{actionId:'act-789'}});
  assert.equal(ambiguous.status, 'awaiting_verification');
  assert.equal(ambiguous.actionId, 'act-789');
});

test('classifies denials and failures without preserving raw error payloads', () => {
  assert.equal(timeline.outcome({error:'private path or text',recovery:{category:'user_denied'}}).status, 'denied');
  const failure = timeline.outcome({error:'sensitive detail',recovery:{category:'unknown'}});
  assert.equal(failure.status, 'failed');
  assert.equal('error' in failure, false);
});

test('marks stopped operations as not executed', () => {
  const result = timeline.outcome({error:'User stopped automation before this tool was executed.'});
  assert.equal(result.status, 'not_executed');
  assert.equal(result.noteCode, 'stopped');
});

test('rejects unsafe tool names, status and note codes', () => {
  const created = timeline.createEvent([], 'read_file;secret', 1);
  assert.equal(created.event.toolName, 'unknown_tool');
  const unchanged = timeline.updateEvent(created.events, created.event.id, 'private_status', 2);
  assert.equal(unchanged[0].status, 'running');
  const updated = timeline.updateEvent(unchanged, created.event.id, 'failed', 3, {noteCode:'raw_user_text'});
  assert.equal(updated[0].noteCode, '');
});

test('formats durations for compact Persian display', () => {
  assert.equal(timeline.formatDuration(450), '450 میلی‌ثانیه');
  assert.equal(timeline.formatDuration(2500), '2.5 ثانیه');
  assert.equal(timeline.statusLabel('awaiting_verification'), 'در انتظار بررسی صفحه');
});
