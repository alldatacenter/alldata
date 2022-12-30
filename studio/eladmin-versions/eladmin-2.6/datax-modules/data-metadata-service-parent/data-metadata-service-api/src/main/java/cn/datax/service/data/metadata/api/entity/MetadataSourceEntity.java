package cn.datax.service.data.metadata.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import cn.datax.service.data.metadata.api.dto.DbSchema;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 数据源信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "metadata_source", autoResultMap = true)
public class MetadataSourceEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 数据源类型
     */
    private String dbType;

    /**
     * 数据源名称
     */
    private String sourceName;

    /**
     * 元数据同步（0否，1同步中, 2是 3-出错）
     */
    private String isSync;

    /**
     * 数据源连接信息
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private DbSchema dbSchema;
}
