package vn.commentary.backend.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHasherTest {

    @Test
    void tokenHashing() {
        String token = TokenHasher.newSessionToken();
        assertEquals(64, token.length());
        assertEquals(32, TokenHasher.newQrToken().length());
        String hash = TokenHasher.sha256(token);
        assertTrue(TokenHasher.matchesHash(token, hash));
        assertFalse(TokenHasher.matchesHash("khac", hash));
        assertFalse(TokenHasher.matchesHash(null, hash));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", TokenHasher.sha256("abc"));
        assertTrue(TokenHasher.safeEquals("a", "a"));
        assertFalse(TokenHasher.safeEquals("a", null));
    }
}
