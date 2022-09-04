package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.tesla.appmanager.api.provider.AppPackageProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.common.util.ZipUtil;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageDTO;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageImportReq;
import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageTagService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RequestMapping("/app-packages")
@RestController
public class GlobalAppPackageController extends AppManagerBaseController {

    @Autowired
    private AppPackageProvider appPackageProvider;

    @Autowired
    private AppPackageTagService appPackageTagService;

    @RequestMapping(value = "import", method = RequestMethod.POST)
    @ResponseBody
    public TeslaBaseResult importPackage(
            @RequestParam("file") MultipartFile file, OAuth2Authentication auth) throws IOException {

        File zipFile = Files.createTempFile("app_package", ".zip").toFile();
        file.transferTo(zipFile);

        Path workDirFile = Files.createTempDirectory("app_package");
        String workDirAbsPath = workDirFile.toFile().getAbsolutePath();
        ZipUtil.unzip(zipFile.getAbsolutePath(), workDirAbsPath);
        String metaYaml = FileUtils.readFileToString(Paths.get(workDirAbsPath, "/meta.yaml").toFile(), StandardCharsets.UTF_8);
        AppPackageSchema metaYamlObject = SchemaUtil.toSchema(AppPackageSchema.class, metaYaml);

        AppPackageImportReq request = AppPackageImportReq.builder().
                appId(metaYamlObject.getAppId()).
                packageVersion(metaYamlObject.getPackageVersion()).
                force(true).
                resetVersion(false).
                packageCreator(getOperator(auth)).build();

        InputStream fileStream = new FileInputStream(zipFile);
        AppPackageDTO appPackageDTO = appPackageProvider.importPackage(request, fileStream, getOperator(auth));
        FileUtils.deleteQuietly(zipFile);

        // 将应用包设置为上架状态
        AppPackageTagDO appPackageTagDO = AppPackageTagDO.builder()
                .appPackageId(appPackageDTO.getId())
                .appId(metaYamlObject.getAppId())
                .tag(DefaultConstant.ON_SALE)
                .build();
        appPackageTagService.insert(appPackageTagDO);

        // 将应用appmeta部分执行launch导入


        return buildSucceedResult(appPackageDTO);
    }

}
