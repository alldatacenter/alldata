package com.platform.mall.service.impl.admin;

import com.github.pagehelper.PageHelper;
import com.platform.mall.mapper.admin.SmsHomeBrandMapper;
import com.platform.mall.entity.admin.SmsHomeBrand;
import com.platform.mall.entity.admin.SmsHomeBrandExample;
import com.platform.mall.service.admin.SmsHomeBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 首页品牌管理Service实现类
 * Created by wulinhao on 2019/9/6.
 */
@Service
public class SmsHomeBrandServiceImpl implements SmsHomeBrandService {
    @Autowired
    private SmsHomeBrandMapper homeBrandMapper;

    @Override
    public int create(List<SmsHomeBrand> homeBrandList) {
        for (SmsHomeBrand smsHomeBrand : homeBrandList) {
            smsHomeBrand.setRecommendStatus(1);
            smsHomeBrand.setSort(0);
            homeBrandMapper.insert(smsHomeBrand);
        }
        return homeBrandList.size();
    }

    @Override
    public int updateSort(Long id, Integer sort) {
        SmsHomeBrand homeBrand = new SmsHomeBrand();
        homeBrand.setId(id);
        homeBrand.setSort(sort);
        return homeBrandMapper.updateByPrimaryKeySelective(homeBrand);
    }

    @Override
    public int delete(List<Long> ids) {
        SmsHomeBrandExample example = new SmsHomeBrandExample();
        example.createCriteria().andIdIn(ids);
        return homeBrandMapper.deleteByExample(example);
    }

    @Override
    public int updateRecommendStatus(List<Long> ids, Integer recommendStatus) {
        SmsHomeBrandExample example = new SmsHomeBrandExample();
        example.createCriteria().andIdIn(ids);
        SmsHomeBrand record = new SmsHomeBrand();
        record.setRecommendStatus(recommendStatus);
        return homeBrandMapper.updateByExampleSelective(record, example);
    }

    @Override
    public List<SmsHomeBrand> list(String brandName, Integer recommendStatus, Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum, pageSize);
        SmsHomeBrandExample example = new SmsHomeBrandExample();
        SmsHomeBrandExample.Criteria criteria = example.createCriteria();
        if (!StringUtils.isEmpty(brandName)) {
            criteria.andBrandNameLike("%" + brandName + "%");
        }
        if (recommendStatus != null) {
            criteria.andRecommendStatusEqualTo(recommendStatus);
        }
        example.setOrderByClause("sort desc");
        return homeBrandMapper.selectByExample(example);
    }
}
