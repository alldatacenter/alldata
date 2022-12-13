package cn.datax.service.data.metadata.controller;

import cn.datax.common.core.R;
import cn.datax.service.data.metadata.api.dto.MetadataAuthorizeDto;
import cn.datax.service.data.metadata.mapstruct.MetadataAuthorizeMapper;
import cn.datax.service.data.metadata.service.MetadataAuthorizeService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import cn.datax.common.base.BaseController;

import java.util.List;

/**
 * <p>
 * 数据授权信息表 前端控制器
 * </p>
 *
 * @author AllDataDC
 * @date 2022-11-23
 */
@Api(tags = {"元数据授权信息表"})
@RestController
@RequestMapping("/authorizes")
public class MetadataAuthorizeController extends BaseController {

    @Autowired
    private MetadataAuthorizeService metadataAuthorizeService;


    @GetMapping("/getAuthorizedMetadata/{id}")
    public R getAuthorizedMetadata(@PathVariable String id) {
        List<String> list = metadataAuthorizeService.getAuthorizedMetadata(id);
        return R.ok().setData(list);
    }

    @PostMapping("/metadata")
    public R metadataAuthorize(@RequestBody @Validated MetadataAuthorizeDto metadataAuthorizeDto) {
        metadataAuthorizeService.metadataAuthorize(metadataAuthorizeDto);
        return R.ok();
    }

    /**
     * 刷新缓存
     *
     * @return
     */
    @GetMapping("/refresh")
    public R refreshCache() {
        metadataAuthorizeService.refreshCache();
        return R.ok();
    }
}
