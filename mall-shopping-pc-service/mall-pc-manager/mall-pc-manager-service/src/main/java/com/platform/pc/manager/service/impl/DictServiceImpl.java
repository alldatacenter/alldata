package com.platform.pc.manager.service.impl;

import com.platform.pc.common.constant.DictConstant;
import com.platform.pc.manager.mapper.TbDictMapper;
import com.platform.pc.manager.pojo.TbDict;
import com.platform.pc.manager.pojo.TbDictExample;
import com.platform.pc.manager.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wulinhao
 */
@Service
public class DictServiceImpl implements DictService {

    @Autowired
    private TbDictMapper tbDictMapper;

    @Override
    public List<TbDict> getDictList() {

        TbDictExample example=new TbDictExample();
        TbDictExample.Criteria criteria=example.createCriteria();
        //条件查询
        criteria.andTypeEqualTo(DictConstant.DICT_EXT);
        List<TbDict> list = tbDictMapper.selectByExample(example);
        return list;
    }

    @Override
    public List<TbDict> getStopList() {

        TbDictExample example=new TbDictExample();
        TbDictExample.Criteria criteria=example.createCriteria();
        //条件查询
        criteria.andTypeEqualTo(DictConstant.DICT_STOP);
        List<TbDict> list = tbDictMapper.selectByExample(example);
        return list;
    }

    @Override
    public int addDict(TbDict tbDict) {

        tbDictMapper.insert(tbDict);
        return 1;
    }

    @Override
    public int updateDict(TbDict tbDict) {

        tbDictMapper.updateByPrimaryKey(tbDict);
        return 1;
    }

    @Override
    public int delDict(int id) {

        tbDictMapper.deleteByPrimaryKey(id);
        return 1;
    }
}
