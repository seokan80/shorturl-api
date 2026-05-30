package com.nh.shorturl.admin.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA(React Router) deep-link 을 처리한다.
 * 확장자가 없는 경로를 /index.html 로 포워딩해서 BrowserRouter 가 클라이언트 사이드 라우팅을 수행하도록 한다.
 *
 * <p>매핑 우선순위상 /api/** 와 /openapi/** 는 구체적인 컨트롤러가 먼저 매칭되므로 여기에 걸리지 않는다.
 * (리다이렉트(/s/{key} 등)는 별도 short-url-redirect 서버가 처리하므로 admin 에는 매핑이 없다.)
 * 정적 자원(/assets/*.js, *.css 등)은 확장자를 가지므로 아래 패턴에 걸리지 않는다.
 */
@Controller
public class SpaFallbackController {

    @GetMapping(value = {"/{path:[^.]*}", "/{path:[^.]*}/{subpath:[^.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
