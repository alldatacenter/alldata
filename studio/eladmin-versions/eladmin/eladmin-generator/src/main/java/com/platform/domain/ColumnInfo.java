
package com.platform.domain;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.platform.utils.GenUtil;
import javax.persistence.*;
import java.io.Serializable;

/**
 * 列的数据信息
 * @author AllDataDC
 * @date 2022-10-27
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "code_column_config")
public class ColumnInfo implements Serializable {

    @Id
    @Column(name = "column_id")
    @ApiModelProperty(value = "ID", hidden = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ApiModelProperty(value = "表名")
    private String tableName;

    @ApiModelProperty(value = "数据库字段名称")
    private String columnName;

    @ApiModelProperty(value = "数据库字段类型")
    private String columnType;

    @ApiModelProperty(value = "数据库字段键类型")
    private String keyType;

    @ApiModelProperty(value = "字段额外的参数")
    private String extra;

    @ApiModelProperty(value = "数据库字段描述")
    private String remark;

    @ApiModelProperty(value = "是否必填")
    private Boolean notNull;

    @ApiModelProperty(value = "是否在列表显示")
    private Boolean listShow;

    @ApiModelProperty(value = "是否表单显示")
    private Boolean formShow;

    @ApiModelProperty(value = "表单类型")
    private String formType;

    @ApiModelProperty(value = "查询 1:模糊 2：精确")
    private String queryType;

    @ApiModelProperty(value = "字典名称")
    private String dictName;

    @ApiModelProperty(value = "日期注解")
    private String dateAnnotation;

    public ColumnInfo(String tableName, String columnName, Boolean notNull, String columnType, String remark, String keyType, String extra) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.columnType = columnType;
        this.keyType = keyType;
        this.extra = extra;
        this.notNull = notNull;
        if(GenUtil.PK.equalsIgnoreCase(keyType) && GenUtil.EXTRA.equalsIgnoreCase(extra)){
            this.notNull = false;
        }
        this.remark = remark;
        this.listShow = true;
        this.formShow = true;
    }
}
