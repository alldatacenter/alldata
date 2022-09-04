package com.alibaba.tesla.appmanager.server.service.appoption;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.enums.AppOptionTypeEnum;
import com.alibaba.tesla.appmanager.common.enums.AppOptionUpdateModeEnum;
import com.alibaba.tesla.appmanager.server.repository.domain.AppOptionDO;

import java.util.List;

/**
 * 应用元信息配置项服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface AppOptionService {

    /**
     * 初始化指定应用的默认应用配置
     *
     * @param appId 应用 ID
     */
    void init(String appId);

    /**
     * 获取指定应用下的指定 Option 内容
     *
     * @param appId 应用 ID
     * @param key   Key
     * @return AppOptionDO
     */
    AppOptionDO getOption(String appId, String key);

    /**
     * 获取指定应用下的全部 Option 内容
     *
     * @param appId 应用 ID
     * @return List of AppOptionDO
     */
    List<AppOptionDO> getOptions(String appId);

    /**
     * 获取指定应用下的全部 Option 内容 (带缓存, 可能有延迟)
     *
     * @param appId 应用 ID
     * @return List of AppOptionDO
     */
    List<AppOptionDO> getCachedOptions(String appId);

    /**
     * 获取指定应用下的全部 Option 内容 (JSONObject 形式)
     *
     * @param appId 应用 ID
     * @return JSONObject
     */
    JSONObject getOptionMap(String appId);

    /**
     * 更新指定应用下的指定 Option 选项
     *
     * @param appId     应用 ID
     * @param key       Key
     * @param value     Value
     * @param valueType Value 类型
     * @return 更新选项后的对象
     */
    AppOptionDO updateOption(String appId, String key, String value, AppOptionTypeEnum valueType);

    /**
     * 批量更新指定应用下的全部 Options
     *
     * @param appId   应用 ID
     * @param options Options Map
     * @param mode    配置更新方式 (append / overwrite)
     * @return 当前应用修改后的全量 Options 列表
     */
    List<AppOptionDO> updateOptions(String appId, JSONObject options, AppOptionUpdateModeEnum mode);

    /**
     * 删除指定应用下的指定 Key 的配置对象
     *
     * @param appId 应用 ID
     * @param key   Key
     * @return 删除计数
     */
    int deleteOption(String appId, String key);

    /**
     * 删除指定应用下的全部 Options
     *
     * @param appId 应用 ID
     * @return 删除的配置项条目数
     */
    int deleteOptions(String appId);
}
