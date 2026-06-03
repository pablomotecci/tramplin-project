package tramplin.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String VALID_SECRET = "this-is-a-valid-secret-key-32bytes-minimum!!";
    private static final long ACCESS_MS = 900_000L;
    private static final long REFRESH_MS = 604_800_000L;

    @Test
    void constructor_whenProdProfileAndDefaultSecret_thenThrowsIllegalStateException() {
        // given
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        // when / then
        assertThatThrownBy(() ->
                new JwtProvider(JwtProvider.INSECURE_DEFAULT_SECRET, ACCESS_MS, REFRESH_MS, env))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_whenProdProfileAndBlankSecret_thenThrowsIllegalStateException() {
        // given
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        // when / then
        assertThatThrownBy(() ->
                new JwtProvider("", ACCESS_MS, REFRESH_MS, env))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_whenProdProfileAndShortSecret_thenThrowsIllegalStateException() {
        // given
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        // when / then
        assertThatThrownBy(() ->
                new JwtProvider("short", ACCESS_MS, REFRESH_MS, env))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_whenDevProfileAndDefaultSecret_thenConstructsSuccessfully() {
        // given
        MockEnvironment env = new MockEnvironment(); // без prod-профиля = dev

        // when / then
        assertThatCode(() ->
                new JwtProvider(JwtProvider.INSECURE_DEFAULT_SECRET, ACCESS_MS, REFRESH_MS, env))
                .doesNotThrowAnyException();
    }

    @Test
    void getTokenType_whenAccessTokenGenerated_thenReturnsAccess() {
        // given
        MockEnvironment env = new MockEnvironment(); // dev
        JwtProvider provider = new JwtProvider(VALID_SECRET, ACCESS_MS, REFRESH_MS, env);
        String token = provider.generateAccessToken(UUID.randomUUID(), "test@test.ru", "APPLICANT");

        // when
        String tokenType = provider.getTokenType(token);

        // then
        assertThat(tokenType).isEqualTo("access");
    }
}
