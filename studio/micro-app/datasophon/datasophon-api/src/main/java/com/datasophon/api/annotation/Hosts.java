package com.datasophon.api.annotation;

import com.datasophon.api.utils.HostsValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HostsValidator.class)
public @interface Hosts {

    /**
     * @return the error message template
     */
    String message() default "invalid host list.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
