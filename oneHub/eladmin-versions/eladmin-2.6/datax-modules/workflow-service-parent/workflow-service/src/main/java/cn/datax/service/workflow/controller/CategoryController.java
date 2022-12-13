package cn.datax.service.workflow.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.workflow.api.dto.CategoryDto;
import cn.datax.service.workflow.api.entity.CategoryEntity;
import cn.datax.service.workflow.api.vo.CategoryVo;
import cn.datax.service.workflow.api.query.CategoryQuery;
import cn.datax.service.workflow.mapstruct.CategoryMapper;
import cn.datax.service.workflow.service.CategoryService;
import cn.datax.service.workflow.service.FlowDefinitionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.flowable.engine.repository.Deployment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 流程分类表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-10
 */
@Api(tags = {"流程分类表"})
@RestController
@RequestMapping("/categorys")
public class CategoryController extends BaseController {

    @Autowired
    private CategoryService categoryService;
	@Autowired
	private FlowDefinitionService flowDefinitionService;
    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 通过ID查询信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取详细信息", notes = "根据url的id来获取详细信息")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getCategoryById(@PathVariable String id) {
        CategoryEntity categoryEntity = categoryService.getCategoryById(id);
        return R.ok().setData(categoryMapper.toVO(categoryEntity));
    }

    @ApiOperation(value = "获取列表", notes = "")
    @GetMapping("/list")
    public R getCategoryList() {
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        List<CategoryEntity> list = categoryService.list(queryWrapper);
        List<CategoryVo> collect = list.stream().map(categoryMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    /**
     * 分页查询信息
     *
     * @param categoryQuery
     * @return
     */
    @ApiOperation(value = "分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "categoryQuery", value = "查询实体categoryQuery", required = true, dataTypeClass = CategoryQuery.class)
    })
    @GetMapping("/page")
    public R getCategoryPage(CategoryQuery categoryQuery) {
        QueryWrapper<CategoryEntity> queryWrapper = new QueryWrapper<>();
        IPage<CategoryEntity> page = categoryService.page(new Page<>(categoryQuery.getPageNum(), categoryQuery.getPageSize()), queryWrapper);
        List<CategoryVo> collect = page.getRecords().stream().map(categoryMapper::toVO).collect(Collectors.toList());
        JsonPage<CategoryVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    /**
     * 添加
     * @param category
     * @return
     */
    @ApiOperation(value = "添加信息", notes = "根据category对象添加信息")
    @ApiImplicitParam(name = "category", value = "详细实体category", required = true, dataType = "CategoryDto")
    @PostMapping()
    public R saveCategory(@RequestBody @Validated({ValidationGroups.Insert.class}) CategoryDto category) {
        CategoryEntity categoryEntity = categoryService.saveCategory(category);
        return R.ok().setData(categoryMapper.toVO(categoryEntity));
    }

    /**
     * 修改
     * @param category
     * @return
     */
    @ApiOperation(value = "修改信息", notes = "根据url的id来指定修改对象，并根据传过来的信息来修改详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "category", value = "详细实体category", required = true, dataType = "CategoryDto")
    })
    @PutMapping("/{id}")
    public R updateCategory(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) CategoryDto category) {
        CategoryEntity categoryEntity = categoryService.updateCategory(category);
        return R.ok().setData(categoryMapper.toVO(categoryEntity));
    }

    /**
     * 删除
     * @param id
     * @return
     */
    @ApiOperation(value = "删除", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deleteCategoryById(@PathVariable String id) {
		 Deployment deployment = flowDefinitionService.getByCategoryId(id);
		 if (deployment != null) {
		 	throw new RuntimeException("该分类下有与之关联的流程定义，不允许删除！");
		 }
		categoryService.deleteCategoryById(id);
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
    public R deleteCategoryBatch(@PathVariable List<String> ids) {
        categoryService.deleteCategoryBatch(ids);
        return R.ok();
    }
}
