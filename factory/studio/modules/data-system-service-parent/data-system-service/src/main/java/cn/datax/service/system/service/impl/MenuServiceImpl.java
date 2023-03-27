package cn.datax.service.system.service.impl;

import cn.datax.common.exception.DataException;
import cn.datax.service.system.api.dto.MenuDto;
import cn.datax.service.system.api.entity.MenuEntity;
import cn.datax.service.system.dao.MenuDao;
import cn.datax.service.system.mapstruct.MenuMapper;
import cn.datax.service.system.service.MenuService;
import cn.datax.common.base.BaseServiceImpl;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class MenuServiceImpl extends BaseServiceImpl<MenuDao, MenuEntity> implements MenuService {

    @Autowired
    private MenuDao menuDao;
    @Autowired
    private MenuMapper menuMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuEntity saveMenu(MenuDto menuDto) {
        MenuEntity menu = menuMapper.toEntity(menuDto);
        menuDao.insert(menu);
        return menu;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuEntity updateMenu(MenuDto menuDto) {
        MenuEntity menu = menuMapper.toEntity(menuDto);
        menuDao.updateById(menu);
        return menu;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenuById(String id) {
        int n = menuDao.selectCount(Wrappers.<MenuEntity>lambdaQuery().eq(MenuEntity::getParentId, id));
        if(n > 0){
            throw new DataException("该菜单下存在子菜单数据");
        }
        menuDao.deleteById(id);
    }
}
