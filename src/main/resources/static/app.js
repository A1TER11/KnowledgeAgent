const state = {
    sessionId: "demo-session",
    userId: "demo-user",
    activeDocumentId: null,
    lastChatResponse: null
};

const elements = {
    storageType: document.getElementById("storageType"),
    remoteChatEnabled: document.getElementById("remoteChatEnabled"),
    chatModel: document.getElementById("chatModel"),
    embeddingModel: document.getElementById("embeddingModel"),
    sessionId: document.getElementById("sessionId"),
    userId: document.getElementById("userId"),
    refreshContextButton: document.getElementById("refreshContextButton"),
    documentForm: document.getElementById("documentForm"),
    documentId: document.getElementById("documentId"),
    documentTitle: document.getElementById("documentTitle"),
    documentContent: document.getElementById("documentContent"),
    saveDocumentButton: document.getElementById("saveDocumentButton"),
    resetDocumentButton: document.getElementById("resetDocumentButton"),
    reseedExamplesButton: document.getElementById("reseedExamplesButton"),
    documentFeedback: document.getElementById("documentFeedback"),
    documentList: document.getElementById("documentList"),
    documentDetail: document.getElementById("documentDetail"),
    memoryList: document.getElementById("memoryList"),
    reloadDocumentsButton: document.getElementById("reloadDocumentsButton"),
    reloadSessionButton: document.getElementById("reloadSessionButton"),
    chatForm: document.getElementById("chatForm"),
    messageInput: document.getElementById("messageInput"),
    chatTimeline: document.getElementById("chatTimeline"),
    answerPreview: document.getElementById("answerPreview"),
    knowledgeHits: document.getElementById("knowledgeHits"),
    memoryHits: document.getElementById("memoryHits"),
    toolCalls: document.getElementById("toolCalls")
};

document.addEventListener("DOMContentLoaded", () => {
    bindEvents();
    bootstrap();
});

function bindEvents() {
    elements.refreshContextButton.addEventListener("click", refreshContext);
    elements.reloadDocumentsButton.addEventListener("click", loadDocuments);
    elements.reloadSessionButton.addEventListener("click", loadSession);
    elements.documentForm.addEventListener("submit", onDocumentSubmit);
    elements.resetDocumentButton.addEventListener("click", resetDocumentEditor);
    elements.reseedExamplesButton.addEventListener("click", reseedExamples);
    elements.chatForm.addEventListener("submit", onChatSubmit);
}

async function bootstrap() {
    syncIdentityState();
    updateDocumentButtonLabel();
    await Promise.all([loadMeta(), loadDocuments(), loadSession(), loadMemories()]);
}

function syncIdentityState() {
    state.sessionId = elements.sessionId.value.trim() || "demo-session";
    state.userId = elements.userId.value.trim() || "demo-user";
    elements.sessionId.value = state.sessionId;
    elements.userId.value = state.userId;
}

function resetDocumentEditor() {
    state.activeDocumentId = null;
    elements.documentId.value = "";
    elements.documentForm.reset();
    renderFeedback("", false);
    updateDocumentButtonLabel();
    renderEmpty(elements.documentDetail, "点击“查看详情”后会在这里展示原文和分块。");
}

function updateDocumentButtonLabel() {
    elements.saveDocumentButton.textContent = state.activeDocumentId ? "更新文档" : "写入知识库";
}

async function refreshContext() {
    syncIdentityState();
    await Promise.all([loadSession(), loadMemories()]);
}

