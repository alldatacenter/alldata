package cn.datax.service.data.market.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import cn.datax.service.data.market.api.dto.FieldRule;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import cn.datax.common.base.BaseEntity;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 数据API脱敏信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "market_api_mask", autoResultMap = true)
public class ApiMaskEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 数据API
     */
    private String apiId;

    /**
     * 脱敏名称
     */
    private String maskName;

    /**
     * 脱敏字段规则配置
     */
    @TableField(value = "config_json", typeHandler = JacksonTypeHandler.class)
    private List<FieldRule> rules;
}
