package org.jeecg.util;

import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * @Description: 接口实例工厂，这里主要是用于提供接口的实例对象
 * @author: HuQi
 * @date: 2021年07月16日 13:48
 */
public class ServiceFactory<T> implements FactoryBean<Object> {

    private Class<T> interfaceType;

    public ServiceFactory(Class<T> interfaceType) {
        this.interfaceType = interfaceType;
    }


    @Override
    public Object getObject() throws Exception {
        //这里主要是创建接口对应的实例，便于注入到spring容器中
        InvocationHandler handler = new ServiceProxy<>(interfaceType);
        return Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[]{interfaceType}, handler);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceType;
    }
}
