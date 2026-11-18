```markdown
# UniVoice — Real-Time Multilingual Chat Application

UniVoice is a full-stack, WhatsApp-style real-time messaging platform featuring automatic, per-recipient language translation for 1-on-1 direct chats and group conversations. Built on an event-driven Java Spring Boot microservices architecture and a React + Vite frontend, UniVoice allows each participant to send messages in their native tongue or transliterated regional script (such as Tanglish) while recipients receive messages translated into their chosen language in real time.

---

## Live Production Architecture

* **Frontend:** React (Vite) hosted on **Vercel** (`https://univoice-frontend-gilt.vercel.app`)
* **Backend Microservices:** 4 Spring Boot 3.3.4 (Java 21) containerized services hosted on **Render**:
  1. `univoice-api-gateway` (`https://univoice-api-gateway.onrender.com`) — Spring Cloud Gateway handling REST routing and global CORS.
  2. `univoice-multilingual-chat` / `user-service` (`https://univoice-multilingual-chat.onrender.com`) — OTP authentication, JWT issuance, user profiles, contacts, and per-chat language preferences.
  3. `univoice-chat-service` (`https://univoice-chat-service.onrender.com`) — STOMP over SockJS WebSockets, direct/group messaging, read receipts, and Kafka event publishing/consumption.
  4. `univoice-translation-worker` (`https://univoice-translation-worker.onrender.com`) — Asynchronous Kafka consumer performing AI translation and publishing translated payloads.
* **Event Streaming:** **Aiven Kafka** (SASL/SSL `SCRAM-SHA-256` with PEM truststore)
* **Persistence & Caching:** **MySQL** (JPA/Hibernate) & **Upstash Redis**

---

## End-to-End Transaction & Message Flow Map

### 1. Authentication & Session Flow
```text
[React Frontend (Vercel)]
       │  POST /api/v1/auth/send-otp  &  POST /api/v1/auth/login
       ▼
[API Gateway (Render)] ──► routes /api/v1/auth/** ──► [User Service (Render)]
                                                             │
                                      ┌──────────────────────┴──────────────────────┐
                                      ▼                                             ▼
                         [Upstash Redis (OTP TTL)]                     [MySQL (User & Preferences)]
                                      │
                                      ▼
                       Returns JWT Access Token + Profile

```

### 2. Real-Time Messaging & Translation Pipeline

```text
[Sender Client]
   │
   ├─► 1. STOMP / SockJS WebSocket (/ws-chat) ──► [Chat Service]
   │                                                   │
   │   2. Persists raw message & emits delivery tick   ├─► [MySQL DB]
   │                                                   │
   │   3. Publishes raw message event                  ▼
   │                                     [Aiven Kafka Topics]
   │                                     • chat-messages-raw
   │                                     • group-messages-raw
   │                                                   │
   │   4. Consumes raw event                           ▼
   │                                     [Translation Worker Service]
   │                                       ├─► Fetches recipient target language
   │                                       │   (Redis Cache / User Service)
   │                                       ├─► Calls AI Translation Engines
   │                                       │   • Sarvam AI (Indian Regional Languages)
   │                                       │   • Groq LLM (Global / European Languages)
   │                                       │
   │   5. Publishes translated event                   ▼
   │                                     [Aiven Kafka Topics]
   │                                     • chat-messages-translated
   │                                     • group-messages-translated
   │                                                   │
   │   6. Consumes translated event                    ▼
   │                                           [Chat Service]
   │                                                   │
   └─► 7. Pushes translated message via STOMP ◄────────┘
          (/user/queue/messages or /topic/group.*)
                  │
                  ▼
          [Recipient Client]
   (Displays Original + Translated Text)

```

---

## Repository Structure

```text
UniVoice/
├── UniVoice-backend/
│   ├── api-gateway/                 # Spring Cloud Gateway (REST routing & CORS)
│   ├── user-service/                # Auth, OTP, JWT, Contacts & Language Preferences
│   ├── chat-service/                # WebSockets (STOMP/SockJS), Chats, Groups & Receipts
│   └── translation-worker-service/  # Kafka Consumer & Sarvam/Groq Translation Engine
└── UniVoice-frontend/               # React + Vite + Tailwind + SockJS/STOMP Client
    └── src/
        └── services/
            ├── api.js               # Axios client with JWT Bearer interceptor
            └── websocket.js         # STOMP over SockJS real-time client

```

---

## Key Features

* **Dual-Engine Multilingual Translation:** Routes Indian regional languages (Tamil, Hindi, Telugu, etc.) through **Sarvam AI** and global languages (English, German, French, Spanish, etc.) through **Groq**, supporting native scripts as well as romanized/transliterated input (e.g., Tanglish).
* **Per-Conversation Language Overrides:** Users set a default native & incoming language in their profile while dynamically overriding target languages per direct chat or group conversation.
* **Real-Time Group & Direct Messaging:** Sub-second STOMP/SockJS delivery with online presence indicators, single/double delivery ticks (`✓` / `✓✓`), and group translation fan-out.
* **Passwordless OTP Authentication:** Phone-number-based OTP verification backed by Redis TTL and stateless JWT session tokens.

---

## Environment Variables Reference

### Frontend (`UniVoice-frontend` on Vercel)

| Variable | Description | Production Value |
| --- | --- | --- |
| `VITE_API_URL` | Base REST URL routed through API Gateway | `https://univoice-api-gateway.onrender.com/api/v1` |
| `VITE_WS_URL` | Direct SockJS/STOMP endpoint on Chat Service | `https://univoice-chat-service.onrender.com/ws-chat` |

### Backend Microservices (`UniVoice-backend` on Render)

| Service | Required Environment Variables |
| --- | --- |
| `api-gateway` | `USER_SERVICE_URL`, `CHAT_SERVICE_URL`, `CHAT_SERVICE_WS_URL`, `FRONTEND_URL` |
| `user-service` | `DB_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `FRONTEND_URL` |
| `chat-service` | `DB_PASSWORD`, `KAFKA_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `FRONTEND_URL` |
| `translation-worker` | `DB_PASSWORD`, `KAFKA_PASSWORD`, `REDIS_PASSWORD`, `SARVAM_API_KEY`, `GROQ_API_KEY`, `USER_SERVICE_URL` |

---

## Local Development Setup

1. **Prerequisites:** Java 21, Maven, Node.js 18+, MySQL, Redis, and a Kafka broker.
2. **Start Backend Services:**
```bash
cd UniVoice-backend/<service-name>
./mvnw spring-boot:run

```


3. **Start React Frontend:**
```bash
cd UniVoice-frontend
npm install
npm run dev

```



```

```
