package cn.datax.service.data.metadata.config;

import cn.datax.common.core.RedisConstant;
import cn.datax.common.redis.service.RedisService;
import cn.datax.service.data.metadata.api.entity.MetadataAuthorizeEntity;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.entity.MetadataTableEntity;
import cn.datax.service.data.metadata.dao.MetadataAuthorizeDao;
import cn.datax.service.data.metadata.dao.MetadataColumnDao;
import cn.datax.service.data.metadata.dao.MetadataSourceDao;
import cn.datax.service.data.metadata.dao.MetadataTableDao;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
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
    private MetadataSourceDao metadataSourceDao;

    @Autowired
    private MetadataTableDao metadataTableDao;

    @Autowired
    private MetadataColumnDao metadataColumnDao;

    @Autowired
    private MetadataAuthorizeDao metadataAuthorizeDao;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
            String sourceKey = RedisConstant.METADATA_SOURCE_KEY;
            Boolean hasSourceKey = redisService.hasKey(sourceKey);
            if (!hasSourceKey) {
                List<MetadataSourceEntity> sourceEntityList = metadataSourceDao.selectList(Wrappers.emptyWrapper());
                redisService.set(sourceKey, sourceEntityList);
            }

            String tableKey = RedisConstant.METADATA_TABLE_KEY;
            Boolean hasTableKey = redisService.hasKey(tableKey);
            if (!hasTableKey) {
                List<MetadataTableEntity> tableEntityList = metadataTableDao.selectList(Wrappers.emptyWrapper());
                Map<String, List<MetadataTableEntity>> tableListMap = tableEntityList.stream().collect(Collectors.groupingBy(MetadataTableEntity::getSourceId));
                redisTemplate.opsForHash().putAll(tableKey, tableListMap);
            }

            String columnKey = RedisConstant.METADATA_COLUMN_KEY;
            Boolean hasColumnKey = redisService.hasKey(columnKey);
            if (!hasColumnKey) {
                List<MetadataColumnEntity> columnEntityList = metadataColumnDao.selectList(Wrappers.emptyWrapper());
                Map<String, List<MetadataColumnEntity>> columnListMap = columnEntityList.stream().collect(Collectors.groupingBy(MetadataColumnEntity::getTableId));
                redisTemplate.opsForHash().putAll(columnKey, columnListMap);
            }

            String authorizeKey = RedisConstant.METADATA_AUTHORIZE_KEY;
            Boolean hasAuthorizeKey = redisService.hasKey(authorizeKey);
            if (!hasAuthorizeKey) {
                List<MetadataAuthorizeEntity> metadataAuthorizeList = metadataAuthorizeDao.selectList(Wrappers.emptyWrapper());
                Map<String, List<MetadataAuthorizeEntity>> authorizeListMap = metadataAuthorizeList.stream().collect(Collectors.groupingBy(MetadataAuthorizeEntity::getRoleId));
                redisTemplate.opsForHash().putAll(authorizeKey, authorizeListMap);
            }
        }
    }
}
