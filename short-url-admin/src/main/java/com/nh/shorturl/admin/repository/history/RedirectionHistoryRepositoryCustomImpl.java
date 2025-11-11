package com.nh.shorturl.admin.repository.history;

import com.nh.shorturl.type.GroupingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RedirectionHistoryRepositoryCustomImpl implements RedirectionHistoryRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Object[]> getStatsByShortUrlId(Long shortUrlId, List<GroupingType> groupBy) {
        if (CollectionUtils.isEmpty(groupBy)) {
            return List.of();
        }

        String selectClause = groupBy.stream()
                .map(this::getExpressionFor)
                .collect(Collectors.joining(", "));

        String groupByClause = groupBy.stream()
                .map(this::getExpressionFor)
                .collect(Collectors.joining(", "));

        String jpql = "SELECT " + selectClause + ", COUNT(h) FROM RedirectionHistory h " +
                      "WHERE h.shortUrl.id = :shortUrlId " +
                      "GROUP BY " + groupByClause +
                      " ORDER BY COUNT(h) DESC";

        Query query = entityManager.createQuery(jpql);

        query.setParameter("shortUrlId", shortUrlId);

        return query.getResultList();
    }

    private String getExpressionFor(GroupingType type) {
        return switch (type) {
            case REFERER -> "h.referer";
            case USER_AGENT -> "h.userAgent";
            case YEAR -> "YEAR(h.redirectAt)";
            case MONTH -> "MONTH(h.redirectAt)";
            case DAY -> "DAY(h.redirectAt)";
            case HOUR -> "HOUR(h.redirectAt)";
        };
    }
}
