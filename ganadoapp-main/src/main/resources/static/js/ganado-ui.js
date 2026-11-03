document.addEventListener("DOMContentLoaded", () => {
    const revealItems = document.querySelectorAll("[data-reveal]");

    if ("IntersectionObserver" in window && revealItems.length) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.18 });

        revealItems.forEach((item) => observer.observe(item));
    } else {
        revealItems.forEach((item) => item.classList.add("is-visible"));
    }

    document.querySelectorAll("[data-confirm]").forEach((element) => {
        element.addEventListener("click", (event) => {
            const message = element.getAttribute("data-confirm") || "Confirma esta acción.";
            if (!window.confirm(message)) {
                event.preventDefault();
            }
        });
    });

    document.querySelectorAll("[data-toggle-password]").forEach((btn) => {
        btn.addEventListener("click", () => {
            const targetId = btn.getAttribute("data-toggle-password");
            const input = targetId ? document.getElementById(targetId) : null;
            if (!input) {
                return;
            }
            const showing = input.type === "text";
            input.type = showing ? "password" : "text";
            btn.innerHTML = showing ? '<i class="fas fa-eye"></i>' : '<i class="fas fa-eye-slash"></i>';
        });
    });

    document.querySelectorAll(".field-enhanced .input").forEach((input) => {
        const field = input.closest(".field-enhanced");
        const syncState = () => {
            if (!field) {
                return;
            }
            if (input.value && input.value.trim().length > 0) {
                field.classList.add("is-filled");
            } else {
                field.classList.remove("is-filled");
            }
        };
        input.addEventListener("input", syncState);
        syncState();
    });

    const passwordInput = document.getElementById("password");
    const strengthFill = document.getElementById("password-strength-fill");
    if (passwordInput && strengthFill) {
        const updateStrength = () => {
            const value = passwordInput.value || "";
            const hasLower = /[a-z]/.test(value);
            const hasUpper = /[A-Z]/.test(value);
            const hasNumber = /\d/.test(value);
            const hasSymbol = /[^A-Za-z0-9]/.test(value);
            let score = 0;
            if (value.length >= 8) score += 1;
            if (hasLower && hasUpper) score += 1;
            if (hasNumber) score += 1;
            if (hasSymbol) score += 1;
            const width = (score / 4) * 100;
            strengthFill.style.width = `${width}%`;
        };
        passwordInput.addEventListener("input", updateStrength);
        updateStrength();
    }

    const sync = crearSyncDiferida();
    sync.inicializar();

    const greet = document.querySelector("[data-greeting]");
    if (greet) {
        const hour = new Date().getHours();
        greet.textContent = hour < 12 ? "Buenos días" : hour < 19 ? "Buenas tardes" : "Buenas noches";
    }

    const sidebar = document.getElementById("app-sidebar");
    const toggle = document.getElementById("sidebar-toggle");
    if (sidebar && toggle) {
        toggle.addEventListener("click", () => document.body.classList.toggle("sidebar-open"));
        document.addEventListener("click", (event) => {
            if (!document.body.classList.contains("sidebar-open")) {
                return;
            }
            if (sidebar.contains(event.target) || toggle.contains(event.target)) {
                return;
            }
            document.body.classList.remove("sidebar-open");
        });
    }

    document.querySelectorAll("[data-faq] summary").forEach((summary) => {
        summary.addEventListener("click", () => {
            const details = summary.parentElement;
            document.querySelectorAll("[data-faq]").forEach((item) => {
                if (item !== details) {
                    item.removeAttribute("open");
                }
            });
        });
    });
});

