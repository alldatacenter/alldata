package cn.datax.service.data.metadata.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.metadata.api.dto.MetadataChangeRecordDto;
import cn.datax.service.data.metadata.api.entity.MetadataChangeRecordEntity;
import cn.datax.service.data.metadata.api.vo.MetadataChangeRecordVo;
import cn.datax.service.data.metadata.api.query.MetadataChangeRecordQuery;
import cn.datax.service.data.metadata.mapstruct.MetadataChangeRecordMapper;
import cn.datax.service.data.metadata.service.MetadataChangeRecordService;
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
 * 元数据变更记录表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-30
 */
@Api(tags = {"元数据变更记录表"})
@RestController
@RequestMapping("/changeRecords")
public class MetadataChangeRecordController extends BaseController {

    @Autowired
    private MetadataChangeRecordService metadataChangeRecordService;

    @Autowired
    private MetadataChangeRecordMapper metadataChangeRecordMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getMetadataChangeRecordById(@PathVariable String id) {
        MetadataChangeRecordEntity metadataChangeRecordEntity = metadataChangeRecordService.getMetadataChangeRecordById(id);
        return R.ok().setData(metadataChangeRecordMapper.toVO(metadataChangeRecordEntity));
    }

    /**
     * 分页查询信息
     *
     * @param metadataChangeRecordQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "metadataChangeRecordQuery", value = "查询实体metadataChangeRecordQuery", required = true, dataTypeClass = MetadataChangeRecordQuery.class)
    })
    @GetMapping("/page")
    public R getMetadataChangeRecordPage(MetadataChangeRecordQuery metadataChangeRecordQuery) {
        QueryWrapper<MetadataChangeRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(metadataChangeRecordQuery.getObjectId()), "r.object_id", metadataChangeRecordQuery.getObjectId());
        queryWrapper.like(StrUtil.isNotBlank(metadataChangeRecordQuery.getFieldName()), "r.field_name", metadataChangeRecordQuery.getFieldName());
        IPage<MetadataChangeRecordEntity> page = metadataChangeRecordService.page(new Page<>(metadataChangeRecordQuery.getPageNum(), metadataChangeRecordQuery.getPageSize()), queryWrapper);
        List<MetadataChangeRecordVo> collect = page.getRecords().stream().map(metadataChangeRecordMapper::toVO).collect(Collectors.toList());
        JsonPage<MetadataChangeRecordVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param metadataChangeRecord
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据metadataChangeRecord对象添加信息")
    @ApiImplicitParam(name = "metadataChangeRecord", value = "详细实体metadataChangeRecord", required = true, dataType = "MetadataChangeRecordDto")
    @PostMapping()
    public R saveMetadataChangeRecord(@RequestBody @Validated({ValidationGroups.Insert.class}) MetadataChangeRecordDto metadataChangeRecord) {
        MetadataChangeRecordEntity metadataChangeRecordEntity = metadataChangeRecordService.saveMetadataChangeRecord(metadataChangeRecord);
        return R.ok().setData(metadataChangeRecordMapper.toVO(metadataChangeRecordEntity));
    }

    /**
     * 修改
     * @param metadataChangeRecord
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "metadataChangeRecord", value = "详细实体metadataChangeRecord", required = true, dataType = "MetadataChangeRecordDto")
    })
    @PutMapping("/{id}")
    public R updateMetadataChangeRecord(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) MetadataChangeRecordDto metadataChangeRecord) {
        MetadataChangeRecordEntity metadataChangeRecordEntity = metadataChangeRecordService.updateMetadataChangeRecord(metadataChangeRecord);
        return R.ok().setData(metadataChangeRecordMapper.toVO(metadataChangeRecordEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteMetadataChangeRecordById(@PathVariable String id) {
        metadataChangeRecordService.deleteMetadataChangeRecordById(id);
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
    public R deleteMetadataChangeRecordBatch(@PathVariable List<String> ids) {
        metadataChangeRecordService.deleteMetadataChangeRecordBatch(ids);
        return R.ok();
    }
}
