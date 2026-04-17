package com.multimodal.rag.config;

import com.multimodal.rag.model.Permission;
import com.multimodal.rag.model.Role;
import com.multimodal.rag.model.User;
import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.repository.PermissionRepository;
import com.multimodal.rag.repository.RoleRepository;
import com.multimodal.rag.repository.UserRepository;
import com.multimodal.rag.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MultimodalDocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;
    private final VectorStoreService vectorStoreService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing system data...");

        // 1. Initialize Permissions
        Permission readPermission = createPermissionIfNotFound("READ", "DOCUMENT", "READ");
        Permission writePermission = createPermissionIfNotFound("WRITE", "DOCUMENT", "WRITE");
        Permission deletePermission = createPermissionIfNotFound("DELETE", "DOCUMENT", "DELETE");
        Permission adminPermission = createPermissionIfNotFound("ADMIN_ACCESS", "SYSTEM", "ALL");
        Permission advancedRetrievalPermission = createPermissionIfNotFound("ADVANCED_RETRIEVAL", "RAG", "SEARCH_PLUS");
        Permission priorityRecommendationPermission = createPermissionIfNotFound("PRIORITY_RECOMMENDATION", "DISCOVERY", "PRIORITY");
        Permission topicPermission = createPermissionIfNotFound("TOPIC_CREATE", "KNOWLEDGE", "CREATE_TOPIC");

        // 2. Initialize Roles
        Role adminRole = createRoleIfNotFound("ADMIN", "System Administrator", 
                Set.of(readPermission, writePermission, deletePermission, adminPermission,
                        advancedRetrievalPermission, priorityRecommendationPermission, topicPermission));
        Role userRole = createRoleIfNotFound("USER", "Standard User", 
                Set.of(readPermission, writePermission));
        Role premiumRole = createRoleIfNotFound("PREMIUM", "Premium Member",
                Set.of(readPermission, writePermission, advancedRetrievalPermission,
                        priorityRecommendationPermission, topicPermission));

        // 3. Initialize Admin User
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setFullName("System Administrator");
            admin.setEmail("admin@multimodal.com");
            admin.setIsActive(true);
        }
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRoles(new HashSet<>(Set.of(adminRole)));
        userRepository.save(admin);
        log.info("Admin user ready: admin/admin123");

        // 4. Initialize Test User
        if (!userRepository.existsByUsername("user")) {
            User testUser = new User();
            testUser.setUsername("user");
            testUser.setFullName("Test User");
            testUser.setEmail("user@test.com");
            testUser.setPasswordHash(passwordEncoder.encode("user123"));
            testUser.setIsActive(true);
            testUser.setRoles(new HashSet<>(Set.of(userRole)));
            userRepository.save(testUser);
            log.info("Created default test user: user/user123");
        }

        User premiumUser = userRepository.findByUsername("premium").orElse(null);
        if (premiumUser == null) {
            premiumUser = new User();
            premiumUser.setUsername("premium");
            premiumUser.setFullName("Premium Member");
            premiumUser.setEmail("premium@multimodal.com");
            premiumUser.setIsActive(true);
        }
        premiumUser.setPasswordHash(passwordEncoder.encode("premium123"));
        premiumUser.setRoles(new HashSet<>(Set.of(premiumRole)));
        userRepository.save(premiumUser);
        log.info("Premium user ready: premium/premium123");

        // 5. Initialize Seed Documents for Discovery Section
        initializeSeedDocuments(userRole);

        log.info("System data initialization completed.");
    }

    private void initializeSeedDocuments(Role userRole) {
        // Create a 'system' user for these documents
        User systemUser = userRepository.findByUsername("system")
                .orElseGet(() -> {
                    User user = new User();
                    user.setUsername("system");
                    user.setFullName("System Intelligence");
                    user.setEmail("system@multimodal.rag");
                    user.setPasswordHash(passwordEncoder.encode("system_secured_123"));
                    user.setIsActive(true);
                    user.setRoles(new HashSet<>(Set.of(userRole)));
                    return userRepository.save(user);
                });

        // Check if system has already uploaded documents
        List<MultimodalDocument> existingDocs = documentRepository.findByUser_Id(systemUser.getId());
        if (!existingDocs.isEmpty()) {
            // Force existing system docs to be public and add tags if missing
            boolean updated = false;
            for (MultimodalDocument d : existingDocs) {
                if (!d.isShared()) { d.setShared(true); updated = true; }
                if (d.getTags() == null || d.getTags().isEmpty()) {
                    d.setTags(inferTags(d.getFileName()));
                    updated = true;
                }
            }
            if (updated) {
                documentRepository.saveAll(existingDocs);
                log.info("Updated existing system documents (public + tags).");
            }

            // Ensure existing seed documents are indexed in the vector store
            indexExistingSeedDocs(existingDocs);

            return;
        }

        log.info("Initializing seed documents for Discovery section...");

        createSeedDoc(systemUser, "基础：什么是多模态 RAG？", "pdf", 1024L,
            "[Text Content] 多模态 RAG (Retrieval-Augmented Generation) 是一种结合了检索与生成的 AI 技术。它不仅处理文字，还能检索图像、视频和音频。通过将不同模态的内容映射到同一个向量空间，系统可以实现’以图搜文’或’以声搜影’。",
            "AI,RAG,技术笔记");

        createSeedDoc(systemUser, "CLIP 模型：连接图像与语言", "png", 2048L,
            "[Image Content] CLIP 是 OpenAI 开发的经典模型。它能将图像和描述它们的文本联系起来。在本系统中，我们使用 CLIP 来生成图像特征向量，以便您可以通过自然语言搜索您的图片库。它通过对比学习（Contrastive Learning）机制实现语义对齐。",
            "AI,深度学习,技术笔记");

        createSeedDoc(systemUser, "Whisper：让 AI 听懂你的音频", "wav", 4096L,
            "[Audio Content] OpenAI 的 Whisper 模型是目前最强大的语音转录 AI。它可以精准地将音频转化为文字，并识别多种语言。本系统集成 Whisper 用于处理您的会议录音、演讲稿，并在后台自动生成摘要。",
            "AI,语音识别,技术笔记");

        createSeedDoc(systemUser, "Llama 3：最先进的开源大模型", "txt", 512L,
            "[Text Content] Llama 3 是 Meta 发布的最新一代大语言模型。它在逻辑推理、指令遵循方面表现出色。本系统将其作为’大脑’，负责最终的回答生成。它会阅读检索到的多模态上下文，为您提供具备事实依据的专业回答。",
            "AI,大模型,技术笔记");

        createSeedDoc(systemUser, "系统使用指南：如何上传多模态文件？", "mp4", 8192L,
            "[Video Content] 您可以直接将录音、剧照或技术文档拖入侧边栏。系统后台会自动启动模态检测：对于文本文件使用 Sentence-Transformers，对于视频和图片使用 CLIP 提取关键帧特征。处理完成后，您可以在仪表盘查看到实时更新的存储占比。",
            "使用指南,RAG");

        createSeedDoc(systemUser, "深度学习：卷积神经网络 (CNN)", "png", 3072L,
            "[Image Content] CNN 是图像识别领域的基石。它模仿人类视觉系统，通过卷积层、池化层和全连接层自动提取图像特征。在多模态 RAG 中，CNN 及其变体（如 ResNet）常用于生成图像的高维表示。",
            "深度学习,AI,技术笔记");

        createSeedDoc(systemUser, "注意力机制：Transformers 的灵魂", "pdf", 1536L,
            "[Text Content] Attention is All You Need。注意力机制允许模型在处理序列时，动态地关注输入的不同部分。这在处理视频帧序列或长文本时至关重要。本系统使用的 Llama 3 核心就是基于多头注意力机制（Multi-Head Attention）构建的。",
            "深度学习,AI,技术笔记");

        log.info("Seed documents initialized successfully.");
    }

    private void createSeedDoc(User user, String name, String type, Long size, String content, String tags) {
        MultimodalDocument doc = new MultimodalDocument();
        doc.setFileName(name);
        doc.setFileType(type);
        doc.setFileSize(size);
        doc.setExtractedContent(content);
        doc.setStatus("COMPLETED");
        doc.setUser(user);
        doc.setShared(true);
        doc.setTags(tags);
        doc.setFilePath("/system/seed/" + name + "." + type);
        doc = documentRepository.save(doc);

        // Add to vector store so RAG retrieval can find it
        addToVectorStore(doc);
    }

    /**
     * Index existing seed documents into the vector store.
     * This is a one-time repair for seed documents that were created
     * before vector store integration was added.
     * We do a test search to determine if vectors already exist.
     */
    private void indexExistingSeedDocs(List<MultimodalDocument> docs) {
        if (docs.isEmpty()) return;

        // Probe the vector store to see if seed docs are already indexed
        try {
            List<org.springframework.ai.document.Document> probe =
                vectorStoreService.search("多模态 RAG", 1);
            if (!probe.isEmpty()) {
                log.info("Seed documents already exist in vector store, skipping re-index.");
                return;
            }
        } catch (Exception e) {
            log.warn("Vector store probe failed, will attempt to index seed docs: {}", e.getMessage());
        }

        int indexed = 0;
        for (MultimodalDocument doc : docs) {
            String content = doc.getExtractedContent();
            if (content != null && !content.isBlank()) {
                addToVectorStore(doc);
                indexed++;
            }
        }
        if (indexed > 0) {
            log.info("Indexed {} seed documents into vector store (first-time repair).", indexed);
        }
    }

    private void addToVectorStore(MultimodalDocument doc) {
        try {
            Map<String, Object> metadata = Map.of(
                "documentId", doc.getId(),
                "userId", doc.getUser().getId(),
                "fileName", doc.getFileName(),
                "fileType", doc.getFileType(),
                "modality", "TEXT"
            );
            vectorStoreService.addDocument(doc.getExtractedContent(), metadata);
            log.info("Indexed seed document '{}' (id={}) into vector store.", doc.getFileName(), doc.getId());
        } catch (Exception e) {
            log.warn("Failed to index seed document '{}' into vector store: {}",
                    doc.getFileName(), e.getMessage());
        }
    }

    private String inferTags(String fileName) {
        if (fileName == null) return "技术笔记";
        String lower = fileName.toLowerCase();
        if (lower.contains("rag") || lower.contains("检索")) return "AI,RAG,技术笔记";
        if (lower.contains("clip") || lower.contains("cnn") || lower.contains("深度学习")) return "深度学习,AI,技术笔记";
        if (lower.contains("whisper") || lower.contains("音频")) return "AI,语音识别,技术笔记";
        if (lower.contains("llama") || lower.contains("大模型")) return "AI,大模型,技术笔记";
        if (lower.contains("指南") || lower.contains("使用")) return "使用指南,RAG";
        if (lower.contains("attention") || lower.contains("transformer")) return "深度学习,AI,技术笔记";
        return "技术笔记";
    }

    private Permission createPermissionIfNotFound(String name, String resource, String action) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setName(name);
                    permission.setResource(resource);
                    permission.setAction(action);
                    return permissionRepository.save(permission);
                });
    }

    private Role createRoleIfNotFound(String name, String description, Set<Permission> permissions) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(description);
                    role.setPermissions(permissions);
                    return roleRepository.save(role);
                });
    }
}
