package cn.datax.service.data.visual.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import cn.datax.service.data.visual.api.dto.SchemaConfig;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据集信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "visual_data_set", autoResultMap = true)
public class DataSetEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 数据源
     */
    private String sourceId;

    /**
     * 数据集名称
     */
    private String setName;

    /**
     * 数据集sql
     */
    private String setSql;

    /**
     * 数据模型定义
     */
    @TableField(value = "schema_json", typeHandler = JacksonTypeHandler.class)
    private SchemaConfig schemaConfig;
}
