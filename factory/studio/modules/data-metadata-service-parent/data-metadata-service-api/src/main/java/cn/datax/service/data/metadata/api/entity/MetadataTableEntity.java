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
 * 数据库表信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Data
@Accessors(chain = true)
@TableName(value = "metadata_table", autoResultMap = true)
public class MetadataTableEntity implements Serializable {

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
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    @TableField(exist = false)
    private String sourceName;
}
