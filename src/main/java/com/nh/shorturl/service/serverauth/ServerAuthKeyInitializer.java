package com.nh.shorturl.service.serverauth;

import com.nh.shorturl.config.RegistrationConfig;
import com.nh.shorturl.entity.ServerAuthKey;
import com.nh.shorturl.repository.ServerAuthKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ServerAuthKeyInitializer implements ApplicationRunner {

    private final ServerAuthKeyRepository serverAuthKeyRepository;
    private final RegistrationConfig registrationConfig;

    @Override
    public void run(ApplicationArguments args) {
        if (serverAuthKeyRepository.count() > 0) {
            return;
        }

        String bootstrapKey = registrationConfig.getRegistrationKey();
        if (!StringUtils.hasText(bootstrapKey)) {
            return;
        }

        ServerAuthKey entity = ServerAuthKey.builder()
            .name("Bootstrap Key")
            .issuedBy("system")
            .description("Generated automatically from registration.key")
            .keyValue(bootstrapKey)
            .active(true)
            .build();
        serverAuthKeyRepository.save(entity);
    }
}
