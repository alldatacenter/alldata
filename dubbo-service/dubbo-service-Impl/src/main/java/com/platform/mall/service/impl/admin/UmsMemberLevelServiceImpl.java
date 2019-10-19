package com.platform.mall.service.impl.admin;

import com.platform.mall.mapper.admin.UmsMemberLevelMapper;
import com.platform.mall.entity.admin.UmsMemberLevel;
import com.platform.mall.entity.admin.UmsMemberLevelExample;
import com.platform.mall.service.admin.UmsMemberLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 会员等级管理Service实现类
 * Created by wulinhao on 2019/9/26.
 */
@Service
public class UmsMemberLevelServiceImpl implements UmsMemberLevelService {
    @Autowired
    private UmsMemberLevelMapper memberLevelMapper;

    @Override
    public List<UmsMemberLevel> list(Integer defaultStatus) {
        UmsMemberLevelExample example = new UmsMemberLevelExample();
        example.createCriteria().andDefaultStatusEqualTo(defaultStatus);
        return memberLevelMapper.selectByExample(example);
    }
}
