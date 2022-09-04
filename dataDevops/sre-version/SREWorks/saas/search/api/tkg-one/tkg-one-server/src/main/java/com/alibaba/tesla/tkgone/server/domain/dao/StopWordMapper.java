package com.alibaba.tesla.tkgone.server.domain.dao;

import com.alibaba.tesla.tkgone.server.domain.config.StopWord;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 停用词
 *
 * @author feiquan
 */
@Mapper
public interface StopWordMapper {
    /**
     * 根据停用词进行查询
     * @param word
     * @return
     */
    StopWord selectByWord(String word);

    /**
     * 新增
     * @param stopWord
     */
    void insert(StopWord stopWord);

    /**
     * 修改
     * @param stopWord
     */
    void update(StopWord stopWord);

    /**
     * 查询所有
     * @return
     */
    List<StopWord> selectAll();

    /**
     * 删除
     * @param id
     */
    void delete(int id);
}
