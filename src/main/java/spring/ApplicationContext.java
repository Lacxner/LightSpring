package spring;

import spring.annotation.Autowired;
import spring.annotation.Component;
import spring.annotation.ComponentScan;
import spring.annotation.Scope;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spring应用上下文
 * @author GaoZiYang
 * @since 2021年11月08日 17:32:48
 */
@SuppressWarnings("all")
public class ApplicationContext {
    /**
     * 是否允许循环引用
     */
    private boolean allowCircularReferences = true;

    /**
     * Spring Bean单例池（一级缓存）
     */
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    /**
     * Spring Bean定义池
     */
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    /**
     * 当前正在创建中的单例实例
     */
    private final Set<String> singletonsCurrentlyInCreation = new HashSet<>();
    /**
     * 提前的单例池（二级缓存）
     */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
    /**
     * 对象工厂集合（三级缓存）
     */
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
    /**
     * 后置处理器集合
     */
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    /**
     * 类路径解析的前缀
     */
    private static final String RESOLVED_CLASS_PATH_PREFIX = "classes\\";
    /**
     * 类路径解析的后缀
     */
    private static final String RESOLVED_CLASS_PATH_SUFFIX = ".class";

    public ApplicationContext() {}

    public ApplicationContext(Class<?> configClass) {
        Objects.requireNonNull(configClass, "配置类不能为空！");
        scan(configClass);
        refresh();
    }

    public ApplicationContext(String...basePackages) {
        Objects.requireNonNull(basePackages, "扫描路径不能为空！");
        scan(basePackages);
        refresh();
    }

    /**
     * 扫描配置文件所指定路径下的所有Spring Bean类，该类必须被 {@link spring.annotation.Component} 声明。
     * <br/>该方法的目的只是扫描组件，并将其构建成一个 {@link spring.BeanDefinition} 对象，放入 <b>BeanDefinitionMap</b> 集合中。
     * <br/>
     * @param cls 配置文件的Class对象
     */
    private void scan(Class<?> cls) {
        Objects.requireNonNull(cls);
        // 解析配置类
        ComponentScan componentScanAnnotation = cls.getDeclaredAnnotation(ComponentScan.class);
        // 获取扫描路径
        String[] basePackages = componentScanAnnotation.value();
        scan(basePackages);
    }

    /**
     * 扫描指定路径下的Spring Bean类
     * @param basePackages 扫描路径
     */
    public void scan(String...basePackages) {
        for (String basePackage : basePackages) {
            String resolvedScanPath = basePackage.replaceAll("\\.+", "/");
            URL url = ApplicationContext.class.getClassLoader().getResource(resolvedScanPath);
            Objects.requireNonNull(url, "未找到此路径！");
            // 获取此抽象路径文件
            String fileName = url.getFile();
            File file = new File(fileName);

            // 递归扫描指定的抽象路径
            recurseScan(file);
        }
    }

