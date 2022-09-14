package com.alibaba.tesla.appmanager.server.service.pack.dag;

import com.alibaba.tesla.dag.local.AbstractLocalDagBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 打包实体的 AppPackage 并放置到 Storage 存储中
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class PackAppPackageToStorageDag extends AbstractLocalDagBase {

    public static String name = "pack_app_package_to_storage";

    @Override
    public void draw() throws Exception {
        node("PackAppPackageToStorageNode");
    }
}
