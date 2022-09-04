package com.alibaba.tesla.authproxy.service;

import com.alibaba.tesla.authproxy.ApplicationException;
import com.alibaba.tesla.authproxy.lib.exceptions.PrivateValidationError;
import com.alibaba.tesla.authproxy.model.UserDO;
import com.alibaba.tesla.authproxy.service.ao.UserGetConditionAO;
import com.alibaba.tesla.authproxy.service.ao.UserGetResultAO;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * <p>Description: 用户信息服务接口 <／p>
 * <p>Copyright: Copyright (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017年5月3日
 */
public interface TeslaUserService {

    /**
     * 保存用户信息，存在更新，不存在添加
     *
     * @param teslaUser
     * @return
     * @throws ApplicationException
     */
    int save(UserDO teslaUser) throws ApplicationException;

    /**
     * 更新用户信息
     *
     * @param teslaUser
     * @return
     * @throws ApplicationException
     */
    int update(UserDO teslaUser) throws ApplicationException;

    /**
     * 添加用户信息
     *
     * @param teslaUser
     * @return
     * @throws ApplicationException
     */
    int insert(UserDO teslaUser) throws ApplicationException;

    /**
     * 根据阿里云Pk获取用户信息
     *
     * @param aliyunId 阿里云Id
     * @return
     * @throws ApplicationException
     */
    UserDO getUserByAliyunId(String aliyunId) throws ApplicationException;

    /**
     * 根据登录名获取用户
     *
     * @param loginName
     * @return
     * @throws ApplicationException
     */
    UserDO getUserByLoginName(String loginName) throws ApplicationException;

    /**
     * 根据登录名和密码获取用户
     *
     * @param loginName
     * @param pwd
     * @return
     * @throws ApplicationException
     */
    UserDO getUserByLoginNameAndPwd(String loginName, String pwd) throws ApplicationException;

    /**
     * 根据登录名和邮箱获取用户
     *
     * @param loginName
     * @param emailAddress
     * @return
     * @throws ApplicationException
     */
    UserDO getUserByLoginNameAndEmail(String loginName, String emailAddress) throws ApplicationException;

    /**
     * 根据阿里云Pk获取用户信息
     *
     * @param aliyunPk 阿里云Pk
     * @return
     * @throws ApplicationException
     */
    UserDO getUserByAliyunPk(String aliyunPk) throws ApplicationException;

    /**
     * 根据BucID查询用户信息
     *
     * @param bucId
     * @return
     * @throws ApplicationException
     */
    UserDO getUserByBucId(String bucId) throws ApplicationException;

    /**
     * 根据 access key id 获取用户
     *
     * @param accessKeyId Access Key Id
     * @return
     * @throws ApplicationException
     */
    UserDO getUserByAccessKeyId(String accessKeyId) throws ApplicationException;

    /**
     * 根据 Emp Id 获取用户
     *
     * @param empId
     * @return
     * @throws ApplicationException
     */
    UserDO getUserByEmpId(String empId) throws ApplicationException;

    /**
     * 根据查询条件获取指定用户的信息 (包含扩展信息)
     *
     * @param condition 查询条件
     */
    UserGetResultAO getEnhancedUser(UserGetConditionAO condition);

    /**
     * 根据用户名称模糊查询用户
     *
     * @param userName
     * @return
     * @throws ApplicationException
     */
    List<UserDO> selectByName(String userName) throws ApplicationException;

    /**
     * 修改用户的国际化语言
     *
     * @param userDo
     * @param lang
     * @throws PrivateValidationError
     */
    void changeLanguage(UserDO userDo, String lang) throws PrivateValidationError;

    /**
     * 修改用户密码
     *
     * @param userDo
     * @param oldPassword
     * @param newPassword
     * @throws ApplicationException
     */
    void changePassword(UserDO userDo, String oldPassword, String newPassword) throws ApplicationException;

    /**
     * 分页查询用户
     *
     * @param page
     * @param size
     * @return
     * @throws ApplicationException
     */
    PageInfo<UserDO> listUserWithPage(int page, int size, String loginName) throws ApplicationException;
}
