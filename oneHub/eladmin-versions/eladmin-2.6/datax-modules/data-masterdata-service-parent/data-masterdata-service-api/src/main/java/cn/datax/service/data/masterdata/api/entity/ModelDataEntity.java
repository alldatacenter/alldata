package cn.datax.service.data.masterdata.api.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ModelDataEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tableName;
    private String id;
    private Map<String, Object> datas;
}
