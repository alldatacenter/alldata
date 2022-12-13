package cn.datax.service.data.standard.controller;

import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.security.annotation.DataInner;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.data.standard.api.dto.ContrastDto;
import cn.datax.service.data.standard.api.entity.ContrastEntity;
import cn.datax.service.data.standard.api.vo.ContrastStatisticVo;
import cn.datax.service.data.standard.api.vo.ContrastTreeVo;
import cn.datax.service.data.standard.api.vo.ContrastVo;
import cn.datax.service.data.standard.api.query.ContrastQuery;
import cn.datax.service.data.standard.mapstruct.ContrastMapper;
import cn.datax.service.data.standard.service.ContrastService;
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
 * 对照表信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-27
 */
@Api(tags = {"对照表信息表"})
@RestController
@RequestMapping("/contrasts")
public class ContrastController extends BaseController {

    @Autowired
    private ContrastService contrastService;

    @Autowired
    private ContrastMapper contrastMapper;

	/**
	 * 根据数据源id查询
	 * @param sourceId
	 * @return
	 */
	@DataInner
	@GetMapping("/source/{sourceId}")
	public ContrastEntity getBySourceId(@PathVariable String sourceId) {
		return contrastService.getBySourceId(sourceId);
	}

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getContrastById(@PathVariable String id) {
        ContrastEntity contrastEntity = contrastService.getContrastById(id);
        return R.ok().setData(contrastMapper.toVO(contrastEntity));
    }

    /**
     * 分页查询信息
     *
     * @param contrastQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "contrastQuery", value = "查询实体contrastQuery", required = true, dataTypeClass = ContrastQuery.class)
    })
    @GetMapping("/page")
    public R getContrastPage(ContrastQuery contrastQuery) {
        QueryWrapper<ContrastEntity> queryWrapper = new QueryWrapper<>();
        IPage<ContrastEntity> page = contrastService.page(new Page<>(contrastQuery.getPageNum(), contrastQuery.getPageSize()), queryWrapper);
        List<ContrastVo> collect = page.getRecords().stream().map(contrastMapper::toVO).collect(Collectors.toList());
        JsonPage<ContrastVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param contrast
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据contrast对象添加信息")
    @ApiImplicitParam(name = "contrast", value = "详细实体contrast", required = true, dataType = "ContrastDto")
    @PostMapping()
    public R saveContrast(@RequestBody @Validated({ValidationGroups.Insert.class}) ContrastDto contrast) {
        ContrastEntity contrastEntity = contrastService.saveContrast(contrast);
        return R.ok().setData(contrastMapper.toVO(contrastEntity));
    }

    /**
     * 修改
     * @param contrast
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "contrast", value = "详细实体contrast", required = true, dataType = "ContrastDto")
    })
    @PutMapping("/{id}")
    public R updateContrast(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) ContrastDto contrast) {
        ContrastEntity contrastEntity = contrastService.updateContrast(contrast);
        return R.ok().setData(contrastMapper.toVO(contrastEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteContrastById(@PathVariable String id) {
        contrastService.deleteContrastById(id);
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
    public R deleteContrastBatch(@PathVariable List<String> ids) {
        contrastService.deleteContrastBatch(ids);
        return R.ok();
    }

    @GetMapping("/tree")
    public R getContrastTree() {
        List<ContrastTreeVo> list = contrastService.getContrastTree();
        return R.ok().setData(list);
    }

    /**
     * 分页查询统计信息
     *
     * @param contrastQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "contrastQuery", value = "查询实体contrastQuery", required = true, dataTypeClass = ContrastQuery.class)
    })
    @GetMapping("/stat")
    public R contrastStat(ContrastQuery contrastQuery) {
        QueryWrapper<ContrastEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(contrastQuery.getSourceName()), "c.source_name", contrastQuery.getSourceName());
        queryWrapper.like(StrUtil.isNotBlank(contrastQuery.getTableName()), "c.table_name", contrastQuery.getTableName());
        queryWrapper.like(StrUtil.isNotBlank(contrastQuery.getColumnName()), "c.column_name", contrastQuery.getColumnName());
        IPage<ContrastEntity> page = contrastService.statistic(new Page<>(contrastQuery.getPageNum(), contrastQuery.getPageSize()), queryWrapper);
        List<ContrastStatisticVo> collect = page.getRecords().stream().map(contrastMapper::toSVO).collect(Collectors.toList());
        JsonPage<ContrastStatisticVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }
}
