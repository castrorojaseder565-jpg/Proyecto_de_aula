(() => {
  const root = document.querySelector('[data-bovibot]');
  if (!root) return;
  const panel = root.querySelector('[data-panel]');
  const launcher = root.querySelector('[data-open]');
  const messages = root.querySelector('[data-messages]');
  const form = root.querySelector('[data-form]');
  const input = root.querySelector('[data-input]');
  const state = root.querySelector('[data-state]');
  const suggestions = root.querySelector('[data-suggestions]');
  const historyKey = 'bovicontrol.bovibot.messages';
  const positionKey = 'bovicontrol.bovibot.position';
  const csrf = root.querySelector('[data-csrf]')?.value || '';
  let busy = false;
  let dragging = false;
  let moved = false;
  let pointerStart;
  let localHistory = [];

  const save = () => sessionStorage.setItem(historyKey, JSON.stringify(localHistory.slice(-12)));
  const addMessage = (role, text, persist = true) => {
    const bubble = document.createElement('article');
    bubble.className = `bovibot-message ${role}`;
    bubble.textContent = text;
    messages.append(bubble);
    messages.scrollTop = messages.scrollHeight;
    if (persist && (role === 'user' || role === 'assistant')) {
      localHistory.push({ role, content: text });
      localHistory = localHistory.slice(-12);
      save();
    }
    return bubble;
  };
  const normalizeAssistantText = text => text.replace(/\banimales?\b/gi, term => {
    const plural = term.toLowerCase().endsWith('s');
    const capitalized = term[0] === term[0].toUpperCase();
    const replacement = plural ? 'ganados' : 'ganado';
    return capitalized ? replacement[0].toUpperCase() + replacement.slice(1) : replacement;
  });
  const restore = () => {
    try {
      localHistory = JSON.parse(sessionStorage.getItem(historyKey) || '[]');
      if (!Array.isArray(localHistory)) localHistory = [];
    } catch { localHistory = []; }
    if (!localHistory.length) addMessage('assistant', 'Hola. Elige una pregunta sugerida o escribe lo que necesitas consultar.', false);
    else {
      localHistory = localHistory.map(item => item.role === 'user' ? item : { ...item, content: normalizeAssistantText(String(item.content || '')) });
      save();
      localHistory.forEach(item => addMessage(item.role === 'user' ? 'user' : 'assistant', String(item.content || ''), false));
    }
  };
  const open = () => { panel.hidden = false; launcher.setAttribute('aria-expanded', 'true'); input.focus(); };
  const close = () => { panel.hidden = true; panel.classList.remove('expanded'); launcher.setAttribute('aria-expanded', 'false'); };
  const setBusy = value => { busy = value; form.querySelector('[data-send]').disabled = value; state.textContent = value ? 'El asistente está pensando…' : 'Enter para enviar'; };

  launcher.addEventListener('click', () => { if (moved) { moved = false; return; } panel.hidden ? open() : close(); });
  root.querySelector('[data-close]').addEventListener('click', close);
  root.querySelector('[data-min]').addEventListener('click', close);
  root.querySelector('[data-full]').addEventListener('click', () => panel.classList.toggle('expanded'));
  suggestions.addEventListener('click', event => {
    const button = event.target.closest('button');
    if (!button) return;
    input.value = button.textContent.trim();
    form.requestSubmit();
  });
  input.addEventListener('keydown', event => {
    if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); form.requestSubmit(); }
  });
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const message = input.value.trim();
    if (!message || busy) return;
    input.value = '';
    addMessage('user', message);
    const pending = addMessage('pending', 'El asistente está preparando la respuesta…', false);
    setBusy(true);
    try {
      const response = await fetch('/api/chat', { method: 'POST', credentials: 'same-origin', headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrf }, body: JSON.stringify({ mensaje: message }) });
      const data = await response.json().catch(() => ({}));
      pending.remove();
      if (!response.ok) throw new Error(data.error || data.mensaje || data.message || (response.status === 403 ? 'No tienes permiso para consultar esa información.' : 'No se pudo completar la consulta. Intenta de nuevo.'));
      addMessage('assistant', data.respuesta || data.mensaje || 'No recibí una respuesta del asistente.');
    } catch (error) { pending.remove(); addMessage('error', error.message || 'Error de conexión.'); }
    finally { setBusy(false); input.focus(); }
  });
  root.querySelector('[data-clear]').addEventListener('click', async () => {
    try {
      const response = await fetch('/api/chat/history', { method: 'DELETE', credentials: 'same-origin', headers: { 'X-CSRF-TOKEN': csrf } });
      if (!response.ok) throw new Error();
      localHistory = [];
      sessionStorage.removeItem(historyKey);
      messages.replaceChildren();
      addMessage('assistant', 'Conversación limpiada. ¿En qué puedo ayudarte?', false);
    } catch { state.textContent = 'No se pudo limpiar; inténtalo de nuevo.'; }
  });

  const restorePosition = () => {
    try {
      const pos = JSON.parse(sessionStorage.getItem(positionKey) || 'null');
      if (!pos) return;
      root.style.left = `${Math.max(0, Math.min(innerWidth - 64, pos.x))}px`;
      root.style.top = `${Math.max(0, Math.min(innerHeight - 64, pos.y))}px`;
      root.style.right = 'auto'; root.style.bottom = 'auto';
    } catch { /* posición predeterminada */ }
  };
  launcher.addEventListener('pointerdown', event => {
    if (event.button !== 0) return;
    dragging = true; moved = false; root.classList.add('is-dragging');
    pointerStart = { x: event.clientX, y: event.clientY };
    launcher.setPointerCapture(event.pointerId);
  });
  launcher.addEventListener('pointermove', event => {
    if (!dragging) return;
    const dx = event.clientX - pointerStart.x, dy = event.clientY - pointerStart.y;
    if (Math.abs(dx) + Math.abs(dy) < 8) return;
    moved = true;
    const bounds = root.getBoundingClientRect();
    const x = Math.max(0, Math.min(innerWidth - bounds.width, bounds.left + dx));
    const y = Math.max(0, Math.min(innerHeight - bounds.height, bounds.top + dy));
    root.style.left = `${x}px`; root.style.top = `${y}px`; root.style.right = 'auto'; root.style.bottom = 'auto';
    pointerStart = { x: event.clientX, y: event.clientY };
  });
  launcher.addEventListener('pointerup', () => {
    if (!dragging) return;
    dragging = false; root.classList.remove('is-dragging');
    if (moved) {
      const bounds = root.getBoundingClientRect();
      const x = bounds.left < innerWidth / 2 ? 12 : innerWidth - bounds.width - 12;
      root.style.left = `${Math.max(0, x)}px`; root.style.top = `${Math.max(0, Math.min(innerHeight - bounds.height, bounds.top))}px`;
      sessionStorage.setItem(positionKey, JSON.stringify({ x: Math.max(0, Math.min(innerWidth - 64, x)), y: bounds.top }));
    }
  });
  launcher.addEventListener('pointercancel', () => { dragging = false; root.classList.remove('is-dragging'); });
  addEventListener('resize', () => {
    const bounds = root.getBoundingClientRect();
    if (bounds.right > innerWidth || bounds.bottom > innerHeight) {
      root.style.left = `${Math.max(0, innerWidth - bounds.width - 12)}px`;
      root.style.top = `${Math.max(0, innerHeight - bounds.height - 12)}px`;
    }
  });
  restore(); restorePosition();
  if (new URLSearchParams(location.search).get('bovibot') === 'open') { open(); history.replaceState({}, '', location.pathname); }
})();
