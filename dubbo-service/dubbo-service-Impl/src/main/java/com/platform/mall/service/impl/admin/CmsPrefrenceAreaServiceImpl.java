package com.platform.mall.service.impl.admin;

import com.platform.mall.mapper.admin.CmsPrefrenceAreaMapper;
import com.platform.mall.entity.admin.CmsPrefrenceArea;
import com.platform.mall.entity.admin.CmsPrefrenceAreaExample;
import com.platform.mall.service.admin.CmsPrefrenceAreaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商品优选Service实现类
 * Created by wulinhao on 2019/9/1.
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
