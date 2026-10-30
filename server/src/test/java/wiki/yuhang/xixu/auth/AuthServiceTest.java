package wiki.yuhang.xixu.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
    private final AppUserRepository users = mock(AppUserRepository.class);
    private final AuthSessionRepository sessions = mock(AuthSessionRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AuthService auth = new AuthService(users, sessions, encoder);

    @Test
    void correctPasswordCreatesHashedSessionAndReturnsUser() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(
            new AppUser(7, "alice", encoder.encode("a-long-test-password"), "Alice", "USER", 1)));

        AuthService.LoginResult result = auth.login("alice", "a-long-test-password").orElseThrow();

        assertEquals("alice", result.user().username());
        assertEquals(86400, result.expiresInSeconds());
        assertEquals(43, result.token().length());
        verify(sessions).create(eq(7L), argThat(hash -> hash.length() == 64 && !hash.equals(result.token())),
            any(LocalDateTime.class), any(LocalDateTime.class));
        verify(users).recordLogin(eq(7L), any(LocalDateTime.class));
    }

    @Test
    void wrongPasswordOrDisabledUserDoesNotCreateSession() {
        String hash = encoder.encode("correct-password");
        when(users.findByUsername("alice")).thenReturn(Optional.of(
            new AppUser(7, "alice", hash, "Alice", "USER", 1)));
        when(users.findByUsername("disabled")).thenReturn(Optional.of(
            new AppUser(8, "disabled", hash, "Disabled", "USER", 0)));

        assertTrue(auth.login("alice", "wrong-password").isEmpty());
        assertTrue(auth.login("disabled", "correct-password").isEmpty());
        verifyNoInteractions(sessions);
    }

    @Test
    void invalidTokenIsRejectedBeforeDatabaseLookup() {
        assertTrue(auth.authenticate("bad-token").isEmpty());
        verifyNoInteractions(sessions);
    }
}
