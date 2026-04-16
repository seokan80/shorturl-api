package com.nh.shorturl.admin.redirect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedirectErrorPageRendererTest {

    private RedirectErrorPageRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new RedirectErrorPageRenderer();
        renderer.loadTemplate();
    }

    @Test
    void render_injectsReasonAsEscapedHtml() {
        String html = renderer.render("URL not found");

        assertThat(html).contains("URL not found");
        assertThat(html).contains("이동 실패");
        assertThat(html).doesNotContain("{{reason}}");
    }

    @Test
    void render_escapesScriptTagsToPreventXss() {
        String payload = "<script>alert('xss')</script>";

        String html = renderer.render(payload);

        // 스크립트 태그는 실행 가능한 형태로 들어가면 안 된다.
        assertThat(html).doesNotContain("<script>alert('xss')</script>");
        // HtmlUtils.htmlEscape 는 <, >, ' 을 모두 엔티티로 치환한다.
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("alert(");
        assertThat(html).contains("&lt;/script&gt;");
    }

    @Test
    void render_handlesNullReasonGracefully() {
        String html = renderer.render(null);

        assertThat(html).doesNotContain("{{reason}}");
        assertThat(html).contains("이동 실패");
    }

    @Test
    void render_escapesCommonEntityCharacters() {
        String html = renderer.render("a & b < c > d \"e\" 'f'");

        assertThat(html).contains("a &amp; b &lt; c &gt; d");
        assertThat(html).doesNotContain("a & b <");
    }
}
