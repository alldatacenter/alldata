package cn.datax.service.data.market.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import cn.datax.service.data.market.api.dto.HttpService;
import cn.datax.service.data.market.api.dto.WebService;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 服务集成表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-20
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "market_service_integration", autoResultMap = true)
public class ServiceIntegrationEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 服务编号
     */
    private String serviceNo;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务类型（1http接口，2webservice接口）
     */
    private String serviceType;

    /**
     * http接口
     */
    @TableField(value = "httpservice_json", typeHandler = JacksonTypeHandler.class)
    private HttpService httpService;

    /**
     * webservice接口
     */
    @TableField(value = "webservice_json", typeHandler = JacksonTypeHandler.class)
    private WebService webService;
}
