package cn.datax.service.data.visual.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ScreenAttr implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String chartName;
    private Integer milliseconds;
    private String border;
    private String backgroundColor;
}
