package com.alibaba.tesla.appmanager.server.service.appoption.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.RedisKeyConstant;
import com.alibaba.tesla.appmanager.common.enums.AppOptionTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.AppOptionUpdateModeEnum;
import com.alibaba.tesla.appmanager.server.repository.AppOptionRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppOptionQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDO;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionConstant;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionService;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionType;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionTypeManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 应用元信息配置项服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class AppOptionServiceImpl implements AppOptionService {

    @Autowired
    private AppOptionRepository appOptionRepository;

    @Autowired
    private AppOptionTypeManager appOptionTypeManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 初始化指定应用的默认应用配置
     *
     * @param appId 应用 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void init(String appId) {
        List<AppOptionDO> currentOptions = getOptions(appId);
        if (currentOptions.size() > 0) {
            log.info("action=appOptionInit|message=no need to init application options|appId={}|optionSize={}",
                    appId, currentOptions.size());
            return;
        }

        List<AppOptionDO> options = generateDefaultOptions(appId);
        int updated = appOptionRepository.batchInsert(options);
        log.info("action=appOptionInit|message=options have inited|appId={}|updated={}", appId, updated);
    }

    /**
     * 获取指定应用下的指定 Option 内容
     *
     * @param appId 应用 ID
     * @param key   Key
     * @return AppOptionDO
     */
    @Override
    public AppOptionDO getOption(String appId, String key) {
        return appOptionRepository.getByCondition(AppOptionQueryCondition.builder().appId(appId).key(key).build());
    }

    /**
     * 获取指定应用下的全部 Option 内容
     *
     * @param appId 应用 ID
     * @return List of AppOptionDO
     */
    @Override
    public List<AppOptionDO> getOptions(String appId) {
        return appOptionRepository.selectByCondition(AppOptionQueryCondition.builder().appId(appId).build());
    }

    /**
     * 获取指定应用下的全部 Option 内容 (带缓存, 可能有延迟)
     *
     * @param appId 应用 ID
     * @return List of AppOptionDO
     */
    @Override
    public List<AppOptionDO> getCachedOptions(String appId) {
        String key = appOptionRedisKey(appId);
        String value = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(value)) {
            try {
                return JSONArray.parseArray(value, AppOptionDO.class);
            } catch (Exception e) {
                // 降级
                log.warn("cannot decode redis value for app option|appId={}|value={}", appId, value);
            }
        }

        List<AppOptionDO> options = getOptions(appId);
        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(options), 10, TimeUnit.MINUTES);
        log.info("set cached options|appId={}|options={}", appId, JSONObject.toJSONString(options));
        return options;
    }

    /**
     * 获取指定应用下的全部 Option 内容 (JSONObject 形式)
     *
     * @param appId 应用 ID
     * @return JSONObject
     */
    @Override
    public JSONObject getOptionMap(String appId) {
        JSONObject optionMap = new JSONObject();
        getOptions(appId).forEach(item ->
                optionMap.put(item.getKey(), appOptionTypeManager.get(item.getValueType()).decode(item.getValue())));
        return optionMap;
    }

    /**
     * 更新指定应用下的指定 Option 选项
     *
     * @param appId     应用 ID
     * @param key       Key
     * @param value     Value
     * @param valueType Value 类型
     * @return 更新选项后的对象
     */
    @Override
    public AppOptionDO updateOption(String appId, String key, String value, AppOptionTypeEnum valueType) {
        AppOptionDO record = AppOptionDO.builder()
                .appId(appId)
                .key(key)
                .value(value)
                .valueType(valueType.toString())
                .build();
        AppOptionQueryCondition condition = AppOptionQueryCondition.builder().appId(appId).key(key).build();
        appOptionRepository.updateByCondition(record, condition);
        log.info("action=appOptionUpdate|message=option has updated|appId={}|key={}|value={}|valueType={}",
                appId, key, value, valueType);
        return appOptionRepository.getByCondition(condition);
    }

    /**
     * 批量更新指定应用下的全部 Options
     *
     * @param appId     应用 ID
     * @param optionMap Options Map
     * @param mode      配置更新方式 (append / overwrite)
     * @return 当前应用修改后的全量 Options 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AppOptionDO> updateOptions(String appId, JSONObject optionMap, AppOptionUpdateModeEnum mode) {
        List<AppOptionDO> options = new ArrayList<>();
        for (Map.Entry<String, Object> entry : optionMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            AppOptionTypeEnum valueTypeEnum = AppOptionConstant.VALUE_TYPE_MAP.get(key);
            if (valueTypeEnum == null) {
                log.info("unknown key when update app options|key={}", key);
                continue;
            }
            AppOptionType appOptionType = appOptionTypeManager.get(valueTypeEnum.toString());
            options.add(AppOptionDO.builder()
                    .appId(appId)
                    .key(key)
                    .valueType(valueTypeEnum.toString())
                    .value(appOptionType.encode(value))
                    .build());
        }

        if (mode == AppOptionUpdateModeEnum.APPEND) {
            options.forEach(option -> {
                AppOptionQueryCondition condition = AppOptionQueryCondition.builder()
                        .appId(appId)
                        .key(option.getKey())
                        .build();
                AppOptionDO record = appOptionRepository.getByCondition(condition);
                if (record == null) {
                    appOptionRepository.insert(option);
                    log.info("action=appOptionUpdate|message=option have inserted|appId={}|key={}|value={}",
                            appId, option.getKey(), option.getValue());
                } else {
                    record.setValue(option.getValue());
                    record.setValueType(option.getValueType());
                    int updated = appOptionRepository.updateByCondition(record, condition);
                    log.info("action=appOptionUpdate|message=option have updated|appId={}|key={}|value={}|updated={}",
                            appId, option.getKey(), option.getValue(), updated);
                }
            });
        } else {
            int deleted = deleteOptions(appId);
            int updated = appOptionRepository.batchInsert(options);
            log.info("action=appOptionUpdate|message=options have updated|appId={}|deleted={}|updated={}",
                    appId, deleted, updated);
        }
        return getOptions(appId);
    }

    /**
     * 删除指定应用下的指定 Key 的配置对象
     *
     * @param appId 应用 ID
     * @param key   Key
     * @return 删除计数
     */
    @Override
    public int deleteOption(String appId, String key) {
        AppOptionQueryCondition condition = AppOptionQueryCondition.builder().appId(appId).key(key).build();
        int deleted = appOptionRepository.deleteByCondition(condition);
        log.info("action=appOptionDelete|message=option has deleted|appId={}|key={}|deleted={}", appId, key, deleted);
        return deleted;
    }

    /**
     * 删除指定应用下的全部 Options
     *
     * @param appId 应用 ID
     * @return 删除的配置项条目数
     */
    @Override
    public int deleteOptions(String appId) {
        AppOptionQueryCondition condition = AppOptionQueryCondition.builder().appId(appId).build();
        int deleted = appOptionRepository.deleteByCondition(condition);
        log.info("action=appOptionDelete|message=options have deleted|appId={}|deleted={}", appId, deleted);
        return deleted;
    }

    /**
     * 获取指定应用的默认全量应用配置选项
     *
     * @param appId 应用 ID
     * @return List of AppOptionDO
     */
    private List<AppOptionDO> generateDefaultOptions(String appId) {
        List<AppOptionDO> options = new ArrayList<>();
        for (Map.Entry<String, AppOptionTypeEnum> entry : AppOptionConstant.VALUE_TYPE_MAP.entrySet()) {
            String key = entry.getKey();
            AppOptionTypeEnum valueTypeEnum = entry.getValue();
            AppOptionType appOptionType = appOptionTypeManager.get(valueTypeEnum.toString());
            String value = AppOptionConstant.DEFAULT_VALUE_MAP.get(key);
            if (value == null) {
                value = appOptionType.defaultValue();
            }
            options.add(AppOptionDO.builder()
                    .appId(appId)
                    .key(key)
                    .value(value)
                    .valueType(valueTypeEnum.toString())
                    .build());
        }
        return options;
    }

    /**
     * 获取指定 appId 的 option 缓存在 redis 中的 Key
     *
     * @param appId 应用 ID
     * @return Redis Key
     */
    private static String appOptionRedisKey(String appId) {
        return RedisKeyConstant.APP_OPTION + appId;
    }
}
