package cn.datax.service.data.metadata.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 元数据变更记录表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "metadata_change_record", autoResultMap = true)
public class MetadataChangeRecordEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 源数据的表名或者能唯一对应的源数据表的标识（可废弃）
     */
    private String objectType;

    /**
     * 源数据表主键
     */
    private String objectId;

    /**
     * 修改的源数据表的字段名
     */
    private String fieldName;

    /**
     * 该字段原来的值
     */
    private String fieldOldValue;

    /**
     * 该字段最新的值
     */
    private String fieldNewValue;

    /**
     * 数据源
     */
    @TableField(exist = false)
    private String sourceId;
    @TableField(exist = false)
    private String sourceName;

    /**
     * 数据库表
     */
    @TableField(exist = false)
    private String tableId;
    @TableField(exist = false)
    private String tableName;
}
