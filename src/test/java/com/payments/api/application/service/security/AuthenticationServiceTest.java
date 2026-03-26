package com.payments.api.application.service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.payments.api.application.port.in.security.TokenResult;
import com.payments.api.application.port.out.security.ClientRepositoryPort;
import com.payments.api.config.JwtProperties;
import com.payments.api.domain.exception.InvalidClientException;
import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private ClientRepositoryPort clientRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    private AuthenticationService authenticationService;

    private static final String TEST_SECRET =
        "test-secret-key-for-unit-tests-hmac-sha256-at-least-32-bytes-long";
    private static final String TEST_ISSUER = "payments-api";
    private static final long TEST_EXPIRATION = 1800L;

    private static final String CLIENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String CLIENT_NAME = "test-service";
    private static final String RAW_SECRET = "raw-plain-secret";
    private static final String HASHED_SECRET = "$2a$10$hashedSecretValue";

    private OAuthClient activeClient;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecretKey(TEST_SECRET);
        jwtProperties.setIssuer(TEST_ISSUER);
        jwtProperties.setExpirationSeconds(TEST_EXPIRATION);

        authenticationService = new AuthenticationService(
            clientRepository, passwordEncoder, jwtProperties, new ObjectMapper()
        );

        activeClient = new OAuthClient(
            CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
            ClientScope.WRITE, true,
            LocalDateTime.now(), null
        );
    }

    @Test
    void validCredentialsReturnsTokenResultWithNonNullAccessToken() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        TokenResult result = authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertEquals("Bearer", result.tokenType());
        assertEquals(TEST_EXPIRATION, result.expiresIn());
    }

    @Test
    void validCredentialsReturnsCorrectScopeString() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        TokenResult result = authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        assertEquals(ClientScope.WRITE.toScopeString(), result.scope());
    }

    @Test
    void validCredentialsJwtContainsCorrectSubClaim() throws Exception {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        TokenResult result = authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        SignedJWT parsed = SignedJWT.parse(result.accessToken());
        JWTClaimsSet claims = parsed.getJWTClaimsSet();
        assertEquals(CLIENT_ID, claims.getSubject());
    }

    @Test
    void validCredentialsJwtContainsCorrectScopeClaim() throws Exception {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        TokenResult result = authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        SignedJWT parsed = SignedJWT.parse(result.accessToken());
        JWTClaimsSet claims = parsed.getJWTClaimsSet();
        assertEquals(ClientScope.WRITE.toScopeString(), claims.getStringClaim("scope"));
    }

    @Test
    void validCredentialsJwtContainsCorrectClientNameClaim() throws Exception {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        TokenResult result = authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        SignedJWT parsed = SignedJWT.parse(result.accessToken());
        JWTClaimsSet claims = parsed.getJWTClaimsSet();
        assertEquals(CLIENT_NAME, claims.getStringClaim("client_name"));
    }

    @Test
    void validCredentialsJwtContainsCorrectIssClaim() throws Exception {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        TokenResult result = authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        SignedJWT parsed = SignedJWT.parse(result.accessToken());
        JWTClaimsSet claims = parsed.getJWTClaimsSet();
        assertEquals(TEST_ISSUER, claims.getIssuer());
    }

    @Test
    void validCredentialsJwtExpClaimIsApproximately1800SecondsAfterIat() throws Exception {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        TokenResult result = authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        SignedJWT parsed = SignedJWT.parse(result.accessToken());
        JWTClaimsSet claims = parsed.getJWTClaimsSet();

        long diffSeconds = (claims.getExpirationTime().getTime() - claims.getIssueTime().getTime()) / 1000;
        assertEquals(TEST_EXPIRATION, diffSeconds);
    }

    @Test
    void validCredentialsJwtContainsNonNullJtiClaim() throws Exception {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        TokenResult result = authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        SignedJWT parsed = SignedJWT.parse(result.accessToken());
        JWTClaimsSet claims = parsed.getJWTClaimsSet();
        assertNotNull(claims.getJWTID());
    }

    @Test
    void validCredentialsCallsUpdateLastUsedAtWithCorrectClientId() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches(RAW_SECRET, HASHED_SECRET)).thenReturn(true);

        authenticationService.authenticate(CLIENT_ID, RAW_SECRET);

        verify(clientRepository).updateLastUsedAt(eq(CLIENT_ID), any(LocalDateTime.class));
    }

    @Test
    void unknownClientIdThrowsInvalidClientException() {
        when(clientRepository.findByClientId("unknown-id")).thenReturn(Optional.empty());

        assertThrows(InvalidClientException.class,
            () -> authenticationService.authenticate("unknown-id", RAW_SECRET));
    }

    @Test
    void inactiveClientThrowsInvalidClientException() {
        OAuthClient inactiveClient = new OAuthClient(
            CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
            ClientScope.READ, false,
            LocalDateTime.now(), null
        );
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(inactiveClient));

        assertThrows(InvalidClientException.class,
            () -> authenticationService.authenticate(CLIENT_ID, RAW_SECRET));
    }

    @Test
    void wrongClientSecretThrowsInvalidClientException() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches("wrong-secret", HASHED_SECRET)).thenReturn(false);

        assertThrows(InvalidClientException.class,
            () -> authenticationService.authenticate(CLIENT_ID, "wrong-secret"));
    }

    @Test
    void unknownClientIdDoesNotCallUpdateLastUsedAt() {
        when(clientRepository.findByClientId("unknown-id")).thenReturn(Optional.empty());

        assertThrows(InvalidClientException.class,
            () -> authenticationService.authenticate("unknown-id", RAW_SECRET));

        verify(clientRepository, never()).updateLastUsedAt(any(), any());
    }

    @Test
    void inactiveClientDoesNotCallUpdateLastUsedAt() {
        OAuthClient inactiveClient = new OAuthClient(
            CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
            ClientScope.READ, false,
            LocalDateTime.now(), null
        );
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(inactiveClient));

        assertThrows(InvalidClientException.class,
            () -> authenticationService.authenticate(CLIENT_ID, RAW_SECRET));

        verify(clientRepository, never()).updateLastUsedAt(any(), any());
    }

    @Test
    void wrongSecretDoesNotCallUpdateLastUsedAt() {
        when(clientRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(activeClient));
        when(passwordEncoder.matches("wrong-secret", HASHED_SECRET)).thenReturn(false);

        assertThrows(InvalidClientException.class,
            () -> authenticationService.authenticate(CLIENT_ID, "wrong-secret"));

        verify(clientRepository, never()).updateLastUsedAt(any(), any());
    }
}
