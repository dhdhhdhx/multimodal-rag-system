-- PostgreSQL + PgVector Schema
-- Database: vector_db
-- PostgreSQL version: 15.4
-- Generated: 2026-06-03 (schema only, no data)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

-- ----------------------------
-- Extensions
-- ----------------------------
CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;

SET default_tablespace = '';
SET default_table_access_method = heap;

-- ----------------------------
-- Table structure for vector_store
-- ----------------------------
CREATE TABLE public.vector_store (
    id uuid DEFAULT public.uuid_generate_v4() NOT NULL,
    content text,
    metadata json,
    embedding public.vector(1024)
);

ALTER TABLE public.vector_store OWNER TO postgres;

-- ----------------------------
-- Index for vector similarity search (HNSW)
-- ----------------------------
-- Run after inserting data:
-- CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
--     ON public.vector_store
--     USING hnsw (embedding vector_cosine_ops)
--     WITH (m = 16, ef_construction = 64);

-- ----------------------------
-- Index for metadata queries
-- ----------------------------
-- CREATE INDEX IF NOT EXISTS vector_store_metadata_idx
--     ON public.vector_store USING gin (metadata);