package com.nh.shorturl.dto.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRequest {
    private String username;
    private String apiKey;
}
