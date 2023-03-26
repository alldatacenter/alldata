package cn.datax.service.system.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.system.api.dto.DictItemDto;
import cn.datax.service.system.api.entity.DictItemEntity;
import cn.datax.service.system.api.vo.DictItemVo;
import cn.datax.service.system.api.query.DictItemQuery;
import cn.datax.service.system.mapstruct.DictItemMapper;
import cn.datax.service.system.service.DictItemService;
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
 * 字典项信息表 前端控制器
 * </p>
 *
 * @author yuwei
 * @date 2022-04-17
 */
@Api(tags = {"字典项信息表"})
@RestController
@RequestMapping("/dict/items")
public class DictItemController extends BaseController {

    @Autowired
    private DictItemService dictItemService;

    @Autowired
    private DictItemMapper dictItemMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getDictItemById(@PathVariable String id) {
        DictItemEntity dictItemEntity = dictItemService.getById(id);
        return R.ok().setData(dictItemMapper.toVO(dictItemEntity));
    }

    /**
     * 分页查询信息
     *
     * @param dictItemQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "dictItemQuery", value = "查询实体dictItemQuery", required = true, dataTypeClass = DictItemQuery.class)
    })
    @GetMapping("/page")
    public R getDictItemPage(DictItemQuery dictItemQuery) {
        QueryWrapper<DictItemEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(dictItemQuery.getItemText()), "item_text", dictItemQuery.getItemText());
        queryWrapper.like(StrUtil.isNotBlank(dictItemQuery.getItemValue()), "item_value", dictItemQuery.getItemValue());
        queryWrapper.eq(StrUtil.isNotBlank(dictItemQuery.getDictId()), "dict_id", dictItemQuery.getDictId());
        IPage<DictItemEntity> page = dictItemService.page(new Page<>(dictItemQuery.getPageNum(), dictItemQuery.getPageSize()), queryWrapper);
        List<DictItemVo> collect = page.getRecords().stream().map(dictItemMapper::toVO).collect(Collectors.toList());
        JsonPage<DictItemVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param dictItem
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据dictItem对象添加信息")
    @ApiImplicitParam(name = "dictItem", value = "详细实体dictItem", required = true, dataType = "DictItemDto")
    @PostMapping()
    public R saveDictItem(@RequestBody @Validated({ValidationGroups.Insert.class}) DictItemDto dictItem) {
        DictItemEntity dictItemEntity = dictItemService.saveDictItem(dictItem);
        return R.ok().setData(dictItemMapper.toVO(dictItemEntity));
    }

    /**
     * 修改
     * @param dictItem
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "dictItem", value = "详细实体dictItem", required = true, dataType = "DictItemDto")
    })
    @PutMapping("/{id}")
    public R updateDictItem(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) DictItemDto dictItem) {
        DictItemEntity dictItemEntity = dictItemService.updateDictItem(dictItem);
        return R.ok().setData(dictItemMapper.toVO(dictItemEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteDictItemById(@PathVariable String id) {
        dictItemService.deleteDictItemById(id);
        return R.ok();
    }

    @ApiOperation(value = "批量删除", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteDictItemBatch(@PathVariable List<String> ids) {
        dictItemService.deleteDictItemBatch(ids);
        return R.ok();
    }
}
