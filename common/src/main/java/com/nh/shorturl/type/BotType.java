package com.nh.shorturl.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 봇 타입 유형
 */
@Getter
@RequiredArgsConstructor
public enum BotType {
    CALLBOT("콜봇"),
    CHATBOT("챗봇");

    private final String description;
}
