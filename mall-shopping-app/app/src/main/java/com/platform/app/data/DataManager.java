package com.platform.app.data;

import com.platform.app.MallShoppingApp;
import com.platform.app.data.dao.Address;
import com.platform.app.data.dao.AddressDao;
import com.platform.app.data.dao.User;
import com.platform.app.data.dao.UserDao;

import java.util.List;

/**
 * <pre>
 *     author : wulinhao
 *     time   : 2019/09/03
 *     desc   : 数据库管理类
 *     version: 1.0
 * </pre>
 */

public class DataManager {

    /**
     * 添加数据
     *
     * @param user
     */
    public static void insertUser(User user) {
        MallShoppingApp.getDaoSession().getUserDao().insert(user);
    }

    /**
     * 删除数据
     *
     * @param id
     */
    public static void deleteUser(Long id) {
        MallShoppingApp.getDaoSession().getUserDao().deleteByKey(id);
    }

    /**
     * 更新数据
     *
     * @param user
     */
    public static void updateUser(User user) {
        MallShoppingApp.getDaoSession().getUserDao().update(user);
    }

    /**
     * 查询条件为Type=Phone的数据
     *
     * @return
     */
    public static List<User> queryUser(String phone) {
        return MallShoppingApp.getDaoSession().getUserDao().queryBuilder().where
                (UserDao.Properties.Phone.eq(phone)).list();
    }


    /**
     * 添加数据
     *
     * @param address
     */
    public static void insertAddress(Address address) {
        MallShoppingApp.getDaoSession().getAddressDao().insert(address);
    }

    /**
     * 删除数据
     *
     * @param id
     */
    public static void deleteAddress(Long id) {
        MallShoppingApp.getDaoSession().getAddressDao().deleteByKey(id);
    }

    /**
     * 更新数据
     *
     * @param address
     */
    public static void updateAddress(Address address) {
        MallShoppingApp.getDaoSession().getAddressDao().update(address);
    }

    /**
     * 查询条件为Type=UserId的数据
     *
     * @return
     */
    public static List<Address> queryAddress(Long userId) {
        return MallShoppingApp.getDaoSession().getAddressDao().queryBuilder().where
                (AddressDao.Properties.UserId.eq(userId)).list();
    }
}
