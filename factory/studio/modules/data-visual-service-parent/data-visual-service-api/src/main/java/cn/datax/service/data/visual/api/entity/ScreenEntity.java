package cn.datax.service.data.visual.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import cn.datax.service.data.visual.api.dto.ScreenConfig;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 可视化酷屏配置信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "visual_screen", autoResultMap = true)
public class ScreenEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 酷屏名称
     */
    private String screenName;

    /**
     * 酷屏缩略图(图片base64)
     */
    private String screenThumbnail;

    /**
     * 酷屏配置
     */
    @TableField(value = "screen_json", typeHandler = JacksonTypeHandler.class)
    private ScreenConfig screenConfig;

    @TableField(exist = false)
    private List<ChartEntity> charts;
}
