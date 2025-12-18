package com.nh.shorturl.admin.util;

import org.springframework.util.StringUtils;

public final class UserAgentParser {

    private static final String UNKNOWN = "UNKNOWN";

    private UserAgentParser() {
    }

    public static UserAgentMetadata parse(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return UserAgentMetadata.unknown();
        }

        String normalized = userAgent.toLowerCase();

        return new UserAgentMetadata(
                resolveDeviceType(normalized),
                resolveOs(normalized),
                resolveBrowser(normalized)
        );
    }

    private static String resolveDeviceType(String normalizedUserAgent) {
        if (containsAny(normalizedUserAgent, "bot", "crawl", "spider")) {
            return "BOT";
        }
        if (containsAny(normalizedUserAgent, "mobile", "iphone", "android")) {
            return "MOBILE";
        }
        if (containsAny(normalizedUserAgent, "tablet", "ipad")) {
            return "TABLET";
        }
        return "DESKTOP";
    }

    private static String resolveOs(String normalizedUserAgent) {
        if (normalizedUserAgent.contains("windows")) {
            return "WINDOWS";
        }
        if (containsAny(normalizedUserAgent, "mac os x", "macintosh")) {
            return "MACOS";
        }
        if (normalizedUserAgent.contains("android")) {
            return "ANDROID";
        }
        if (containsAny(normalizedUserAgent, "iphone", "ipad", "ios")) {
            return "IOS";
        }
        if (normalizedUserAgent.contains("linux")) {
            return "LINUX";
        }
        return UNKNOWN;
    }

    private static String resolveBrowser(String normalizedUserAgent) {
        if (containsAny(normalizedUserAgent, "edg", "edge")) {
            return "EDGE";
        }
        if (containsAny(normalizedUserAgent, "opr", "opera")) {
            return "OPERA";
        }
        if (containsAny(normalizedUserAgent, "chrome")) {
            return "CHROME";
        }
        if (normalizedUserAgent.contains("safari") && !normalizedUserAgent.contains("chrome")) {
            return "SAFARI";
        }
        if (normalizedUserAgent.contains("firefox")) {
            return "FIREFOX";
        }
        if (containsAny(normalizedUserAgent, "msie", "trident")) {
            return "IE";
        }
        return UNKNOWN;
    }

    private static boolean containsAny(String source, String... needles) {
        for (String needle : needles) {
            if (source.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public record UserAgentMetadata(String deviceType, String os, String browser) {
        public static UserAgentMetadata unknown() {
            return new UserAgentMetadata(UNKNOWN, UNKNOWN, UNKNOWN);
        }
    }
}
