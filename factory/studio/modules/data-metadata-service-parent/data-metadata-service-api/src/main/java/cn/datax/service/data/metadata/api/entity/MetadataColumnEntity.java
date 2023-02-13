package cn.datax.service.data.metadata.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 元数据信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Data
@Accessors(chain = true)
@TableName(value = "metadata_column", autoResultMap = true)
public class MetadataColumnEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 所属数据源
     */
    private String sourceId;

    /**
     * 所属数据表
     */
    private String tableId;

    /**
     * 字段名称
     */
    private String columnName;

    /**
     * 字段注释
     */
    private String columnComment;

    /**
     * 字段是否主键(1是0否)
     */
    private String columnKey;

    /**
     * 字段是否允许为空(1是0否)
     */
    private String columnNullable;

    /**
     * 字段序号
     */
    private String columnPosition;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 数据长度
     */
    private String dataLength;

    /**
     * 数据精度
     */
    private String dataPrecision;

    /**
     * 数据小数位
     */
    private String dataScale;

    /**
     * 数据默认值
     */
    private String dataDefault;

    @TableField(exist = false)
    private String sourceName;

    @TableField(exist = false)
    private String tableName;

    @TableField(exist = false)
    private String tableComment;
}
