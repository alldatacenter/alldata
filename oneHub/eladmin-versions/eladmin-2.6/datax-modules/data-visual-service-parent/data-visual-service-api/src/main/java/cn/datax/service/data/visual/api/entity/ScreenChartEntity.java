package cn.datax.service.data.visual.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.datax.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 可视化酷屏和图表关联表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Data
@Accessors(chain = true)
@TableName("visual_screen_chart")
public class ScreenChartEntity implements Serializable {

    private static final long serialVersionUID=1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 酷屏ID
     */
    private String screenId;

    /**
     * 图表ID
     */
    private String chartId;
}
