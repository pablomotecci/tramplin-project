package tramplin.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InnValidatorTest {

    private InnValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InnValidator();
    }

    @Test
    void isValid_whenValidTenDigitInn_thenTrue() {
        // given
        String inn = "7707083893";

        // when
        boolean result = validator.isValid(inn, null);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenValidTwelveDigitInn_thenTrue() {
        // given
        String inn = "500100732259";

        // when
        boolean result = validator.isValid(inn, null);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isValid_whenTenDigitInnWithWrongChecksum_thenFalse() {
        // given
        String inn = "7707083890";

        // when
        boolean result = validator.isValid(inn, null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenTwelveDigitInnWithWrongChecksum_thenFalse() {
        // given
        String inn = "500100732250";

        // when
        boolean result = validator.isValid(inn, null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenElevenDigits_thenFalse() {
        // given
        String inn = "12345678901";

        // when
        boolean result = validator.isValid(inn, null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenContainsNonDigits_thenFalse() {
        // given
        String inn = "77070838AB";

        // when
        boolean result = validator.isValid(inn, null);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenNull_thenTrue() {
        // given
        String inn = null;

        // when
        boolean result = validator.isValid(inn, null);

        // then
        assertThat(result).isTrue();
    }
}
