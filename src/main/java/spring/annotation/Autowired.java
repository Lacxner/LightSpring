package spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动注入注解，先根据类型进行自动注入，如果未找到指定类型对象或者找到多个，就通过Bean名称进行查找，如果还是未找到，则返回空或抛出异常
 * @author GaoZiYang
 * @since 2021年11月09日 14:55:33
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Autowired {
    /**
     * 指定要注入Bean的名称
     * @return 要注入Bean的名称
     */
    String value() default "";

    /**
     * 是否必须注入Bean
     * @return 如果必须要注入则返回true，反之返回false
     */
    boolean required() default true;
}
