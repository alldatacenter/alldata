package cn.datax.service.data.masterdata.dao;

import cn.datax.service.data.masterdata.api.entity.ModelCommentEntity;
import cn.datax.service.data.masterdata.api.entity.ModelDataEntity;
import cn.datax.service.data.masterdata.api.entity.ModelEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface OracleDynamicDao {

    void createTable(ModelEntity modelEntity);

    void commentTable(ModelCommentEntity modelCommentEntity);

    void commentColumn(ModelCommentEntity modelCommentEntity);

    void dropTable(@Param("tableName") String tableName);

    void insertData(ModelDataEntity modelDataEntity);

    void updateData(ModelDataEntity modelDataEntity);

    void deleteData(ModelDataEntity modelDataEntity);

    IPage<Map<String, Object>> getPageModelDatas(Page<Object> page, @Param(Constants.WRAPPER) Wrapper wrapper, @Param("tableName") String tableName);

    Map<String, Object> getData(ModelDataEntity modelDataEntity);
}
