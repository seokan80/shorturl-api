package com.nh.shorturl.admin.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.List;

public class RequestInfoUtils {

    private RequestInfoUtils() { } // static only

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }

    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }

    public static String getReferer(HttpServletRequest request) {
        return request.getHeader("Referer");
    }

    public static String getCountry(HttpServletRequest request) {
        return getFirstHeaderValue(request, List.of("CF-IPCountry", "X-AppEngine-Country", "X-Country-Code", "X-Country"));
    }

    public static String getCity(HttpServletRequest request) {
        return getFirstHeaderValue(request, List.of("X-AppEngine-City", "X-City"));
    }

    private static String getFirstHeaderValue(HttpServletRequest request, List<String> headerNames) {
        for (String headerName : headerNames) {
            String headerValue = request.getHeader(headerName);
            if (StringUtils.hasText(headerValue)) {
                return headerValue;
            }
        }
        return null;
    }
}
