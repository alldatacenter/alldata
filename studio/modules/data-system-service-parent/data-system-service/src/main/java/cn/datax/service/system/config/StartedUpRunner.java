package cn.datax.service.system.config;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.redis.service.RedisService;
import cn.datax.service.system.api.entity.ConfigEntity;
import cn.datax.service.system.api.entity.DictEntity;
import cn.datax.service.system.dao.ConfigDao;
import cn.datax.service.system.dao.DictDao;
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
    private RedisService redisService;
    @Autowired
    private DictDao dictDao;
    @Autowired
    private ConfigDao configDao;

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
            String dictKey = RedisConstant.SYSTEM_DICT_KEY;
            Boolean hasDictKey = redisService.hasKey(dictKey);
            if (!hasDictKey) {
                List<DictEntity> dictEntityList = dictDao.queryDictList(DataConstant.EnableState.ENABLE.getKey());
                redisService.set(dictKey, dictEntityList);
            }
            String configKey = RedisConstant.SYSTEM_CONFIG_KEY;
            Boolean hasConfigKey = redisService.hasKey(configKey);
            if (!hasConfigKey) {
                List<ConfigEntity> configEntityList = configDao.queryConfigList(DataConstant.EnableState.ENABLE.getKey());
                Map<String, Object> map = configEntityList.stream().collect(Collectors.toMap(ConfigEntity::getConfigKey, ConfigEntity::getConfigValue));
                redisService.hmset(configKey, map);
            }
        }
    }
}
