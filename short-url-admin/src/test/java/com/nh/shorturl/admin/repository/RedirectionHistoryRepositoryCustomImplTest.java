package com.nh.shorturl.admin.repository;

import com.nh.shorturl.type.GroupingType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectionHistoryRepositoryCustomImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private RedirectionHistoryRepositoryCustomImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RedirectionHistoryRepositoryCustomImpl();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
    }

    @Test
    void getStatsByShortUrlId_returnsEmpty_whenGroupByIsNull() {
        List<Object[]> result = repository.getStatsByShortUrlId(1L, null);

        assertThat(result).isEmpty();
        verify(entityManager, never()).createQuery(anyString());
    }

    @Test
    void getStatsByShortUrlId_returnsEmpty_whenGroupByIsEmpty() {
        List<Object[]> result = repository.getStatsByShortUrlId(1L, List.of());

        assertThat(result).isEmpty();
        verify(entityManager, never()).createQuery(anyString());
    }

    @Test
    void getStatsByShortUrlId_buildsJpqlForSingleGroupBy() {
        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        when(entityManager.createQuery(jpqlCaptor.capture())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        repository.getStatsByShortUrlId(7L, List.of(GroupingType.REFERER));

        String jpql = jpqlCaptor.getValue();
        assertThat(jpql).contains("SELECT h.referer, COUNT(h) FROM RedirectionHistory h");
        assertThat(jpql).contains("WHERE h.shortUrl.id = :shortUrlId");
        assertThat(jpql).contains("GROUP BY h.referer");
        assertThat(jpql).contains("ORDER BY COUNT(h) DESC");
        verify(query).setParameter("shortUrlId", 7L);
    }

    @Test
    void getStatsByShortUrlId_buildsJpqlForMultipleGroupBy() {
        ArgumentCaptor<String> jpqlCaptor = ArgumentCaptor.forClass(String.class);
        when(entityManager.createQuery(jpqlCaptor.capture())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        repository.getStatsByShortUrlId(9L, List.of(GroupingType.YEAR, GroupingType.MONTH));

        String jpql = jpqlCaptor.getValue();
        assertThat(jpql).contains("SELECT YEAR(h.redirectAt), MONTH(h.redirectAt), COUNT(h)");
        assertThat(jpql).contains("GROUP BY YEAR(h.redirectAt), MONTH(h.redirectAt)");
    }

    @Test
    void getStatsByShortUrlId_mapsAllGroupingTypesWithoutThrowing() {
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        repository.getStatsByShortUrlId(1L, List.of(GroupingType.values()));

        verify(entityManager).createQuery(anyString());
    }
}
