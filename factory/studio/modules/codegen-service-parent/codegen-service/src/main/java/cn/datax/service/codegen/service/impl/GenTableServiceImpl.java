package cn.datax.service.codegen.service.impl;

import cn.datax.common.base.BaseServiceImpl;
import cn.datax.service.codegen.dao.GenTableDao;
import cn.datax.service.codegen.api.dto.GenTableDto;
import cn.datax.service.codegen.api.entity.GenTableEntity;
import cn.datax.service.codegen.mapstruct.GenTableMapper;
import cn.datax.service.codegen.service.GenTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 代码生成信息表 服务实现类
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-19
 */
@Service
public class GenTableServiceImpl extends BaseServiceImpl<GenTableDao, GenTableEntity> implements GenTableService {

    @Autowired
    private GenTableDao genTableDao;

    @Autowired
    private GenTableMapper genTableMapper;

    @Override
    public void saveGenTable(GenTableDto genTableDto) {
        GenTableEntity genTable = genTableMapper.toEntity(genTableDto);
        genTableDao.insert(genTable);
    }

    @Override
    public void updateGenTable(GenTableDto genTableDto) {
        GenTableEntity genTable = genTableMapper.toEntity(genTableDto);
        genTableDao.updateById(genTable);
    }

    @Override
    public GenTableEntity getGenTableById(String id) {
        GenTableEntity genTableEntity = super.getById(id);
        return genTableEntity;
    }

    @Override
    public void deleteGenTableById(String id) {
        genTableDao.deleteById(id);
    }
}
