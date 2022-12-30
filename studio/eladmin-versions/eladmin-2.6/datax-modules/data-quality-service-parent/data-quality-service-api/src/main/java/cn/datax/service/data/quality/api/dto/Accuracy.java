package cn.datax.service.data.quality.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 准确性
 */
@Data
public class Accuracy implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 最大长度
     */
    private Integer maxLength;
}
