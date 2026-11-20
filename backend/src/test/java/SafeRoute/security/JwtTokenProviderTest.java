package SafeRoute.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    private final String testSecret = "SafeRouteAiSuperSecretKeyForJwtSigningMustBeAtLeast256BitsLong2026";
    private final long testExpiration = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", testExpiration);
    }

    @Test
    @DisplayName("Should generate valid JWT token with claims")
    void testGenerateToken() {
        String token = jwtTokenProvider.generateToken("user@saferoute.com", 1L, "USER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("user@saferoute.com", jwtTokenProvider.extractEmail(token));
        assertEquals(1L, jwtTokenProvider.extractUserId(token));
        assertEquals("USER", jwtTokenProvider.extractRole(token));
        assertFalse(jwtTokenProvider.isTokenExpired(token));
    }

    @Test
    @DisplayName("Should reject invalid or tampered JWT token")
    void testValidateInvalidToken() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalid.signature";

        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    @DisplayName("Should detect expired JWT token")
    void testExpiredToken() {
        // Set expiration to negative time to simulate already expired token
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", -1000L);
        String expiredToken = jwtTokenProvider.generateToken("expired@saferoute.com", 2L, "USER");

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
        assertTrue(jwtTokenProvider.isTokenExpired(expiredToken));
    }
}
