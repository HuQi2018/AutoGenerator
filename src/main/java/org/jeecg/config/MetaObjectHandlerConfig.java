package org.jeecg.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.jeecg.util.GeneratorCodeUtil;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @Description: 设置mybatis plus默认值方法
 * @author: HuQi
 * @date: 2021年07月03日 09:04
 */

@Component
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Date currentDate = new Date();
        //创建时间默认当前时间
        setFieldValByName("createBy", GeneratorCodeUtil.getUser().getUsername(), metaObject);
        setFieldValByName("updateBy", GeneratorCodeUtil.getUser().getUsername(), metaObject);
        setFieldValByName("createTime", currentDate,metaObject);
        setFieldValByName("updateTime", currentDate,metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Date currentDate = new Date();
        //修改时间
        setFieldValByName("updateBy",GeneratorCodeUtil.getUser().getUsername(), metaObject);
        setFieldValByName("updateTime",currentDate,metaObject);
    }
}
