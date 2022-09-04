package com.alibaba.tesla.appmanager.plugin.api;

import com.alibaba.tesla.appmanager.api.provider.PluginProvider;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.common.util.ZipUtil;
import com.alibaba.tesla.appmanager.domain.dto.PluginMetaDTO;
import com.alibaba.tesla.appmanager.domain.schema.PluginDefinitionSchema;
import com.alibaba.tesla.appmanager.plugin.repository.domain.PluginDefinitionDO;
import com.alibaba.tesla.appmanager.plugin.service.PluginDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Plugin Provider 实现
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class PluginProviderImpl implements PluginProvider {


    private final PluginDefinitionService pluginDefinitionService;

    public PluginProviderImpl(PluginDefinitionService pluginDefinitionService) {
        this.pluginDefinitionService = pluginDefinitionService;
    }

    @Override
    public PluginMetaDTO create(MultipartFile pluginUploadFile) throws IOException {

        File zipFile = Files.createTempFile("plugin", ".zip").toFile();
        pluginUploadFile.transferTo(zipFile);

        Path workDirFile = Files.createTempDirectory("plugin");
        String workDirAbsPath = workDirFile.toFile().getAbsolutePath();
        ZipUtil.unzip(zipFile.getAbsolutePath(), workDirAbsPath);
        zipFile.delete();

        String defYaml = FileUtils.readFileToString(Paths.get(workDirAbsPath, "/definition.yaml").toFile(), StandardCharsets.UTF_8);
        PluginDefinitionSchema defYamlObject = SchemaUtil.toSchema(PluginDefinitionSchema.class, defYaml);

        PluginDefinitionDO pluginDefinitionRecord = PluginDefinitionDO.builder()
                .pluginVersion(defYamlObject.getPluginVersion())
                .pluginName(defYamlObject.getPluginName())
                .pluginDescription(defYamlObject.getPluginDescription())
                .packagePath("")
                .build();

        pluginDefinitionService.create(pluginDefinitionRecord);

        PluginMetaDTO pluginMeta = PluginMetaDTO.builder()
                .pluginName(pluginDefinitionRecord.getPluginName())
                .pluginVersion(pluginDefinitionRecord.getPluginVersion())
                .pluginDescription(pluginDefinitionRecord.getPluginDescription())
                .build();

        return pluginMeta;
    }
}
