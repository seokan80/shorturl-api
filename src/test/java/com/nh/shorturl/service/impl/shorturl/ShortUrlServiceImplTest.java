package com.nh.shorturl.service.impl.shorturl;

import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.entity.ShortUrl;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.ShortUrlRepository;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.service.shorturl.ShortUrlServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceImplTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ShortUrlServiceImpl shortUrlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shortUrlService, "baseUrl", "https://sho.rt/");
    }

    @Test
    @DisplayName("단축 URL 생성 시 사용자 정보와 Base URL이 포함된 응답을 반환한다")
    void createShortUrl_generatesResponse() {
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://example.com/page");

        User user = User.builder()
                .id(7L)
                .username("my-service")
                .apiKey("api-key")
                .build();

        given(userRepository.findByUsername("my-service")).willReturn(Optional.of(user));
        given(shortUrlRepository.existsByShortUrl(org.mockito.ArgumentMatchers.anyString())).willReturn(false);

        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 18, 12, 0);
        given(shortUrlRepository.save(org.mockito.ArgumentMatchers.any(ShortUrl.class)))
                .willAnswer(invocation -> {
                    ShortUrl entity = invocation.getArgument(0);
                    entity.setId(55L);
                    ReflectionTestUtils.setField(entity, "createdAt", createdAt);
                    return entity;
                });

        ShortUrlResponse response = shortUrlService.createShortUrl(request, "my-service");

        assertThat(response.getId()).isEqualTo(55L);
        assertThat(response.getLongUrl()).isEqualTo("https://example.com/page");
        assertThat(response.getCreatedBy()).isEqualTo("my-service");
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getShortUrl()).startsWith("https://sho.rt/");

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(captor.capture());
        ShortUrl saved = captor.getValue();
        assertThat(saved.getLongUrl()).isEqualTo("https://example.com/page");
        assertThat(saved.getCreateBy()).isEqualTo("my-service");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getExpiredAt()).isNotNull();
    }

    @Test
    @DisplayName("ID로 단축 URL을 조회하면 DTO로 반환한다")
    void getShortUrl_returnsResponse() {
        User user = User.builder()
                .id(2L)
                .username("another")
                .apiKey("api")
                .build();

        ShortUrl entity = ShortUrl.builder()
                .id(99L)
                .shortUrl("key12345")
                .longUrl("https://example.com/id")
                .createBy("another")
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 20, 11, 0);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);

        given(shortUrlRepository.findById(99L)).willReturn(Optional.of(entity));

        ShortUrlResponse response = shortUrlService.getShortUrl(99L);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getShortKey()).isEqualTo("key12345");
        assertThat(response.getLongUrl()).isEqualTo("https://example.com/id");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("단축 키로 조회 시 Base URL이 포함된 응답을 반환한다")
    void getShortUrlByKey_returnsResponse_withBaseUrl() {
        User user = User.builder()
                .id(7L)
                .username("my-service")
                .apiKey("api-key")
                .build();

        ShortUrl entity = ShortUrl.builder()
                .id(42L)
                .shortUrl("a1B2c3D4")
                .longUrl("https://example.com/page")
                .createBy("my-service")
                .user(user)
                .expiredAt(LocalDateTime.now().plusHours(1))
                .build();

        LocalDateTime createdAt = LocalDateTime.of(2025, 1, 18, 12, 34, 56);
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);

        given(shortUrlRepository.findByShortUrl("a1B2c3D4")).willReturn(Optional.of(entity));

        ShortUrlResponse response = shortUrlService.getShortUrlByKey("a1B2c3D4");

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getShortKey()).isEqualTo("a1B2c3D4");
        assertThat(response.getShortUrl()).isEqualTo("https://sho.rt/a1B2c3D4");
        assertThat(response.getLongUrl()).isEqualTo("https://example.com/page");
        assertThat(response.getCreatedBy()).isEqualTo("my-service");
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getExpiredAt()).isEqualTo(entity.getExpiredAt().toString());
    }

    @Test
    @DisplayName("만료된 단축 키를 조회하면 예외를 던진다")
    void getShortUrlByKey_throwsWhenExpired() {
        User user = User.builder()
                .id(1L)
                .username("tester")
                .apiKey("api-key")
                .build();

        ShortUrl expired = ShortUrl.builder()
                .id(100L)
                .shortUrl("expiredKey")
                .longUrl("https://example.com/expired")
                .createBy("tester")
                .user(user)
                .expiredAt(LocalDateTime.now().minusMinutes(5))
                .build();

        given(shortUrlRepository.findByShortUrl("expiredKey")).willReturn(Optional.of(expired));

        assertThatThrownBy(() -> shortUrlService.getShortUrlByKey("expiredKey"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료");
    }

    @Test
    @DisplayName("단축 URL 삭제 시 Repository delete가 호출된다")
    void deleteShortUrl_removesEntity() {
        ShortUrl entity = ShortUrl.builder()
                .id(200L)
                .shortUrl("deleteMe")
                .longUrl("https://example.com/delete")
                .createBy("tester")
                .user(User.builder().id(9L).username("tester").apiKey("key").build())
                .build();

        given(shortUrlRepository.findById(200L)).willReturn(Optional.of(entity));

        shortUrlService.deleteShortUrl(200L);

        verify(shortUrlRepository).delete(entity);
    }

    @Test
    @DisplayName("단축 키를 통해 원본 URL을 반환한다")
    void resolveOriginalUrl_returnsLongUrl() {
        ShortUrl entity = ShortUrl.builder()
                .id(300L)
                .shortUrl("redirectKey")
                .longUrl("https://example.com/original")
                .createBy("tester")
                .user(User.builder().id(1L).username("tester").apiKey("key").build())
                .expiredAt(LocalDateTime.now().plusMinutes(10))
                .build();

        given(shortUrlRepository.findByShortUrl("redirectKey")).willReturn(Optional.of(entity));

        String longUrl = shortUrlService.resolveOriginalUrl("redirectKey");

        assertThat(longUrl).isEqualTo("https://example.com/original");
    }

    @Test
    @DisplayName("사용자별 단축 URL 목록을 페이징하여 조회한다")
    void listShortUrls_withUsername_returnsUserUrls() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .apiKey("key")
                .build();

        ShortUrl url1 = ShortUrl.builder()
                .id(1L)
                .shortUrl("abc123")
                .longUrl("https://example.com/1")
                .createBy("testuser")
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        ShortUrl url2 = ShortUrl.builder()
                .id(2L)
                .shortUrl("def456")
                .longUrl("https://example.com/2")
                .createBy("testuser")
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(2))
                .build();

        ReflectionTestUtils.setField(url1, "createdAt", LocalDateTime.of(2025, 1, 1, 10, 0));
        ReflectionTestUtils.setField(url2, "createdAt", LocalDateTime.of(2025, 1, 2, 10, 0));

        List<ShortUrl> content = Arrays.asList(url1, url2);
        Page<ShortUrl> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);

        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));
        given(shortUrlRepository.findByUser(user, PageRequest.of(0, 10))).willReturn(page);

        ResultList<ShortUrlResponse> result = shortUrlService.listShortUrls(PageRequest.of(0, 10), "testuser");

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getElements()).hasSize(2);
        assertThat(result.getElements().get(0).getId()).isEqualTo(1L);
        assertThat(result.getElements().get(1).getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("사용자 없이 단축 URL 목록을 페이징하여 전체 조회한다")
    void listShortUrls_withoutUsername_returnsAllUrls() {
        User user1 = User.builder().id(1L).username("user1").apiKey("key1").build();
        User user2 = User.builder().id(2L).username("user2").apiKey("key2").build();

        ShortUrl url1 = ShortUrl.builder()
                .id(1L)
                .shortUrl("abc123")
                .longUrl("https://example.com/1")
                .createBy("user1")
                .user(user1)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        ShortUrl url2 = ShortUrl.builder()
                .id(2L)
                .shortUrl("def456")
                .longUrl("https://example.com/2")
                .createBy("user2")
                .user(user2)
                .expiredAt(LocalDateTime.now().plusDays(2))
                .build();

        ReflectionTestUtils.setField(url1, "createdAt", LocalDateTime.of(2025, 1, 1, 10, 0));
        ReflectionTestUtils.setField(url2, "createdAt", LocalDateTime.of(2025, 1, 2, 10, 0));

        List<ShortUrl> content = Arrays.asList(url1, url2);
        Page<ShortUrl> page = new PageImpl<>(content, PageRequest.of(0, 10), 2);

        given(shortUrlRepository.findAll(PageRequest.of(0, 10))).willReturn(page);

        ResultList<ShortUrlResponse> result = shortUrlService.listShortUrls(PageRequest.of(0, 10), null);

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getElements()).hasSize(2);
    }

    @Test
    @DisplayName("단축 URL의 만료일을 수정한다")
    void updateShortUrlExpiration_updatesExpiration() {
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .apiKey("key")
                .build();

        ShortUrl entity = ShortUrl.builder()
                .id(100L)
                .shortUrl("abc123")
                .longUrl("https://example.com/test")
                .createBy("testuser")
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        ReflectionTestUtils.setField(entity, "createdAt", LocalDateTime.of(2025, 1, 1, 10, 0));

        LocalDateTime newExpiration = LocalDateTime.of(2025, 12, 31, 23, 59, 59);
        ShortUrlUpdateRequest request = new ShortUrlUpdateRequest();
        request.setExpiredAt(newExpiration);

        given(shortUrlRepository.findById(100L)).willReturn(Optional.of(entity));
        given(shortUrlRepository.save(entity)).willReturn(entity);

        ShortUrlResponse response = shortUrlService.updateShortUrlExpiration(100L, request, "testuser");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(entity.getExpiredAt()).isEqualTo(newExpiration);
        verify(shortUrlRepository).save(entity);
    }

    @Test
    @DisplayName("다른 사용자의 단축 URL 만료일을 수정하려 하면 예외를 던진다")
    void updateShortUrlExpiration_throwsWhenUnauthorized() {
        User owner = User.builder()
                .id(1L)
                .username("owner")
                .apiKey("key")
                .build();

        ShortUrl entity = ShortUrl.builder()
                .id(100L)
                .shortUrl("abc123")
                .longUrl("https://example.com/test")
                .createBy("owner")
                .user(owner)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        LocalDateTime newExpiration = LocalDateTime.of(2025, 12, 31, 23, 59, 59);
        ShortUrlUpdateRequest request = new ShortUrlUpdateRequest();
        request.setExpiredAt(newExpiration);

        given(shortUrlRepository.findById(100L)).willReturn(Optional.of(entity));

        assertThatThrownBy(() -> shortUrlService.updateShortUrlExpiration(100L, request, "attacker"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("자신이 생성한 URL만 수정할 수 있습니다");
    }
}