function crearSyncDiferida() {
    const DB_NAME = "bovicontrol_offline";
    const DB_VERSION = 1;
    const STORE_QUEUE = "sync_queue";
    const FORM_ACTIONS = new Set([
        "/ganado/guardar",
        "/ganado/leche/registrar",
        "/vacuna/guardar"
    ]);
    const MAX_RETRIES = 5;

    let dbPromise = null;
    let isSyncing = false;
    let statusNode = null;
    let panelStatusNode = null;
    let panelQueueBody = null;
    let manualSyncBtn = null;
    let retryFailedBtn = null;

    function inicializar() {
        if (!("indexedDB" in window)) {
            return;
        }
        dbPromise = openDb();
        crearIndicador();
        enlazarPanelSincronizacion();
        bindFormulariosOffline();
        actualizarIndicador();
        if (navigator.onLine) {
            sincronizarPendientes();
        }
        window.addEventListener("online", () => {
            toast("Conexión recuperada. Sincronizando datos pendientes...");
            sincronizarPendientes();
        });
        window.addEventListener("offline", () => {
            actualizarIndicador();
            toast("Sin Conexión. Los nuevos registros se guardarán en este dispositivo.");
        });
        setInterval(() => {
            if (navigator.onLine) {
                sincronizarPendientes();
            }
        }, 30000);
    }

    function normalizarPath(action) {
        try {
            return new URL(action, window.location.origin).pathname;
        } catch (_) {
            return action || "";
        }
    }

    function bindFormulariosOffline() {
        document.querySelectorAll("form").forEach((form) => {
            const actionPath = normalizarPath(form.getAttribute("action"));
            if (!FORM_ACTIONS.has(actionPath)) {
                return;
            }

            form.addEventListener("submit", async (event) => {
                if (navigator.onLine) {
                    return;
                }
                event.preventDefault();
                try {
                    await encolarFormulario(form, actionPath);
                    toast("Registro guardado sin internet. Se sincronizará automáticamente.");
                    form.reset();
                    actualizarIndicador();
                } catch (error) {
                    console.error("No se pudo guardar el registro offline:", error);
                    toast("No se pudo guardar el registro offline. Intenta nuevamente.");
                }
            });
        });
    }

    async function encolarFormulario(form, actionPath) {
        const formData = new FormData(form);
        const payload = {};
        for (const [key, value] of formData.entries()) {
            payload[key] = String(value);
        }
        const fingerprint = crearFingerprint(actionPath, payload);
        const existeReciente = await existePendienteReciente(fingerprint);
        if (existeReciente) {
            toast("Este registro ya está en cola de sincronización.");
            return;
        }

        const item = {
            id: crypto.randomUUID(),
            action: actionPath,
            method: (form.getAttribute("method") || "post").toUpperCase(),
            payload,
            fingerprint,
            createdAt: Date.now(),
            retries: 0,
            lastError: null,
            nextRetryAt: null,
            lastAttemptAt: null
        };
        await addQueueItem(item);
    }

    function crearIndicador() {
        statusNode = document.createElement("div");
        statusNode.setAttribute("id", "sync-status-chip");
        statusNode.style.position = "fixed";
        statusNode.style.right = "16px";
        statusNode.style.bottom = "16px";
        statusNode.style.zIndex = "9999";
        statusNode.style.padding = "10px 14px";
        statusNode.style.borderRadius = "999px";
        statusNode.style.fontSize = "12px";
        statusNode.style.fontWeight = "700";
        statusNode.style.boxShadow = "0 8px 20px rgba(12, 32, 19, 0.18)";
        statusNode.style.background = "#eef3ef";
        statusNode.style.color = "#1f3a2b";
        statusNode.style.cursor = "pointer";
        statusNode.addEventListener("click", () => {
            if (navigator.onLine) {
                sincronizarPendientes();
            } else {
                toast("Sin Conexión. La sincronización iniciará cuando vuelva internet.");
            }
        });
        document.body.appendChild(statusNode);
    }

    function enlazarPanelSincronizacion() {
        panelStatusNode = document.getElementById("sync-panel-status");
        panelQueueBody = document.getElementById("sync-queue-body");
        manualSyncBtn = document.getElementById("sync-now-btn");
        retryFailedBtn = document.getElementById("sync-retry-btn");
        if (manualSyncBtn) {
            manualSyncBtn.addEventListener("click", () => {
                if (navigator.onLine) {
                    sincronizarPendientes();
                } else {
                    toast("Sin Conexión. La sincronización iniciará cuando vuelva internet.");
                }
            });
        }
        if (retryFailedBtn) {
            retryFailedBtn.addEventListener("click", async () => {
                await resetErroresApendiente();
                if (navigator.onLine) {
                    sincronizarPendientes();
                } else {
                    toast("Fallidos preparados. Se enviarán cuando vuelva internet.");
                }
            });
        }
    }

    async function actualizarIndicador() {
        if (!statusNode) {
            return;
        }
        const pendientes = await countQueue();
        const items = await getQueueItems();
        const online = navigator.onLine;
        if (!online) {
            statusNode.textContent = `Offline · ${pendientes} pendiente(s)`;
            statusNode.style.background = "#fff4e5";
            statusNode.style.color = "#7a4a00";
            renderPanel(online, pendientes, items);
            return;
        }
        if (isSyncing) {
            statusNode.textContent = `Sincronizando... ${pendientes} pendiente(s)`;
            statusNode.style.background = "#e7f0ff";
            statusNode.style.color = "#1e3a8a";
            renderPanel(online, pendientes, items);
            return;
        }
        statusNode.textContent = pendientes > 0 ? `En línea · ${pendientes} pendiente(s)` : "En línea · Todo sincronizado";
        statusNode.style.background = pendientes > 0 ? "#f3f4f6" : "#eaf7ed";
        statusNode.style.color = pendientes > 0 ? "#374151" : "#1f5130";
        renderPanel(online, pendientes, items);
    }

    async function sincronizarPendientes() {
        if (isSyncing || !navigator.onLine) {
            return;
        }
        isSyncing = true;
        await actualizarIndicador();
        let exitos = 0;
        let fallos = 0;
        try {
            const items = await getQueueItems();
            const ahora = Date.now();
            for (const item of items) {
                if (item.nextRetryAt && item.nextRetryAt > ahora) {
                    continue;
                }
                try {
                    await enviarItem(item);
                    await deleteQueueItem(item.id);
                    exitos += 1;
                } catch (error) {
                    await markQueueError(item.id, error?.message || "Error de sincronización");
                    fallos += 1;
                }
            }
            if (exitos > 0 || fallos > 0) {
                toast(`Sincronización: ${exitos} ok, ${fallos} con error.`);
            }
        } finally {
            isSyncing = false;
            await actualizarIndicador();
        }
    }

    async function enviarItem(item) {
        const body = new URLSearchParams(item.payload);
        const response = await fetch(item.action, {
            method: item.method,
            headers: {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
            },
            body,
            credentials: "include"
        });
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        if (response.url && response.url.includes("/login")) {
            throw new Error("Sesión expirada. Inicia sesión nuevamente.");
        }
    }

    function toast(message) {
        const node = document.createElement("div");
        node.textContent = message;
        node.style.position = "fixed";
        node.style.left = "50%";
        node.style.bottom = "72px";
        node.style.transform = "translateX(-50%)";
        node.style.padding = "10px 14px";
        node.style.background = "#0f1d14";
        node.style.color = "#fff";
        node.style.borderRadius = "10px";
        node.style.fontSize = "13px";
        node.style.zIndex = "10000";
        node.style.opacity = "0";
        node.style.transition = "opacity .2s ease";
        document.body.appendChild(node);
        requestAnimationFrame(() => {
            node.style.opacity = "1";
        });
        setTimeout(() => {
            node.style.opacity = "0";
            setTimeout(() => node.remove(), 250);
        }, 2400);
    }

    function renderPanel(online, pendientes, items) {
        if (panelStatusNode) {
            panelStatusNode.textContent = online
                ? (isSyncing ? `Sincronizando ${pendientes} registro(s)...` : `En línea. Pendientes: ${pendientes}.`)
                : `Sin Conexión. Pendientes: ${pendientes}.`;
        }
        if (!panelQueueBody) {
            return;
        }
        if (!items || items.length === 0) {
            panelQueueBody.innerHTML = '<tr><td colspan="6"><div class="empty-card" style="padding:14px;">Sin registros pendientes.</div></td></tr>';
            return;
        }
        panelQueueBody.innerHTML = items.map((item) => {
            const tipo = mapEntity(item.action);
            const accion = mapAction(item.action);
            const conError = (item.retries || 0) > 0;
            const estado = conError ? "Error" : "Pendiente";
            const estadoClass = conError ? "sync-state sync-state-error" : "sync-state sync-state-pending";
            const error = item.lastError ? sanitizarHtml(item.lastError) : "-";
            const fecha = formatDateTime(item.createdAt);
            const intentos = item.retries || 0;
            return `<tr>
                <td>${tipo}</td>
                <td>${accion}</td>
                <td>${fecha}</td>
                <td>${intentos}</td>
                <td><span class="${estadoClass}">${estado}</span></td>
                <td>${error}</td>
            </tr>`;
        }).join("");
    }

    function mapEntity(action) {
        if (action.includes("/ganado/leche/")) return "Leche";
        if (action.includes("/vacuna/")) return "Vacuna";
        if (action.includes("/ganado/")) return "Ganado";
        return "Registro";
    }

    function mapAction(action) {
        if (action.endsWith("/guardar")) return "Guardar";
        if (action.endsWith("/registrar")) return "Registrar";
        return "Enviar";
    }

    function sanitizarHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function openDb() {
        return new Promise((resolve, reject) => {
            const req = indexedDB.open(DB_NAME, DB_VERSION);
            req.onupgradeneeded = () => {
                const db = req.result;
                if (!db.objectStoreNames.contains(STORE_QUEUE)) {
                    const store = db.createObjectStore(STORE_QUEUE, { keyPath: "id" });
                    store.createIndex("createdAt", "createdAt", { unique: false });
                }
            };
            req.onsuccess = () => resolve(req.result);
            req.onerror = () => reject(req.error);
        });
    }

    async function withStore(mode, fn) {
        const db = await dbPromise;
        return new Promise((resolve, reject) => {
            const tx = db.transaction(STORE_QUEUE, mode);
            const store = tx.objectStore(STORE_QUEUE);
            const result = fn(store);
            tx.oncomplete = () => resolve(result);
            tx.onerror = () => reject(tx.error);
        });
    }

    function requestToPromise(request) {
        return new Promise((resolve, reject) => {
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    async function addQueueItem(item) {
        return withStore("readwrite", (store) => store.add(item));
    }

    async function getQueueItems() {
        const db = await dbPromise;
        return new Promise((resolve, reject) => {
            const tx = db.transaction(STORE_QUEUE, "readonly");
            const store = tx.objectStore(STORE_QUEUE);
            const req = store.getAll();
            req.onsuccess = () => {
                const rows = Array.isArray(req.result) ? req.result : [];
                rows.sort((a, b) => a.createdAt - b.createdAt);
                resolve(rows);
            };
            req.onerror = () => reject(req.error);
        });
    }

    async function deleteQueueItem(id) {
        return withStore("readwrite", (store) => store.delete(id));
    }

    async function markQueueError(id, errorMessage) {
        const db = await dbPromise;
        const tx = db.transaction(STORE_QUEUE, "readwrite");
        const store = tx.objectStore(STORE_QUEUE);
        const current = await requestToPromise(store.get(id));
        if (!current) {
            return;
        }
        current.retries = (current.retries || 0) + 1;
        if (current.retries > MAX_RETRIES) {
            current.retries = MAX_RETRIES;
        }
        current.lastError = errorMessage;
        current.lastAttemptAt = Date.now();
        const delay = Math.min(300000, 10000 * Math.pow(2, Math.max(0, current.retries - 1)));
        current.nextRetryAt = Date.now() + delay;
        await requestToPromise(store.put(current));
    }

    async function countQueue() {
        const db = await dbPromise;
        const tx = db.transaction(STORE_QUEUE, "readonly");
        const store = tx.objectStore(STORE_QUEUE);
        return requestToPromise(store.count());
    }

    async function resetErroresApendiente() {
        const items = await getQueueItems();
        const fallidos = items.filter((item) => (item.retries || 0) > 0);
        if (fallidos.length === 0) {
            toast("No hay registros fallidos para reintentar.");
            return;
        }
        const db = await dbPromise;
        const tx = db.transaction(STORE_QUEUE, "readwrite");
        const store = tx.objectStore(STORE_QUEUE);
        await Promise.all(fallidos.map((item) => {
            const limpio = { ...item, retries: 0, lastError: null, nextRetryAt: null };
            return requestToPromise(store.put(limpio));
        }));
        toast(`Se marcaron ${fallidos.length} fallido(s) para reintento.`);
        await actualizarIndicador();
    }

    async function existePendienteReciente(fingerprint) {
        const items = await getQueueItems();
        const ahora = Date.now();
        return items.some((item) =>
            item.fingerprint === fingerprint &&
            (ahora - (item.createdAt || 0) < 60000)
        );
    }

    function crearFingerprint(action, payload) {
        const ordered = Object.keys(payload).sort().map((k) => `${k}=${payload[k]}`).join("&");
        return `${action}|${ordered}`;
    }

    function formatDateTime(value) {
        if (!value) {
            return "-";
        }
        const d = new Date(value);
        const p = (n) => String(n).padStart(2, "0");
        return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
    }

    return { inicializar };
}








