package cn.datax.service.data.market.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.DataConstant;
import cn.datax.common.security.annotation.DataInner;
import cn.datax.service.data.market.api.entity.ApiMaskEntity;
import cn.datax.service.data.market.api.entity.DataApiEntity;
import cn.datax.service.data.market.service.ApiMaskService;
import cn.datax.service.data.market.service.DataApiService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inner")
public class InnerController extends BaseController {

    @Autowired
    private DataApiService dataApiService;

    @Autowired
    private ApiMaskService apiMaskService;

    @DataInner
    @GetMapping("/apis/{id}")
    public DataApiEntity getDataApiById(@PathVariable("id") String id) {
        DataApiEntity dataApiEntity = dataApiService.getDataApiById(id);
        return dataApiEntity;
    }

    @DataInner
    @GetMapping("/apis/release/list")
    public List<DataApiEntity> getReleaseDataApiList() {
        QueryWrapper<DataApiEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.ApiState.RELEASE.getKey());
        List<DataApiEntity> dataApiEntityList = dataApiService.list(queryWrapper);
        return dataApiEntityList;
    }

    @DataInner
    @GetMapping("/apiMasks/api/{id}")
    public ApiMaskEntity getApiMaskByApiId(@PathVariable("id") String id) {
        ApiMaskEntity apiMaskEntity = apiMaskService.getApiMaskByApiId(id);
        return apiMaskEntity;
    }
}
