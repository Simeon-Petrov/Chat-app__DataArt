let stompClient = null;
let currentSubscription = null;

function connectWebSocket() {
    const socket = new SockJS('http://localhost:8080/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect(
        { Authorization: 'Bearer ' + token },
        function () {
            console.log('WebSocket connected');
            subscribeToPresence();
            sendPresence('ONLINE');
        },
        function (error) {
            console.log('WS error:', error);
            setTimeout(connectWebSocket, 3000);
        }
    );
}

function subscribeToRoom(roomId) {
    if (currentSubscription) {
        currentSubscription.unsubscribe();
    }
    currentSubscription = stompClient.subscribe(
        '/topic/room.' + roomId,
        function (msg) {
            const message = JSON.parse(msg.body);
            appendMessage(message);
        }
    );
}

function subscribeToPresence() {
    stompClient.subscribe('/topic/presence', function (msg) {
        const data = JSON.parse(msg.body);
        const dot = document.getElementById('dot-' + data.userId);
        if (dot) {
            dot.className = 'presence-dot ' + data.status.toLowerCase();
        }
    });
}

function sendPresence(status) {
    if (stompClient && stompClient.connected) {
        stompClient.send('/app/presence', {}, JSON.stringify({ status }));
    }
}

function sendMessageWS(roomId, content, replyToId) {
    if (!stompClient || !stompClient.connected) return;
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        roomId, content, replyToId: replyToId || null
    }));
}

let afkTimer = null;
function resetAfkTimer() {
    clearTimeout(afkTimer);
    sendPresence('ONLINE');
    afkTimer = setTimeout(() => sendPresence('AFK'), 60000);
}

document.addEventListener('mousemove', resetAfkTimer);
document.addEventListener('keypress', resetAfkTimer);