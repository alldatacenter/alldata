package com.alibaba.tesla.authproxy.model.mapper;

import com.alibaba.tesla.authproxy.model.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>Title: UserMapper.java<／p>
 * <p>Description: 用户信息数据访问接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
@Mapper
public interface UserMapper {

    /**
     * 根据主键删除
     *
     * @param id
     * @return
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 添加
     *
     * @param record
     * @return
     */
    int insert(UserDO record);

    /**
     * 根据主键获取
     *
     * @param id
     * @return
     */
    UserDO selectByPrimaryKey(Long id);

    /**
     * 选择没有用户 ID 的用户列表
     *
     * @return
     */
    List<UserDO> selectNoUserId();

    /**
     * 根据 user id 列表获取数据
     *
     * @param userIds user id 列表
     * @return
     */
    List<UserDO> findAllByUserIdIn(@Param("userIds") List<String> userIds);

    /**
     * 根据BucId查询用户信息
     *
     * @param bucId
     * @return
     */
    UserDO getByBucId(String bucId);

    /**
     * 根据员工ID查询用户信息
     *
     * @param empId
     * @return
     */
    UserDO getByEmpId(String empId);

    /**
     * 根据loginName查询用户信息
     *
     * @param loginName
     * @return
     */
    UserDO getByLoginName(String loginName);

    /**
     * 根据登录名和密码获取用户
     *
     * @param loginName
     * @param pwd
     * @return
     */
    UserDO getByLoginNameAndPassword(String loginName, String pwd);

    /**
     * 根据登录名和邮箱获取用户
     *
     * @param loginName
     * @param email
     * @return
     */
    UserDO getByLoginNameAndEmail(String loginName, String email);

    /**
     * 根据登录名获取用户信息
     *
     * @param aliyunId
     * @return
     */
    UserDO getByAliyunId(String aliyunId);

    /**
     * 根据阿里云PK获取用户信息
     *
     * @param aliyunPk
     * @return
     */
    UserDO getByAliyunPk(String aliyunPk);

    /**
     * 根据 Access Id 获取用户信息
     *
     * @param accessId Access Id
     */
    UserDO getByAccessKeyId(String accessId);

    /**
     * 根据主键更新
     *
     * @param record
     * @return
     */
    int updateByPrimaryKey(UserDO record);

    /**
     * 更新用戶密碼
     *
     * @param loginName
     * @param newPassword
     * @return
     */
    int updateLoginPassword(String loginName, String newPassword, Date time);

    /**
     * 根据用户名称模糊查询
     *
     * @param userName
     * @return
     */
    List<UserDO> selectByName(@Param(value = "userName") String userName);

    /**
     * 根据参数查询用户信息列表
     *
     * @param params
     * @return
     */
    List<UserDO> selectByParams(Map<String, Object> params);

    /**
     * 根据 userId 参数获取对应的用户信息
     *
     * @param userId 用户 ID
     * @return
     */
    UserDO getByUserId(@Param("userId") String userId);

    /**
     * 根据 Page 获取所有用户列表
     *
     * @param limit 当前页
     * @param offset 每页大小
     * @return
     */
    List<UserDO> selectByPages(@Param("limit") Integer limit, @Param("offset") Integer offset);
}