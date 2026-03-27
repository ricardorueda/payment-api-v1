package com.payments.api.domain.security.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientScopeTest {

    @Test
    void readToScopeStringReturnsRead() {
        assertEquals("read", ClientScope.READ.toScopeString());
    }

    @Test
    void writeToScopeStringReturnsWriteRead() {
        assertEquals("write read", ClientScope.WRITE.toScopeString());
    }

    @Test
    void clientAdminToScopeStringReturnsClientAdmin() {
        assertEquals("client:admin", ClientScope.CLIENT_ADMIN.toScopeString());
    }

    @Test
    void fromStringReadReturnsRead() {
        assertEquals(ClientScope.READ, ClientScope.fromString("read"));
    }

    @Test
    void fromStringWriteReturnsWrite() {
        assertEquals(ClientScope.WRITE, ClientScope.fromString("write"));
    }

    @Test
    void fromStringClientAdminReturnsClientAdmin() {
        assertEquals(ClientScope.CLIENT_ADMIN, ClientScope.fromString("client:admin"));
    }

    @Test
    void fromStringInvalidThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ClientScope.fromString("invalid"));
    }

    @Test
    void fromStringEmptyThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ClientScope.fromString(""));
    }

    @Test
    void fromStringIsCaseInsensitive() {
        assertEquals(ClientScope.READ, ClientScope.fromString("READ"));
        assertEquals(ClientScope.WRITE, ClientScope.fromString("WRITE"));
        assertEquals(ClientScope.CLIENT_ADMIN, ClientScope.fromString("CLIENT:ADMIN"));
    }
}
