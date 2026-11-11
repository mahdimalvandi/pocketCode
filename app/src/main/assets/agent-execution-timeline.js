'use strict';

(function attachExecutionTimeline(root, factory) {
  const api = factory();
  if (typeof module === 'object' && module.exports) module.exports = api;
  if (root) root.PocketExecutionTimeline = api;
})(typeof globalThis !== 'undefined' ? globalThis : this, function createExecutionTimeline() {
  const MAX_EVENTS = 100;
  const TOOL_LABELS = Object.freeze({
    list_files: 'فهرست فایل‌های فضای کاری',
    read_file: 'خواندن فایل فضای کاری',
    write_file: 'نوشتن فایل فضای کاری',
    list_phone_files: 'فهرست پوشهٔ انتخاب‌شده',
    phone_storage_roots: 'پیداکردن حافظه‌های گوشی',
    list_phone_storage: 'فهرست حافظهٔ گوشی',
    read_phone_storage: 'خواندن فایل از حافظهٔ گوشی',
    read_phone_file: 'خواندن فایل از پوشهٔ انتخاب‌شده',
    write_phone_file: 'نوشتن در پوشهٔ انتخاب‌شده',
    list_apps: 'فهرست اپ‌های گوشی',
    open_app: 'بازکردن اپ',
    read_screen: 'خواندن صفحه',
    capture_screen: 'گرفتن تصویر صفحه',
    tap_screen: 'لمس صفحه',
    click_element: 'کلیک روی عنصر',
    type_text: 'تایپ در فیلد',
    scroll_element: 'اسکرول صفحه',
    phone_back: 'بازگشت در اپ',
    set_task_plan: 'ساخت برنامهٔ کار',
    update_task_step: 'به‌روزرسانی مرحله',
    resume_task: 'ادامهٔ برنامه',
    clear_task_plan: 'پاک‌کردن برنامه',
    finish_task: 'بستن برنامهٔ کار'
  });
  const VALID_STATUSES = new Set([
    'running', 'awaiting_approval', 'awaiting_verification',
    'completed', 'observed', 'failed', 'denied', 'not_executed', 'unverified'
  ]);
  const TERMINAL_STATUSES = new Set([
    'completed', 'observed', 'failed', 'denied', 'not_executed', 'unverified'
  ]);
  const STATUS_LABELS = Object.freeze({
    running: 'در حال اجرا',
    awaiting_approval: 'در انتظار تأیید',
    awaiting_verification: 'در انتظار بررسی صفحه',
    completed: 'پاسخ ابزار دریافت شد',
    observed: 'مشاهدهٔ تازه دریافت شد',
    failed: 'ناموفق',
    denied: 'ردشده',
    not_executed: 'اجرا نشد',
    unverified: 'نتیجه تأیید نشد'
  });
  const NOTE_LABELS = Object.freeze({
    approval_requested: 'برای ادامه، تأیید لازم است',
    approval_denied: 'تأیید داده نشد',
    fresh_observation: 'صفحه دوباره خوانده شد؛ تطبیق نتیجه با هدف بر عهدهٔ Agent است',
    action_pending: 'پذیرش درخواست از سوی اندروید، موفقیت نهایی را ثابت نمی‌کند',
    execution_failed: 'اجرای ابزار با خطا متوقف شد',
    not_executed: 'این فراخوانی اجرا نشد',
    stopped: 'اجرای خودکار متوقف شد',
    no_result: 'نتیجهٔ ابزار در دسترس نبود',
    unverified: 'مشاهدهٔ لازم برای تأیید نتیجه دریافت نشد'
  });
  let sequence = 0;

  function safeToolName(value) {
    const name = String(value || '').trim().toLowerCase();
    return /^[a-z0-9_]{1,48}$/.test(name) ? name : 'unknown_tool';
  }

  function restore(events) {
    if (!Array.isArray(events)) return [];
    return events.slice(-MAX_EVENTS).filter(event => event && typeof event === 'object').map((event, index) => {
      const toolName = safeToolName(event.toolName);
      const rawStartedAt = Number(event.startedAt);
      const startedAt = Number.isFinite(rawStartedAt) ? Math.min(8640000000000000, Math.max(0, rawStartedAt)) : 0;
      const status = VALID_STATUSES.has(event.status) ? event.status : 'failed';
      const rawFinishedAt = Number(event.finishedAt);
      const finishedAt = TERMINAL_STATUSES.has(status) && Number.isFinite(rawFinishedAt)
        ? Math.max(startedAt, Math.min(8640000000000000, rawFinishedAt)) : null;
      const actionId = typeof event.actionId === 'string' && /^[a-z0-9_-]{1,96}$/i.test(event.actionId)
        ? event.actionId : null;
      const noteCode = Object.prototype.hasOwnProperty.call(NOTE_LABELS, event.noteCode) ? event.noteCode : '';
      const id = typeof event.id === 'string' && /^[a-z0-9_-]{1,96}$/i.test(event.id)
        ? event.id : 'restored-' + index.toString(36);
      return {
        id, toolName, label: TOOL_LABELS[toolName] || 'اجرای ابزار', status,
        startedAt, finishedAt,
        durationMs: finishedAt === null ? null : Math.max(0, Number(finishedAt) - startedAt),
        actionId, noteCode
      };
    });
  }

  function createEvent(events, toolName, now = Date.now()) {
    const timestamp = Number.isFinite(Number(now)) ? Number(now) : Date.now();
    const name = safeToolName(toolName);
    const event = {
      id: 'exec-' + timestamp.toString(36) + '-' + (++sequence).toString(36),
      toolName: name,
      label: TOOL_LABELS[name] || 'اجرای ابزار',
      status: 'running',
      startedAt: timestamp,
      finishedAt: null,
      durationMs: null,
      actionId: null,
      noteCode: ''
    };
    return { events: restore(events).concat(event).slice(-MAX_EVENTS), event };
  }

  function updateEvent(events, id, status, now = Date.now(), details = {}) {
    if (!VALID_STATUSES.has(status)) return restore(events);
    const timestamp = Number.isFinite(Number(now)) ? Number(now) : Date.now();
    return restore(events).map(event => {
      if (!event || event.id !== id) return event;
      const terminal = TERMINAL_STATUSES.has(status);
      const allowedNote = Object.prototype.hasOwnProperty.call(NOTE_LABELS, details.noteCode)
        ? details.noteCode : '';
      const actionId = typeof details.actionId === 'string' && /^[a-z0-9_-]{1,96}$/i.test(details.actionId)
        ? details.actionId : event.actionId;
      return {
        ...event,
        status,
        finishedAt: terminal ? timestamp : null,
        durationMs: terminal ? Math.max(0, timestamp - event.startedAt) : null,
        actionId,
        noteCode: allowedNote
      };
    });
  }

  function outcome(result) {
    if (!result || typeof result !== 'object') return { status: 'failed', noteCode: 'no_result' };
    const category = result.recovery && result.recovery.category;
    if (result.error && category === 'user_denied') return { status: 'denied', noteCode: 'approval_denied' };
    if (result.error === 'User stopped automation before this tool was executed.') {
      return { status: 'not_executed', noteCode: 'stopped' };
    }
    const verification = result.verificationRequired || result.result?.verificationRequired;
    if (verification && typeof verification.actionId === 'string') {
      return {
        status: 'awaiting_verification',
        noteCode: 'action_pending',
        actionId: verification.actionId
      };
    }
    if (result.error) return { status: 'failed', noteCode: 'execution_failed' };
    return { status: 'completed', noteCode: '' };
  }

  function statusLabel(status) {
    return STATUS_LABELS[status] || 'نامشخص';
  }

  function noteLabel(noteCode) {
    return NOTE_LABELS[noteCode] || '';
  }

  function formatDuration(value) {
    const ms = Math.max(0, Number(value) || 0);
    if (ms < 1000) return Math.round(ms) + ' میلی‌ثانیه';
    const seconds = ms / 1000;
    return (seconds < 10 ? seconds.toFixed(1) : Math.round(seconds).toString()) + ' ثانیه';
  }

  return Object.freeze({
    MAX_EVENTS, TOOL_LABELS, STATUS_LABELS,
    restore, createEvent, updateEvent, outcome, statusLabel, noteLabel, formatDuration
  });
});
