# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Multimodal RAG (Retrieval-Augmented Generation) Knowledge Management System** that enables users to upload, search, and chat with documents across multiple modalities (text, images, audio, video). The system uses vector embeddings and LLM-based question answering.

### Architecture

The system is composed of four main services:

1. **Java Backend** (Spring Boot 3.4.1, Java 17)
   - REST API serving at port 8080
   - Handles business logic, authentication, document management, topic system
   - Integrates with MySQL for metadata and PostgreSQL+PgVector for vector storage
   - Uses Spring AI for LLM integration (OpenAI-compatible API)
   - Uses Redis for view count buffering and caching

2. **Python AI Service** (FastAPI)
   - Serves at port 8000
   - Provides multimodal embedding capabilities:
     - Text: sentence-transformers (all-MiniLM-L6-v2, 384-dim)
     - Images: CLIP (vit-base-patch32, 512-dim)
     - Audio: Whisper (base) transcription + text embedding
     - Video: keyframe extraction + CLIP
   - Lazy-loads models on first request to avoid startup delays

3. **Vue Frontend** (Vue 3 + TypeScript + Element Plus)
   - Serves at port 5174 (Docker) or 8888 (dev)
   - Vite dev server proxies `/api` to backend at 8080

4. **Databases**
   - MySQL (port 3308): User data, document metadata, topics, annotations, chat sessions
   - PostgreSQL+PgVector (port 15432): Vector embeddings for semantic search
   - Redis (port 6379): View count buffering, session cache

### Service Communication

The Java backend calls the Python service via `PythonEmbeddingClient` (backend/src/main/java/com/multimodal/rag/client/PythonEmbeddingClient.java) to:
- Generate embeddings for uploaded files
- Transcribe audio content

## Development Commands

### Start Full Stack (Docker)

```bash
# From project root
docker-compose up --build
```

This starts all services at:
- Backend: http://localhost:8082
- Frontend: http://localhost:5174
- Python Service: http://localhost:8000

### Individual Services

**Backend (Java/Spring Boot)**
```bash
cd backend
mvn clean package -DskipTests
java -jar target/*.jar
# Runs on http://localhost:8080
```

