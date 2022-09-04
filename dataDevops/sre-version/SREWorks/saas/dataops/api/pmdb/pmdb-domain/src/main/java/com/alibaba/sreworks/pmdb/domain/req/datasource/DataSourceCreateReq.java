package com.alibaba.sreworks.pmdb.domain.req.datasource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.common.constant.Constant;
import com.alibaba.sreworks.pmdb.common.constant.DataSourceConstant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * 数据源配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="数据源配置")
public class DataSourceCreateReq extends DataSourceBaseReq {
    @Override
    public String getName() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "数据源名称不允许为空");
        return name;
    }

    @Override
    public String getType() {
        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "数据源类型不允许为空");
        Preconditions.checkArgument(DataSourceConstant.DATASOURCE_TYPES.contains(type.toLowerCase()), "数据源类型非法");
        type = type.toLowerCase();
        return type;
    }

    @Override
    public String getAppId() {
        return appId != null ? appId : Constant.SREWORKS_APP_ID;
    }

    @Override
    public JSONObject getConnectConfig() {
        Preconditions.checkArgument(!CollectionUtils.isEmpty(connectConfig), "数据源链接配置不允许为空");
        JSONObject result = null;
        if (getType().equals(DataSourceConstant.ES)) {
            try {
                ESConnectConfig esConnectConfig = JSONObject.toJavaObject(connectConfig, ESConnectConfig.class);
                result = (JSONObject)JSONObject.toJSON(esConnectConfig);
            } catch (Exception ex) {
                Preconditions.checkArgument(false, "数据源链接配置错误, 合法格式" + ESConnectConfig.getTipMsg());
            }

        }

        if (getType().equals(DataSourceConstant.MYSQL)) {
            try {
                MysqlConnectConfig mysqlConnectConfig = JSONObject.toJavaObject(connectConfig, MysqlConnectConfig.class);
                result = (JSONObject)JSONObject.toJSON(mysqlConnectConfig);
            } catch (Exception ex) {
                Preconditions.checkArgument(false, "数据源链接配置错误, 合法格式" + MysqlConnectConfig.getTipMsg());
            }
        }

        return result;
    }
}
