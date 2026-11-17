package com.payments.api.config;

import com.payments.api.application.port.in.security.ManageClientUseCase;
import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.application.service.security.ClientManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityBeanConfiguration {

    @Bean
    public ManageClientUseCase manageClientUseCase(ClientRepositoryPort clientRepository,
                                                    BCryptPasswordEncoder passwordEncoder,
                                                    BootstrapProperties bootstrapProperties) {
        return new ClientManagementService(clientRepository, passwordEncoder,
                bootstrapProperties.getClientId());
    }
}
