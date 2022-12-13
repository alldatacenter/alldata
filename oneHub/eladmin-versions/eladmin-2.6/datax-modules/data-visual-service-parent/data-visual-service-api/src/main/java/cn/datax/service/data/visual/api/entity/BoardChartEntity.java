package cn.datax.service.data.visual.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 可视化看板和图表关联表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-11
 */
@Data
@Accessors(chain = true)
@TableName("visual_board_chart")
public class BoardChartEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 看板ID
     */
    private String boardId;

    /**
     * 图表ID
     */
    private String chartId;
}
