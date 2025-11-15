package com.nh.shorturl.dto.request.shorturl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ShortUrlUpdateRequest {
    private LocalDateTime expireDate;
}
