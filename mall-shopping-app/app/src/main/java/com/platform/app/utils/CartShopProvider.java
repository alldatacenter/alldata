package com.platform.app.utils;

import android.content.Context;
import android.util.SparseArray;

import com.platform.app.bean.HotGoods;
import com.platform.app.bean.ShoppingCart;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wulinhao
 * Time  2019/8/9
 * Describe: 购物车 数据的存储
 * 这里使用的是内存数据 本地数据  的方法
 * 在真实项目中,一般本地数据  换成 网络数据的获取(sp中get) 与提交(sp中 put)即可
 * <p>
 * 此处没有把数据上传至服务器.通过内存存储  本地存储
 * 内存存储就是普通的集合,本地存储通过sp进行
 * 为了确保数据的统一性,内存操作后,必须要对sp进行操作
 */

public class CartShopProvider {

    public static final String CART_JSON = "cart_json";

    private Context mContext;

    public SparseArray<ShoppingCart> datas = null;             //类似于hanshMap

    private Gson mGson = new Gson();

    public CartShopProvider(Context context) {
        datas = new SparseArray<>(10);
        this.mContext = context;
        listToSparse();
    }

    /**
     * 存入
     */
    public void put(ShoppingCart cart) {

        ShoppingCart temp = datas.get(cart.getId());

        if (temp != null) {
            //说明购物车中已经存在这个数据
            temp.setCount(cart.getCount() + 1);
        } else {
            //没有这个数据.直接赋值
            temp = cart;
            temp.setCount(1);
        }
        datas.put(cart.getId(), temp);     //内存上进行操作
        savaDataToSp();                    //本地存储操作(更新  删除也是类似)
    }


    /**
     * 存入
     */
    public void put(HotGoods.ListBean goods) {

        ShoppingCart cart = convertData(goods);
        put(cart);
    }


    /**
     * 更新
     */
    public void updata(ShoppingCart cart) {
        datas.put(cart.getId(), cart);
        savaDataToSp();
    }

    /**
     * 删除
     */
    public void delete(ShoppingCart cart) {
        datas.delete(cart.getId());
        savaDataToSp();
    }

    public List<ShoppingCart> getAll() {

        return getDataFormLoad();
    }

    /**
     * 从本地读取数据
     */
    public List<ShoppingCart> getDataFormLoad() {

        String dataStr = PreferencesUtils.getString(mContext, CART_JSON);
        List<ShoppingCart> carts = null;
        if (dataStr != null) {
            carts = mGson.fromJson(dataStr, new TypeToken<List<ShoppingCart>>() {
            }.getType());
        }
        return carts;
    }

    /**
     * 将数据保存在sp中
     */
    public void savaDataToSp() {
        List<ShoppingCart> carts = sparseToList();
        PreferencesUtils.putString(mContext, CART_JSON, mGson.toJson(carts));
    }


    /**
     * 将SparseArray类型数据转化为List数据
     * 一开始是通过SparseArray类型进行操作的.但读取时 是list类型
     * 所以需要转化
     */
    private List<ShoppingCart> sparseToList() {

        int size = datas.size();
        List<ShoppingCart> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(datas.valueAt(i));
        }
        return list;
    }


    /**
     * 必须有这个操作.
     * 因为本地数据和 存储数据是分开(独立的)
     * 一进入时,必须将本地读取到的数据 赋值给内存数据(list集合中)  ---->在构造方法那里调用
     */
    private void listToSparse() {

        List<ShoppingCart> carts = getDataFormLoad();
        if (carts != null && carts.size() > 0) {
            for (ShoppingCart cart : carts) {
                datas.put(cart.getId(), cart);
            }
        }
    }

    public ShoppingCart convertData(HotGoods.ListBean item) {

        ShoppingCart cart = new ShoppingCart();

        cart.setId(item.getId());
        cart.setImgUrl(item.getImgUrl());
        cart.setName(item.getName());
        cart.setPrice(item.getPrice());

        return cart;
    }

}
