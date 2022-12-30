package com.alibaba.sreworks.pmdb.domain.req.datasource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.common.constant.DataSourceConstant;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * 数据源配置
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/09/08 19:34
 */
@ApiModel(value="数据源配置")
public class DataSourceUpdateReq extends DataSourceBaseReq {

    @ApiModelProperty(value = "数据源id", required = true)
    Integer id;

    public Integer getId() {
        Preconditions.checkArgument(id != null, "数据源id不允许为空");
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public JSONObject getConnectConfig() {
        if (CollectionUtils.isEmpty(connectConfig)) {
            return null;
        }

        Preconditions.checkArgument(StringUtils.isNotEmpty(type), "需要提供数据源类型");

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
