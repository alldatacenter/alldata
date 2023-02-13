package cn.datax.service.data.visual.api.entity;

import cn.datax.common.base.DataScopeBaseEntity;
import cn.datax.service.data.visual.api.dto.BoardConfig;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 可视化看板配置信息表
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "visual_board", autoResultMap = true)
public class BoardEntity extends DataScopeBaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 看板名称
     */
    private String boardName;

    /**
     * 看板缩略图(图片base64)
     */
    private String boardThumbnail;

    /**
     * 看板配置
     */
    @TableField(value = "board_json", typeHandler = JacksonTypeHandler.class)
    private BoardConfig boardConfig;

    @TableField(exist = false)
    private List<ChartEntity> charts;
}
