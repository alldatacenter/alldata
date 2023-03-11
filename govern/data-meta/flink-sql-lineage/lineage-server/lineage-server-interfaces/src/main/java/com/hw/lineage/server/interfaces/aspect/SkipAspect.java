package com.hw.lineage.server.interfaces.aspect;

import java.lang.annotation.*;

/**
 * @description: SkipAspect
 * @author: HamaWhite
 * @version: 1.0.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkipAspect {
}
