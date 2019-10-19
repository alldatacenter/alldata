package com.platform.mall.service.impl.mobile;

import com.github.pagehelper.PageHelper;
import com.platform.mall.mapper.mobile.LitemallFeedbackMapper;
import com.platform.mall.entity.mobile.LitemallFeedback;
import com.platform.mall.entity.mobile.LitemallFeedbackExample;
import com.platform.mall.service.mobile.LitemallFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wulinhao
 * @date 2018/8/27 11:39
 */
@Service
public class LitemallFeedbackServiceImpl implements LitemallFeedbackService {
    @Autowired
    private LitemallFeedbackMapper feedbackMapper;

    public Integer add(LitemallFeedback feedback) {
        feedback.setAddTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        return feedbackMapper.insertSelective(feedback);
    }

    public List<LitemallFeedback> querySelective(Integer userId, String username, Integer page, Integer limit, String sort, String order) {
        LitemallFeedbackExample example = new LitemallFeedbackExample();
        LitemallFeedbackExample.Criteria criteria = example.createCriteria();

        if (userId != null) {
            criteria.andUserIdEqualTo(userId);
        }
        if (!StringUtils.isEmpty(username)) {
            criteria.andUsernameLike("%" + username + "%");
        }
        criteria.andDeletedEqualTo(false);

        if (!StringUtils.isEmpty(sort) && !StringUtils.isEmpty(order)) {
            example.setOrderByClause(sort + " " + order);
        }

        PageHelper.startPage(page, limit);
        return feedbackMapper.selectByExample(example);
    }
}
