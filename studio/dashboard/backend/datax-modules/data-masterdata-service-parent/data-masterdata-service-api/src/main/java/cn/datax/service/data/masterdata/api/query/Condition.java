package cn.datax.service.data.masterdata.api.query;

import lombok.Data;

import java.io.Serializable;

@Data
public class Condition implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据库字段名
     */
    private String column;

    /**
     * 字段值
     */
    private String value;

    /**
     * 查询类型，如like,eq,gt,ge,lt,le,eq,ne,between
     */
    private String queryType;

    /**
     * 查询类型between时left查询字段值
     */
    private String leftValue;

    /**
     * 查询类型between时right查询字段值
     */
    private String rightValue;
}
