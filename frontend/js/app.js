let currentRoomId = null;
let replyToId = null;
let currentPage = 0;
let isLoadingMore = false;
let hasMoreMessages = true;

window.onload = function () {
    const savedUser = localStorage.getItem('user');
    if (token && savedUser) {
        currentUser = JSON.parse(savedUser);
        showMainScreen();
    }
};

function showMainScreen() {
    document.getElementById('auth-screen').style.display = 'none';
    document.getElementById('main-screen').style.display = 'flex';
    document.getElementById('current-username').textContent = currentUser.username;
    loadRooms();
    loadFriends();
    connectWebSocket();

    document.getElementById('messages').addEventListener('scroll', function() {
        if (this.scrollTop === 0) loadMoreMessages();
    });

    document.addEventListener('paste', function(e) {
        if (!currentRoomId) return;
        const items = e.clipboardData?.items;
        if (!items) return;
        for (const item of items) {
            if (item.kind === 'file') {
                const file = item.getAsFile();
                if (!file) return;
                uploadPastedFile(file);
            }
        }
    });
}

async function openRoom(room) {
    currentRoomId = room.id;
    window.currentRoomId = room.id;
    currentPage = 0;
    hasMoreMessages = true;

    document.getElementById('chat-header').textContent = '# ' + room.name;
    document.getElementById('input-area').style.display = 'block';

    subscribeToRoom(room.id);

    const messages = await loadMessages(room.id, 0);
    const container = document.getElementById('messages');
    container.innerHTML = '';
    messages.forEach(appendMessage);
    container.scrollTop = container.scrollHeight;
}

async function loadMoreMessages() {
    if (isLoadingMore || !hasMoreMessages || !currentRoomId) return;
    isLoadingMore = true;
    currentPage++;
    try {
        const data = await apiCall('GET', `/rooms/${currentRoomId}/messages?page=${currentPage}`);
        if (!data || data.content.length === 0) {
            hasMoreMessages = false;
            isLoadingMore = false;
            return;
        }
        const container = document.getElementById('messages');
        const oldHeight = container.scrollHeight;
        const messages = data.content.reverse();
        messages.forEach(msg => {
            if (!msg || !msg.sender) return;
            const isOwn = currentUser && msg.sender.id === currentUser.id;
            const div = document.createElement('div');
            div.className = 'message' + (isOwn ? ' own' : '');
            div.id = 'msg-' + msg.id;
            let html = `<div class="message-sender">${msg.sender.username}</div>`;
            html += `<div class="message-content">${escapeHtml(msg.content || '')}</div>`;
            const time = new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            html += `<div class="message-time">${time}</div>`;
            div.innerHTML = html;
            container.insertBefore(div, container.firstChild);
        });
        container.scrollTop = container.scrollHeight - oldHeight;
    } catch (e) {
        console.error('Error loading more:', e);
    }
    isLoadingMore = false;
}

function appendMessage(msg) {
    if (!msg || !msg.sender) return;
    if (msg.room && msg.room.id) incrementUnread(msg.room.id);
    const container = document.getElementById('messages');
    const isOwn = currentUser && msg.sender.id === currentUser.id;

    const div = document.createElement('div');
    div.className = 'message' + (isOwn ? ' own' : '');
    div.id = 'msg-' + msg.id;

    let html = `<div class="message-sender">${msg.sender.username}</div>`;

    if (msg.replyTo) {
        html += `<div class="reply-quote">↩ ${msg.replyTo.content || '[deleted]'}</div>`;
    }

    html += `<div class="message-content">${escapeHtml(msg.content || '')}</div>`;

    if (msg.editedAt) {
        html += `<span class="message-edited">edited</span>`;
    }

    const time = new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    html += `<div class="message-time">${time}</div>`;

    html += `<div class="message-actions">
        <button onclick="replyTo(${msg.id}, '${escapeHtml(msg.content || '')}')">Reply</button>`;

    if (isOwn) {
        html += `<button onclick="editMsg(${msg.id})">Edit</button>
                 <button onclick="deleteMsg(${msg.id})">Delete</button>`;
    }
    html += `</div>`;

    div.innerHTML = html;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;

    window.lastMessageId = msg.id;
}

function escapeHtml(text) {
    return text.replace(/&/g, '&amp;')
               .replace(/</g, '&lt;')
               .replace(/>/g, '&gt;');
}

function replyTo(id, content) {
    replyToId = id;
    document.getElementById('reply-preview').style.display = 'flex';
    document.getElementById('reply-text').textContent = '↩ ' + content.substring(0, 50);
    document.getElementById('message-input').focus();
}

function cancelReply() {
    replyToId = null;
    document.getElementById('reply-preview').style.display = 'none';
}

async function uploadPastedFile(file) {
    if (!currentRoomId) return;
    try {
        const msg = await apiCall('POST', `/rooms/${currentRoomId}/messages`, {
            content: '📎 ' + (file.name || 'pasted file'),
            replyToId: null
        });
        appendMessage(msg);

        const formData = new FormData();
        formData.append('file', file);
        const headers = {};
        if (token) headers['Authorization'] = 'Bearer ' + token;
        await fetch(`${API}/files/upload/${msg.id}`, {
            method: 'POST',
            headers,
            body: formData
        });
        alert('File uploaded: ' + (file.name || 'pasted file'));
    } catch (e) {
        console.error('Paste upload error:', e);
    }
}

const unreadCounts = {};

function incrementUnread(roomId) {
    if (currentRoomId === roomId) return;
    unreadCounts[roomId] = (unreadCounts[roomId] || 0) + 1;
    updateUnreadBadge(roomId);
}

function clearUnread(roomId) {
    unreadCounts[roomId] = 0;
    updateUnreadBadge(roomId);
}

function updateUnreadBadge(roomId) {
    const count = unreadCounts[roomId] || 0;
    const badge = document.getElementById('unread-' + roomId);
    if (badge) {
        badge.textContent = count > 0 ? count : '';
        badge.style.display = count > 0 ? 'inline' : 'none';
    }
}

async function sendMessage() {
    const input = document.getElementById('message-input');
    const content = input.value.trim();
    if (!content || !currentRoomId) return;

    try {
        const msg = await apiCall('POST', `/rooms/${currentRoomId}/messages`, {
            content: content,
            replyToId: replyToId || null
        });
        input.value = '';
        cancelReply();
        appendMessage(msg);
    } catch (e) {
        console.error('Send error:', e);
    }
}

function handleKey(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
}

async function editMsg(messageId) {
    const newContent = prompt('Edit message:');
    if (!newContent) return;
    try {
        await apiCall('PUT', `/rooms/${currentRoomId}/messages/${messageId}`,
            { content: newContent });
        const el = document.getElementById('msg-' + messageId);
        if (el) el.querySelector('.message-content').textContent = newContent;
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function deleteMsg(messageId) {
    if (!confirm('Delete message?')) return;
    try {
        await apiCall('DELETE', `/rooms/${currentRoomId}/messages/${messageId}`);
        const el = document.getElementById('msg-' + messageId);
        if (el) el.remove();
    } catch (e) {
        alert('Error: ' + e.message);
    }
}