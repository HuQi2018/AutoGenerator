package org.jeecg.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static org.jeecg.util.FileUtil.getFilePath;

/**
 * @Description: 类加载器模块工具
 * @author: HuQi
 * @date: 2021年07月15日 18:43
 */
@Slf4j
public class ClassLoaderUtil extends URLClassLoader{

    //保存已经加载过的Class对象
    private Map<String,Class> cacheClassMap = new HashMap<>();

    //需要注册的spring bean的name集合
    private List<String> registeredBean = new ArrayList<>();

    private ApplicationContext applicationContext;

    private ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    //获取bean工厂，用来实现动态注入bean
    //不能使用其他类加载器加载bean
    //否则会出现异常:类未找到，类未定义
    private DefaultListableBeanFactory getBeanFactory(){
        return (DefaultListableBeanFactory) getApplicationContext().getAutowireCapableBeanFactory();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * @Description: 类加载器模块工具的构造方法，因继承URLClassLoader，前两个参数必须有，第三个为业务层传输数据的实体类
     * @param urls class文件存放地址
     * @param classLoader classLoader上下文的类加载器
     * @param fileList 要加载的文件地址列表
     * @param parent 包目录
     * @Author HuQi
     * @create 2021-08-24 15:25
     */
    public ClassLoaderUtil(URL[] urls, ClassLoader classLoader, List<String> fileList, String parent) {
        super(urls, classLoader);
        System.out.println(fileList);
        for (String file:fileList){
            file = getFilePath(file).replace("\\", ".").replace("/", ".");
            file = file.substring(file.indexOf(parent));
            String className = file.replace(".class", "").replaceAll("/", ".");

            Class<?> aClass = null;
            try {
                aClass = loadClass(className);
            } catch (ClassNotFoundException e) {
                log.error(className + "类加载失败！");
            }

            //将变量首字母置小写
            String beanName =  className.substring(className.lastIndexOf(".")+1);
            className = StringUtils.uncapitalize(beanName);

            cacheClassMap.put(className,aClass);
        }
        if (cacheClassMap.size()==0){
            log.info("无需要加载的字节码文件！");
        }
    }

    /**
     * @Description: loadClass方法
     * @param name 类名
     * @return java.lang.Class<?>
     * @Author HuQi
     * @create 2021-08-03 09:29
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if(findLoadedClass(name)==null){
            return super.loadClass(name);
        }else{
            return cacheClassMap.get(name);
        }

    }

    /**
     * @Description: 初始化spring bean，主要方法，将所有字节码加载到Spring内部，同时将Controller加载到RequestMappingHandlerMapping
     * @return java.util.List<java.lang.String>
     * @Author HuQi
     * @create 2021-08-03 09:33
     */
    public List<String> initBean(){
        List<String> controllers = new ArrayList<>();
        for (Map.Entry<String, Class> entry : cacheClassMap.entrySet()) {
            String beanName = entry.getKey();
            Class<?> cla = entry.getValue();

            load(beanName, cla);
            if (beanName.endsWith("Controller")){
                controllers.add(beanName);
            }
        }
        return controllers;
    }

    /**
     * @Description: 实例化加载方法
     * @param beanName 类名
     * @param cla 类的Class类型
     * @Author HuQi
     * @create 2021-08-11 14:14
     */
    private void load(String beanName, Class<?> cla) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(cla);
        GenericBeanDefinition beanDefinition = (GenericBeanDefinition) beanDefinitionBuilder.getRawBeanDefinition();
        //设置当前bean定义对象是单利的
        beanDefinition.setScope("singleton");

        if (beanName.endsWith("Mapper")) {
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(cla);
            beanDefinition.setBeanClass(MapperFactoryBean.class);
            beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
        }

        // service和mapper为接口注入时无法直接实例化，需另做配置
        if (beanName.endsWith("Service")){
            // 如果采用definition.getConstructorArgumentValues()，也就是构造方法注入
            // 则FactoryBean中需要提供包含该属性的构造方法，否则会注入失败
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(cla);
            //注意，这里的BeanClass是生成Bean实例的工厂，不是Bean本身。
            // FactoryBean是一种特殊的Bean，其返回的对象不是指定类的一个实例，
            // 其返回的是该工厂Bean的getObject方法所返回的对象。
            beanDefinition.setBeanClass(ServiceProxy.class);
            // 这里采用的是byType方式注入，类似的还有byName等
            beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
        }

        try {
            getApplicationContext().getBean(beanName);
        }catch (Exception e){
            if (e.getMessage().startsWith("No bean named ")) {
                getBeanFactory().registerBeanDefinition(beanName, beanDefinition);
                registeredBean.add(beanName);
//                log.info("注册bean:" + beanName);
            }
        }
    }

    //获取当前类加载器注册的bean
    public List<String> getRegisteredBean() {
        return registeredBean;
    }
}