**Python AI Service**
```bash
cd python-services
python -m venv venv
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

**Frontend (Vue)**
```bash
cd frontend
npm install
npm run dev     # Serves on http://localhost:8888
npm run build   # Production build
```

### Databases

**MySQL**
```bash
# Via Docker Compose (port 3308)
docker exec -it multimodal-mysql mysql -uroot -p
# Database: knowledge_management
```

**PostgreSQL+PgVector**
```bash
# Via Docker Compose (port 15432)
docker exec -it multimodal-pgvector psql -U user -d vector_db
```

## Key Configuration Files

- `docker-compose.yml`: Orchestrates all services
- `backend/src/main/resources/application.yml`: Backend configuration (databases, API keys, timeouts)
- `backend/pom.xml`: Maven dependencies
- `frontend/vite.config.ts`: Vite dev server with API proxy to port 8080
- `python-services/requirements.txt`: Python dependencies

## Environment Variables (Important)

The backend requires these environment variables (set in `application.yml` or via shell):

- `SPRING_DATASOURCE_URL`: MySQL connection string
- `SPRING_AI_VECTORSTORE_PGVECTOR_DATASOURCE_URL`: PostgreSQL connection string
- `MULTIMODAL_PYTHON_SERVICE_BASE_URL`: Python service URL (default: http://localhost:8000)
- `OPENAI_API_KEY`: For LLM chat (can use Alibaba DashScope via base-url override)
- `ALIYUN_OSS_*`: For cloud storage (optional)
- `SPRING_DATA_REDIS_*`: Redis connection (used by ViewCountBufferService)

## Critical Architecture Details

### RAG Flow

1. User uploads document → Backend extracts content via Apache Tika
2. Backend calls Python service for multimodal embedding
3. Embedding stored in PgVector for semantic search
4. Metadata stored in MySQL
5. User queries → Backend retrieves similar documents via vector + keyword search
6. Context + query sent to LLM → Response returned with sources

### Search Strategy (RagService.java)

The system uses **hybrid search** combining:
- Vector similarity search (PgVector)
- Keyword search (MySQL LIKE queries)
- Results merged and deduplicated
- Premium users get more results (8 vs 5)

### Topic System

Topic feature with hierarchical structure, subscription, and recommendation:

**Backend models:**
- `Topic` — topics with name, description, parentId (1-level hierarchy), ownerId, isPublic
- `TopicDocument` — many-to-many association between topics and documents
- `TopicSubscription` — user subscriptions to public topics

**Backend APIs** (`/api/topics`):
- CRUD: POST, GET, PUT, DELETE
- Document association: POST/DELETE `/{id}/documents/{docId}`, GET `/{id}/documents/paged`
- Subscription: POST/DELETE `/{id}/subscribe`, GET `/{id}/subscription-status`
- Discovery: GET `/hot`, `/recommended`, `/public/paged`, `/subscribed`
- Tree structure: GET `/tree`

**Frontend pages:**
- `/topics` — TopicsPage.vue (public topic discovery with search)
- `/topic/:id` — TopicDetailPage.vue (topic detail + subscribe + document list)
- `/manage` — topic sidebar in ManagePage.vue (CRUD for own topics)
- `/admin/topics` — AdminTopics.vue (admin management)

**Recommendation algorithm** (TopicService.java):
- Collects user's document tags as interest profile
- Matches interest tags against public topic names/descriptions
- Falls back to hot topics if no user tags found
- Ranks by: relevance score → subscriber count

### View Count Buffering (ViewCountBufferService.java)

Uses Redis INCR to buffer view increments, flushes to MySQL every 60 seconds via @Scheduled task. Falls back to direct MySQL write if Redis is unavailable.

### Authentication

- JWT-based authentication via `JwtTokenProvider`
- Default secret: `multimodal-rag-secret-key-change-in-production-min-256-bits`
- Roles: USER, PREMIUM, ADMIN
- PREMIUM/ADMIN can create topics

### File Preview

- PDF and Office files trigger download (Content-Disposition: attachment)
- Images, audio, video open in a new tab with HTML wrapper
- File preview uses streaming (HttpServletResponse) for performance
- Chinese filename encoding uses RFC 5987 format

### Python Model Caching

Models are cached in `python-services/.model-cache/huggingface` to avoid re-downloading.
Uses `HF_ENDPOINT=https://hf-mirror.com` for Chinese network environments.

### File Upload Limits

Max file size: 200MB (configured in `application.yml`)

### JPA Entity Serialization

When returning entities from API, prefer converting to `Map<String, Object>` instead of returning JPA entities directly. This avoids `ByteBuddyInterceptor` serialization errors from lazy-loaded relationships. See TopicController.getDocumentsByTopic() for the pattern.

## Directory Structure

```
multimodal-rag-system/
├── backend/                    # Java Spring Boot backend
│   ├── src/main/java/com/multimodal/rag/
│   │   ├── client/            # PythonEmbeddingClient (calls Python service)
│   │   ├── config/            # Spring configurations (Security, VectorStore, Redis)
│   │   ├── controller/        # REST endpoints (Knowledge, Topic, Chat, Admin, etc.)
│   │   ├── model/             # JPA entities (User, MultimodalDocument, Topic, TopicDocument, TopicSubscription)
│   │   ├── repository/        # JPA repositories
│   │   ├── security/          # JWT auth (JwtTokenProvider, JwtAuthenticationFilter)
│   │   └── service/           # Business logic (RagService, TopicService, ViewCountBufferService, etc.)
│   └── pom.xml
├── python-services/            # FastAPI AI service
│   ├── main.py                # FastAPI app with embedding endpoints
│   ├── models/                # TextEmbedder, ImageEmbedder, AudioTranscriber, VideoProcessor
│   ├── utils/                 # VectorAligner for dimension alignment
│   └── requirements.txt
├── frontend/                   # Vue 3 + TypeScript UI
│   ├── src/
│   │   ├── api.ts              # Axios instance + topicApi helpers
│   │   ├── router.ts           # Route definitions
│   │   ├── views/              # Pages (HomePage, TopicDetailPage, TopicsPage, ManagePage, etc.)
│   │   ├── views/admin/        # Admin pages (AdminTopics, AdminUsers, AdminDocuments, etc.)
│   │   ├── components/         # Shared components (ArticleCard, TopicCard, PageHeader)
│   │   └── utils/              # Auth utilities
│   ├── package.json
│   └── vite.config.ts
└── docker-compose.yml          # Full stack orchestration
```
