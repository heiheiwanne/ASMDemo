package com.demo.lib.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Java类描述
 *
 * @author : xmq
 * @date : 2019/1/10 下午7:18
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface Cost {
}
