package com.alibaba.sreworks.dataset.api.subject;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.BasicApi;

import java.util.List;

/**
 * 数据主题接口
 */
public interface SubjectService extends BasicApi {

    /**
     * 根据主题ID查询主题信息
     * @param subjectId 主题ID
     * @return
     */
    JSONObject getSubjectById(Integer subjectId);

    /**
     * 查询所有的主题信息
     * @return
     */
    List<JSONObject> getSubjects();
}
