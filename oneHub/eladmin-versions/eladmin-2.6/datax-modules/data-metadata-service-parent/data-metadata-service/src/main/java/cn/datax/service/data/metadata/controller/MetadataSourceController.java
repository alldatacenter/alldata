package cn.datax.service.data.metadata.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.database.DbQuery;
import cn.datax.common.database.core.DbColumn;
import cn.datax.common.database.core.DbTable;
import cn.datax.common.database.core.PageResult;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.metadata.api.dto.MetadataSourceDto;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.api.query.DbDataQuery;
import cn.datax.service.data.metadata.api.query.MetadataSourceQuery;
import cn.datax.service.data.metadata.api.vo.MetadataSourceVo;
import cn.datax.service.data.metadata.mapstruct.MetadataSourceMapper;
import cn.datax.service.data.metadata.service.MetadataSourceService;
import cn.hutool.core.util.StrUtil;
import com.aspose.words.Document;
import com.aspose.words.SaveFormat;
import com.aspose.words.SaveOptions;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 数据源信息表 前端控制器
 * </p>
 *
 * @author yuwei
 * @since 2020-03-14
 */
@Api(tags = {"数据源信息表"})
@RestController
@RequestMapping("/sources")
public class MetadataSourceController extends BaseController {

    @Autowired
    private MetadataSourceService metadataSourceService;

