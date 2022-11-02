package com.alibaba.sreworks.dataset.services.inter;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.api.inter.DataInterfaceParamsService;
import com.alibaba.sreworks.dataset.common.exception.InterfaceNotExistException;
import com.alibaba.sreworks.dataset.common.exception.ParamException;
import com.alibaba.sreworks.dataset.domain.primary.*;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceParamCreateReq;
import com.alibaba.sreworks.dataset.domain.req.inter.DataInterfaceParamUpdateReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 数据接口参数Service
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/07/20 22:06
 */

@Slf4j
@Service
public class DataInterfaceParamsServiceImpl implements DataInterfaceParamsService {

    @Autowired
    DataInterfaceParamsMapper paramsMapper;

    @Autowired
    DataInterfaceConfigMapper interfaceConfigMapper;

    @Override
    public List<JSONObject> getParams(Integer interfaceId) {
        DataInterfaceParamsExample example = new DataInterfaceParamsExample();
        example.createCriteria().andInterfaceIdEqualTo(interfaceId);
        List<DataInterfaceParams> params = paramsMapper.selectByExample(example);
        return convertToJSONObjects(params);
    }

    @Override
    public int addInterfaceParam( DataInterfaceParamCreateReq paramReq) throws Exception {
        DataInterfaceParams interfaceParam = new DataInterfaceParams();

        Date now = new Date();
        interfaceParam.setGmtCreate(now);
        interfaceParam.setGmtModified(now);
        interfaceParam.setLabel(paramReq.getLabel());
        interfaceParam.setName(paramReq.getName());
        interfaceParam.setInterfaceId(paramReq.getInterfaceId());
        interfaceParam.setType(paramReq.getType());
        interfaceParam.setRequired(paramReq.getRequired());
        interfaceParam.setDefaultValue(paramReq.getDefaultValue());

        return paramsMapper.insert(interfaceParam);
    }

    @Override
    public int updateInterfaceParam(DataInterfaceParamUpdateReq paramReq) throws Exception {
        DataInterfaceParams interfaceParam = new DataInterfaceParams();
        interfaceParam.setId(paramReq.getId());
        interfaceParam.setGmtModified(new Date());
        interfaceParam.setLabel(paramReq.getLabel());
        interfaceParam.setName(paramReq.getName());
        interfaceParam.setInterfaceId(paramReq.getInterfaceId());
        interfaceParam.setType(paramReq.getType());
        interfaceParam.setRequired(paramReq.getRequired());
        interfaceParam.setDefaultValue(paramReq.getDefaultValue());

        return paramsMapper.updateByPrimaryKeySelective(interfaceParam);
    }

    @Override
    public int deleteInterfaceParams(Integer interfaceId) throws Exception {
        if (buildInInterface(interfaceId)) {
            throw new ParamException("内置模型不允许修改");
        }

        DataInterfaceParamsExample example = new DataInterfaceParamsExample();
        example.createCriteria().andInterfaceIdEqualTo(interfaceId);

        return paramsMapper.deleteByExample(example);
    }

    @Override
    public int deleteInterfaceParam(Integer interfaceId, Long paramId) throws Exception {
        if (buildInInterface(interfaceId)) {
            throw new ParamException("内置模型不允许修改");
        }
        return paramsMapper.deleteByPrimaryKey(paramId);
    }

    private boolean buildInInterface(Integer interfaceId) throws Exception {
        DataInterfaceConfig interfaceConfig = interfaceConfigMapper.selectByPrimaryKey(interfaceId);
        if (interfaceConfig == null) {
            throw new InterfaceNotExistException(String.format("数据接口不存在,接口ID%s", interfaceId));
        }
        return interfaceConfig.getBuildIn();
    }

    public List<DataInterfaceParams> getParamsDto(Integer interfaceId) {
        DataInterfaceParamsExample example = new DataInterfaceParamsExample();
        example.createCriteria().andInterfaceIdEqualTo(interfaceId);
        return paramsMapper.selectByExample(example);
    }

}
