package spring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于指定Spring Bean的作用域
 * <br/>
 * <ol>
 *     <li>
 *         singleton：单例。
 *     </li>
 *     <li>
 *         prototype：原型。
 *     </li>
 * </ol>
 * @author GaoZiYang
 * @since 2021年11月09日 11:02:06
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Scope {
    /**
     * 默认为单例（singleton）
     * @return Spring Bean的作用域
     */
    String value() default "singleton";
}