    @Autowired
    private MetadataSourceMapper metadataSourceMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getMetadataSourceById(@PathVariable String id) {
        MetadataSourceEntity metadataSourceEntity = metadataSourceService.getMetadataSourceById(id);
        return R.ok().setData(metadataSourceMapper.toVO(metadataSourceEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getMetadataSourceList() {
//        QueryWrapper<MetadataSourceEntity> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
//        List<MetadataSourceEntity> list = metadataSourceService.list(queryWrapper);
        List<MetadataSourceEntity> list = metadataSourceService.getMetadataSourceList();
        List<MetadataSourceVo> collect = list.stream().map(metadataSourceMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 分页查询信息
     *
     * @param metadataSourceQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metadataSourceQuery", value = "查询实体metadataSourceQuery", required = true, dataTypeClass = MetadataSourceQuery.class)
    })
    @GetMapping("/page")
    public R getMetadataSourcePage(MetadataSourceQuery metadataSourceQuery) {
        QueryWrapper<MetadataSourceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(metadataSourceQuery.getSourceName()), "s.source_name", metadataSourceQuery.getSourceName());
        IPage<MetadataSourceEntity> page = metadataSourceService.pageWithAuth(new Page<>(metadataSourceQuery.getPageNum(), metadataSourceQuery.getPageSize()), queryWrapper);
        List<MetadataSourceVo> collect = page.getRecords().stream().map(metadataSourceMapper::toVO).collect(Collectors.toList());
        JsonPage<MetadataSourceVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param metadataSourceDto
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据metadataSourceDto对象添加信息")
    @ApiImplicitParam(name = "metadataSourceDto", value = "详细实体metadataSourceDto", required = true, dataType = "MetadataSourceDto")
    @PostMapping()
    public R saveMetadataSource(@RequestBody @Validated({ValidationGroups.Insert.class}) MetadataSourceDto metadataSourceDto) {
        metadataSourceService.saveMetadataSource(metadataSourceDto);
		metadataSourceService.refreshMetadata();
        return R.ok();
    }

    /**
     * 修改
     * @param metadataSourceDto
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "metadataSourceDto", value = "详细实体metadataSourceDto", required = true, dataType = "MetadataSourceDto")
    })
    @PutMapping("/{id}")
    public R updateMetadataSource(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) MetadataSourceDto metadataSourceDto) {
        metadataSourceService.updateMetadataSource(metadataSourceDto);
		metadataSourceService.refreshMetadata();
        return R.ok();
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteMetadataSourceById(@PathVariable String id) {
        metadataSourceService.deleteMetadataSourceById(id);
		metadataSourceService.refreshMetadata();
        return R.ok();
    }

    @ApiOperation(value = "批量删除", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteMetadataSourceBatch(@PathVariable List<String> ids) {
        metadataSourceService.deleteMetadataSourceBatch(ids);
		metadataSourceService.refreshMetadata();
        return R.ok();
    }

    /**
     * 检测数据库连通性
     * @param metadataSourceDto
     * @return
     */
    @ApiOperation(value = "数据库连通性", notes = "根据数据库配置信息检测数据库连通性")
    @ApiImplicitParam(name = "dataSource", value = "详细实体dataSource", required = true, dataType = "DataSourceDto")
    @PostMapping("/checkConnection")
    public R checkConnection(@RequestBody @Validated({ValidationGroups.Insert.class}) MetadataSourceDto metadataSourceDto) {
        DbQuery dbQuery = metadataSourceService.checkConnection(metadataSourceDto);
        Boolean valid = dbQuery.valid();
        return valid ? R.ok() : R.error("数据库连接有误，请检查数据库配置是否正确");
    }

    /**
     * 数据库表
     * @param id
     * @return
     */
    @ApiOperation(value = "数据库表", notes = "根据数据源的id来获取指定数据库表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    })
    @GetMapping("/{id}/tables")
    public R getDbTables(@PathVariable String id) {
        List<DbTable> tables = metadataSourceService.getDbTables(id);
        return R.ok().setData(tables);
    }

    /**
     * 数据库表结构
     * @param id
     * @return
     */
    @ApiOperation(value = "数据库表结构", notes = "根据数据源的id来获取指定数据库表的表结构")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "tableName", value = "数据库表", required = true, dataType = "String", paramType = "path")
    })
    @GetMapping("/{id}/{tableName}/columns")
    public R getDbTableColumns(@PathVariable String id, @PathVariable String tableName) {
        List<DbColumn> columns = metadataSourceService.getDbTableColumns(id, tableName);
        return R.ok().setData(columns);
    }

    @ApiOperation(value = "获取SQL结果", notes = "根据数据源的id来获取SQL结果")
    @ApiImplicitParam(name = "dbDataQuery", value = "详细实体dbDataQuery", required = true, dataType = "DbDataQuery")
    @PostMapping("/queryList")
    public R queryList(@RequestBody @Validated DbDataQuery dbDataQuery) {
        DbQuery dbQuery = metadataSourceService.getDbQuery(dbDataQuery.getDataSourceId());
        List<Map<String, Object>> list = dbQuery.queryList(dbDataQuery.getSql());
        return R.ok().setData(list);
    }

    @ApiOperation(value = "分页获取SQL结果", notes = "根据数据源的id来分页获取SQL结果")
    @ApiImplicitParam(name = "dbDataQuery", value = "详细实体dbDataQuery", required = true, dataType = "DbDataQuery")
    @PostMapping("/queryByPage")
    public R queryByPage(@RequestBody @Validated DbDataQuery dbDataQuery) {
        DbQuery dbQuery = metadataSourceService.getDbQuery(dbDataQuery.getDataSourceId());
        PageResult<Map<String, Object>> page = dbQuery.queryByPage(dbDataQuery.getSql(), dbDataQuery.getOffset(), dbDataQuery.getPageSize());
        page.setPageNum(dbDataQuery.getPageNum()).setPageSize(dbDataQuery.getPageSize());
        return R.ok().setData(page);
    }

    @ApiOperation(value = "同步", notes = "根据url的id来指定同步对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @PostMapping("/sync/{id}")
    public R syncMetadata(@PathVariable String id) {
        metadataSourceService.syncMetadata(id);
        return R.ok();

    }

    @ApiOperation(value = "数据库设计文档", notes = "根据url的id来指定生成数据库设计文档对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @PostMapping("/word/{id}")
    public void wordMetadata(@PathVariable String id, HttpServletResponse response) throws Exception {
        // 清空response
        response.reset();
        // 设置response的Header
        response.setContentType("application/octet-stream;charset=utf-8");
        // 设置content-disposition响应头控制浏览器以下载的形式打开文件
        response.addHeader("Content-Disposition", "attachment;filename=" + new String("数据库设计文档.doc".getBytes()));
        Document doc = metadataSourceService.wordMetadata(id);
        OutputStream out = response.getOutputStream();
        doc.save(out, SaveOptions.createSaveOptions(SaveFormat.DOC));
        out.flush();
        out.close();
    }

    /**
     * 刷新参数缓存
     *
     * @return
     */
    @GetMapping("/refresh")
    public R refreshMetadata() {
        metadataSourceService.refreshMetadata();
        return R.ok();
    }
}
