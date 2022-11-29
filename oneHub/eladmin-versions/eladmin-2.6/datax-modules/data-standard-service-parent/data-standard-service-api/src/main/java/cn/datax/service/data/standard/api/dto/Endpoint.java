package cn.datax.service.data.standard.api.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class Endpoint implements Serializable {

    private static final long serialVersionUID=1L;

    @NotBlank(message = "源端点不能为空")
    private String sourceId;

    @NotBlank(message = "目标端点不能为空")
    private String targetId;
}
