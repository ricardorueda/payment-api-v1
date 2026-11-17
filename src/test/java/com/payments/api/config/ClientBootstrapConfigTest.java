package com.payments.api.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientBootstrapConfigTest {

    private static final String BOOTSTRAP_CLIENT_ID = "00000000-0000-0000-0000-000000000001";
    private static final String BOOTSTRAP_CLIENT_NAME = "admin-client";
    private static final String BOOTSTRAP_CLIENT_SECRET = "admin-secret-change-in-production";
    private static final String HASHED_SECRET = "$2a$10$hashedAdminSecret";

    @Mock
    private ClientRepositoryPort clientRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ApplicationArguments applicationArguments;

    private BootstrapProperties bootstrapProperties;
    private ClientBootstrapConfig clientBootstrapConfig;

    @BeforeEach
    void setUp() {
        bootstrapProperties = new BootstrapProperties();
        bootstrapProperties.setClientId(BOOTSTRAP_CLIENT_ID);
        bootstrapProperties.setClientName(BOOTSTRAP_CLIENT_NAME);
        bootstrapProperties.setClientSecret(BOOTSTRAP_CLIENT_SECRET);

        clientBootstrapConfig = new ClientBootstrapConfig(bootstrapProperties, clientRepository, passwordEncoder);
    }

    @Test
    void whenAdminClientDoesNotExistSaveIsCalledOnce() throws Exception {
        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(false);
        when(passwordEncoder.encode(BOOTSTRAP_CLIENT_SECRET)).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));

        clientBootstrapConfig.run(applicationArguments);

        verify(clientRepository).save(any(OAuthClient.class));
    }

    @Test
    void whenAdminClientDoesNotExistSavedClientHasCorrectClientId() throws Exception {
        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(false);
        when(passwordEncoder.encode(BOOTSTRAP_CLIENT_SECRET)).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));

        clientBootstrapConfig.run(applicationArguments);

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().getClientId()).isEqualTo(BOOTSTRAP_CLIENT_ID);
    }

    @Test
    void whenAdminClientDoesNotExistSavedClientHasCorrectClientName() throws Exception {
        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(false);
        when(passwordEncoder.encode(BOOTSTRAP_CLIENT_SECRET)).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));

        clientBootstrapConfig.run(applicationArguments);

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().getClientName()).isEqualTo(BOOTSTRAP_CLIENT_NAME);
    }

    @Test
    void whenAdminClientDoesNotExistSavedClientHasClientAdminScope() throws Exception {
        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(false);
        when(passwordEncoder.encode(BOOTSTRAP_CLIENT_SECRET)).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));

        clientBootstrapConfig.run(applicationArguments);

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().getScope()).isEqualTo(ClientScope.CLIENT_ADMIN);
    }

    @Test
    void whenAdminClientDoesNotExistSavedClientIsActive() throws Exception {
        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(false);
        when(passwordEncoder.encode(BOOTSTRAP_CLIENT_SECRET)).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));

        clientBootstrapConfig.run(applicationArguments);

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void whenAdminClientDoesNotExistSavedClientSecretIsBCryptHashed() throws Exception {
        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(false);
        when(passwordEncoder.encode(BOOTSTRAP_CLIENT_SECRET)).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));

        clientBootstrapConfig.run(applicationArguments);

        ArgumentCaptor<OAuthClient> captor = ArgumentCaptor.forClass(OAuthClient.class);
        verify(clientRepository).save(captor.capture());
        assertThat(captor.getValue().getHashedSecret()).isEqualTo(HASHED_SECRET);
        assertThat(captor.getValue().getHashedSecret()).isNotEqualTo(BOOTSTRAP_CLIENT_SECRET);
    }

    @Test
    void whenAdminClientAlreadyExistsSaveIsNotCalled() throws Exception {
        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(true);

        clientBootstrapConfig.run(applicationArguments);

        verify(clientRepository, never()).save(any());
    }

    @Test
    void whenAdminClientAlreadyExistsInfoLogIsEmitted() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ClientBootstrapConfig.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(true);

        clientBootstrapConfig.run(applicationArguments);

        logger.detachAppender(listAppender);

        assertThat(listAppender.list)
                .anyMatch(event -> event.getLevel() == Level.INFO
                        && event.getFormattedMessage().contains(BOOTSTRAP_CLIENT_ID));
    }

    @Test
    void whenAdminClientIsSeededSuccessfullyInfoLogIsEmitted() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(ClientBootstrapConfig.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        when(clientRepository.existsByClientId(BOOTSTRAP_CLIENT_ID)).thenReturn(false);
        when(passwordEncoder.encode(BOOTSTRAP_CLIENT_SECRET)).thenReturn(HASHED_SECRET);
        when(clientRepository.save(any(OAuthClient.class))).thenAnswer(inv -> inv.getArgument(0));

        clientBootstrapConfig.run(applicationArguments);

        logger.detachAppender(listAppender);

        assertThat(listAppender.list)
                .anyMatch(event -> event.getLevel() == Level.INFO
                        && event.getFormattedMessage().contains(BOOTSTRAP_CLIENT_ID));
    }
}
