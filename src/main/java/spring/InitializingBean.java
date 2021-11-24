package spring;

/**
 * 初始化Spring Bean的钩子
 * @author GaoZiYang
 * @since 2021年11月10日 17:05:02
 */
public interface InitializingBean {
    /**
     * Spring Bean属性设置的后置处理器
     */
    void afterPropertiesSet();
}
