package cn.datax.service.data.standard.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 对照表信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "standard_contrast", autoResultMap = true)
public class ContrastEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 数据源主键
     */
    private String sourceId;

    /**
     * 数据源
     */
    private String sourceName;

    /**
     * 数据表主键
     */
    private String tableId;

    /**
     * 数据表
     */
    private String tableName;

    /**
     * 数据表名称
     */
    private String tableComment;

    /**
     * 对照字段主键
     */
    private String columnId;

    /**
     * 对照字段
     */
    private String columnName;

    /**
     * 对照字段名称
     */
    private String columnComment;

    /**
     * 标准类别主键
     */
    private String gbTypeId;

    /**
     * 标准类别编码
     */
    @TableField(exist = false)
    private String gbTypeCode;

    /**
     * 标准类别名称
     */
    @TableField(exist = false)
    private String gbTypeName;

    /**
     * 绑定标准字段
     */
    private String bindGbColumn;

    /**
     * 对照数量
     */
    @TableField(exist = false)
    private Integer mappingCount;

    /**
     * 未对照数量
     */
    @TableField(exist = false)
    private Integer unMappingCount;
}
