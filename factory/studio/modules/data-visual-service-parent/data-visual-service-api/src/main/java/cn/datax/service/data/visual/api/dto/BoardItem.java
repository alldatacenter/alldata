package cn.datax.service.data.visual.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class BoardItem implements Serializable {

    private static final long serialVersionUID=1L;

    private Integer x;
    private Integer y;
    private Integer w;
    private Integer h;
    private String i;
}
