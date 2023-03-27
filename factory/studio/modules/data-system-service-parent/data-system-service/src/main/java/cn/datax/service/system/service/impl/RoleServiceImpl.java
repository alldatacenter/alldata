package cn.datax.service.system.service.impl;

import cn.datax.common.exception.DataException;
import cn.datax.service.system.api.dto.RoleDto;
import cn.datax.service.system.api.entity.RoleDeptEntity;
import cn.datax.service.system.api.entity.RoleEntity;
import cn.datax.service.system.api.entity.RoleMenuEntity;
import cn.datax.service.system.dao.RoleDao;
import cn.datax.service.system.dao.RoleDeptDao;
import cn.datax.service.system.dao.RoleMenuDao;
import cn.datax.service.system.mapstruct.RoleMapper;
import cn.datax.service.system.service.RoleService;
import cn.datax.common.base.BaseServiceImpl;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yuwei
 * @date 2022-09-04
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class RoleServiceImpl extends BaseServiceImpl<RoleDao, RoleEntity> implements RoleService {

    @Autowired
    private RoleDao roleDao;
    @Autowired
    private RoleMenuDao roleMenuDao;
    @Autowired
    private RoleDeptDao roleDeptDao;
    @Autowired
    private RoleMapper roleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleEntity saveRole(RoleDto roleDto) {
        RoleEntity role = roleMapper.toEntity(roleDto);
        int n = roleDao.selectCount(Wrappers.<RoleEntity>lambdaQuery().eq(RoleEntity::getRoleName, role.getRoleName()));
        if(n > 0){
            throw new DataException("该角色名已存在");
        }
        roleDao.insert(role);
        if(CollUtil.isNotEmpty(roleDto.getMenuList())){
            insertBatchMenu(roleDto.getMenuList(), role.getId());
        }
        if(CollUtil.isNotEmpty(roleDto.getDeptList())){
            insertBatchDept(roleDto.getDeptList(), role.getId());
        }
        return role;
    }

    private void insertBatchMenu(List<String> menus, String roleId) {
        List<RoleMenuEntity> roleMenuList = menus
                .stream().map(menuId -> {
                    RoleMenuEntity roleMenu = new RoleMenuEntity();
                    roleMenu.setRoleId(roleId);
                    roleMenu.setMenuId(menuId);
                    return roleMenu;
                }).collect(Collectors.toList());
        roleMenuDao.insertBatch(roleMenuList);
    }

    private void insertBatchDept(List<String> depts, String roleId) {
        List<RoleDeptEntity> roleDeptList = depts
                .stream().map(deptId -> {
                    RoleDeptEntity roleDept = new RoleDeptEntity();
                    roleDept.setRoleId(roleId);
                    roleDept.setDeptId(deptId);
                    return roleDept;
                }).collect(Collectors.toList());
        roleDeptDao.insertBatch(roleDeptList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleEntity updateRole(RoleDto roleDto) {
        RoleEntity role = roleMapper.toEntity(roleDto);
        roleDao.updateById(role);
        roleMenuDao.delete(Wrappers.<RoleMenuEntity>lambdaQuery()
                .eq(RoleMenuEntity::getRoleId, role.getId()));
        if(CollUtil.isNotEmpty(roleDto.getMenuList())){
            insertBatchMenu(roleDto.getMenuList(), role.getId());
        }
        roleDeptDao.delete(Wrappers.<RoleDeptEntity>lambdaQuery()
                .eq(RoleDeptEntity::getRoleId, role.getId()));
        if(CollUtil.isNotEmpty(roleDto.getDeptList())){
            insertBatchDept(roleDto.getDeptList(), role.getId());
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleById(String id) {
        roleMenuDao.deleteByRoleId(id);
        roleDeptDao.deleteByRoleId(id);
        roleDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleBatch(List<String> ids) {
        roleMenuDao.deleteByRoleIds(ids);
        roleDeptDao.deleteByRoleIds(ids);
        roleDao.deleteBatchIds(ids);
    }
}
