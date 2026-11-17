package com.payments.api.adapter.in.rest.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLoggingFilterTest {

    private static final String CLIENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String CLIENT_NAME = "test-service";
    private static final String SCOPE = "write read";

    @Mock
    private FilterChain filterChain;

    private AuditLoggingFilter filter;
    private ObjectMapper objectMapper;
    private ListAppender<ILoggingEvent> listAppender;
    private Logger auditLogger;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        filter = new AuditLoggingFilter(objectMapper);

        auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        listAppender = new ListAppender<>();
        listAppender.start();
        auditLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        auditLogger.detachAppender(listAppender);
    }

    private Jwt buildTestJwt() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(CLIENT_ID)
                .claim("client_name", CLIENT_NAME)
                .claim("scope", SCOPE)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .build();
    }

    @Test
    void authenticatedRequestLogsAllRequiredPciDss40AccessFields() throws Exception {
        Jwt jwt = buildTestJwt();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/payments");
        request.setRemoteAddr("192.168.1.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        filter.doFilter(request, response, filterChain);

        assertThat(listAppender.list).hasSize(1);
        Map<String, Object> event = objectMapper.readValue(
                listAppender.list.get(0).getFormattedMessage(), new TypeReference<Map<String, Object>>() {});

        assertThat(event.get("type")).isEqualTo("ACCESS");
        assertThat(event.get("event")).isEqualTo("API_REQUEST");
        assertThat(event.get("clientId")).isEqualTo(CLIENT_ID);
        assertThat(event.get("clientName")).isEqualTo(CLIENT_NAME);
        assertThat(event.get("scope")).isEqualTo(SCOPE);
        assertThat(event.get("method")).isEqualTo("GET");
        assertThat(event.get("path")).isEqualTo("/api/payments");
        assertThat(event.get("status")).isEqualTo(200);
        assertThat(event.get("sourceIp")).isEqualTo("192.168.1.1");
        assertThat(event).containsKey("durationMs");
        assertThat(event).containsKey("timestamp");
    }

    @Test
    void unauthenticatedRequestLogsAnonymousClientIdAndExcludesClientNameAndScope() throws Exception {
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(401);

        filter.doFilter(request, response, filterChain);

        assertThat(listAppender.list).hasSize(1);
        Map<String, Object> event = objectMapper.readValue(
                listAppender.list.get(0).getFormattedMessage(), new TypeReference<Map<String, Object>>() {});

        assertThat(event.get("clientId")).isEqualTo("anonymous");
        assertThat(event).doesNotContainKey("clientName");
        assertThat(event).doesNotContainKey("scope");
    }

    @Test
    void filterCallsDoFilterExactlyOnce() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void filterLogsActualResponseStatusCodeAfterChainExecution() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(403);

        filter.doFilter(request, response, filterChain);

        Map<String, Object> event = objectMapper.readValue(
                listAppender.list.get(0).getFormattedMessage(), new TypeReference<Map<String, Object>>() {});
        assertThat(event.get("status")).isEqualTo(403);
    }

    @Test
    void filterLogsDurationMsAsNonNegativeLongValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        Map<String, Object> event = objectMapper.readValue(
                listAppender.list.get(0).getFormattedMessage(), new TypeReference<Map<String, Object>>() {});
        assertThat(((Number) event.get("durationMs")).longValue()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void filterLogsTimestampAsValidIso8601InstantString() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        Map<String, Object> event = objectMapper.readValue(
                listAppender.list.get(0).getFormattedMessage(), new TypeReference<Map<String, Object>>() {});
        String timestamp = (String) event.get("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(Instant.parse(timestamp)).isNotNull();
    }

    @Test
    void filterDoesNotPropagateExceptionFromAuditLogWriteFailure() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));
        AuditLoggingFilter failingFilter = new AuditLoggingFilter(failingMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        failingFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
