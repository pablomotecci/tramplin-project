package tramplin.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Валидатор {@link Inn}. Реализует проверку контрольных цифр ИНН по алгоритму ФНС РФ.
 *
 * <ul>
 *   <li>10 цифр (юрлицо): одна контрольная цифра в позиции 10.</li>
 *   <li>12 цифр (ИП/физлицо): две контрольные цифры в позициях 11 и 12.</li>
 * </ul>
 */
public class InnValidator implements ConstraintValidator<Inn, String> {

    /** Весовые коэффициенты для контрольной цифры 10-значного ИНН. */
    private static final int[] WEIGHTS_10 = {2, 4, 10, 3, 5, 9, 4, 6, 8};
    /** Веса для 11-й цифры 12-значного ИНН. */
    private static final int[] WEIGHTS_11 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
    /** Веса для 12-й цифры 12-значного ИНН. */
    private static final int[] WEIGHTS_12 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null отдаём на откуп @NotBlank/@NotNull — отдельная ответственность.
        if (value == null) {
            return true;
        }
        // Только ASCII 0–9. Character.isDigit пропустил бы Unicode-цифры (арабско-индийские,
        // деванагари и т.п.), для которых charAt(i) - '0' ниже дал бы мусорную контрольную сумму.
        if (!isAsciiDigits(value)) {
            return false;
        }
        return switch (value.length()) {
            case 10 -> value.charAt(9) - '0' == checkDigit(value, WEIGHTS_10);
            case 12 -> value.charAt(10) - '0' == checkDigit(value, WEIGHTS_11)
                    && value.charAt(11) - '0' == checkDigit(value, WEIGHTS_12);
            default -> false;
        };
    }

    /** Строго ASCII-цифры 0–9, в отличие от {@link Character#isDigit}. */
    private static boolean isAsciiDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /** Контрольная цифра = (Σ цифра[i]·вес[i]) mod 11 mod 10. */
    private static int checkDigit(String inn, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += (inn.charAt(i) - '0') * weights[i];
        }
        return sum % 11 % 10;
    }
}
