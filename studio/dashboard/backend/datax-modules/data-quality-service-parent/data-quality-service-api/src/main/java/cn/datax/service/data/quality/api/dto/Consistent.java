package cn.datax.service.data.quality.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 一致性
 */
@Data
public class Consistent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String gbTypeId;
    private String gbTypeCode;
    private String gbTypeName;
    private String bindGbColumn;
}
