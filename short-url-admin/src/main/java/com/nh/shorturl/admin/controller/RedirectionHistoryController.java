package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.history.RedirectionHistoryService;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/r/history")
public class RedirectionHistoryController {

    private final RedirectionHistoryService redirectionHistoryService;

    public RedirectionHistoryController(RedirectionHistoryService redirectionHistoryService) {
        this.redirectionHistoryService = redirectionHistoryService;
    }

    @GetMapping("/{shortUrlId}/count")
    public ResultEntity<?> register(@PathVariable Long shortUrlId) {

        int count = redirectionHistoryService.getRedirectCount(shortUrlId);

        return new ResultEntity<>(count);
    }

    @PostMapping("/{shortUrlId}/stats")
    public ResultEntity<List<Map<String, Object>>> getStats(
            @PathVariable Long shortUrlId,
            @RequestBody RedirectionStatsRequest request) {
        List<Map<String, Object>> stats = redirectionHistoryService.getStats(shortUrlId, request);
        return new ResultEntity<>(stats);
    }
}
