package wiki.yuhang.xixu.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

        AuthService.LoginResult result = auth.login("alice", "a-long-test-password",
            "Browser/1.0", "127.0.0.1").orElseThrow();

        assertEquals("alice", result.user().username());
        assertEquals(86400, result.expiresInSeconds());
        assertEquals(43, result.token().length());
        verify(sessions).lockUser(7L);
        verify(sessions).create(eq(7L), argThat(hash -> hash.length() == 64 && !hash.equals(result.token())),
            eq("Browser/1.0"), eq("127.0.0.1"));
        verify(sessions).keepNewestActive(7L, 5);
        verify(sessions).purgeOld(7L);
        verify(users).recordLogin(7L);
    }

    @Test
    void wrongPasswordOrDisabledUserDoesNotCreateSession() {
        String hash = encoder.encode("correct-password");
        when(users.findByUsername("alice")).thenReturn(Optional.of(
            new AppUser(7, "alice", hash, "Alice", "USER", 1)));
        when(users.findByUsername("disabled")).thenReturn(Optional.of(
            new AppUser(8, "disabled", hash, "Disabled", "USER", 0)));

        assertTrue(auth.login("alice", "wrong-password", null, null).isEmpty());
        assertTrue(auth.login("disabled", "correct-password", null, null).isEmpty());
        verifyNoInteractions(sessions);
    }

    @Test
    void invalidTokenIsRejectedBeforeDatabaseLookup() {
        assertTrue(auth.authenticate("bad-token").isEmpty());
        verifyNoInteractions(sessions);
    }
}
