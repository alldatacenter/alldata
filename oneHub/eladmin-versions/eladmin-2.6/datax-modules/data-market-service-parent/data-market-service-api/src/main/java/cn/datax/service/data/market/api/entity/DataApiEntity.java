package cn.datax.service.data.market.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import cn.datax.service.data.market.api.dto.ExecuteConfig;
import cn.datax.service.data.market.api.dto.RateLimit;
import cn.datax.service.data.market.api.dto.ReqParam;
import cn.datax.service.data.market.api.dto.ResParam;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 数据API信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "market_api", autoResultMap = true)
public class DataApiEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * API名称
     */
    private String apiName;

    /**
     * API版本
     */
    private String apiVersion;

    /**
     * API路径
     */
    private String apiUrl;

	/**
	 * 数据源id
	 */
	private String sourceId;

    /**
     * 请求类型
     */
    private String reqMethod;

    /**
     * 返回格式
     */
    private String resType;

    /**
     * IP黑名单多个，隔开
     */
    private String deny;

    /**
     * 限流配置
     */
    @TableField(value = "limit_json", typeHandler = JacksonTypeHandler.class)
    private RateLimit rateLimit;


    /**
     * 执行配置
     */
    @TableField(value = "config_json", typeHandler = JacksonTypeHandler.class)
    private ExecuteConfig executeConfig;

    /**
     * 请求参数
     */
    @TableField(value = "req_json", typeHandler = JacksonTypeHandler.class)
    private List<ReqParam> reqParams;

    /**
     * 返回字段
     */
    @TableField(value = "res_json", typeHandler = JacksonTypeHandler.class)
    private List<ResParam> resParams;
}
