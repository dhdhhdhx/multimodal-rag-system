# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is a **multimodal RAG (Retrieval-Augmented Generation) knowledge management system** that supports text, images, audio, and video documents. It uses vector embeddings for semantic search and AI-powered question answering.

**Architecture:**
- **Frontend**: Vue.js 3 + TypeScript (Vite, Element Plus UI)
- **Backend**: Java 17 + Spring Boot 3.4.1 + Spring AI 1.0.0-M4
- **Vector DB**: PostgreSQL with pgvector extension (1024-dim embeddings)
- **Metadata DB**: MySQL 8.0
- **AI Service**: Python FastAPI service for multimodal embeddings (CLIP, Whisper, Transformers)
- **Authentication**: JWT with Spring Security
- **Storage**: Alibaba Cloud OSS (optional) or local `uploads/` directory

## Development Commands

### Frontend (Vue.js)
```bash
cd frontend
npm install
npm run dev          # Dev server on 127.0.0.1:8888 (proxies /api to backend:8080)
npm run build        # Production build (TypeScript check + Vite build)
npm run preview      # Preview production build
```

### Backend (Spring Boot)
```bash
cd backend
mvn clean install    # Build project
mvn spring-boot:run  # Run development server on port 8080
mvn package          # Create executable JAR
java -jar target/*.jar  # Run JAR
```

### Python AI Service
```bash
cd python-services
python -m venv venv
venv\Scripts\activate      # Windows
source venv/bin/activate   # Linux/Mac
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Docker (All Services)
```bash
docker-compose up -d      # Start all services
docker-compose down       # Stop all services
```

### Testing & Data Generation
```bash
# Generate test data
cd scripts
python generate_test_data.py

# JMeter load testing
jmeter -n -t scripts/load_test.jmx -l results/test_results.jtl -e -o results/html_report
```

## Architecture & Data Flow

### Document Upload Pipeline
1. **Upload**: User uploads file via frontend → saved to `uploads/` or OSS
2. **Modality Detection**: File extension determines type (TEXT/IMAGE/AUDIO/VIDEO)
3. **Content Processing**:
   - **TEXT**: Apache Tika parsing → chunking (800 chars, 150 overlap) → embedding
   - **IMAGE**: CLIP embedding (512-dim → aligned to 384-dim) + tags
   - **AUDIO**: Whisper transcription → text chunking → embedding
   - **VIDEO**: Keyframe extraction → CLIP embedding → tags
4. **Vector Storage**: Embeddings stored in PGVector with metadata
5. **Metadata**: Document info stored in MySQL

### RAG Query Pipeline
1. **User Query**: Submitted via chat interface
2. **Vector Search**: Searches user's private docs (top 5) + public docs (top 3)
3. **Context Assembly**: Merges results, builds context (max 6000 chars)
4. **LLM Generation**: Sends query + context to LLM (Qwen/compatible)
5. **Response**: Returns answer with source citations

### Key Services

**RagService** (`backend/src/main/java/com/multimodal/rag/service/RagService.java`):
- Main RAG orchestration
- Searches user documents + public documents
- Builds context with max length limit
- Returns answer with source tracking

**KnowledgeService** (`backend/src/main/java/com/multimodal/rag/service/KnowledgeService.java`):
- Document upload and processing
- Modality detection and routing
- Integration with Python service for non-text files

**VectorStoreService** (`backend/src/main/java/com/multimodal/rag/service/VectorStoreService.java`):
- Wraps Spring AI PGVectorStore
- User-scoped search via metadata filters
- Retry logic for transient embedding API errors

**MultimodalEmbeddingService** (`backend/src/main/java/com/multimodal/rag/service/MultimodalEmbeddingService.java`):
- File type detection
- Routes to appropriate embedding method

## Configuration

### Required Environment Variables
- `OPENAI_API_KEY` - For LLM and text embeddings (Alibaba Dashscope/Qwen)
- `JWT_SECRET` - For token signing (min 256 bits)

### Optional Environment Variables
- `SPRING_DATASOURCE_URL` - MySQL connection (default: `jdbc:mysql://127.0.0.1:3307/knowledge_management`)
- `SPRING_AI_VECTORSTORE_PGVECTOR_DATASOURCE_URL` - PGVector connection (default: `jdbc:postgresql://127.0.0.1:5432/vector_db`)
- `MULTIMODAL_PYTHON_SERVICE_BASE_URL` - Python service URL (default: `http://localhost:8000`)
- `ALIYUN_OSS_*` - For cloud storage (otherwise uses local `uploads/`)

### Application Structure
```
backend/
├── src/main/java/com/multimodal/rag/
│   ├── controller/          # REST endpoints
│   ├── service/             # Business logic
│   ├── repository/          # JPA repositories
│   ├── model/               # JPA entities
│   ├── config/              # Spring configuration
│   ├── security/            # JWT + Security
│   └── client/              # External API clients
├── src/main/resources/
│   └── application.yml      # Main configuration

frontend/
├── src/
│   ├── views/               # Vue components (public/, user/, admin/)
│   ├── api.ts               # Axios client with /api proxy
│   ├── router.ts            # Vue Router config
│   └── main.ts              # App entry point
└── vite.config.ts           # Dev server config (port 8888)

python-services/
├── main.py                  # FastAPI app
├── models/                  # Model handlers (text, image, audio, video)
├── utils/                   # Vector alignment, utilities
└── requirements.txt         # Python dependencies
```

## Authentication & Authorization

- **JWT**: Access tokens (1 hour) + Refresh tokens (7 days)
- **Roles**: USER, ADMIN
- **Public Endpoints**: `/api/auth/**`, `/api/knowledge/public/**`
- **Protected Endpoints**: All other `/api/*` require authentication
- **Admin Endpoints**: `/api/admin/**` require ADMIN role

## Important Constraints

### Spring AI Version Limitation
Spring AI 1.0.0-M4 does not support custom embeddings directly. The `addDocumentWithEmbedding()` method in `VectorStoreService` accepts embeddings but currently uses Spring AI's default embedding service. Future upgrade needed for full multimodal embedding support.

### Vector Search Filtering
PGVector metadata filtering uses string comparison for userId values due to Spring AI M4's filter DSL behavior. Filter expression: `"userId == '" + userId + "'"`

### File Upload Limits
- Max file size: 200MB (configured in `application.yml`)
- Chunking: 800 characters with 150 char overlap for text documents
- Vector dimensions: 1024 (configurable)

## Database Schema

**MySQL Tables:**
- `user` - User accounts with roles
- `multimodal_document` - File metadata and content
- `chat_session` / `chat_message` - Conversation history
- `document_access_log` - Usage analytics
- `query_log` - Query statistics

**PostgreSQL (PGVector):**
- `vector_store` - Embeddings with metadata (JSONB)

## Supported File Types

- **Text**: PDF, DOC, DOCX, TXT, MD, RTF
- **Images**: JPG, JPEG, PNG, GIF, BMP, WEBP, TIFF
- **Audio**: MP3, WAV, M4A, FLAC, OGG, AAC
- **Video**: MP4, AVI, MOV, WMV, FLV, MKV, WEBM
