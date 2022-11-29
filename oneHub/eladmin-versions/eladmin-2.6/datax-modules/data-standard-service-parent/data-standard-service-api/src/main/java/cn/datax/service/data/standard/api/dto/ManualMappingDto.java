package cn.datax.service.data.standard.api.dto;

import cn.datax.common.validate.ValidationGroups;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Data
public class ManualMappingDto implements Serializable {

    private static final long serialVersionUID=1L;

    @Valid
    @NotEmpty(message = "对照关系不能为空")
    @Size(min = 1, message="对照关系长度不能少于{min}位")
    private List<Endpoint> endpoints;
}
