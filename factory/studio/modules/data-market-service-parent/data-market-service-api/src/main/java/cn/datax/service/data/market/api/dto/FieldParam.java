package cn.datax.service.data.market.api.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@ApiModel(value = "字段信息Model")
@Data
public class FieldParam implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 列名
     */
    private String columnName;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 数据长度
     */
    private Integer dataLength;

    /**
     * 数据精度
     */
    private Integer dataPrecision;

    /**
     * 数据小数位
     */
    private Integer dataScale;

    /**
     * 是否主键
     */
    private String columnKey;

    /**
     * 是否允许为空
     */
    private String columnNullable;

    /**
     * 列的序号
     */
    private Integer columnPosition;

    /**
     * 列默认值
     */
    private String dataDefault;

    /**
     * 列注释
     */
    private String columnComment;

    /**
     * 作为请求参数
     */
    private String reqable;

    /**
     * 作为返回参数
     */
    private String resable;
}
