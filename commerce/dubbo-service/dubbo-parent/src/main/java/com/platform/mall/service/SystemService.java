package com.platform.mall.service;

import com.platform.mall.entity.DataTablesResult;
import com.platform.mall.entity.TbBase;
import com.platform.mall.entity.TbLog;
import com.platform.mall.entity.TbOrderItem;
import com.platform.mall.entity.TbShiroFilter;

import java.util.List;

/**
 * @author wulinhao
 */
public interface SystemService {

    /**
     * 获得shiro过滤链配置
     * @return
     */
    List<TbShiroFilter> getShiroFilter();

    /**
     * 统计过滤链数目
     * @return
     */
    Long countShiroFilter();

    /**
     * 添加shiro过滤链
     * @param tbShiroFilter
     * @return
     */
    int addShiroFilter(TbShiroFilter tbShiroFilter);

    /**
     * 更新shiro过滤链
     * @param tbShiroFilter
     * @return
     */
    int updateShiroFilter(TbShiroFilter tbShiroFilter);

    /**
     * 删除shiro过滤链
     * @param id
     * @return
     */
    int deleteShiroFilter(int id);

    /**
     * 获取网站基础设置
     * @return
     */
    TbBase getBase();

    /**
     * 更新网站基础设置
     * @param tbBase
     * @return
     */
    int updateBase(TbBase tbBase);

    /**
     * 获取本周热销商品
     * @return
     */
    TbOrderItem getWeekHot();

    /**
     * 添加日志
     * @param tbLog
     * @return
     */
    int addLog(TbLog tbLog);

    /**
     * 获取日志列表
     * @param draw
     * @param start
     * @param length
     * @param search
     * @param orderCol
     * @param orderDir
     * @return
     */
    DataTablesResult getLogList(int draw, int start, int length, String search,String orderCol,String orderDir);

    /**
     * 统计日志数量
     * @return
     */
    Long countLog();

    /**
     * 删除日志
     * @param id
     * @return
     */
    int deleteLog(int id);
}
