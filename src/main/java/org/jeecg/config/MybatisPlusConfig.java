package org.jeecg.config;

import org.apache.commons.collections4.IteratorUtils;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.optimize.JsqlParserCountOptimize;
import com.google.common.collect.Sets;
import org.apache.ibatis.session.SqlSessionFactory;
//import org.jeecg.util.MybatisMapperRefresh;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author HuQi
 * Spring boot方式 -Mybatis-plus 分页
 */
@Configuration
@MapperScan(value={"org.jeecg.**.mapper*"})
public class MybatisPlusConfig {

    @Autowired
    private MybatisPlusProperties mybatisPlusProperties;

    @Bean
    public PaginationInterceptor paginationInterceptor() {
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        // 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
        paginationInterceptor.setOverflow(false);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setLimit(500);
        // 开启 count 的 join 优化,只针对部分 left join
        paginationInterceptor.setCountSqlParser(new JsqlParserCountOptimize());
        return paginationInterceptor;
    }


//    /**
//     * 自动刷新插件
//     *
//     * @return
//     */
//    @ConditionalOnProperty("mybatis-plus.global-config.refresh")
//    @Bean
//    public MybatisMapperRefresh mybatisMapperRefresh(ApplicationContext applicationContext,
//                                                     SqlSessionFactory sqlSessionFactory) {
//        Set<Resource> mapperLocations = Sets.newLinkedHashSet();
//        for (String xx : mybatisPlusProperties.getMapperLocations()) {
//            try {
//                mapperLocations.addAll(Arrays.asList(applicationContext.getResources(xx)));
//            } catch (Exception e) {
//                continue;
//            }
//        }
//        List<Resource> list = IteratorUtils.toList(mapperLocations.iterator());
//        Resource[] array = list.toArray(new Resource[list.size()]);
//        MybatisMapperRefresh mybatisMapperRefresh = new MybatisMapperRefresh(array, sqlSessionFactory, 10, 5, true);
//        return mybatisMapperRefresh;
//    }
}
