package cn.datax.service.system.service;

import cn.datax.service.system.api.dto.ConfigDto;
import cn.datax.service.system.api.entity.ConfigEntity;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 系统参数配置信息表 服务类
 * </p>
 *
 * @author yuwei
 * @date 2022-05-19
 */
public interface ConfigService extends BaseService<ConfigEntity> {

    ConfigEntity saveConfig(ConfigDto sysConfig);

    ConfigEntity updateConfig(ConfigDto sysConfig);

    ConfigEntity getConfigById(String id);

    void deleteConfigById(String id);

    void deleteConfigBatch(List<String> ids);

    String getConfig(String key);

    void refreshConfig();
}
