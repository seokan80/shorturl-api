package com.nh.shorturl.redirect.service;

import com.nh.shorturl.type.BotType;
import jakarta.servlet.http.HttpServletRequest;

public interface RedirectionHistoryService {
    void saveRedirectionHistory(String shortUrlKey, HttpServletRequest request, BotType botType, String botServiceKey,
            String surveyId, String surveyVer);
}
