
package com.platform;

import io.swagger.annotations.Api;
import com.platform.annotation.rest.AnonymousGetMapping;
import com.platform.utils.SpringContextHolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开启审计功能 -> @EnableJpaAuditing
 *
 * @author AllDataDC
 * @date 2023-01-27
 */

@EnableAsync
@RestController
@Api(hidden = true)
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class SystemServiceApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SystemServiceApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(SystemServiceApplication.class);
        springApplication.addListeners(new ApplicationPidFileWriter());
        springApplication.run(args);
    }

    /**
     * 访问首页提示
     *
     * @return /
     */
    @AnonymousGetMapping("/")
    public String index() {
        return "Studio service started successfully";
    }
}
