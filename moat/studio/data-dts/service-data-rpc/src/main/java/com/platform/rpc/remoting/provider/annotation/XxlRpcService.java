package com.platform.rpc.remoting.provider.annotation;

import java.lang.annotation.*;

/**
 * rpc service annotation, skeleton of stub ("@Inherited" allow service use "Transactional")
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface XxlRpcService {

    /**
     * @return
     */
    String version() default "";

}
