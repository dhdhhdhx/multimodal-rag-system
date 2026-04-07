package com.multimodal.rag.service;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.repository.DocumentAccessLogRepository;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

    private final MultimodalDocumentRepository documentRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final VectorStoreService vectorStoreService;

    public Map<String, List<MultimodalDocument>> getDiscoveryData(Long userId, int limit) {
        log.info("Fetching discovery data for user: {}", userId);
        Map<String, List<MultimodalDocument>> discovery = new HashMap<>();
        Pageable topN = PageRequest.of(0, limit);

        // 1. Featured (Public Seed Documents)
        List<MultimodalDocument> featured = documentRepository.findTopFeatured(topN);
        log.info("Found {} featured documents for discovery", featured.size());
        discovery.put("featured", featured);

        // 2. Collaborative Filtering (People like you also viewed)
        List<MultimodalDocument> collaborative = recommendByCollaborativeFiltering(userId, limit);
        log.info("Found {} collaborative recommendations", collaborative.size());
        discovery.put("collaborative", collaborative);

        // 3. Guess You Like (Semantic Recommendations)
        List<MultimodalDocument> guessYouLike = recommendDocuments(userId, limit);
        log.info("Found {} semantic recommendations", guessYouLike.size());
        discovery.put("guessYouLike", guessYouLike);

        // 4. New Arrivals (Latest public/system documents)
        List<MultimodalDocument> newArrivals = documentRepository.findTopFeatured(PageRequest.of(0, 5));
        log.info("Found {} new arrival documents", newArrivals.size());
        discovery.put("newArrivals", newArrivals);

        // 5. Recent Documents (User's specific recent uploads)
        List<MultimodalDocument> recent = documentRepository.findTop5ByUser_IdOrderByUploadTimeDesc(userId);
        log.info("Found {} recent documents for user statistics", recent.size());
        discovery.put("recentDocuments", recent);

        return discovery;
    }

    public List<MultimodalDocument> recommendByCollaborativeFiltering(Long userId, int limit) {
        log.info("Generating User-based Collaborative Filtering recommendations for user: {}", userId);
        
        // 1. Get current user's history
        List<Long> myDocIds = accessLogRepository.findRecentViewedDocumentIds(userId, PageRequest.of(0, 20));
        if (myDocIds.isEmpty()) return new ArrayList<>();

        // 2. Find similar users (who viewed same documents)
        List<Long> similarUserIds = accessLogRepository.findOtherUsersBySharedDocuments(myDocIds, userId);
        if (similarUserIds.isEmpty()) return new ArrayList<>();

        // 3. Find documents viewed by these users but not by me
        List<Object[]> recommendedData = accessLogRepository.findRecommendedDocumentIdsBySimilarUsers(
            similarUserIds, myDocIds, PageRequest.of(0, limit)
        );

        if (recommendedData.isEmpty()) return new ArrayList<>();

        List<Long> recommendedIds = recommendedData.stream()
            .map(row -> (Long) row[0])
            .collect(Collectors.toList());

        // 4. Return actual document entities (filtering for visibility)
        return documentRepository.findAllById(recommendedIds).stream()
            .filter(doc -> doc.isShared() || (doc.getUser() != null && doc.getUser().getId().equals(userId)))
            .collect(Collectors.toList());
    }

    public List<MultimodalDocument> recommendDocuments(Long userId, int limit) {
        log.info("Generating recommendations for user: {}", userId);
        
        // 1. Get recent interactions
        List<Long> recentDocIds = accessLogRepository.findRecentViewedDocumentIds(userId, PageRequest.of(0, 5));
        
        if (recentDocIds.isEmpty()) {
            // Fallback: recommend popular public documents
            log.info("No recent interactions found for user {}, returning public featured documents", userId);
            return documentRepository.findTopFeatured(PageRequest.of(0, limit));
        }

        // 2. Get the actual document contents to build a "profile"
        List<MultimodalDocument> recentDocs = documentRepository.findAllById(recentDocIds);
        
        // 3. For each recent doc, find similar documents (Searching within User's own OR Public docs)
        Set<Long> recommendedIds = new HashSet<>();
        List<MultimodalDocument> finalRecommendations = new ArrayList<>();

        for (MultimodalDocument doc : recentDocs) {
            String seedContent = doc.getExtractedContent();
            if (seedContent != null && !seedContent.isEmpty()) {
                // Search for similar docs (top 3 for each seed) - Passing userId to ensure isolation/visibility
                List<Document> similarDocs = vectorStoreService.search(seedContent, 3, userId);
                
                for (Document sDoc : similarDocs) {
                    Object docIdObj = sDoc.getMetadata().get("documentId");
                    if (docIdObj != null) {
                        Long simId = Long.valueOf(docIdObj.toString());
                        // Don't recommend the seed itself or duplicates
                        if (!recentDocIds.contains(simId) && !recommendedIds.contains(simId)) {
                            documentRepository.findById(simId).ifPresent(foundDoc -> {
                                // Double check visibility (should be handled by vector store filter too)
                                if (foundDoc.isShared() || (foundDoc.getUser() != null && foundDoc.getUser().getId().equals(userId))) {
                                    if (finalRecommendations.size() < limit) {
                                        finalRecommendations.add(foundDoc);
                                        recommendedIds.add(simId);
                                    }
                                }
                            });
                        }
                    }
                }
            }
            if (finalRecommendations.size() >= limit) break;
        }

        // 4. If still under limit, fill with public ones
        if (finalRecommendations.size() < limit) {
             List<MultimodalDocument> fallback = documentRepository.findTopFeatured(PageRequest.of(0, 5));
             for (MultimodalDocument f : fallback) {
                 if (!recommendedIds.contains(f.getId()) && !recentDocIds.contains(f.getId())) {
                     finalRecommendations.add(f);
                     recommendedIds.add(f.getId());
                 }
                 if (finalRecommendations.size() >= limit) break;
             }
        }

        return finalRecommendations;
    }
}
