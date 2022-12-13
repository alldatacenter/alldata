package cn.datax.service.data.standard.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 字典对照信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "standard_contrast_dict", autoResultMap = true)
public class ContrastDictEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 字典对照主键
     */
    private String contrastId;

    /**
     * 数据源
     */
    @TableField(exist = false)
    private String sourceName;

    /**
     * 数据表
     */
    @TableField(exist = false)
    private String tableName;

    /**
     * 对照字段
     */
    @TableField(exist = false)
    private String columnName;

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
     * 字典编码
     */
    private String colCode;

    /**
     * 字典名称
     */
    private String colName;

    /**
     * 对照的标准字典
     */
    private String contrastGbId;

    /**
     * 对照的标准编码
     */
    @TableField(exist = false)
    private String contrastGbCode;

    /**
     * 对照的标准名称
     */
    @TableField(exist = false)
    private String contrastGbName;
}
