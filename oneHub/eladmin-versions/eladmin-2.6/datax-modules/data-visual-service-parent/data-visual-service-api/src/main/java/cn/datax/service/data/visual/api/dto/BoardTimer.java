package cn.datax.service.data.visual.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BoardTimer implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private Integer milliseconds;
}
