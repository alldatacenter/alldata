package cn.datax.service.workflow.config;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.redis.service.RedisService;
import cn.datax.service.workflow.api.entity.BusinessEntity;
import cn.datax.service.workflow.dao.BusinessDao;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StartedUpRunner implements ApplicationRunner {

    private final ConfigurableApplicationContext context;
    private final Environment environment;

    @Autowired
    private BusinessDao businessDao;

    @Autowired
    private RedisService redisService;

    @Override
    public void run(ApplicationArguments args) {
        if (context.isActive()) {
            String banner = "-----------------------------------------\n" +
                    "服务启动成功，时间：" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()) + "\n" +
                    "服务名称：" + environment.getProperty("spring.application.name") + "\n" +
                    "端口号：" + environment.getProperty("server.port") + "\n" +
                    "-----------------------------------------";
            System.out.println(banner);

            // 项目启动时，初始化缓存
            String businessKey = RedisConstant.WORKFLOW_BUSINESS_KEY;
            Boolean hasBusinessKey = redisService.hasKey(businessKey);
            if (!hasBusinessKey) {
                List<BusinessEntity> businessEntityList = businessDao.selectList(Wrappers.<BusinessEntity>lambdaQuery()
                        .eq(BusinessEntity::getStatus, DataConstant.EnableState.ENABLE.getKey()));
                Map<String, Object> map = businessEntityList.stream().collect(Collectors.toMap(BusinessEntity::getBusinessCode, v -> v, (v1, v2) -> v2));
                redisService.hmset(businessKey, map);
            }
        }
    }
}
