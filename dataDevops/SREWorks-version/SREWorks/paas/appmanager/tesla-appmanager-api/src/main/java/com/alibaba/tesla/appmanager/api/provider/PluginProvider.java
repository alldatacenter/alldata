package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.domain.dto.PluginMetaDTO;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


/**
 * Plugin Provider
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface PluginProvider {


    PluginMetaDTO create(MultipartFile pluginUploadFile) throws IOException;
}
