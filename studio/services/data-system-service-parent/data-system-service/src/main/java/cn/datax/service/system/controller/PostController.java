package cn.datax.service.system.controller;

import cn.datax.common.core.DataConstant;
import cn.datax.common.core.JsonPage;
import cn.datax.common.core.R;
import cn.datax.common.validate.ValidationGroups;
import cn.datax.service.system.api.dto.PostDto;
import cn.datax.service.system.api.entity.PostEntity;
import cn.datax.service.system.api.query.PostQuery;
import cn.datax.service.system.api.vo.PostVo;
import cn.datax.service.system.mapstruct.PostMapper;
import cn.datax.service.system.service.PostService;
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
 *  前端控制器
 * </p>
 *
 * @author yuwei
 * @date 2022-09-11
 */
@Api(value="系统管理接口", tags = {"系统管理"})
@RestController
@RequestMapping("/posts")
public class PostController extends BaseController {

    @Autowired
    private PostService postService;

    @Autowired
    private PostMapper postMapper;

    @ApiOperation(value = "获取岗位详细信息", notes = "根据url的id来获取岗位详细信息")
    @ApiImplicitParam(name = "id", value = "岗位ID", required = true, dataType = "String", paramType = "path")
    @GetMapping("/{id}")
    public R getPostById(@PathVariable String id) {
        PostEntity postEntity = postService.getById(id);
        return R.ok().setData(postMapper.toVO(postEntity));
    }

    @ApiOperation(value = "获取岗位列表", notes = "")
    @GetMapping("/list")
    public R getPostList() {
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        List<PostEntity> list = postService.list(queryWrapper);
        List<PostVo> collect = list.stream().map(postMapper::toVO).collect(Collectors.toList());
        return R.ok().setData(collect);
    }

    @ApiOperation(value = "岗位分页查询", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "postQuery", value = "查询实体postQuery", required = true, dataTypeClass = PostQuery.class)
    })
    @GetMapping("/page")
    public R getPostPage(PostQuery postQuery) {
        QueryWrapper<PostEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(postQuery.getPostName()), "post_name", postQuery.getPostName());
        IPage<PostEntity> page = postService.page(new Page<>(postQuery.getPageNum(), postQuery.getPageSize()), queryWrapper);
        List<PostVo> collect = page.getRecords().stream().map(postMapper::toVO).collect(Collectors.toList());
        JsonPage<PostVo> jsonPage = new JsonPage<>(page.getCurrent(), page.getSize(), page.getTotal(), collect);
        return R.ok().setData(jsonPage);
    }

    @ApiOperation(value = "创建岗位", notes = "根据post对象创建岗位")
    @ApiImplicitParam(name = "post", value = "岗位详细实体post", required = true, dataType = "PostDto")
    @PostMapping()
    public R savePost(@RequestBody @Validated({ValidationGroups.Insert.class}) PostDto post) {
        PostEntity postEntity = postService.savePost(post);
        return R.ok().setData(postMapper.toVO(postEntity));
    }

    @ApiOperation(value = "更新岗位详细信息", notes = "根据url的id来指定更新对象，并根据传过来的post信息来更新岗位详细信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "岗位ID", required = true, dataType = "String", paramType = "path"),
            @ApiImplicitParam(name = "post", value = "岗位详细实体post", required = true, dataType = "PostDto")
    })
    @PutMapping("/{id}")
    public R updatePost(@PathVariable String id, @RequestBody @Validated({ValidationGroups.Update.class}) PostDto post) {
        PostEntity postEntity = postService.updatePost(post);
        return R.ok().setData(postMapper.toVO(postEntity));
    }

    @ApiOperation(value = "删除岗位", notes = "根据url的id来指定删除对象")
    @ApiImplicitParam(name = "id", value = "岗位ID", required = true, dataType = "String", paramType = "path")
    @DeleteMapping("/{id}")
    public R deletePost(@PathVariable String id) {
        postService.deletePostById(id);
        return R.ok();
    }

    @ApiOperation(value = "批量删除岗位", notes = "根据url的ids来批量删除对象")
    @ApiImplicitParam(name = "ids", value = "岗位ID集合", required = true, dataType = "List", paramType = "path")
    @DeleteMapping("/batch/{ids}")
    public R deletePostBatch(@PathVariable List<String> ids) {
        postService.deletePostBatch(ids);
        return R.ok();
    }
}

