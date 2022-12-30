package cn.datax.service.data.standard.api.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ContrastTreeVo implements Serializable {

    private static final long serialVersionUID=1L;

    private String id;
    private String label;
    private String name;
    /**
     * 数据
     */
    private Object data;
    private List<ContrastTreeVo> children;
}
