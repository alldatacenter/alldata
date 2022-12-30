package cn.datax.service.data.visual.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 可视化图表配置信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "visual_chart", autoResultMap = true)
public class ChartEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 图表名称
     */
    private String chartName;

    /**
     * 图表缩略图(图片base64)
     */
    private String chartThumbnail;

    /**
     * 图表配置
     */
    @TableField(value = "chart_json")
    private String chartConfig;
}
