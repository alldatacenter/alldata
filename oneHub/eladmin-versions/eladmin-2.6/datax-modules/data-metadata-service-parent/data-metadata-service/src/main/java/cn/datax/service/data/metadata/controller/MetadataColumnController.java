package cn.datax.service.data.metadata.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.metadata.api.dto.MetadataColumnDto;
import cn.datax.service.data.metadata.api.entity.MetadataColumnEntity;
import cn.datax.service.data.metadata.api.vo.MetadataColumnVo;
import cn.datax.service.data.metadata.api.query.MetadataColumnQuery;
import cn.datax.service.data.metadata.api.vo.MetadataTreeVo;
import cn.datax.service.data.metadata.mapstruct.MetadataColumnMapper;
import cn.datax.service.data.metadata.service.MetadataColumnService;
import cn.hutool.core.util.StrUtil;
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

import cn.datax.common.base.BaseController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 元数据信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-29
 */
@Api(tags = {"元数据信息表"})
@RestController
@RequestMapping("/columns")
public class MetadataColumnController extends BaseController {

    @Autowired
    private MetadataColumnService metadataColumnService;

    @Autowired
    private MetadataColumnMapper metadataColumnMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getDataMetadataColumnById(@PathVariable String id) {
        MetadataColumnEntity metadataColumnEntity = metadataColumnService.getMetadataColumnById(id);
        return R.ok().setData(metadataColumnMapper.toVO(metadataColumnEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getDataMetadataColumnList(MetadataColumnQuery metadataColumnQuery) {
//        QueryWrapper<MetadataColumnEntity> queryWrapper = new QueryWrapper<>();
//        queryWrapper.like(StrUtil.isNotBlank(metadataColumnQuery.getColumnName()), "column_name", metadataColumnQuery.getColumnName());
//        queryWrapper.eq(StrUtil.isNotBlank(metadataColumnQuery.getSourceId()), "source_id", metadataColumnQuery.getSourceId());
//        queryWrapper.eq(StrUtil.isNotBlank(metadataColumnQuery.getTableId()), "table_id", metadataColumnQuery.getTableId());
//        List<MetadataColumnEntity> list = metadataColumnService.list(queryWrapper);
        List<MetadataColumnEntity> list = metadataColumnService.getDataMetadataColumnList(metadataColumnQuery);
        List<MetadataColumnVo> collect = list.stream().map(metadataColumnMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 分页查询信息
     *
     * @param metadataColumnQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "dataMetadataColumnQuery", value = "查询实体dataMetadataColumnQuery", required = true, dataTypeClass = MetadataColumnQuery.class)
    })
    @GetMapping("/page")
    public R getDataMetadataColumnPage(MetadataColumnQuery metadataColumnQuery) {
        QueryWrapper<MetadataColumnEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(metadataColumnQuery.getColumnName()), "c.column_name", metadataColumnQuery.getColumnName());
        queryWrapper.eq(StrUtil.isNotBlank(metadataColumnQuery.getSourceId()), "c.source_id", metadataColumnQuery.getSourceId());
        queryWrapper.eq(StrUtil.isNotBlank(metadataColumnQuery.getTableId()), "c.table_id", metadataColumnQuery.getTableId());
        IPage<MetadataColumnEntity> page = metadataColumnService.pageWithAuth(new Page<>(metadataColumnQuery.getPageNum(), metadataColumnQuery.getPageSize()), queryWrapper);
        List<MetadataColumnVo> collect = page.getRecords().stream().map(metadataColumnMapper::toVO).collect(Collectors.toList());
        JsonPage<MetadataColumnVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param dataMetadataColumn
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据dataMetadataColumn对象添加信息")
    @ApiImplicitParam(name = "dataMetadataColumn", value = "详细实体dataMetadataColumn", required = true, dataType = "DataMetadataColumnDto")
    @PostMapping()
    public R saveDataMetadataColumn(@RequestBody @Validated({ValidationGroups.Insert.class}) MetadataColumnDto dataMetadataColumn) {
        MetadataColumnEntity metadataColumnEntity = metadataColumnService.saveMetadataColumn(dataMetadataColumn);
        return R.ok().setData(metadataColumnMapper.toVO(metadataColumnEntity));
    }

    /**
     * 修改
     * @param dataMetadataColumn
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "dataMetadataColumn", value = "详细实体dataMetadataColumn", required = true, dataType = "DataMetadataColumnDto")
    })
    @PutMapping("/{id}")
    public R updateDataMetadataColumn(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) MetadataColumnDto dataMetadataColumn) {
        MetadataColumnEntity metadataColumnEntity = metadataColumnService.updateMetadataColumn(dataMetadataColumn);
        return R.ok().setData(metadataColumnMapper.toVO(metadataColumnEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteDataMetadataColumnById(@PathVariable String id) {
        metadataColumnService.deleteMetadataColumnById(id);
        return R.ok();
    }

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除角色", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteDataMetadataColumnBatch(@PathVariable List<String> ids) {
        metadataColumnService.deleteMetadataColumnBatch(ids);
        return R.ok();
    }

    /**
     * 获取层级树
     * @param level 层级database、table、column
     * @return
     */
    @ApiOperation(value = "获取层级树", notes = "根据url的层级来获取树对象")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "level", value = "层级", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "metadataColumnQuery", value = "查询实体metadataColumnQuery", required = false, dataType = "MetadataColumnQuery")
    })
    @GetMapping("/tree/{level}")
    public R getDataMetadataTree(@PathVariable String level, MetadataColumnQuery metadataColumnQuery) {
        List<MetadataTreeVo> list = metadataColumnService.getDataMetadataTree(level, metadataColumnQuery);
        return R.ok().setData(list);
    }
}
