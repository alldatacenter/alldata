package com.alibaba.sreworks.dataset.services.subject;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.domain.DomainService;
import com.alibaba.sreworks.dataset.api.subject.SubjectService;
import com.alibaba.sreworks.dataset.domain.primary.DataSubject;
import com.alibaba.sreworks.dataset.domain.primary.DataSubjectExample;
import com.alibaba.sreworks.dataset.domain.primary.DataSubjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 数据主题
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/14 14:19
 */
@Slf4j
@Service
public class SubjectServiceImpl implements SubjectService {

    @Autowired
    DataSubjectMapper dataSubjectMapper;

    @Autowired
    DomainService domainService;

    @Override
    public JSONObject getSubjectById(Integer subjectId) {
        DataSubject dataSubject = dataSubjectMapper.selectByPrimaryKey(subjectId);
        JSONObject result = convertToJSONObject(dataSubject);
        if (result.isEmpty()) {
            return result;
        }

        Long domainCount = domainService.countDomainsBySubject(subjectId);
        result.put("domainCount", domainCount);
        return result;
    }

    @Override
    public List<JSONObject> getSubjects() {
        DataSubjectExample example = new DataSubjectExample();
        List<DataSubject> dataSubjects = dataSubjectMapper.selectByExampleWithBLOBs(example);
        List<JSONObject> results = convertToJSONObjects(dataSubjects);
        if (results.isEmpty()) {
            return results;
        }

        Map<Integer, Long> domainCounts = domainService.countDomains();
        results.forEach(result -> result.put("domainCount", domainCounts.getOrDefault(result.getInteger("id"), 0L)));

        return results;
    }
}
