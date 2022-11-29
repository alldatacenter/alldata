package cn.datax.service.data.market.mapping.config;

import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.api.feign.DataApiServiceFeign;
import cn.datax.service.data.market.mapping.handler.MappingHandlerMapping;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartedUpRunner implements ApplicationRunner {

    private final ConfigurableApplicationContext context;
    private final Environment environment;

    @Autowired
    private DataApiServiceFeign dataApiServiceFeign;

    @Autowired
    private MappingHandlerMapping mappingHandlerMapping;

    @Override
    public void run(ApplicationArguments args) {
        if (context.isActive()) {
            String banner = "-----------------------------------------\n" +
                    "服务启动成功，时间：" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()) + "\n" +
                    "服务名称：" + environment.getProperty("spring.application.name") + "\n" +
                    "端口号：" + environment.getProperty("server.port") + "\n" +
                    "-----------------------------------------";
            System.out.println(banner);

            // 项目启动时，初始化已发布的接口
            List<DataApiEntity> releaseDataApiList = dataApiServiceFeign.getReleaseDataApiList();
            if (CollUtil.isNotEmpty(releaseDataApiList)) {
                releaseDataApiList.forEach(api -> mappingHandlerMapping.registerMapping(api));
            }
        }
    }
}
