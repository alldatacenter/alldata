package com.alibaba.sreworks.other.server.controllers;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.StringUtil;
import com.alibaba.sreworks.domain.DO.SreworksFile;
import com.alibaba.sreworks.domain.repository.SreworksFileRepository;
import com.alibaba.sreworks.other.server.params.SreworksFileCreateRequestParam;
import com.alibaba.sreworks.other.server.params.SreworksFileModifyRequestParam;
import com.alibaba.sreworks.other.server.services.MinioService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/sreworksFile")
public class SreworksFileController extends BaseController {

    @Autowired
    MinioService minioService;

    @Autowired
    private SreworksFileRepository sreworksFileRepository;

    @RequestMapping(value = "put", method = RequestMethod.POST)
    public TeslaBaseResult put(@RequestParam("file") MultipartFile file, String responseType) throws Exception {
        String uploadSubPath = System.getenv("UPLOAD_SUB_PATH");
        uploadSubPath = StringUtil.isEmpty(uploadSubPath) ? "v0.1" : uploadSubPath;
        String name = UUID.randomUUID().toString();
        String namePath = uploadSubPath + "/" + name;
        minioService.putObject(
            namePath,
            file,
            ImmutableMap.of(
                "creator", getUserEmployeeId(),
                "originalFilename", Objects.requireNonNull(file.getOriginalFilename())
            )
        );
        if ("relative".equals(responseType)) {
            return buildSucceedResult(minioService.relativeObjectUrl(namePath));
        }
        return buildSucceedResult(minioService.objectUrl(name));
    }

    @RequestMapping(value = "create", method = RequestMethod.POST)
    public TeslaBaseResult create(@RequestBody SreworksFileCreateRequestParam param) {

        SreworksFile sreworksFile = SreworksFile.builder()
            .gmtCreate(System.currentTimeMillis())
            .gmtModified(System.currentTimeMillis())
            .operator(getUserEmployeeId())
            .category(param.getCategory())
            .name(param.getName())
            .alias(param.getAlias())
            .fileId(param.fileId())
            .type(param.getType())
            .description(param.getDescription())
            .build();
        sreworksFileRepository.saveAndFlush(sreworksFile);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "modify", method = RequestMethod.POST)
    public TeslaBaseResult modify(@RequestBody SreworksFileModifyRequestParam param) {

        SreworksFile sreworksFile = sreworksFileRepository.findFirstById(param.getId());

        sreworksFile.setGmtModified(System.currentTimeMillis());
        sreworksFile.setOperator(getUserEmployeeId());
        sreworksFile.setAlias(param.getAlias());
        sreworksFile.setType(param.getType());
        sreworksFile.setDescription(param.getDescription());

        sreworksFileRepository.saveAndFlush(sreworksFile);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "delete", method = RequestMethod.DELETE)
    public TeslaBaseResult delete(Long id) throws Exception {

        SreworksFile sreworksFile = sreworksFileRepository.findFirstById(id);
        minioService.removeObject(sreworksFile.getFileId());
        sreworksFileRepository.deleteById(id);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(String category) {

        List<SreworksFile> sreworksFileList = sreworksFileRepository.findAllByCategory(category);
        return buildSucceedResult(sreworksFileList.stream().map(sreworksFile -> {

            String fileObjectUrl = minioService.objectUrl(sreworksFile.getFileId());
            JSONObject sreworksFileJsonObject = sreworksFile.toJsonObject();
            sreworksFileJsonObject.put("fileObjectUrl", fileObjectUrl);
            return sreworksFileJsonObject;

        }).collect(Collectors.toList()));

    }

    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(String category, String name) {

        JSONObject sreworksFileJsonObject;
        SreworksFile sreworksFile = sreworksFileRepository.findFirstByCategoryAndName(category, name);
        if(sreworksFile != null) {
            sreworksFileJsonObject = sreworksFile.toJsonObject();
            String fileObjectUrl = minioService.objectUrl(sreworksFile.getFileId());
            sreworksFileJsonObject.put("fileObjectUrl", fileObjectUrl);
        }else{
            sreworksFileJsonObject = new JSONObject();
        }
        return buildSucceedResult(sreworksFileJsonObject);
    }

    @RequestMapping(value = "listWithType", method = RequestMethod.GET)
    public TeslaBaseResult listWithType(String category, String type) {

        List<SreworksFile> sreworksFileList = sreworksFileRepository.findAllByCategoryAndType(category, type);
        return buildSucceedResult(sreworksFileList.stream().map(sreworksFile -> {

            String fileObjectUrl = minioService.objectUrl(sreworksFile.getFileId());
            JSONObject sreworksFileJsonObject = sreworksFile.toJsonObject();
            sreworksFileJsonObject.put("fileObjectUrl", fileObjectUrl);
            return sreworksFileJsonObject;

        }).collect(Collectors.toList()));

    }

}
