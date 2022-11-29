package cn.datax.service.data.visual.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ColumnParse implements Serializable {

    private static final long serialVersionUID=1L;

    private String col;
    private String alias;
}
