package com.payments.api.adapter.out.persistence.security;

import com.payments.api.domain.security.model.OAuthClient;
import com.payments.api.domain.security.valueobject.ClientScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthClientPersistenceAdapterTest {

    @Mock
    private OAuthClientJpaRepository jpaRepository;

    @InjectMocks
    private OAuthClientPersistenceAdapter adapter;

    private static final String CLIENT_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String CLIENT_NAME = "test-service";
    private static final String HASHED_SECRET = "$2a$10$hashedSecret";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2025, 6, 15, 10, 30, 0);
    private static final LocalDateTime LAST_USED_AT = LocalDateTime.of(2025, 6, 15, 12, 0, 0);

    private OAuthClient sampleDomain;
    private OAuthClientEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleDomain = new OAuthClient(CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
                ClientScope.WRITE, true, CREATED_AT, LAST_USED_AT);

        sampleEntity = new OAuthClientEntity(CLIENT_ID, CLIENT_NAME, HASHED_SECRET,
                ClientScope.WRITE, true, CREATED_AT, LAST_USED_AT);
    }

    @Test
    void saveCallsJpaRepositoryAndReturnsCorrectlyMappedDomain() {
        when(jpaRepository.save(any(OAuthClientEntity.class))).thenReturn(sampleEntity);

        OAuthClient result = adapter.save(sampleDomain);

        verify(jpaRepository, times(1)).save(any(OAuthClientEntity.class));
        assertEquals(CLIENT_ID, result.getClientId());
        assertEquals(CLIENT_NAME, result.getClientName());
        assertEquals(HASHED_SECRET, result.getHashedSecret());
        assertEquals(ClientScope.WRITE, result.getScope());
        assertTrue(result.isActive());
        assertEquals(CREATED_AT, result.getCreatedAt());
        assertEquals(LAST_USED_AT, result.getLastUsedAt());
    }

    @Test
    void findByClientIdReturnsOptionalWithDomainWhenEntityFound() {
        when(jpaRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(sampleEntity));

        Optional<OAuthClient> result = adapter.findByClientId(CLIENT_ID);

        assertTrue(result.isPresent());
        assertEquals(CLIENT_ID, result.get().getClientId());
        assertEquals(CLIENT_NAME, result.get().getClientName());
        assertEquals(HASHED_SECRET, result.get().getHashedSecret());
        assertEquals(ClientScope.WRITE, result.get().getScope());
    }

    @Test
    void findByClientIdReturnsEmptyOptionalWhenEntityNotFound() {
        when(jpaRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.empty());

        Optional<OAuthClient> result = adapter.findByClientId(CLIENT_ID);

        assertFalse(result.isPresent());
    }

    @Test
    void findByClientNameReturnsOptionalWithDomainWhenEntityFound() {
        when(jpaRepository.findByClientName(CLIENT_NAME)).thenReturn(Optional.of(sampleEntity));

        Optional<OAuthClient> result = adapter.findByClientName(CLIENT_NAME);

        assertTrue(result.isPresent());
        assertEquals(CLIENT_NAME, result.get().getClientName());
    }

    @Test
    void findAllMapsAllEntitiesToDomainObjects() {
        OAuthClientEntity secondEntity = new OAuthClientEntity(
                "another-id", "another-service", HASHED_SECRET,
                ClientScope.READ, true, CREATED_AT, null);
        when(jpaRepository.findAll()).thenReturn(List.of(sampleEntity, secondEntity));

        List<OAuthClient> result = adapter.findAll();

        assertEquals(2, result.size());
        assertEquals(CLIENT_ID, result.get(0).getClientId());
        assertEquals("another-id", result.get(1).getClientId());
    }

    @Test
    void deleteByClientIdDelegatesToJpaRepositoryDeleteById() {
        adapter.deleteByClientId(CLIENT_ID);

        verify(jpaRepository, times(1)).deleteById(CLIENT_ID);
    }

    @Test
    void existsByClientIdReturnsTrueWhenRepositoryReturnsTrue() {
        when(jpaRepository.existsByClientId(CLIENT_ID)).thenReturn(true);

        assertTrue(adapter.existsByClientId(CLIENT_ID));
    }

    @Test
    void existsByClientNameReturnsFalseWhenRepositoryReturnsFalse() {
        when(jpaRepository.existsByClientName(CLIENT_NAME)).thenReturn(false);

        assertFalse(adapter.existsByClientName(CLIENT_NAME));
    }

    @Test
    void updateLastUsedAtFindsEntitySetsTimestampAndSaves() {
        LocalDateTime newTimestamp = LocalDateTime.of(2025, 6, 15, 14, 0, 0);
        when(jpaRepository.findByClientId(CLIENT_ID)).thenReturn(Optional.of(sampleEntity));
        when(jpaRepository.save(any(OAuthClientEntity.class))).thenReturn(sampleEntity);

        adapter.updateLastUsedAt(CLIENT_ID, newTimestamp);

        verify(jpaRepository, times(1)).findByClientId(CLIENT_ID);
        assertEquals(newTimestamp, sampleEntity.getLastUsedAt());
        verify(jpaRepository, times(1)).save(sampleEntity);
    }
}
