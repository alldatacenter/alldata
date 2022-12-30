package com.platform.mall.service.impl.mobile;

import com.platform.mall.mapper.mobile.StatMapper;
import com.platform.mall.service.mobile.StatService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class StatServiceImpl implements StatService {
    @Resource
    private StatMapper statMapper;


    public List<Map> statUser() {
        return statMapper.statUser();
    }

    public List<Map> statOrder() {
        return statMapper.statOrder();
    }

    public List<Map> statGoods() {
        return statMapper.statGoods();
    }
}
