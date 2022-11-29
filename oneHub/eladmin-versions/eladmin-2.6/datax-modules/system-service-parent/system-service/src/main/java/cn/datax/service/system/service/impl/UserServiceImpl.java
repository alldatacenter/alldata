package cn.datax.service.system.service.impl;

import cn.datax.common.base.DataScope;
import cn.datax.common.core.DataConstant;
import cn.datax.common.core.RedisConstant;
import cn.datax.common.exception.DataException;
import cn.datax.common.redis.service.RedisService;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.system.api.dto.UserDto;
import cn.datax.service.system.api.dto.UserPasswordDto;
import cn.datax.service.system.api.entity.*;
import cn.datax.service.system.api.vo.RoleVo;
import cn.datax.service.system.api.vo.UserInfo;
import cn.datax.service.system.api.vo.UserVo;
import cn.datax.service.system.api.vo.route.MetaVo;
import cn.datax.service.system.api.vo.route.RouteVo;
import cn.datax.service.system.dao.*;
import cn.datax.service.system.mapstruct.UserMapper;
import cn.datax.service.system.service.UserService;
import cn.datax.common.base.BaseServiceImpl;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author yuwei
 * @since 2019-09-04
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class UserServiceImpl extends BaseServiceImpl<UserDao, UserEntity> implements UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserPostDao userPostDao;

    @Autowired
    private UserRoleDao userRoleDao;

    @Autowired
    private MenuDao menuDao;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisService redisService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserEntity saveUser(UserDto userDto) {
        UserEntity user = userMapper.toEntity(userDto);
        int n = userDao.selectCount(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, user.getUsername()));
        if(n > 0){
            throw new DataException("该用户名已存在");
        }
        String passwordEncode = new BCryptPasswordEncoder().encode(user.getPassword());
        user.setPassword(passwordEncode);
        userDao.insert(user);
        if(CollUtil.isNotEmpty(userDto.getRoleList())){
            insertBatchRole(userDto.getRoleList(), user.getId());
        }
        if(CollUtil.isNotEmpty(userDto.getPostList())){
            insertBatchPost(userDto.getPostList(), user.getId());
        }
        return user;
    }

    private void insertBatchPost(List<String> posts, String userId) {
        List<UserPostEntity> userPostList = posts
                .stream().map(postId -> {
                    UserPostEntity userPost = new UserPostEntity();
                    userPost.setUserId(userId);
                    userPost.setPostId(postId);
                    return userPost;
                }).collect(Collectors.toList());
        userPostDao.insertBatch(userPostList);
    }

    private void insertBatchRole(List<String> roles, String userId) {
        List<UserRoleEntity> userRoleList = roles
                .stream().map(roleId -> {
                    UserRoleEntity userRole = new UserRoleEntity();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                }).collect(Collectors.toList());
        userRoleDao.insertBatch(userRoleList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserEntity updateUser(UserDto userDto) {
        UserEntity user = userMapper.toEntity(userDto);
        userDao.updateById(user);
        userRoleDao.delete(Wrappers.<UserRoleEntity>lambdaQuery()
                .eq(UserRoleEntity::getUserId, user.getId()));
        if(CollUtil.isNotEmpty(userDto.getRoleList())){
            insertBatchRole(userDto.getRoleList(), user.getId());
        }
        userPostDao.delete(Wrappers.<UserPostEntity>lambdaQuery()
                .eq(UserPostEntity::getUserId, user.getId()));
        if(CollUtil.isNotEmpty(userDto.getPostList())){
            insertBatchPost(userDto.getPostList(), user.getId());
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserById(String id) {
        userRoleDao.deleteByUserId(id);
        userPostDao.deleteByUserId(id);
        userDao.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserBatch(List<String> ids) {
        userRoleDao.deleteByUserIds(ids);
        userPostDao.deleteByUserIds(ids);
        userDao.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPassword(UserPasswordDto userPasswordDto) {
        String userId = SecurityUtil.getUserId();
        UserEntity userEntity = userDao.selectById(userId);
        if(!new BCryptPasswordEncoder().matches(userPasswordDto.getOldPassword(), userEntity.getPassword())){
            throw new DataException("旧密码不正确");
        }
        String passwordEncode = new BCryptPasswordEncoder().encode(userPasswordDto.getPassword());
        userDao.updateUserPassword(passwordEncode, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(UserPasswordDto userPasswordDto) {
        Object o = redisService.hget(RedisConstant.SYSTEM_CONFIG_KEY, "sys.user.password");
        String password = (String) Optional.ofNullable(o).orElseThrow(() -> new DataException("请先配置初始化密码"));
        String passwordEncode = new BCryptPasswordEncoder().encode(password);
        userDao.updateUserPassword(passwordEncode, userPasswordDto.getId());
    }

    @Override
    public UserInfo getUserByUsername(String username) {
        UserInfo userInfo = new UserInfo();
        UserEntity userEntity = userDao.selectOne(Wrappers.<UserEntity>query()
                .lambda().eq(UserEntity::getUsername, username));
        if(null != userEntity){
            UserVo userVo =  userMapper.toVO(userEntity);
            userInfo.setUserVo(userVo);
            if(CollUtil.isNotEmpty(userVo.getRoles())){
                Set<String> permissions = new HashSet<>();
                List<String> roleIds = userVo.getRoles().stream()
                        .map(RoleVo::getId).collect(Collectors.toList());
                List<MenuEntity> menuEntitys = menuDao.selectMenuByRoleIds(roleIds);
                if(CollUtil.isNotEmpty(menuEntitys)){
                    List<String> permissionList = menuEntitys.stream()
                            .filter(menuEntity -> StrUtil.isNotBlank(menuEntity.getMenuPerms()))
                            .map(MenuEntity::getMenuPerms)
                            .collect(Collectors.toList());
                    permissions.addAll(permissionList);
                    userInfo.setPerms(ArrayUtil.toArray(permissions, String.class));
                }
            }
        }
        return userInfo;
    }

    @Override
    public IPage<UserEntity> pageDataScope(IPage<UserEntity> page, Wrapper<UserEntity> queryWrapper, DataScope dataScope) {
        return baseMapper.selectPageDataScope(page, queryWrapper, dataScope);
    }

    @Override
    public Map<String, Object> getRouteById() {
        String userId = SecurityUtil.getUserId();
        Map<String, Object> map = new HashMap<>();
        List<String> perms = new ArrayList<>();
        List<RouteVo> routes = new ArrayList<>();
        List<MenuEntity> menuEntitys = menuDao.selectMenuByUserId(userId);
        if(CollUtil.isNotEmpty(menuEntitys)){
            Set<String> permSet = menuEntitys.stream()
                    .filter(menuEntity -> StrUtil.isNotBlank(menuEntity.getMenuPerms()))
                    .map(MenuEntity::getMenuPerms)
                    .collect(Collectors.toSet());
            perms = new ArrayList<>(permSet);
            List<MenuEntity> menus = menuEntitys.stream().filter(menuEntity -> DataConstant.MenuType.MODULE.getKey().equals(menuEntity.getMenuType())
                    || DataConstant.MenuType.MENU.getKey().equals(menuEntity.getMenuType()))
                    .collect(Collectors.toList());
            routes = getRouteTree(menus, "0");
        }
        map.put("perms", perms);
        map.put("routes", routes);
        return map;
    }

    public static List<RouteVo> getRouteTree(List<MenuEntity> list, String pid){
        List<RouteVo> result = new ArrayList<>();
        List<RouteVo> temp;
        for(MenuEntity entity : list){
            if(entity.getParentId().equals(pid)){
                MetaVo metaVo = new MetaVo();
                metaVo.setIcon(entity.getMenuIcon());
                metaVo.setTitle(entity.getMenuName());
                RouteVo routeVo = new RouteVo();
                routeVo.setName(entity.getMenuName());
                routeVo.setComponent(entity.getMenuComponent());
                routeVo.setPath(entity.getMenuPath());
                routeVo.setRedirect(entity.getMenuRedirect());
                routeVo.setMeta(metaVo);
                routeVo.setHidden(DataConstant.TrueOrFalse.TRUE.getKey().equals(entity.getMenuHidden()));
                temp = getRouteTree(list, entity.getId());
                if(CollUtil.isNotEmpty(temp)){
                    routeVo.setChildren(temp);
                }
                result.add(routeVo);
            }
        }
        return result;
    }

    @Override
    public List<UserEntity> getAuditUsers() {
        String roleCode = "audit";
        String userId = SecurityUtil.getUserId();
        List<UserEntity> list = userDao.getAuditUsers(roleCode, userId);
        return list;
    }
}
