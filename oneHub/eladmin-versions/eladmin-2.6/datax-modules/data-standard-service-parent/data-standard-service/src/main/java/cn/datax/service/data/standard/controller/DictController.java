package cn.datax.service.data.standard.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.standard.api.dto.DictDto;
import cn.datax.service.data.standard.api.entity.DictEntity;
import cn.datax.service.data.standard.api.vo.DictVo;
import cn.datax.service.data.standard.api.query.DictQuery;
import cn.datax.service.data.standard.mapstruct.DictMapper;
import cn.datax.service.data.standard.service.DictService;
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
 * 数据标准字典表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-26
 */
@Api(tags = {"数据标准字典表"})
@RestController
@RequestMapping("/dicts")
public class DictController extends BaseController {

    @Autowired
    private DictService dictService;

    @Autowired
    private DictMapper dictMapper;

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
        DictEntity dictEntity = dictService.getDictById(id);
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
        queryWrapper.eq(StrUtil.isNotBlank(dictQuery.getTypeId()), "d.type_id", dictQuery.getTypeId());
        queryWrapper.like(StrUtil.isNotBlank(dictQuery.getGbCode()), "d.gb_code", dictQuery.getGbCode());
        queryWrapper.like(StrUtil.isNotBlank(dictQuery.getGbName()), "d.gb_name", dictQuery.getGbName());
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

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除角色", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deleteDictBatch(@PathVariable List<String> ids) {
        dictService.deleteDictBatch(ids);
        return R.ok();
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
