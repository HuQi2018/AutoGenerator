package org.jeecg.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @Description:  swagger启动类
 * @author: HuQi
 * @date: 2021年07月01日 13:10
 */
@Configuration
@EnableSwagger2
@EnableKnife4j  //第三方swagger增强API注解
public class SwaggerConfig {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("后台管理接口")
                .apiInfo(apiInfo())
                .host("localhost:8084")
                .select()
                .apis(RequestHandlerSelectors.basePackage("org.jeecg"))
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("代码生成器项目接口文档")
                .description("系统化信息化管理平台")
                .termsOfServiceUrl("")
                .version("1.0")
                .build();
    }
}