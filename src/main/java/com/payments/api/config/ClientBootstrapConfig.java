package com.payments.api.config;

import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ClientBootstrapConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClientBootstrapConfig.class);

    private final BootstrapProperties bootstrapProperties;
    private final ClientRepositoryPort clientRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public ClientBootstrapConfig(BootstrapProperties bootstrapProperties,
                                 ClientRepositoryPort clientRepository,
                                 BCryptPasswordEncoder passwordEncoder) {
        this.bootstrapProperties = bootstrapProperties;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String clientId = bootstrapProperties.getClientId();

        if (clientRepository.existsByClientId(clientId)) {
            log.info("Bootstrap admin client '{}' already exists — skipping seed", clientId);
            return;
        }

        String hashedSecret = passwordEncoder.encode(bootstrapProperties.getClientSecret());
        OAuthClient adminClient = new OAuthClient(
                clientId,
                bootstrapProperties.getClientName(),
                hashedSecret,
                ClientScope.CLIENT_ADMIN,
                true,
                LocalDateTime.now(),
                null
        );

        clientRepository.save(adminClient);
        log.info("Bootstrap admin client '{}' seeded successfully", clientId);
    }
}
