-- ============================================================
-- FULLTEXT Index for Resilience Fallback Search
-- ============================================================
-- When the vector database (PgVector) is unavailable, the system
-- degrades to MySQL FULLTEXT search on extracted_content.
--
-- Prerequisites:
--   - MySQL 5.6+ with InnoDB engine (supports FULLTEXT on InnoDB)
--   - The table multimodal_documents must exist
--
-- Run this migration manually or via your migration tool:
--   mysql -u root -p knowledge_management < add_fulltext_index.sql
-- ============================================================

-- Create FULLTEXT index with ngram parser for Chinese+English mixed content
-- ngram parser tokenizes by character n-grams, which is essential for CJK text.
-- May take time on large tables.
ALTER TABLE multimodal_documents
    ADD FULLTEXT INDEX ft_extracted_content (extracted_content) WITH PARSER ngram;

-- ============================================================
-- Verification (run after migration):
-- ============================================================
-- SHOW INDEX FROM multimodal_documents WHERE Index_type = 'FULLTEXT';
--
-- SELECT id, file_name,
--        MATCH(extracted_content) AGAINST('测试关键词' IN BOOLEAN MODE) AS relevance
-- FROM multimodal_documents
-- WHERE MATCH(extracted_content) AGAINST('测试关键词' IN BOOLEAN MODE)
-- ORDER BY relevance DESC
-- LIMIT 10;