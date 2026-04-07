-- Seed more data for recommendations
USE knowledge_management;

-- 1. Make current document public so it can be 'shared'
UPDATE multimodal_documents SET is_public = 1 WHERE id = 13;

-- 2. Add some 'System' documents
INSERT INTO multimodal_documents (id, file_name, file_type, file_path, file_size, extracted_content, upload_time, status, is_public, user_id)
VALUES 
(14, 'RAG_System_Architecture.pdf', 'pdf', '/uploads/seed1.pdf', 1024, 'This document describes the overall architecture of a multimodal RAG system using Spring Boot, PgVector and FastAPI.', NOW(), 'COMPLETED', 1, 1),
(15, 'Collaborative_Filtering_Explained.docx', 'docx', '/uploads/seed2.docx', 2048, 'Collaborative filtering is a method of making automatic predictions about the interests of a user by collecting preferences from many users.', NOW(), 'COMPLETED', 1, 1);

-- 3. Clear old/bad logs
DELETE FROM document_access_logs WHERE document_id NOT IN (SELECT id FROM multimodal_documents);

-- 4. Simulate Interactions to trigger Collaborative Filtering
-- Current User (ID 1) views Document 13
INSERT INTO document_access_logs (user_id, document_id, access_type, created_at) VALUES (1, 13, 'VIEW', NOW());

-- Other User (ID 2) views Document 13 (The 'Bridge' interaction)
INSERT INTO document_access_logs (user_id, document_id, access_type, created_at) VALUES (2, 13, 'VIEW', NOW());

-- Other User (ID 2) views Document 14 (The 'Recommended' content)
INSERT INTO document_access_logs (user_id, document_id, access_type, created_at) VALUES (2, 14, 'VIEW', NOW());

-- 5. Add some 'Hot' data
INSERT INTO document_access_logs (user_id, document_id, access_type, created_at) VALUES (1, 15, 'VIEW', NOW());
INSERT INTO document_access_logs (user_id, document_id, access_type, created_at) VALUES (2, 15, 'VIEW', NOW());
