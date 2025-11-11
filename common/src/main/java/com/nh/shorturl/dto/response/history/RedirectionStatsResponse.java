package com.nh.shorturl.dto.response.history;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RedirectionStatsResponse {
    private final String referer;
    private final long count;
}
