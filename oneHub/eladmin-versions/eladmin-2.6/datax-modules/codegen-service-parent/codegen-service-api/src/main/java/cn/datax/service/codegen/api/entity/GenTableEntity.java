package cn.datax.service.codegen.api.entity;

import cn.datax.common.base.BaseEntity;
import cn.datax.service.codegen.api.dto.GenColumnDto;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 代码生成信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("gen_table")
public class GenTableEntity extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 表名称
     */
    private String tableName;

    /**
     * 表描述
     */
    private String tableComment;

    /**
     * 实体类名称
     */
    private String className;

    /**
     * 生成包路径
     */
    private String packageName;

    /**
     * 生成模块名
     */
    private String moduleName;

    /**
     * 生成业务名
     */
    private String businessName;

    /**
     * 生成功能名
     */
    private String functionName;

    /**
     * 生成功能作者
     */
    private String functionAuthor;

    /**
     * 表字段
     */
    @TableField(value = "column_json", typeHandler = JacksonTypeHandler.class)
    private List<GenColumnDto> columns;
}