    /**
     * 递归扫描指定的抽象路径，该路径可能为文件也可能为目录
     * @param file 抽象路径
     */
    private void recurseScan(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recurseScan(f);
                }
            }
        } else {
            buildBeanDefinition(file);
        }
    }

    /**
     * 构建BeanDefinition
     * @param file 抽象路径
     */
    private void buildBeanDefinition(File file) {
        String name = file.getName();
        if (name.endsWith(RESOLVED_CLASS_PATH_SUFFIX)) {
            String absolutePath = file.getAbsolutePath();
            name = absolutePath.substring(absolutePath.indexOf(RESOLVED_CLASS_PATH_PREFIX) + RESOLVED_CLASS_PATH_PREFIX.length(), absolutePath.lastIndexOf(RESOLVED_CLASS_PATH_SUFFIX));
            String solvedName = name.replaceAll("\\\\+", ".");
            try {
                Class<?> beanClass = ApplicationContext.class.getClassLoader().loadClass(solvedName);
                // 检查是否为组件，如果为组件注解表示其为Spring Bean
                if (beanClass.isAnnotationPresent(Component.class)) {
                    // 判断当前Spring Bean是单例还是原型
                    Component componentAnnotation = beanClass.getDeclaredAnnotation(Component.class);
                    String beanName = componentAnnotation.value();

                    // Component组件如果未指定名称，默认为首字母小写的类名
                    if (beanName.length() == 0) {
                        String className = beanClass.getSimpleName();
                        char[] charArray = className.toCharArray();
                        // 如果首字母为大写则将其转换为小写
                        if (charArray[0] >= 65 && charArray[0] <= 90) {
                            charArray[0] += 32;
                        }
                        beanName = String.valueOf(charArray);
                    }

                    // 创建Spring Bean定义
                    BeanDefinition beanDefinition = new BeanDefinition();
                    beanDefinition.setCls(beanClass);
                    beanDefinition.setName(beanName);
                    if (beanClass.isAnnotationPresent(Scope.class)) {
                        Scope scopeAnnotation = beanClass.getDeclaredAnnotation(Scope.class);
                        beanDefinition.setScope(scopeAnnotation.value());
                    } else {
                        beanDefinition.setScope("singleton");
                    }
                    beanDefinitionMap.put(beanName, beanDefinition);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据 BeanDefinition 初始化组件实例。
     * <br/>Spring Bean对象分为 <b>单例（Singleton）</b> 和 <b>原型（Prototype）</b> 两种，默认为单例，单例对象会被放入 <b>单例池</b> 中，
     * 而原型对象则是在使用是创建。
     */
    public void refresh() {
        finishInitialization();
    }

    /**
     * 完成BeanDefinition的初始化工作
     */
    private void finishInitialization() {
       for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
           getBean(entry.getKey());
       }
    }

    /**
     * 根据Spring Bean名称获取实例，可用于单例和实例对象
     * @param cls Spring Bean类型
     * @param beanName Spring Bean名称
     * @return Bean对象
     */
    private Object doGetBean(Class<?> cls, String beanName) {
        Objects.requireNonNull(beanName);

        Object bean = null;
        boolean isMultiple = false;
        // 如果指定了类型，则先根据类型进行查找
        if (cls != null) {
            for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
                BeanDefinition beanDefinition = entry.getValue();
                if (cls.isAssignableFrom(beanDefinition.getCls())) {
                    if (bean == null) {
                        bean = doGetBean(cls, beanDefinition.getName());
                    } else {
                        isMultiple = true;
                        break;
                    }
                }
            }
        }

        // 根据名称进行查找
        if (cls == null || isMultiple || bean == null) {
            if (beanDefinitionMap.containsKey(beanName)) {
                BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
                if (beanDefinition.isSingleton()) {
                    bean = getSingleton(beanDefinition.getName());
                } else {
                    return createBean(beanDefinition);
                }
            }
        }

        // 如果根据名称也无法找到，则进行创建
        if (bean == null) {
            bean = getSingleton(beanName, () -> createBean(beanDefinitionMap.get(beanName)));
        }

        // 注册后置处理器
        if (bean instanceof BeanPostProcessor && !beanPostProcessors.contains(bean)) {
            beanPostProcessors.add((BeanPostProcessor) bean);
        }

        addSingleton(beanName, bean);
        return bean;
    }

    /**
     * 将完整的单例实例添加至单例池中
     * @param beanName Spring Bean名称
     * @param bean Spring Bean实例
     */
    private void addSingleton(String beanName, Object bean) {
        singletonObjects.put(beanName, bean);
        earlySingletonObjects.remove(beanName);
        singletonFactories.remove(beanName);
    }

    /**
     * 获取Spring Bean对象
     * @param beanName Spring Bean名称
     * @return Spring Bean对象
     */
    public Object getBean(String beanName) {
        return doGetBean(null, beanName);
    }

    /**
     * 获取单例实例
     * <p>
     *     先从单例池中获取，如果没有则从二级缓冲中获取，如果还是没有再通过对象工厂创建一个新单例实例。
     * </p>
     * <p>
     *     二级缓存的作用是在循环依赖中提高性能和实现单例对象，将对象工厂创建的新对象存入另一个早期的单例池中，但此时并不是一个完全的Spring Bean，
     * 而对象工厂每次创建都会消耗资源，所以将其缓存起来可以提高性能，同时也实现了对象单例。
     * </p>
     * @param beanName 实例的名称
     * @return 实例对象
     */
    private Object getSingleton(String beanName) {
        Object singletonObject = singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            singletonObject = earlySingletonObjects.get(beanName);
            if (singletonObject == null) {
                ObjectFactory<?> objectFactory = singletonFactories.get(beanName);
                if (objectFactory != null) {
                    singletonObject = objectFactory.getObject();
                    earlySingletonObjects.put(beanName, singletonObject);
                    singletonFactories.remove(beanName);
                }
            }
        }
        return singletonObject;
    }

    /**
     * 获取单例实例
     * <p>
     *     先从单例池中获取，如果获取不到就通过指定的对象工厂创建
     * </p>
     * @param beanName Spring Bean名称
     * @param objectFactory 对象工厂
     * @return 单例实例
     */
    private Object getSingleton(String beanName, ObjectFactory<?> objectFactory) {
        try {
            beforeSingletonCreation(beanName);

            Object singletonObject = singletonObjects.get(beanName);
            if (singletonObject == null) {
                singletonObject = objectFactory.getObject();
            }
            return singletonObject;
        } finally {
            afterSingletonCreation(beanName);
        }
    }

    /**
     * 在创建单例实例前记录Spring Bean名称
     * @param beanName Spring Bean名称
     */
    private void beforeSingletonCreation(String beanName) {
        singletonsCurrentlyInCreation.add(beanName);
    }

    /**
     * 在创建单例实例后移除Spring Bean名称
     * @param beanName Spring Bean名称
     */
    private void afterSingletonCreation(String beanName) {
        singletonsCurrentlyInCreation.remove(beanName);
    }

    /**
     * 判断某个单例实例是否正在创建当中
     * @param beanName 单例实例的名称
     * @return 如果返回true表示正在创建该单例实例，否则返回false
     */
    private boolean isSingletonCurrentlyInCreation(String beanName) {
        return singletonsCurrentlyInCreation.contains(beanName);
    }

    /**
     * 根据Spring Bean的定义创建Bean对象
     * @param beanDefinition Spring Bean定义
     * @return Bean对象
     */
    private Object createBean(BeanDefinition beanDefinition) {
        Objects.requireNonNull(beanDefinition);
        // 创建Spring Bean实例
        Object beanInstance = createBeanInstance(beanDefinition.getCls());

        // 是否允许提前暴露单例实例
        boolean earlySingletonExposure = beanDefinition.isSingleton()
                && allowCircularReferences
                && isSingletonCurrentlyInCreation(beanDefinition.getName());
        if (earlySingletonExposure) {
            // 这里暂未作任何加工
            singletonFactories.put(beanDefinition.getName(), () -> beanInstance);
        }

        // 自动注入
        populateBean(beanDefinition, beanInstance);

        // 初始化Spring Bean
        Object exposedBean = beanInstance;
        exposedBean = initializeBean(beanDefinition.getName(), exposedBean);
        return exposedBean;
    }

    /**
     * 初始化实例对象
     * @param beanName Spring Bean名称
     * @param bean 实例对象
     * @return 完成初始化的实例对象
     */
    private Object initializeBean(String beanName, Object bean) {
        invokeAwareMethods(beanName, bean);
        Object wrappedBean = bean;
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(beanName, wrappedBean);
        invokeInitMethods(beanName, wrappedBean);
        wrappedBean = applyBeanPostProcessorsAfterInitialization(beanName, wrappedBean);
        return wrappedBean;
    }

    /**
     * 在初始化之前应用后置处理器
     * <p>
     *     后置处理器使用了责任链模式，每次处理完的结果会交给下一个后置处理器，如果其中有个后置处理器返回null，则停止调用。
     * </p>
     * @param beanName Spring Bean名称
     * @param bean Spring Bean实例
     * @return 处理后的Spring Bean
     */
    private Object applyBeanPostProcessorsBeforeInitialization(String beanName, Object bean) {
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            Object result = beanPostProcessor.postProcessAfterInitialization(beanName, bean);
            if (result == null) {
                return bean;
            }
            bean = result;
        }
        return bean;
    }

    /**
     * 在初始化之后应用后置处理器
     * <p>
     *     后置处理器使用了责任链模式，每次处理完的结果会交给下一个后置处理器，如果其中有个后置处理器返回null，则停止调用。
     * </p>
     * @param beanName Spring Bean名称
     * @param bean Spring Bean实例
     * @return 处理后的Spring Bean
     */
    private Object applyBeanPostProcessorsAfterInitialization(String beanName, Object bean) {
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            Object result = beanPostProcessor.postProcessAfterInitialization(beanName, bean);
            if (result == null) {
                return bean;
            }
            bean = result;
        }
        return bean;
    }

    /**
     * 调用初始化方法
     * @param beanName Spring Bean名称
     * @param bean Spring Bean实例
     */
    private void invokeInitMethods(String beanName, Object bean) {
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }

    /**
     * 调用编织方法
     * @param beanName Spring Bean名称
     * @param bean Spring Bean实例
     */
    private void invokeAwareMethods(String beanName, Object bean) {
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
    }

    /**
     * 创建Spring Bean实例
     * @param cls Spring Bean的类型对象
     * @return Spring Bean实例
     */
    private Object createBeanInstance(Class<?> cls) {
        try {
            Constructor<?> constructor = cls.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据注解 {@link spring.annotation.Autowired} 自动注入填充Bean中的值
     * @param beanDefinition 要进行自动填充Bean的BeanDefinition
     */
    private void populateBean(BeanDefinition beanDefinition, Object bean) {
        for (Field field : beanDefinition.getCls().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                try {
                    field.setAccessible(true);
                    Autowired autowiredAnnotation = field.getDeclaredAnnotation(Autowired.class);
                    String name = "".equals(autowiredAnnotation.value()) ? field.getName() : autowiredAnnotation.value();
                    Object autowiredBean = getBean(name);
                    if (autowiredBean == null && autowiredAnnotation.required()) {
                        throw new NullPointerException("未找到指定对象！");
                    }
                    field.set(bean, autowiredBean);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 在Spring Bean创建完成后执行钩子函数
     * @param bean Spring Bean对象
     * @param beanDefinition Spring Bean定义
     */
    private void awareBean(Object bean, BeanDefinition beanDefinition) {
        // BeanNameAware钩子回调
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanDefinition.getName());
        }

        // InitializingBean钩子回调
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }
    }

    @FunctionalInterface
    private interface ObjectFactory<T> {
        /**
         * 使用对象工厂获取对象实例，在对象工厂中可以对实例进行加工，例如AOP操作
         * @return 对象实例
         */
        T getObject();
    }

    public boolean isAllowCircularReferences() {
        return allowCircularReferences;
    }

    public void setAllowCircularReferences(boolean allowCircularReferences) {
        this.allowCircularReferences = allowCircularReferences;
    }
}
