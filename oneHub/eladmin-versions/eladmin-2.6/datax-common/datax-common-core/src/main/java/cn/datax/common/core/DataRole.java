package cn.datax.common.core;

import lombok.Data;

import java.io.Serializable;

@Data
public class DataRole implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String dataScope;
}