async function onDocumentSubmit(event) {
    event.preventDefault();

    const payload = {
        title: elements.documentTitle.value.trim(),
        content: elements.documentContent.value.trim()
    };

    if (!payload.title || !payload.content) {
        renderFeedback("请先填写文档标题和内容。", true);
        return;
    }

    const isUpdate = Boolean(state.activeDocumentId);
    setButtonBusy(event.submitter, true, isUpdate ? "更新中..." : "写入中...");
    try {
        const response = await fetchJson(isUpdate ? `/api/docs/${encodeURIComponent(state.activeDocumentId)}` : "/api/docs/upload", {
            method: isUpdate ? "PUT" : "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        });

        renderFeedback(
            isUpdate
                ? `已更新《${response.title}》，当前共有 ${response.chunkCount} 个分块。`
                : `已收录《${response.title}》，生成 ${response.chunkCount} 个分块。`,
            false
        );
        state.activeDocumentId = response.documentId;
        elements.documentId.value = response.documentId;
        updateDocumentButtonLabel();
        await loadDocuments();
        await loadDocumentDetail(response.documentId);
    } catch (error) {
        renderFeedback(error.message, true);
    } finally {
        setButtonBusy(event.submitter, false, state.activeDocumentId ? "更新文档" : "写入知识库");
    }
}

async function reseedExamples(event) {
    setButtonBusy(event.currentTarget, true, "导入中...");
    try {
        const response = await fetchJson("/api/docs/reseed-examples", {
            method: "POST"
        });

        if (response.examplesDirectoryMissing) {
            renderFeedback("未找到 examples 目录，无法导入示例文档。", true);
            return;
        }

        renderFeedback(
            response.importedCount > 0
                ? `已重新导入 ${response.importedCount} 份示例文档。`
                : "示例文档已全部存在，无需重新导入。",
            false
        );
        await loadDocuments();
    } catch (error) {
        renderFeedback(error.message, true);
    } finally {
        setButtonBusy(event.currentTarget, false, "重新导入示例");
    }
}

async function onChatSubmit(event) {
    event.preventDefault();
    syncIdentityState();

    const message = elements.messageInput.value.trim();
    if (!message) {
        return;
    }

    const payload = {
        sessionId: state.sessionId,
        userId: state.userId,
        message
    };

    setButtonBusy(event.submitter, true, "发送中...");
    optimisticAppendMessage("USER", message, new Date().toISOString());
    elements.messageInput.value = "";

    try {
        const response = await fetchJson("/api/chat", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(payload)
        });
        state.lastChatResponse = response;
        renderLastResponse(response);
        await Promise.all([loadSession(), loadMemories()]);
    } catch (error) {
        renderAnswerPreview(error.message, true);
        await loadSession();
    } finally {
        setButtonBusy(event.submitter, false, "发送消息");
    }
}

async function loadMeta() {
    try {
        const meta = await fetchJson("/api/meta");
        elements.storageType.textContent = meta.storageType;
        elements.chatModel.textContent = meta.chatModel;
        elements.embeddingModel.textContent = meta.embeddingModel;
        elements.remoteChatEnabled.textContent = meta.remoteChatEnabled ? "Remote Chat On" : "Fallback Only";
        elements.remoteChatEnabled.className = `meta-pill ${meta.remoteChatEnabled ? "status-good" : "status-off"}`;
    } catch (error) {
        elements.storageType.textContent = "读取失败";
        elements.remoteChatEnabled.textContent = "配置不可用";
    }
}

async function loadDocuments() {
    try {
        const documents = await fetchJson("/api/docs");
        if (!documents.length) {
            renderEmpty(elements.documentList, "暂无文档。");
            return;
        }

        clearEmptyState(elements.documentList);
        elements.documentList.innerHTML = documents.map(document => `
            <article class="info-card document-row">
                <div>
                    <strong>${escapeHtml(document.title)}</strong>
                    <div class="chip-row">
                        <span class="chip">ID ${escapeHtml(shorten(document.documentId, 12))}</span>
                        <span class="chip">${document.chunkCount} chunks</span>
                    </div>
                </div>
                <div class="document-row-actions">
                    <button type="button" class="ghost-button" data-action="detail" data-document-id="${escapeHtml(document.documentId)}">查看详情</button>
                    <button type="button" class="ghost-button" data-action="delete" data-document-id="${escapeHtml(document.documentId)}">删除</button>
                </div>
            </article>
        `).join("");

        attachDocumentListEvents();
    } catch (error) {
        renderEmpty(elements.documentList, error.message);
    }
}

function attachDocumentListEvents() {
    elements.documentList.querySelectorAll("[data-action='detail']").forEach(button => {
        button.addEventListener("click", () => loadDocumentDetail(button.dataset.documentId));
    });

    elements.documentList.querySelectorAll("[data-action='delete']").forEach(button => {
        button.addEventListener("click", () => deleteDocument(button.dataset.documentId));
    });
}

async function loadDocumentDetail(documentId) {
    try {
        const detail = await fetchJson(`/api/docs/${encodeURIComponent(documentId)}`);
        state.activeDocumentId = detail.documentId;
        elements.documentId.value = detail.documentId;
        elements.documentTitle.value = detail.title;
        elements.documentContent.value = detail.rawContent;
        updateDocumentButtonLabel();

        clearEmptyState(elements.documentDetail);
        elements.documentDetail.innerHTML = `
            <article class="detail-card">
                <strong>${escapeHtml(detail.title)}</strong>
                <div class="chip-row">
                    <span class="chip">ID ${escapeHtml(shorten(detail.documentId, 12))}</span>
                    <span class="chip">${detail.chunkCount} chunks</span>
                </div>
                <div class="detail-body">${escapeHtml(detail.rawContent)}</div>
            </article>
            <div class="chunk-list">
                ${detail.chunks.map(chunk => `
                    <article class="detail-card">
                        <strong>${escapeHtml(chunk.title)}</strong>
                        <div class="chip-row">
                            <span class="chip">Chunk ${escapeHtml(shorten(chunk.chunkId, 12))}</span>
                        </div>
                        <div class="detail-body">${escapeHtml(chunk.content)}</div>
                    </article>
                `).join("")}
            </div>
        `;
    } catch (error) {
        renderEmpty(elements.documentDetail, error.message);
        renderFeedback(error.message, true);
    }
}

async function deleteDocument(documentId) {
    try {
        await fetchJson(`/api/docs/${encodeURIComponent(documentId)}`, {
            method: "DELETE"
        });
        if (state.activeDocumentId === documentId) {
            resetDocumentEditor();
        }
        renderFeedback("文档已删除。", false);
        await loadDocuments();
    } catch (error) {
        renderFeedback(error.message, true);
    }
}

async function loadSession() {
    syncIdentityState();
    try {
        const session = await fetchJson(`/api/sessions/${encodeURIComponent(state.sessionId)}`);
        if (!session.messages.length) {
            renderEmpty(elements.chatTimeline, "发送第一条消息开始演示。");
            return;
        }

        clearEmptyState(elements.chatTimeline);
        elements.chatTimeline.innerHTML = session.messages.map(message => `
            <article class="message ${message.role === "USER" ? "message-user" : "message-assistant"}">
                <div class="message-head">
                    <span class="message-role ${message.role === "USER" ? "message-role-user" : "message-role-assistant"}">${message.role}</span>
                    <span class="message-time">${formatTime(message.createdAt)}</span>
                </div>
                <div class="message-body">${escapeHtml(message.content)}</div>
            </article>
        `).join("");
        elements.chatTimeline.scrollTop = elements.chatTimeline.scrollHeight;
    } catch (error) {
        renderEmpty(elements.chatTimeline, error.message);
    }
}

async function loadMemories() {
    syncIdentityState();
    try {
        const memoryResponse = await fetchJson(`/api/memories/${encodeURIComponent(state.userId)}`);
        if (!memoryResponse.memories.length) {
            renderEmpty(elements.memoryList, "当前用户还没有长期记忆。");
            return;
        }

        clearEmptyState(elements.memoryList);
        elements.memoryList.innerHTML = memoryResponse.memories.map(memory => `
            <article class="memory-card">
                <strong>${escapeHtml(memory.memoryType)}</strong>
                <div>${escapeHtml(memory.content)}</div>
                <div class="chip-row">
                    <span class="chip">${escapeHtml(memory.source)}</span>
                    <span class="chip">${formatTime(memory.createdAt)}</span>
                </div>
            </article>
        `).join("");
    } catch (error) {
        renderEmpty(elements.memoryList, error.message);
    }
}

function renderLastResponse(response) {
    renderAnswerPreview(response.answer, false);
    renderSignals(
        elements.knowledgeHits,
        response.knowledgeSnippets,
        snippet => `
            <article class="signal-card">
                <strong>${escapeHtml(snippet.title)}</strong>
                <div>${escapeHtml(snippet.content)}</div>
                <div class="chip-row">
                    <span class="chip">score ${Number(snippet.score).toFixed(3)}</span>
                </div>
            </article>
        `,
        "暂无知识命中"
    );
    renderSignals(
        elements.memoryHits,
        response.longTermMemories,
        memory => `
            <article class="signal-card">
                <strong>${escapeHtml(memory.memoryType)}</strong>
                <div>${escapeHtml(memory.content)}</div>
                <div class="chip-row">
                    <span class="chip">score ${Number(memory.score).toFixed(3)}</span>
                </div>
            </article>
        `,
        "暂无命中"
    );
    renderSignals(
        elements.toolCalls,
        response.toolCalls,
        tool => `
            <article class="signal-card">
                <strong>${escapeHtml(tool.toolName)}</strong>
                <div>${escapeHtml(tool.summary || "已完成调用")}</div>
                ${renderToolDetails(tool.details)}
            </article>
        `,
        "这次回答没有额外调用工具。"
    );
}

function renderSignals(container, items, renderer, emptyText) {
    if (!items || !items.length) {
        renderEmpty(container, emptyText);
        return;
    }
    clearEmptyState(container);
    container.innerHTML = items.map(renderer).join("");
}

function renderAnswerPreview(content, isError) {
    elements.answerPreview.className = `insight-content${isError ? " status-off" : ""}`;
    elements.answerPreview.textContent = content;
}

function renderToolDetails(details) {
    if (!details || !String(details).trim()) {
        return "";
    }

    const normalized = String(details).trim();
    const parsed = tryParseJson(normalized);
    if (parsed) {
        return `<div class="tool-details">${renderStructuredValue(parsed)}</div>`;
    }

    return `
        <details class="tool-details">
            <summary>查看过程细节</summary>
            <pre>${escapeHtml(normalized)}</pre>
        </details>
    `;
}

function renderStructuredValue(value) {
    if (value == null) {
        return `<div class="detail-item"><span class="detail-label">结果</span><div class="detail-value">无</div></div>`;
    }

    if (Array.isArray(value)) {
        if (!value.length) {
            return `<div class="detail-item"><span class="detail-label">结果</span><div class="detail-value">空列表</div></div>`;
        }
        return value.map((item, index) => `
            <div class="detail-group">
                <div class="detail-group-title">结果 ${index + 1}</div>
                ${renderStructuredValue(item)}
            </div>
        `).join("");
    }

    if (typeof value === "object") {
        return Object.entries(value).map(([key, entryValue]) => `
            <div class="detail-item">
                <span class="detail-label">${escapeHtml(formatDetailLabel(key))}</span>
                <div class="detail-value">${renderDetailValue(entryValue)}</div>
            </div>
        `).join("");
    }

    return `<div class="detail-item"><span class="detail-label">结果</span><div class="detail-value">${escapeHtml(String(value))}</div></div>`;
}

function renderDetailValue(value) {
    if (value == null) {
        return "无";
    }
    if (Array.isArray(value) || typeof value === "object") {
        return renderStructuredValue(value);
    }
    return escapeHtml(String(value));
}

function formatDetailLabel(key) {
    return String(key)
        .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
        .replaceAll("_", " ")
        .replace(/\s+/g, " ")
        .trim();
}

function tryParseJson(value) {
    if (!(value.startsWith("{") || value.startsWith("["))) {
        return null;
    }
    try {
        return JSON.parse(value);
    } catch (error) {
        console.debug("tool details is not valid JSON", error);
        return null;
    }
}

function renderFeedback(message, isError) {
    elements.documentFeedback.textContent = message;
    elements.documentFeedback.style.color = isError ? "#9a4d24" : "var(--accent-strong)";
}

function renderEmpty(container, message) {
    container.className = `${container.className.replace(/\bempty-state\b/g, "").trim()} empty-state`.trim();
    container.textContent = message;
}

function optimisticAppendMessage(role, content, createdAt) {
    clearEmptyState(elements.chatTimeline);
    const article = document.createElement("article");
    article.className = `message ${role === "USER" ? "message-user" : "message-assistant"}`;
    article.innerHTML = `
        <div class="message-head">
            <span class="message-role ${role === "USER" ? "message-role-user" : "message-role-assistant"}">${escapeHtml(role)}</span>
            <span class="message-time">${formatTime(createdAt)}</span>
        </div>
        <div class="message-body">${escapeHtml(content)}</div>
    `;
    elements.chatTimeline.appendChild(article);
    elements.chatTimeline.scrollTop = elements.chatTimeline.scrollHeight;
}

function clearEmptyState(container) {
    container.className = container.className.replace(/\bempty-state\b/g, "").replace(/\s+/g, " ").trim();
}

async function fetchJson(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) {
        let message = `请求失败 (${response.status})`;
        try {
            const text = await response.text();
            if (text) {
                message = text;
            }
        } catch (error) {
            console.error(error);
        }
        throw new Error(message);
    }
    if (response.status === 204) {
        return {};
    }
    return response.json();
}

function setButtonBusy(button, busy, busyText) {
    if (!button) {
        return;
    }
    if (!button.dataset.label) {
        button.dataset.label = button.textContent;
    }
    button.disabled = busy;
    button.textContent = busy ? busyText : button.dataset.label;
}

function formatTime(value) {
    const date = new Date(value);
    return new Intl.DateTimeFormat("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(date);
}

function shorten(value, length) {
    return value.length <= length ? value : `${value.slice(0, length)}...`;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
