package cn.datax.service.data.masterdata.api.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class ModelCommentEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tableName;
    private String columnName;
    private String comment;
}
