package com.multimodal.rag.service;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.DocumentAccessLogRepository;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import com.multimodal.rag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecommendationService {

    private final MultimodalDocumentRepository documentRepository;
    private final DocumentAccessLogRepository accessLogRepository;
    private final VectorStoreService vectorStoreService;
    private final UserRepository userRepository;

    public Map<String, List<MultimodalDocument>> getDiscoveryData(Long userId, int limit) {
        log.info("Fetching discovery data for user: {}", userId);
        boolean premiumUser = isPremiumUser(userId);
        int effectiveLimit = premiumUser ? Math.max(limit, 8) : limit;
        int recentLimit = premiumUser ? 8 : 5;

        Map<String, List<MultimodalDocument>> discovery = new HashMap<>();
        Pageable topN = PageRequest.of(0, effectiveLimit);

        List<MultimodalDocument> featured = documentRepository.findTopFeatured(topN);
        log.info("Found {} featured documents for discovery", featured.size());
        discovery.put("featured", featured);

        List<MultimodalDocument> collaborative = recommendByCollaborativeFiltering(userId, effectiveLimit);
        log.info("Found {} collaborative recommendations", collaborative.size());
        discovery.put("collaborative", collaborative);

        List<MultimodalDocument> guessYouLike = recommendDocuments(userId, effectiveLimit);
        log.info("Found {} semantic recommendations", guessYouLike.size());
        discovery.put("guessYouLike", guessYouLike);

        List<MultimodalDocument> newArrivals = documentRepository.findTopFeatured(PageRequest.of(0, premiumUser ? 8 : 5));
        log.info("Found {} new arrival documents", newArrivals.size());
        discovery.put("newArrivals", newArrivals);

        List<MultimodalDocument> recent = documentRepository.findByUser_Id(
                userId,
                PageRequest.of(0, recentLimit, Sort.by(Sort.Direction.DESC, "uploadTime"))
        ).getContent();
        log.info("Found {} recent documents for user statistics", recent.size());
        discovery.put("recentDocuments", recent);

        return discovery;
    }

    public List<MultimodalDocument> recommendByCollaborativeFiltering(Long userId, int limit) {
        log.info("Generating User-based Collaborative Filtering recommendations for user: {}", userId);

        List<Long> myDocIds = accessLogRepository.findRecentViewedDocumentIds(userId, PageRequest.of(0, 20));
        if (myDocIds.isEmpty()) return new ArrayList<>();

        List<Long> similarUserIds = accessLogRepository.findOtherUsersBySharedDocuments(myDocIds, userId);
        if (similarUserIds.isEmpty()) return new ArrayList<>();

        List<Object[]> recommendedData = accessLogRepository.findRecommendedDocumentIdsBySimilarUsers(
                similarUserIds, myDocIds, PageRequest.of(0, limit)
        );

        if (recommendedData.isEmpty()) return new ArrayList<>();

        List<Long> recommendedIds = recommendedData.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toList());

        return documentRepository.findAllById(recommendedIds).stream()
                .filter(doc -> doc.isShared() || (doc.getUser() != null && doc.getUser().getId().equals(userId)))
                .collect(Collectors.toList());
    }

    public List<MultimodalDocument> recommendDocuments(Long userId, int limit) {
        log.info("Generating recommendations for user: {}", userId);
        boolean premiumUser = isPremiumUser(userId);
        int effectiveLimit = premiumUser ? Math.max(limit, 8) : limit;

        List<Long> recentDocIds = accessLogRepository.findRecentViewedDocumentIds(
                userId,
                PageRequest.of(0, premiumUser ? 8 : 5)
        );

        if (recentDocIds.isEmpty()) {
            log.info("No recent interactions found for user {}, returning public featured documents", userId);
            return documentRepository.findTopFeatured(PageRequest.of(0, effectiveLimit));
        }

        List<MultimodalDocument> recentDocs = documentRepository.findAllById(recentDocIds);
        Set<Long> recommendedIds = new HashSet<>();
        List<MultimodalDocument> finalRecommendations = new ArrayList<>();

        for (MultimodalDocument doc : recentDocs) {
            String seedContent = doc.getExtractedContent();
            if (seedContent != null && !seedContent.isEmpty()) {
                List<Document> similarDocs = vectorStoreService.search(seedContent, premiumUser ? 5 : 3, userId);

                for (Document sDoc : similarDocs) {
                    Object docIdObj = sDoc.getMetadata().get("documentId");
                    if (docIdObj == null) {
                        continue;
                    }
                    Long simId = Long.valueOf(docIdObj.toString());
                    if (!recentDocIds.contains(simId) && !recommendedIds.contains(simId)) {
                        documentRepository.findById(simId).ifPresent(foundDoc -> {
                            if (foundDoc.isShared() || (foundDoc.getUser() != null && foundDoc.getUser().getId().equals(userId))) {
                                if (finalRecommendations.size() < effectiveLimit) {
                                    finalRecommendations.add(foundDoc);
                                    recommendedIds.add(simId);
                                }
                            }
                        });
                    }
                }
            }
            if (finalRecommendations.size() >= effectiveLimit) {
                break;
            }
        }

        if (finalRecommendations.size() < effectiveLimit) {
            List<MultimodalDocument> fallback = documentRepository.findTopFeatured(PageRequest.of(0, premiumUser ? 8 : 5));
            for (MultimodalDocument f : fallback) {
                if (!recommendedIds.contains(f.getId()) && !recentDocIds.contains(f.getId())) {
                    finalRecommendations.add(f);
                    recommendedIds.add(f.getId());
                }
                if (finalRecommendations.size() >= effectiveLimit) {
                    break;
                }
            }
        }

        return finalRecommendations;
    }

    private boolean isPremiumUser(Long userId) {
        return userRepository.findById(userId)
                .map(User::getRoles)
                .stream()
                .flatMap(Set::stream)
                .anyMatch(role -> "PREMIUM".equalsIgnoreCase(role.getName()));
    }
}
