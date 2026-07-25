/**
 * terminal.js — Interactive terminal panel for Odysseus.
 *
 * Uses /api/shell/stream (SSE) for output and /api/shell/exec for execution.
 * No external deps — plain DOM + fetch. Supports command history, Ctrl+C,
 * and clear. Admin-only (server rejects non-admin callers).
 */

import { topToolWindowZ } from './toolWindowZOrder.js';
import { makeWindowDraggable } from './windowDrag.js';
import { applyEdgeDock, clearDockSide } from './modalSnap.js';
import { bindMenuDismiss, dismissOrRemove } from './escMenuStack.js';
import * as Modals from './modalManager.js';

const API_BASE = window.location.origin;

let _open = false;
let _history = [];      // command history
let _histIdx = -1;      // history navigation index
let _currentInput = ''; // saved input before history nav
let _abortCtrl = null;  // AbortController for running stream
let _running = false;   // true while a command is streaming

// ── public ────────────────────────────────────────────────────────────────

export function isOpen() { return _open; }

export function openPanel() {
  if (_open) {
    const pane = document.getElementById('terminal-pane');
    if (pane) { pane.style.display = ''; _focusInput(); }
    return;
  }
  _open = true;
  _buildPane();
}

export function closePanel() {
  if (!_open) return;
  _open = false;
  _abortRunning();
  document.getElementById('terminal-pane')?.remove();
  document.getElementById('terminal-pane-backdrop')?.remove();
  document.getElementById('tool-terminal-btn')?.classList.remove('active');
  try { Modals.unregister('terminal-panel'); } catch {}
  try { window._restoreSidebarIfRouteCollapsed?.(); } catch {}
}

export function togglePanel() {
  _open ? closePanel() : openPanel();
}

// ── private ───────────────────────────────────────────────────────────────

function _abortRunning() {
  if (_abortCtrl) { try { _abortCtrl.abort(); } catch {} _abortCtrl = null; }
  _running = false;
}

function _buildPane() {
  // Backdrop (click-outside closes)
  const backdrop = document.createElement('div');
  backdrop.id = 'terminal-pane-backdrop';
  backdrop.className = 'notes-pane-backdrop'; // reuse notes overlay style
  backdrop.addEventListener('click', closePanel);
  document.body.appendChild(backdrop);

  // Pane
  const pane = document.createElement('div');
  pane.id = 'terminal-pane';
  pane.className = 'terminal-pane notes-pane'; // borrow base positioning CSS
  pane.setAttribute('role', 'dialog');
  pane.setAttribute('aria-label', 'Terminal');
  pane.innerHTML = `
    <div class="terminal-header notes-pane-header">
      <h4 class="notes-pane-title">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
          style="vertical-align:-2.5px;margin-right:6px">
          <polyline points="4 17 10 11 4 5"/><line x1="12" y1="19" x2="20" y2="19"/>
        </svg>Terminal
      </h4>
      <div style="display:flex;gap:6px;align-items:center;">
        <button id="terminal-clear-btn" class="notes-action-btn" title="Clear output" aria-label="Clear terminal">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="3 6 5 6 21 6"/>
            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
            <path d="M10 11v6"/><path d="M14 11v6"/>
            <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
          </svg>
        </button>
        <button id="terminal-close-btn" class="modal-close-btn notes-pane-close" aria-label="Close terminal">✖</button>
      </div>
    </div>
    <div id="terminal-output" class="terminal-output" aria-live="polite" aria-label="Terminal output"></div>
    <div class="terminal-input-row">
      <span class="terminal-prompt">$&nbsp;</span>
      <input id="terminal-input" class="terminal-input" type="text"
        placeholder="Enter command…" autocomplete="off" autocorrect="off"
        autocapitalize="off" spellcheck="false" aria-label="Terminal input"/>
      <button id="terminal-run-btn" class="terminal-run-btn" title="Run (Enter)" aria-label="Run command">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="5 3 19 12 5 21 5 3"/>
        </svg>
      </button>
    </div>`;

  // Positioning — right-side slide-in panel (same as notes)
  pane.style.cssText = 'right:0;top:0;height:100%;width:min(520px,95vw);';
  document.body.appendChild(pane);

  // Z-order
  const z = topToolWindowZ();
  pane.style.zIndex = z;
  backdrop.style.zIndex = z - 1;

  // Sidebar active state
  document.getElementById('tool-terminal-btn')?.classList.add('active');

  // Draggable header
  makeWindowDraggable(pane, pane.querySelector('.terminal-header'));
  applyEdgeDock(pane, 'right');

  // Register with modal manager for Esc handling
  try {
    Modals.register('terminal-panel', { close: closePanel, el: pane });
  } catch {}
  bindMenuDismiss('terminal-panel', closePanel);

  // Wire buttons
  pane.querySelector('#terminal-close-btn').addEventListener('click', closePanel);
  pane.querySelector('#terminal-clear-btn').addEventListener('click', _clearOutput);
  pane.querySelector('#terminal-run-btn').addEventListener('click', _submitCommand);

  // Wire input
  const input = pane.querySelector('#terminal-input');
  input.addEventListener('keydown', _onKeyDown);

  // Welcome line
  _appendLine('system', 'Odysseus Terminal — type a command and press Enter');
  _appendLine('system', 'Host filesystem available at /host-home');
  _focusInput();

  // Stop scroll backdrop-click from triggering close
  pane.addEventListener('click', e => e.stopPropagation());
}

