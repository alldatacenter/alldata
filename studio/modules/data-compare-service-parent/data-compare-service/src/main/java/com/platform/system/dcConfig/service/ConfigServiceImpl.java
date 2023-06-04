package com.platform.system.dcConfig.service;

import java.util.List;
import javax.annotation.PostConstruct;

import com.platform.common.constant.Constants;
import com.platform.common.constant.UserConstants;
import com.platform.common.utils.StringUtils;
import com.platform.common.utils.text.Convert;
import com.platform.system.dcConfig.domain.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.platform.common.exception.ServiceException;
import com.platform.system.dcConfig.mapper.ConfigMapper;

/**
 * 参数配置 服务层实现
 * 
 * @author AllDataDC
 */
@Service
public class ConfigServiceImpl implements IConfigService
{
    @Autowired
    private ConfigMapper configMapper;

    /**
     * 项目启动时，初始化参数到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingConfigCache();
    }

    /**
     * 查询参数配置信息
     * 
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public Config selectConfigById(Long configId)
    {
        Config config = new Config();
        config.setConfigId(configId);
        return configMapper.selectConfig(config);
    }

    /**
     * 根据键名查询参数配置信息
     * 
     * @param configKey 参数名称
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey)
    {
        Config config = new Config();
        config.setConfigKey(configKey);
        Config retConfig = configMapper.selectConfig(config);
        if (StringUtils.isNotNull(retConfig)) {
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 查询参数配置列表
     * 
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<Config> selectConfigList(Config config)
    {
        return configMapper.selectConfigList(config);
    }

    /**
     * 新增参数配置
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(Config config)
    {
        config.setCreateBy("admin");
        int row = configMapper.insertConfig(config);
        return row;
    }

    /**
     * 修改参数配置
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int updateConfig(Config config)
    {
        config.setUpdateBy("admin");
        int row = configMapper.updateConfig(config);
        return row;
    }

    /**
     * 批量删除参数配置对象
     * 
     * @param ids 需要删除的数据ID
     */
    @Override
    public void deleteConfigByIds(String ids)
    {
        Long[] configIds = Convert.toLongArray(ids);
        for (Long configId : configIds)
        {
            Config config = selectConfigById(configId);
            if (StringUtils.equals(UserConstants.YES, config.getConfigType()))
            {
                throw new ServiceException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            configMapper.deleteConfigById(configId);
        }
    }

    /**
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache() {}

    /**
     * 清空参数缓存数据
     */
    @Override
    public void clearConfigCache(){}

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache()
    {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     * 
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public String checkConfigKeyUnique(Config config)
    {
        Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        Config info = configMapper.checkConfigKeyUnique(config.getConfigKey());
        if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue())
        {
            return UserConstants.CONFIG_KEY_NOT_UNIQUE;
        }
        return UserConstants.CONFIG_KEY_UNIQUE;
    }

    /**
     * 获取cache name
     * 
     * @return 缓存名
     */
    private String getCacheName()
    {
        return Constants.SYS_CONFIG_CACHE;
    }

    /**
     * 设置cache key
     * 
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey)
    {
        return Constants.SYS_CONFIG_KEY + configKey;
    }
}
