package cn.datax.service.data.masterdata.service.impl;

import cn.datax.common.exception.DataException;
import cn.datax.common.utils.SecurityUtil;
import cn.datax.service.data.masterdata.api.entity.ModelDataEntity;
import cn.datax.service.data.masterdata.api.query.ModelDataQuery;
import cn.datax.service.data.masterdata.dao.MysqlDynamicDao;
import cn.datax.service.data.masterdata.service.ModelDataService;
import cn.datax.service.data.masterdata.utils.SearchUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class ModelDataServiceImpl implements ModelDataService {

    @Autowired
    private MysqlDynamicDao dynamicDao;

    private static String DEFAULT_PRIMARY_KEY = "id";
    private static String DEFAULT_CREATE_BY = "create_by";
    private static String DEFAULT_CREATE_TIME = "create_time";
    private static String DEFAULT_CREATE_DEPT = "create_dept";
    private static String DEFAULT_UPDATE_BY = "update_by";
    private static String DEFAULT_UPDATE_TIME = "update_time";

    private static List<String> SUPER_COLUMNS = Arrays.asList("id", "create_time");

    @Override
    public IPage<Map<String, Object>> getPageModelDatas(ModelDataQuery modelDataQuery) {
        String tableName = modelDataQuery.getTableName();
        if (StrUtil.isBlank(tableName)) {
            throw new DataException("数据库表为空");
        }
        QueryWrapper queryWrapper = SearchUtil.parseWhereSql(modelDataQuery);
        List<String> columns = modelDataQuery.getColumns();
        columns.addAll(SUPER_COLUMNS);
        String[] array = columns.toArray(new String[columns.size()]);
        queryWrapper.select(array);
        IPage<Map<String, Object>> page = dynamicDao.getPageModelDatas(new Page<>(modelDataQuery.getPageNum(), modelDataQuery.getPageSize()), queryWrapper, tableName);
        return page;
    }

    @Override
    public void addModelData(ModelDataEntity modelDataEntity) {
        String tableName = modelDataEntity.getTableName();
        if (StrUtil.isBlank(tableName)) {
            throw new DataException("数据库表为空");
        }
        Map<String, Object> datas = modelDataEntity.getDatas();
        datas.put(DEFAULT_PRIMARY_KEY, new DefaultIdentifierGenerator().nextId(null));
        datas.put(DEFAULT_CREATE_BY, SecurityUtil.getUserId());
        datas.put(DEFAULT_CREATE_TIME, LocalDateTime.now());
        datas.put(DEFAULT_CREATE_DEPT, SecurityUtil.getUserDeptId());
        datas.put(DEFAULT_UPDATE_BY, SecurityUtil.getUserId());
        datas.put(DEFAULT_UPDATE_TIME, LocalDateTime.now());
        dynamicDao.insertData(modelDataEntity);
    }

    @Override
    public void updateModelData(ModelDataEntity modelDataEntity) {
        String tableName = modelDataEntity.getTableName();
        if (StrUtil.isBlank(tableName)) {
            throw new DataException("数据库表为空");
        }
        String id = modelDataEntity.getId();
        if (StrUtil.isBlank(id)) {
            throw new DataException("数据库主键为空");
        }
        Map<String, Object> datas = modelDataEntity.getDatas();
        datas.put(DEFAULT_UPDATE_BY, SecurityUtil.getUserId());
        datas.put(DEFAULT_UPDATE_TIME, LocalDateTime.now());
        dynamicDao.updateData(modelDataEntity);
    }

    @Override
    public void delModelData(ModelDataEntity modelDataEntity) {
        String tableName = modelDataEntity.getTableName();
        if (StrUtil.isBlank(tableName)) {
            throw new DataException("数据库表为空");
        }
        String id = modelDataEntity.getId();
        if (StrUtil.isBlank(id)) {
            throw new DataException("数据库主键为空");
        }
        dynamicDao.deleteData(modelDataEntity);
    }

    @Override
    public Map<String, Object> getModelDataById(ModelDataEntity modelDataEntity) {
        String tableName = modelDataEntity.getTableName();
        if (StrUtil.isBlank(tableName)) {
            throw new DataException("数据库表为空");
        }
        String id = modelDataEntity.getId();
        if (StrUtil.isBlank(id)) {
            throw new DataException("数据库主键为空");
        }
        Map<String, Object> data = dynamicDao.getData(modelDataEntity);
        return data;
    }
}
