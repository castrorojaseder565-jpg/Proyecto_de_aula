(() => {
    const form = document.getElementById("chatForm");
    const input = document.getElementById("chatMessage");
    const messages = document.getElementById("chatMessages");
    const sendButton = document.getElementById("chatSend");
    const status = document.getElementById("chatStatus");
    const csrfToken = document.getElementById("csrfToken").value;

    function addMessage(role, content, isError = false) {
        const article = document.createElement("article");
        article.className = `chat-message ${role === "user" ? "user-message" : "assistant-message"}${isError ? " chat-error" : ""}`;
        const label = document.createElement("span");
        label.className = "chat-message-label";
        label.textContent = role === "user" ? "Tú" : "Asistente";
        const paragraph = document.createElement("p");
        paragraph.textContent = content;
        article.append(label, paragraph);
        messages.append(article);
        messages.scrollTop = messages.scrollHeight;
    }

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const mensaje = input.value.trim();
        if (!mensaje) return;

        addMessage("user", mensaje);
        input.value = "";
        input.disabled = true;
        sendButton.disabled = true;
        status.textContent = "Consultando los registros y preparando la respuesta…";

        try {
            const response = await fetch("/api/chat", {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-TOKEN": csrfToken
                },
                body: JSON.stringify({ mensaje })
            });
            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.error || "No fue posible procesar la pregunta.");
            addMessage("assistant", data.respuesta);
            status.textContent = "";
        } catch (error) {
            addMessage("assistant", error.message || "No fue posible conectar con el asistente.", true);
            status.textContent = "Revisa la configuración del asistente y vuelve a intentarlo.";
        } finally {
            input.disabled = false;
            sendButton.disabled = false;
            input.focus();
        }
    });

    input.addEventListener("keydown", (event) => {
        if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
            event.preventDefault();
            form.requestSubmit();
        }
    });
})();
