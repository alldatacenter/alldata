package cn.datax.service.data.visual.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
public class BoardConfig implements Serializable {

    private static final long serialVersionUID=1L;

    @NotEmpty(message = "布局不能为空")
    private List<BoardItem> layout;

    private List<BoardTimer> interval;
}
