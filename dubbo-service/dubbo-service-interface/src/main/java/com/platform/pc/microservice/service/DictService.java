package com.platform.pc.microservice.service;



import com.platform.mall.pojo.TbDict;

import java.util.List;

/**
 * @author wulinhao
 */
public interface DictService {

    /**
     * 获取扩展词库列表
     * @return
     */
    List<TbDict> getDictList();

    /**
     * 获取停用词库列表
     * @return
     */
    List<TbDict> getStopList();

    /**
     * 添加
     * @param tbDict
     * @return
     */
    int addDict(TbDict tbDict);

    /**
     * 更新
     * @param tbDict
     * @return
     */
    int updateDict(TbDict tbDict);

    /**
     * 删除
     * @param id
     * @return
     */
    int delDict(int id);
}
