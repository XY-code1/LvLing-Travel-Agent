package com.guido.scenicai.common.interceptor;

import java.lang.annotation.*;

/**
 * Mark admin write operations for automatic sys_log recording (log_type=OPERATION).
 *
 * <p>Usage: annotate any Controller method that performs a write (insert/update/delete).
 * The aspect captures method name + args as request_summary (desensitized),
 * return value as response_summary, and logs errors automatically.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** Business description, e.g. "新增景区" */
    String value() default "";
}
