const API = 'http://localhost:8080/api';
let token = localStorage.getItem('token');
let currentUser = null;

async function apiCall(method, path, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch(API + path, {
        method,
        headers,
        body: body ? JSON.stringify(body) : null
    });

    if (!res.ok) {
        const err = await res.text();
        throw new Error(err);
    }

    return res.json().catch(() => null);
}

async function login() {
    const email = document.getElementById('login-email').value;
    const password = document.getElementById('login-password').value;
    try {
        const data = await apiCall('POST', '/auth/login', { email, password });
        token = data.token;
        currentUser = { id: data.userId, username: data.username, email: data.email };
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(currentUser));
        showMainScreen();
    } catch (e) {
        document.getElementById('auth-error').textContent = 'Invalid credentials';
    }
}

async function register() {
    const username = document.getElementById('reg-username').value;
    const email = document.getElementById('reg-email').value;
    const password = document.getElementById('reg-password').value;
    try {
        const data = await apiCall('POST', '/auth/register', { username, email, password });
        token = data.token;
        currentUser = { id: data.userId, username: data.username, email: data.email };
        localStorage.setItem('token', token);
        localStorage.setItem('user', JSON.stringify(currentUser));
        showMainScreen();
    } catch (e) {
        document.getElementById('auth-error').textContent = 'Registration failed';
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    token = null;
    currentUser = null;
    document.getElementById('main-screen').style.display = 'none';
    document.getElementById('auth-screen').style.display = 'flex';
}

async function loadRooms() {
    try {
        const rooms = await apiCall('GET', '/rooms');
        const list = document.getElementById('rooms-list');
        list.innerHTML = '';
        const roomArray = Array.isArray(rooms) ? rooms : [rooms];
        roomArray.forEach(room => {
            const div = document.createElement('div');
            div.className = 'room-item';
            div.innerHTML = `# ${room.name} <span id="unread-${room.id}" style="display:none;background:#e74c3c;color:white;border-radius:99px;font-size:11px;padding:1px 6px;float:right"></span>`;
            div.addEventListener('click', function() {
                document.querySelectorAll('.room-item').forEach(el => el.classList.remove('active'));
                this.classList.add('active');
                currentRoomId = room.id;
                window.currentRoomId = room.id;
                clearUnread(room.id);
                document.getElementById('chat-header').textContent = '# ' + room.name;
                document.getElementById('input-area').style.display = 'block';
                subscribeToRoom(room.id);
                loadMessages(room.id).then(messages => {
                    const container = document.getElementById('messages');
                    container.innerHTML = '';
                    messages.forEach(appendMessage);
                    container.scrollTop = container.scrollHeight;
                });
            });
            list.appendChild(div);
        });
    } catch (e) {
        console.error('Error loading rooms:', e);
    }
}

async function loadFriends() {
    try {
        const friends = await apiCall('GET', '/friends');
        const list = document.getElementById('friends-list');
        list.innerHTML = '';
        if (!Array.isArray(friends)) return;
        friends.forEach(f => {
            const friend = f.sender.id === currentUser.id ? f.receiver : f.sender;
            const div = document.createElement('div');
            div.className = 'friend-item';
            div.style = 'cursor:pointer';
            div.innerHTML = `<span class="presence-dot offline" id="dot-${friend.id}"></span>💬 ${friend.username}`;
            div.addEventListener('click', () => openDm(friend.username));
            list.appendChild(div);
        });
    } catch (e) {
        console.error('Error loading friends:', e);
    }
}

async function openDm(username) {
    try {
        const room = await apiCall('POST', `/users/dm/${username}`);
        currentRoomId = room.id;
        window.currentRoomId = room.id;
        document.getElementById('chat-header').textContent = '💬 ' + username;
        document.getElementById('input-area').style.display = 'block';
        document.querySelectorAll('.room-item').forEach(el => el.classList.remove('active'));
        subscribeToRoom(room.id);
        const messages = await loadMessages(room.id, 0);
        const container = document.getElementById('messages');
        container.innerHTML = '';
        messages.forEach(appendMessage);
        container.scrollTop = container.scrollHeight;
    } catch (e) {
        alert('Error opening DM: ' + e.message);
    }
}

async function loadMembers(roomId) {
    try {
        const members = await apiCall('GET', `/rooms/${roomId}/members`);
        const list = document.getElementById('members-list');
        list.innerHTML = '';
        if (!Array.isArray(members)) return;
        members.forEach(m => {
            const div = document.createElement('div');
            div.className = 'member-item';
            div.innerHTML = `
                <div style="display:flex;align-items:center;justify-content:space-between">
                    <div style="display:flex;align-items:center;gap:6px">
                        <span class="presence-dot offline" id="dot-${m.user.id}"></span>
                        <span>${m.user.username} ${m.role !== 'MEMBER' ? `<span style="font-size:10px;color:#7c83fd">(${m.role})</span>` : ''}</span>
                    </div>
                    ${m.user.id !== currentUser.id ? `<button onclick="banMember(${roomId}, ${m.user.id})" style="background:#e74c3c;border:none;color:white;padding:2px 6px;border-radius:3px;cursor:pointer;font-size:11px">Ban</button>` : ''}
                </div>
            `;
            list.appendChild(div);
        });
        sendPresence('ONLINE');
    } catch (e) {
        console.error('Error loading members:', e);
    }
}

async function banMember(roomId, userId) {
    if (!confirm('Ban this user from the room?')) return;
    try {
        await apiCall('POST', `/rooms/${roomId}/ban/${userId}`);
        alert('User banned!');
        loadMembers(roomId);
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function loadMessages(roomId, page = 0) {
    try {
        const data = await apiCall('GET', `/rooms/${roomId}/messages?page=${page}`);
        return data.content.reverse();
    } catch (e) {
        console.error('Error loading messages:', e);
        return [];
    }
}

async function uploadFile() {
    if (!window.currentRoomId || !window.lastMessageId) return;
    const file = document.getElementById('file-input').files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    const headers = {};
    if (token) headers['Authorization'] = 'Bearer ' + token;

    await fetch(`${API}/files/upload/${window.lastMessageId}`, {
        method: 'POST',
        headers,
        body: formData
    });
}

function showTab(tab) {
    document.getElementById('login-form').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('register-form').style.display = tab === 'register' ? 'block' : 'none';
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    event.target.classList.add('active');
}

function showCreateRoom() {
    document.getElementById('modal-title').textContent = 'Create Room';
    document.getElementById('modal-body').innerHTML = `
        <input id="room-name" placeholder="Room name">
        <input id="room-desc" placeholder="Description">
        <select id="room-type" style="width:100%;padding:8px;margin-bottom:8px;background:#0f3460;color:white;border:none;border-radius:4px">
            <option value="PUBLIC">Public</option>
            <option value="PRIVATE">Private</option>
        </select>
        <button onclick="createRoom()">Create</button>
    `;
    document.getElementById('modal').style.display = 'flex';
}

async function createRoom() {
    const name = document.getElementById('room-name').value;
    const description = document.getElementById('room-desc').value;
    const type = document.getElementById('room-type').value;
    try {
        await apiCall('POST', '/rooms', { name, description, type });
        closeModal();
        loadRooms();
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

function showChangePassword() {
    document.getElementById('modal-title').textContent = 'Change Password';
    document.getElementById('modal-body').innerHTML = `
        <input type="password" id="cp-current" placeholder="Current password">
        <input type="password" id="cp-new" placeholder="New password (min 6 chars)">
        <button onclick="changePassword()">Change</button>
    `;
    document.getElementById('modal').style.display = 'flex';
}

async function changePassword() {
    const currentPassword = document.getElementById('cp-current').value;
    const newPassword = document.getElementById('cp-new').value;
    try {
        await apiCall('POST', '/users/me/password', { currentPassword, newPassword });
        alert('Password changed successfully!');
        closeModal();
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

function showDeleteAccount() {
    document.getElementById('modal-title').textContent = 'Delete Account';
    document.getElementById('modal-body').innerHTML = `
        <p style="color:#ff6b6b;margin-bottom:12px">This will permanently delete your account and all your rooms!</p>
        <button onclick="deleteAccount()" style="background:#e74c3c">Delete my account</button>
    `;
    document.getElementById('modal').style.display = 'flex';
}

async function deleteAccount() {
    if (!confirm('Are you absolutely sure?')) return;
    try {
        await apiCall('DELETE', '/users/me');
        alert('Account deleted.');
        logout();
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

function showAddFriend() {
    document.getElementById('modal-title').textContent = 'Add Friend';
    document.getElementById('modal-body').innerHTML = `
        <input id="friend-username" placeholder="Enter username">
        <button onclick="sendFriendRequest()">Send Request</button>
        <hr style="margin:12px 0;border-color:#0f3460">
        <h4 style="margin-bottom:8px;color:#888">Pending Requests</h4>
        <div id="pending-list"></div>
    `;
    document.getElementById('modal').style.display = 'flex';
    loadPendingRequests();
}

async function sendFriendRequest() {
    const username = document.getElementById('friend-username').value.trim();
    if (!username) return;
    try {
        const user = await apiCall('GET', `/users/${username}`);
        await apiCall('POST', `/friends/request/${user.id}`);
        alert('Friend request sent!');
        document.getElementById('friend-username').value = '';
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function loadPendingRequests() {
    try {
        const pending = await apiCall('GET', '/friends/pending');
        const list = document.getElementById('pending-list');
        if (!list) return;
        list.innerHTML = '';
        if (!Array.isArray(pending) || pending.length === 0) {
            list.innerHTML = '<p style="color:#888;font-size:13px">No pending requests</p>';
            return;
        }
        pending.forEach(f => {
            const div = document.createElement('div');
            div.style = 'display:flex;justify-content:space-between;align-items:center;margin-bottom:6px';
            div.innerHTML = `
                <span>${f.sender.username}</span>
                <div>
                    <button onclick="acceptFriend(${f.id})" style="background:#51cf66;border:none;color:white;padding:3px 8px;border-radius:3px;cursor:pointer;margin-right:4px">✓</button>
                    <button onclick="declineFriend(${f.id})" style="background:#e74c3c;border:none;color:white;padding:3px 8px;border-radius:3px;cursor:pointer">✗</button>
                </div>
            `;
            list.appendChild(div);
        });
    } catch (e) {
        console.error('Error loading pending:', e);
    }
}

async function acceptFriend(friendshipId) {
    try {
        await apiCall('POST', `/friends/accept/${friendshipId}`);
        alert('Friend added!');
        loadPendingRequests();
        loadFriends();
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function declineFriend(friendshipId) {
    try {
        await apiCall('DELETE', `/friends/${friendshipId}`);
        loadPendingRequests();
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

function showRoomSearch() {
    document.getElementById('modal-title').textContent = 'Browse Public Rooms';
    document.getElementById('modal-body').innerHTML = `
        <div style="display:flex;gap:8px;margin-bottom:12px">
            <input id="room-search-input" placeholder="Search rooms..." style="flex:1" oninput="searchRooms()">
            <button onclick="searchRooms()">Search</button>
        </div>
        <div id="room-search-results"></div>
    `;
    document.getElementById('modal').style.display = 'flex';
    searchRooms();
}

async function searchRooms() {
    try {
        const rooms = await apiCall('GET', '/rooms');
        const query = document.getElementById('room-search-input')?.value.toLowerCase() || '';
        const results = document.getElementById('room-search-results');
        if (!results) return;
        const filtered = Array.isArray(rooms) ? rooms.filter(r =>
            r.name.toLowerCase().includes(query) ||
            (r.description && r.description.toLowerCase().includes(query))
        ) : [];
        if (filtered.length === 0) {
            results.innerHTML = '<p style="color:#888;font-size:13px">No rooms found</p>';
            return;
        }
        results.innerHTML = '';
        filtered.forEach(room => {
            const div = document.createElement('div');
            div.style = 'display:flex;justify-content:space-between;align-items:center;padding:8px;background:#0f3460;border-radius:4px;margin-bottom:6px';
            div.innerHTML = `
                <div>
                    <div style="font-weight:bold"># ${room.name}</div>
                    <div style="font-size:12px;color:#888">${room.description || 'No description'}</div>
                </div>
                <button onclick="joinAndOpen(${room.id})" style="background:#7c83fd;border:none;color:white;padding:4px 10px;border-radius:3px;cursor:pointer">Join</button>
            `;
            results.appendChild(div);
        });
    } catch (e) {
        console.error('Search error:', e);
    }
}

async function joinAndOpen(roomId) {
    try {
        await apiCall('POST', `/rooms/${roomId}/join`);
    } catch (e) {
        // Already a member — continue
    }
    closeModal();
    loadRooms();
}

function showSessions() {
    document.getElementById('modal-title').textContent = 'Active Sessions';
    document.getElementById('modal-body').innerHTML = `<div id="sessions-list">Loading...</div>`;
    document.getElementById('modal').style.display = 'flex';
    loadSessions();
}

async function loadSessions() {
    try {
        const sessions = await apiCall('GET', '/users/me/sessions');
        const list = document.getElementById('sessions-list');
        if (!list) return;
        if (!Array.isArray(sessions) || sessions.length === 0) {
            list.innerHTML = '<p style="color:#888">No active sessions</p>';
            return;
        }
        list.innerHTML = '';
        sessions.forEach(s => {
            const div = document.createElement('div');
            div.style = 'padding:8px;background:#0f3460;border-radius:4px;margin-bottom:6px';
            div.innerHTML = `
                <div style="font-size:12px;color:#888">${s.ipAddress || 'Unknown IP'}</div>
                <div style="font-size:12px;color:#888;margin-bottom:4px">${s.userAgent ? s.userAgent.substring(0, 50) + '...' : 'Unknown browser'}</div>
                <button onclick="deleteSession(${s.id})" style="background:#e74c3c;border:none;color:white;padding:2px 8px;border-radius:3px;cursor:pointer;font-size:11px">Logout this session</button>
            `;
            list.appendChild(div);
        });
    } catch (e) {
        console.error('Error loading sessions:', e);
    }
}

async function deleteSession(sessionId) {
    try {
        await apiCall('DELETE', `/users/me/sessions/${sessionId}`);
        loadSessions();
    } catch (e) {
        alert('Error: ' + e.message);
    }
}