function _focusInput() {
  document.getElementById('terminal-input')?.focus();
}

function _clearOutput() {
  const out = document.getElementById('terminal-output');
  if (out) out.innerHTML = '';
  _focusInput();
}

function _onKeyDown(e) {
  if (e.key === 'Enter') {
    e.preventDefault();
    _submitCommand();
    return;
  }
  if (e.key === 'c' && e.ctrlKey) {
    e.preventDefault();
    if (_running) {
      _abortRunning();
      _appendLine('stderr', '^C');
      _setPromptReady();
    }
    return;
  }
  // History navigation
  if (e.key === 'ArrowUp') {
    e.preventDefault();
    if (_history.length === 0) return;
    if (_histIdx === -1) { _currentInput = e.target.value; _histIdx = _history.length - 1; }
    else if (_histIdx > 0) _histIdx--;
    e.target.value = _history[_histIdx];
    // Move cursor to end
    setTimeout(() => e.target.setSelectionRange(e.target.value.length, e.target.value.length), 0);
    return;
  }
  if (e.key === 'ArrowDown') {
    e.preventDefault();
    if (_histIdx === -1) return;
    if (_histIdx < _history.length - 1) { _histIdx++; e.target.value = _history[_histIdx]; }
    else { _histIdx = -1; e.target.value = _currentInput; }
    return;
  }
}

async function _submitCommand() {
  if (_running) return;
  const input = document.getElementById('terminal-input');
  if (!input) return;
  const cmd = input.value.trim();
  if (!cmd) return;

  // Add to history (deduplicate consecutive identical)
  if (_history.length === 0 || _history[_history.length - 1] !== cmd) {
    _history.push(cmd);
    if (_history.length > 200) _history.shift();
  }
  _histIdx = -1;
  _currentInput = '';
  input.value = '';

  // Echo the command
  _appendLine('cmd', '$ ' + cmd);
  _setPromptBusy();

  // Handle built-in commands
  if (cmd === 'clear') { _clearOutput(); _setPromptReady(); return; }
  if (cmd === 'exit' || cmd === 'quit') { closePanel(); return; }

  _running = true;
  _abortCtrl = new AbortController();

  try {
    const res = await fetch(`${API_BASE}/api/shell/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: cmd, timeout: 0, use_pty: false }),
      signal: _abortCtrl.signal,
      credentials: 'include',
    });

    if (!res.ok) {
      const txt = await res.text().catch(() => `HTTP ${res.status}`);
      _appendLine('stderr', `Error: ${res.status} — ${txt.slice(0, 200)}`);
      _setPromptReady();
      _running = false;
      return;
    }

    // Read SSE stream
    const reader = res.body.getReader();
    const dec = new TextDecoder();
    let buf = '';

    while (true) {
      let value, done;
      try {
        ({ value, done } = await reader.read());
      } catch (readErr) {
        if (readErr.name !== 'AbortError') _appendLine('stderr', `Stream error: ${readErr.message}`);
        break;
      }
      if (done) break;
      buf += dec.decode(value, { stream: true });
      const lines = buf.split('\n');
      buf = lines.pop(); // keep partial line
      for (const line of lines) {
        if (!line.startsWith('data: ')) continue;
        let payload;
        try { payload = JSON.parse(line.slice(6)); } catch { continue; }
        if (payload.stream === 'stdout' || payload.stream === 'stderr') {
          _appendLine(payload.stream, payload.data ?? '');
        } else if ('exit_code' in payload) {
          const code = payload.exit_code;
          if (code !== 0 && code !== null) {
            _appendLine('system', `[exit ${code}]`);
          }
          break;
        } else if (payload.error) {
          _appendLine('stderr', payload.error);
        }
      }
    }
  } catch (err) {
    if (err.name !== 'AbortError') _appendLine('stderr', `Error: ${err.message}`);
  }

  _running = false;
  _abortCtrl = null;
  _setPromptReady();
  _focusInput();
}

function _appendLine(type, text) {
  const out = document.getElementById('terminal-output');
  if (!out) return;
  const line = document.createElement('div');
  line.className = `terminal-line terminal-line-${type}`;
  // Preserve whitespace and wrap long lines
  line.textContent = text;
  out.appendChild(line);
  // Auto-scroll to bottom
  out.scrollTop = out.scrollHeight;
}

function _setPromptBusy() {
  const btn = document.getElementById('terminal-run-btn');
  const input = document.getElementById('terminal-input');
  if (btn) { btn.disabled = true; btn.style.opacity = '0.4'; }
  if (input) input.placeholder = 'Running… (Ctrl+C to cancel)';
}

function _setPromptReady() {
  const btn = document.getElementById('terminal-run-btn');
  const input = document.getElementById('terminal-input');
  if (btn) { btn.disabled = false; btn.style.opacity = ''; }
  if (input) input.placeholder = 'Enter command…';
}
