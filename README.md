# Chat App

A classic web-based online chat application built with Java Spring Boot, PostgreSQL, WebSocket/STOMP, and a vanilla JavaScript frontend.

## Repository

https://github.com/Simeon-Petrov/Chat-app__DataArt

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.2, Spring Security, JWT, Spring WebSocket/STOMP
- **Database:** PostgreSQL 16 with Flyway migrations
- **Frontend:** Vanilla JavaScript, HTML, CSS, SockJS, STOMP.js
- **Infrastructure:** Docker, Docker Compose, Nginx

## Features

- User registration and login with JWT authentication
- Persistent login across browser sessions
- Public and private chat rooms
- Room search catalog
- Real-time messaging via WebSocket/STOMP
- Message reply, edit, and delete
- File and image upload (button + paste)
- Friend requests and friend list
- Personal messaging (DM) between friends
- Online / AFK / Offline presence indicators
- Unread message indicators
- Infinite scroll for message history
- Room member management with Ban and Invite
- Active sessions panel with logout
- Password change
- Delete account
- Modal dialogs for all admin actions

## How to Run

### Prerequisites
- Docker Desktop installed and running

### Start the application

```bash
docker compose up --build
```

This will start:
- **PostgreSQL** database on port 5432
- **Spring Boot backend** on port 8080
- **Nginx frontend** on port 80

### Access the application

Open your browser and go to:
```
http://localhost
```

### Stop the application

```bash
docker compose down
```

## Project Structure

```
Task1/
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/chat/backend/
│           │   ├── config/
│           │   ├── controller/
│           │   ├── dto/
│           │   ├── model/
│           │   ├── repository/
│           │   ├── security/
│           │   ├── service/
│           │   └── websocket/
│           └── resources/
│               ├── application.yaml
│               └── db/migration/
│                   └── V1__init.sql
└── frontend/
    ├── Dockerfile
    ├── index.html
    ├── css/
    │   └── style.css
    └── js/
        ├── api.js
        ├── app.js
        └── websocket.js
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | Login |
| GET | /api/rooms | Get public rooms |
| POST | /api/rooms | Create room |
| POST | /api/rooms/{id}/join | Join room |
| POST | /api/rooms/{id}/invite/{userId} | Invite user to room |
| POST | /api/rooms/{id}/ban/{userId} | Ban user from room |
| GET | /api/rooms/{id}/members | Get room members |
| GET | /api/rooms/{id}/messages | Get messages (paginated) |
| POST | /api/friends/request/{id} | Send friend request |
| POST | /api/friends/accept/{id} | Accept friend request |
| POST | /api/users/dm/{username} | Start DM conversation |
| GET | /api/users/me/sessions | Get active sessions |
| POST | /api/users/me/password | Change password |
| DELETE | /api/users/me | Delete account |

## WebSocket

Connect to `/ws` using SockJS and STOMP protocol.

| Destination | Description |
|-------------|-------------|
| /app/chat.send | Send message |
| /app/chat.edit | Edit message |
| /app/chat.delete | Delete message |
| /app/presence | Update presence status |
| /topic/room.{id} | Subscribe to room messages |
| /topic/presence | Subscribe to presence updates |

---

## Author

Simeon Petrov
