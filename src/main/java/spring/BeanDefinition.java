package spring;

import java.util.Objects;

/**
 * Spring Bean定义
 * @author GaoZiYang
 * @since 2021年08月10日 23:33:48
 */
public class BeanDefinition {
    /**
     * Bean对象
     */
    private Class<?> clazz;

    /**
     * 名称
     */
    private String name;

    /**
     * 作用域
     */
    private String scope;

    public Class<?> getCls() {
        return clazz;
    }

    public void setCls(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * 判断是否为单例
     * @return 如果返回true表示为单例实例，反之为原型实例
     */
    public boolean isSingleton() {
        Objects.requireNonNull(scope, "作用域为空！");
        return "singleton".equals(scope) || "".equals(scope);
    }
}
