package tramplin.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Проверка российского ИНН: только цифры, длина 10 (юрлицо) или 12 (ИП/физлицо)
 * и корректные контрольные цифры по алгоритму ФНС.
 * <p>
 * {@code null} считается валидным — комбинируйте с {@link jakarta.validation.constraints.NotBlank},
 * если поле обязательно.
 */
@Documented
@Constraint(validatedBy = InnValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface Inn {

    String message() default "Некорректный ИНН: ожидается 10 или 12 цифр с верной контрольной суммой";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
