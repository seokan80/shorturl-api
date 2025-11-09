package com.nh.shorturl.dto.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenReissueRequest {
    private String username;
    private String refreshToken;
}
