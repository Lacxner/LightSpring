package spring;

/**
 * Spring Bean名称的自动编织器
 * @author GaoZiYang
 * @since 2021年11月09日 17:44:11
 */
public interface BeanNameAware {
    /**
     * Spring会为该方法的 name 参数注入Spring Bean的名称
     * @param beanName Spring Bean的名称
     */
    void setBeanName(String beanName);
}
