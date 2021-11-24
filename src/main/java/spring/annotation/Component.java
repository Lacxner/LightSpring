package spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 组件标识
 * @author GaoZiYang
 * @since 2021年11月08日 17:39:20
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
    /**
     * 组件名称
     * @return 组件名称
     */
    String value() default "";
}
