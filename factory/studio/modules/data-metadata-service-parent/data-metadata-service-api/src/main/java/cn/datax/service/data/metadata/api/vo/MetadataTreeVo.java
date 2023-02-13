package cn.datax.service.data.metadata.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MetadataTreeVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    /**
     * 数据层级 database、table、column
     */
    private String type;
    private String label;
    private String name;
    private String code;
    private List<MetadataTreeVo> children;
}
