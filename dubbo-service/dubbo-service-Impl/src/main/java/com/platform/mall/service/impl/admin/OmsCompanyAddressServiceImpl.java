package com.platform.mall.service.impl.admin;

import com.platform.mall.mapper.admin.OmsCompanyAddressMapper;
import com.platform.mall.entity.admin.OmsCompanyAddress;
import com.platform.mall.entity.admin.OmsCompanyAddressExample;
import com.platform.mall.service.admin.OmsCompanyAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收货地址管理Service实现类
 * Created by wulinhao on 2019/9/18.
 */
@Service
public class OmsCompanyAddressServiceImpl implements OmsCompanyAddressService {
    @Autowired
    private OmsCompanyAddressMapper companyAddressMapper;

    @Override
    public List<OmsCompanyAddress> list() {
        return companyAddressMapper.selectByExample(new OmsCompanyAddressExample());
    }
}
