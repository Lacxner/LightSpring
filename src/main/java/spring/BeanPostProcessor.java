package spring;

/**
 * Spring Bean的后置处理器
 * @author GaoZiYang
 * @since 2021年11月10日 17:45:33
 */
public interface BeanPostProcessor {
    /**
     * Spring Bean初始化前的钩子函数
     * @param bean Spring Bean对象
     * @param beanName Bean名称
     * @return Spring Bean对象
     */
    default Object postProcessBeforeInitialization(String beanName, Object bean) {
        return bean;
    }

    /**
     * Spring Bean初始化后的钩子函数
     * @param bean Spring Bean对象
     * @param beanName Bean名称
     * @return Spring Bean对象
     */
    default Object postProcessAfterInitialization(String beanName, Object bean) {
        return bean;
    }
}
