package com.datasophon.api.service.impl;

import com.datasophon.api.service.FrameServiceService;
import com.datasophon.common.Constants;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.FrameServiceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;


import com.datasophon.dao.mapper.FrameInfoMapper;
import com.datasophon.dao.entity.FrameInfoEntity;
import com.datasophon.api.service.FrameInfoService;


@Service("frameInfoService")
public class FrameInfoServiceImpl extends ServiceImpl<FrameInfoMapper, FrameInfoEntity> implements FrameInfoService {

    @Autowired
    private FrameServiceService frameServiceService;

    @Override
    public Result getAllClusterFrame() {
        List<FrameInfoEntity> list = this.list();
        for (FrameInfoEntity frameInfo : list) {
            List<FrameServiceEntity> frameServiceList = frameServiceService.list(new QueryWrapper<FrameServiceEntity>()
                    .eq(Constants.FRAME_ID, frameInfo.getId())
                    .orderByAsc(Constants.SORT_NUM));
            frameInfo.setFrameServiceList(frameServiceList);
        }
        return Result.success(list);
    }
}
