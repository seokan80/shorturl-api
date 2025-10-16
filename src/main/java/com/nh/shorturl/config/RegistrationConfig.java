package com.nh.shorturl.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class RegistrationConfig {

    @Value("${registration.key}")
    private String registrationKey;
}
