package cn.datax.service.data.visual.service;

import cn.datax.service.data.visual.api.entity.ScreenEntity;
import cn.datax.service.data.visual.api.dto.ScreenDto;
import cn.datax.common.base.BaseService;

import java.util.List;

/**
 * <p>
 * 可视化酷屏配置信息表 服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-15
 */
public interface ScreenService extends BaseService<ScreenEntity> {

    ScreenEntity saveScreen(ScreenDto screen);

    ScreenEntity updateScreen(ScreenDto screen);

    ScreenEntity getScreenById(String id);

    void deleteScreenById(String id);

    void deleteScreenBatch(List<String> ids);

    void copyScreen(String id);

    void buildScreen(ScreenDto screen);
}
