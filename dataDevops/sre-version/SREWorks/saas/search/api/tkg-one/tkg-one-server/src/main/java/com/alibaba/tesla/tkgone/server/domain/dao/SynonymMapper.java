package com.alibaba.tesla.tkgone.server.domain.dao;

import com.alibaba.tesla.tkgone.server.domain.config.Synonym;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 同义词
 *
 * @author feiquan
 */
@Mapper
public interface SynonymMapper {
    /**
     * 根据同义词进行查询
     * @param word
     * @return
     */
    Synonym selectByWord(String word);

    /**
     * 新增
     * @param synonym
     */
    void insert(Synonym synonym);

    /**
     * 修改
     * @param synonym
     */
    void update(Synonym synonym);

    /**
     * 查询所有
     * @return
     */
    List<Synonym> selectAll();

    /**
     * 删除
     * @param id
     */
    void delete(int id);
}
