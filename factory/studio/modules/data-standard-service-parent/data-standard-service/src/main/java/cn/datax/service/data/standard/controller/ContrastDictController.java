package cn.datax.service.data.standard.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.standard.api.dto.ContrastDictDto;
import cn.datax.service.data.standard.api.entity.ContrastDictEntity;
import cn.datax.service.data.standard.api.vo.ContrastDictVo;
import cn.datax.service.data.standard.api.query.ContrastDictQuery;
import cn.datax.service.data.standard.mapstruct.ContrastDictMapper;
import cn.datax.service.data.standard.service.ContrastDictService;
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
 * 字典对照信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Api(tags = {"字典对照信息表"})
@RestController
@RequestMapping("/contrastDicts")
public class ContrastDictController extends BaseController {

    @Autowired
    private ContrastDictService contrastDictService;

    @Autowired
    private ContrastDictMapper contrastDictMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getContrastDictById(@PathVariable String id) {
        ContrastDictEntity contrastDictEntity = contrastDictService.getContrastDictById(id);
        return R.ok().setData(contrastDictMapper.toVO(contrastDictEntity));
    }

    /**
     * 分页查询信息
     *
     * @param contrastDictQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "contrastDictQuery", value = "查询实体contrastDictQuery", required = true, dataTypeClass = ContrastDictQuery.class)
    })
    @GetMapping("/page")
    public R getContrastDictPage(ContrastDictQuery contrastDictQuery) {
        QueryWrapper<ContrastDictEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StrUtil.isNotBlank(contrastDictQuery.getContrastId()), "d.contrast_id", contrastDictQuery.getContrastId());
        queryWrapper.like(StrUtil.isNotBlank(contrastDictQuery.getColCode()), "d.col_code", contrastDictQuery.getColCode());
        queryWrapper.like(StrUtil.isNotBlank(contrastDictQuery.getColName()), "d.col_name", contrastDictQuery.getColName());
        IPage<ContrastDictEntity> page = contrastDictService.page(new Page<>(contrastDictQuery.getPageNum(), contrastDictQuery.getPageSize()), queryWrapper);
        List<ContrastDictVo> collect = page.getRecords().stream().map(contrastDictMapper::toVO).collect(Collectors.toList());
        JsonPage<ContrastDictVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param contrastDict
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据contrastDict对象添加信息")
    @ApiImplicitParam(name = "contrastDict", value = "详细实体contrastDict", required = true, dataType = "ContrastDictDto")
    @PostMapping()
    public R saveContrastDict(@RequestBody @Validated({ValidationGroups.Insert.class}) ContrastDictDto contrastDict) {
        ContrastDictEntity contrastDictEntity = contrastDictService.saveContrastDict(contrastDict);
        return R.ok().setData(contrastDictMapper.toVO(contrastDictEntity));
    }

    /**
     * 修改
     * @param contrastDict
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "contrastDict", value = "详细实体contrastDict", required = true, dataType = "ContrastDictDto")
    })
    @PutMapping("/{id}")
    public R updateContrastDict(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ContrastDictDto contrastDict) {
        ContrastDictEntity contrastDictEntity = contrastDictService.updateContrastDict(contrastDict);
        return R.ok().setData(contrastDictMapper.toVO(contrastDictEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteContrastDictById(@PathVariable String id) {
        contrastDictService.deleteContrastDictById(id);
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
    public R deleteContrastDictBatch(@PathVariable List<String> ids) {
        contrastDictService.deleteContrastDictBatch(ids);
        return R.ok();
    }
}
