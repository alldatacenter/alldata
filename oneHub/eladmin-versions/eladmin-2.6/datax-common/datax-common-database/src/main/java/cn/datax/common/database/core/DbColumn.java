package cn.datax.common.database.core;

import lombok.Data;

@Data
public class DbColumn {

    /**
     * 列名
     */
    private String colName;

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
     * 是否主键
     */
    private Boolean colKey;

    /**
     * 是否允许为空
     */
    private Boolean nullable;

    /**
     * 列的序号
     */
    private Integer colPosition;

    /**
     * 列默认值
     */
    private String dataDefault;

    /**
     * 列注释
     */
    private String colComment;
}
