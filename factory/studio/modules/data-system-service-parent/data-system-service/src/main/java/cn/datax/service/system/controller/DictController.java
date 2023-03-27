package cn.datax.service.system.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.system.api.dto.DictDto;
import cn.datax.service.system.api.entity.DictEntity;
import cn.datax.service.system.api.entity.DictItemEntity;
import cn.datax.service.system.api.vo.DictItemVo;
import cn.datax.service.system.api.vo.DictVo;
import cn.datax.service.system.api.query.DictQuery;
import cn.datax.service.system.mapstruct.DictItemMapper;
import cn.datax.service.system.mapstruct.DictMapper;
import cn.datax.service.system.service.DictService;
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
 * 字典编码信息表 前端控制器
 * </p>
 *
 * @author yuwei
 * @date 2022-04-17
 */
@Api(tags = {"字典编码信息表"})
@RestController
@RequestMapping("/dicts")
public class DictController extends BaseController {

    @Autowired
    private DictService dictService;

    @Autowired
    private DictMapper dictMapper;

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
    public R getDictById(@PathVariable String id) {
        DictEntity dictEntity = dictService.getById(id);
        return R.ok().setData(dictMapper.toVO(dictEntity));
    }

    /**
     * 分页查询信息
     *
     * @param dictQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "dictQuery", value = "查询实体dictQuery", required = true, dataTypeClass = DictQuery.class)
    })
    @GetMapping("/page")
    public R getDictPage(DictQuery dictQuery) {
        QueryWrapper<DictEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(dictQuery.getDictName()), "dict_name", dictQuery.getDictName());
        queryWrapper.like(StrUtil.isNotBlank(dictQuery.getDictCode()), "dict_code", dictQuery.getDictCode());
        IPage<DictEntity> page = dictService.page(new Page<>(dictQuery.getPageNum(), dictQuery.getPageSize()), queryWrapper);
        List<DictVo> collect = page.getRecords().stream().map(dictMapper::toVO).collect(Collectors.toList());
        JsonPage<DictVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param dict
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据dict对象添加信息")
    @ApiImplicitParam(name = "dict", value = "详细实体dict", required = true, dataType = "DictDto")
    @PostMapping()
    public R saveDict(@RequestBody @Validated({ValidationGroups.Insert.class}) DictDto dict) {
        DictEntity dictEntity = dictService.saveDict(dict);
        return R.ok().setData(dictMapper.toVO(dictEntity));
    }

    /**
     * 修改
     * @param dict
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "dict", value = "详细实体dict", required = true, dataType = "DictDto")
    })
    @PutMapping("/{id}")
    public R updateDict(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) DictDto dict) {
        DictEntity dictEntity = dictService.updateDict(dict);
        return R.ok().setData(dictMapper.toVO(dictEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteDictById(@PathVariable String id) {
        dictService.deleteDictById(id);
        return R.ok();
    }

    @ApiOperation(value = "批量删除", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteDictBatch(@PathVariable List<String> ids) {
        dictService.deleteDictBatch(ids);
        return R.ok();
    }

    /**
     * 获取字典项
     *
     * @return
     */
    @GetMapping("/code/{code}")
    public R getDictItems(@PathVariable String code) {
        List<DictItemEntity> list = dictService.getDictItems(code);
        List<DictItemVo> collect = list.stream().map(dictItemMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 刷新字典缓存
     *
     * @return
     */
    @GetMapping("/refresh")
    public R refreshDict() {
        dictService.refreshDict();
        return R.ok();
    }
}
