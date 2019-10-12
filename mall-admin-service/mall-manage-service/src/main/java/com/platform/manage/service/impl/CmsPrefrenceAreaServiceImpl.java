package com.platform.manage.service.impl;

import com.platform.mbg.mapper.CmsPrefrenceAreaMapper;
import com.platform.mbg.model.CmsPrefrenceArea;
import com.platform.mbg.model.CmsPrefrenceAreaExample;
import com.platform.manage.service.CmsPrefrenceAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品优选Service实现类
 * Created by wulinhao on 2019/6/1.
 */
@Service
public class CmsPrefrenceAreaServiceImpl implements CmsPrefrenceAreaService {
    @Autowired
    private CmsPrefrenceAreaMapper prefrenceAreaMapper;

    @Override
    public List<CmsPrefrenceArea> listAll() {
        return prefrenceAreaMapper.selectByExample(new CmsPrefrenceAreaExample());
    }
}
