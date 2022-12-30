package cn.datax.service.data.visual.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
public class ScreenConfig implements Serializable {

    private static final long serialVersionUID=1L;

    private Integer width;
    private Integer height;
    private Integer scale;
    private String backgroundImage;
    @NotEmpty(message = "布局不能为空")
    private List<ScreenItem> layout;
    private List<ScreenAttr> property;
}
