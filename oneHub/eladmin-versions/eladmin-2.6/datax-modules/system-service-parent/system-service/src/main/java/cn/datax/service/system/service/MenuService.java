package cn.datax.service.system.service;

import cn.datax.service.system.api.dto.MenuDto;
import cn.datax.service.system.api.entity.MenuEntity;
import cn.datax.common.base.BaseService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-11
 */
public interface MenuService extends BaseService<MenuEntity> {

    MenuEntity saveMenu(MenuDto menu);

    MenuEntity updateMenu(MenuDto menu);

    void deleteMenuById(String id);
}
