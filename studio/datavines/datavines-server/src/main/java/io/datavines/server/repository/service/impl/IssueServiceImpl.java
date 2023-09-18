/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.repository.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.datavines.common.utils.StringUtils;
import io.datavines.core.enums.Status;
import io.datavines.core.exception.DataVinesServerException;
import io.datavines.server.api.dto.bo.issue.IssueCreate;
import io.datavines.server.api.dto.bo.issue.IssueUpdate;
import io.datavines.server.repository.entity.Issue;
import io.datavines.server.repository.entity.JobIssueRel;
import io.datavines.server.repository.mapper.IssueMapper;
import io.datavines.server.repository.mapper.JobIssueRelMapper;
import io.datavines.server.repository.service.IssueService;
import io.datavines.server.utils.ContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("issueService")
public class IssueServiceImpl extends ServiceImpl<IssueMapper, Issue> implements IssueService {

    @Autowired
    private JobIssueRelMapper jobIssueRelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long create(IssueCreate issueCreate) throws DataVinesServerException {

        if (issueCreate == null) {
            throw new DataVinesServerException("issue is null");
        }

        if (StringUtils.isEmpty(issueCreate.getTitle()) || StringUtils.isEmpty(issueCreate.getTitle())) {
            throw new DataVinesServerException("issue title or content can not be null");
        }

        Issue issue = new Issue();
        BeanUtils.copyProperties(issueCreate, issue);

        issue.setStatus("good");
        issue.setCreateTime(LocalDateTime.now());
        issue.setUpdateTime(LocalDateTime.now());

        if (baseMapper.insert(issue) <= 0) {
            log.info("create issue fail : {}", issue);
            throw new DataVinesServerException(Status.CREATE_ENV_ERROR, issue.getTitle());
        }

        JobIssueRel jobIssueRel = new JobIssueRel();
        jobIssueRel.setIssueId(issue.getId());
        jobIssueRel.setJobId(issueCreate.getJobId());
        jobIssueRel.setCreateTime(LocalDateTime.now());
        jobIssueRel.setUpdateTime(LocalDateTime.now());

        jobIssueRelMapper.insert(jobIssueRel);

        return issue.getId();
    }

    @Override
    public Long updateStatus(IssueUpdate issueUpdate) throws DataVinesServerException {

        Issue issue = getById(issueUpdate.getId());
        if ( issue == null) {
            throw new DataVinesServerException(Status.ISSUE_NOT_EXIST_ERROR, issueUpdate.getId());
        }

        issue.setStatus(issueUpdate.getStatus());
        issue.setUpdateTime(LocalDateTime.now());

        if (baseMapper.updateById(issue) <= 0) {
            log.info("update issue fail : {}", issue);
            throw new DataVinesServerException(Status.UPDATE_ISSUE_ERROR, issue.getTitle());
        }

        return issueUpdate.getId();
    }

    @Override
    public Issue getById(long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public int deleteById(long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByJobId(long jobId) {
        List<JobIssueRel> relList = jobIssueRelMapper.selectList(new QueryWrapper<JobIssueRel>().eq("job_id", jobId));
        if (CollectionUtils.isNotEmpty(relList)) {
            remove(new QueryWrapper<Issue>().in("id", relList.stream().map(JobIssueRel::getIssueId).collect(Collectors.toList())));
            jobIssueRelMapper.delete(new QueryWrapper<JobIssueRel>().eq("job_id", jobId));
            return true;
        }
        return false;
    }
}
