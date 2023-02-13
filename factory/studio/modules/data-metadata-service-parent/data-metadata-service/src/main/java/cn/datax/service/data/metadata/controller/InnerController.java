package cn.datax.service.data.metadata.controller;

import cn.datax.common.base.BaseController;
import cn.datax.common.core.DataConstant;
import cn.datax.common.security.annotation.DataInner;
import cn.datax.service.data.metadata.api.entity.MetadataSourceEntity;
import cn.datax.service.data.metadata.service.MetadataSourceService;
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
    private MetadataSourceService metadataSourceService;

    @DataInner
    @GetMapping("/sources/{id}")
    public MetadataSourceEntity getMetadataSourceById(@PathVariable("id") String id) {
        MetadataSourceEntity metadataSourceEntity = metadataSourceService.getMetadataSourceById(id);
        return metadataSourceEntity;
    }

    @DataInner
    @GetMapping("/sources/list")
    public List<MetadataSourceEntity> getMetadataSourceList() {
        QueryWrapper<MetadataSourceEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", DataConstant.EnableState.ENABLE.getKey());
        List<MetadataSourceEntity> list = metadataSourceService.list(queryWrapper);
        return list;
    }
}
