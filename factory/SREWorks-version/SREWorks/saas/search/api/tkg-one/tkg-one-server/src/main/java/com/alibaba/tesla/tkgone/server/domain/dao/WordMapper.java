package com.alibaba.tesla.tkgone.server.domain.dao;

import com.alibaba.tesla.tkgone.server.domain.config.Word;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 自定义分词
 *
 * @author feiquan
 */
@Mapper
public interface WordMapper {
    /**
     * 根据单词进行查询
     * @param word
     * @return
     */
    Word selectByWord(String word);

    /**
     * 新增
     * @param word
     */
    void insert(Word word);

    /**
     * 修改
     * @param word
     */
    void update(Word word);

    /**
     * 查询所有
     * @return
     */
    List<Word> selectAll();

    /**
     * 删除
     * @param id
     */
    void delete(int id);
}
