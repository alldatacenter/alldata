package cn.datax.service.codegen.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.codegen.api.dto.GenTableDto;
import cn.datax.service.codegen.api.entity.GenTableEntity;
import cn.datax.service.codegen.mapstruct.GenTableMapper;
import cn.datax.service.codegen.api.query.GenTableQuery;
import cn.datax.service.codegen.service.GenTableService;
import cn.datax.service.codegen.api.vo.GenTableVo;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 代码生成信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-19
 */
@Api(tags = {"代码生成信息表"})
@RestController
@RequestMapping("/codegen/genTable")
public class GenTableController extends BaseController {

    @Autowired
    private GenTableService genTableService;

    @Autowired
    private GenTableMapper genTableMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getGenTableById(@PathVariable String id) {
        GenTableEntity genTableEntity = genTableService.getGenTableById(id);
        return R.ok().setData(genTableMapper.toVO(genTableEntity));
    }

    /**
     * 分页查询信息
     *
     * @param genTableQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "genTableQuery", value = "查询实体genTableQuery", required = true, dataTypeClass = GenTableQuery.class)
    })
    @GetMapping("/page")
    public R getGenTablePage(GenTableQuery genTableQuery) {
        QueryWrapper<GenTableEntity> queryWrapper = new QueryWrapper<>();
        IPage<GenTableEntity> page = genTableService.page(new Page<>(genTableQuery.getPageNum(), genTableQuery.getPageSize()), queryWrapper);
        List<GenTableVo> collect = page.getRecords().stream().map(genTableMapper::toVO).collect(Collectors.toList());
        JsonPage<GenTableVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param genTable
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据genTable对象添加信息")
    @ApiImplicitParam(name = "genTable", value = "详细实体genTable", required = true, dataType = "GenTableDto")
    @PostMapping()
    public R saveGenTable(@RequestBody @Validated({ValidationGroups.Insert.class}) GenTableDto genTable) {
        genTableService.saveGenTable(genTable);
        return R.ok();
    }

    /**
     * 修改
     * @param genTable
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "genTable", value = "详细实体genTable", required = true, dataType = "GenTableDto")
    })
    @PutMapping("/{id}")
    public R updateGenTable(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) GenTableDto genTable) {
        genTableService.updateGenTable(genTable);
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
    public R deleteGenTableById(@PathVariable String id) {
        genTableService.deleteGenTableById(id);
        return R.ok();
    }
}
