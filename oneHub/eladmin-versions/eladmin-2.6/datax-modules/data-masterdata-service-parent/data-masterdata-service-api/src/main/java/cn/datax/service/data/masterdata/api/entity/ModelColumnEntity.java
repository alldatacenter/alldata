package cn.datax.service.data.masterdata.api.entity;

import cn.datax.service.data.masterdata.api.enums.MysqlDataTypeEnum;
import cn.datax.service.data.masterdata.api.parser.ColumnParser;
import cn.datax.service.data.masterdata.api.parser.DataType;
import cn.datax.service.data.masterdata.api.parser.mysql.MysqlColumnParser;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.datax.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 主数据模型列信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("masterdata_model_column")
public class ModelColumnEntity extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 模型表主键
     */
    private String modelId;

    /**
     * 列名称
     */
    private String columnName;

    /**
     * 列描述
     */
    private String columnComment;

    /**
     * 列类型
     */
    private String columnType;

    /**
     * 列长度
     */
    private String columnLength;

    /**
     * 列小数位数
     */
    private String columnScale;

    /**
     * 列默认值
     */
    private String defaultValue;

    /**
     * 是否系统默认（0否，1是）
     */
    private String isSystem;

    /**
     * 是否主键（0否，1是）
     */
    private String isPk;

    /**
     * 是否必填（0否，1是）
     */
    private String isRequired;

    /**
     * 是否为插入字段（0否，1是）
     */
    private String isInsert;

    /**
     * 是否编辑字段（0否，1是）
     */
    private String isEdit;

    /**
     * 是否详情字段（0否，1是）
     */
    private String isDetail;

    /**
     * 是否列表字段（0否，1是）
     */
    private String isList;

    /**
     * 是否查询字段（0否，1是）
     */
    private String isQuery;

    /**
     * 查询方式（EQ等于、NE不等于、GT大于、GE大于等于、LT小于、LE小于等于、LIKE模糊、BETWEEN范围）
     */
    private String queryType;

    /**
     * 是否绑定数据标准（0否，1是）
     */
    private String isBindDict;

    /**
     * 绑定数据标准类别
     */
    private String bindDictTypeId;

    /**
     * 绑定数据标准字典字段（GB_CODE，GB_NAME）
     */
    private String bindDictColumn;

    /**
     * 显示类型（input文本框、textarea文本域、select下拉框、checkbox复选框、radio单选框、datetime日期控件）
     */
    private String htmlType;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 列属性
     */
    @TableField(exist = false)
    private String columnDefinition;

    public String getColumnDefinition() {
        ColumnParser columnParser = new MysqlColumnParser();
        DataType parse = columnParser.mysqlParse(MysqlDataTypeEnum.match(this.columnType, MysqlDataTypeEnum.VARCHAR));
        return parse.fillTypeString(this.columnLength, this.columnScale);
    }
}
