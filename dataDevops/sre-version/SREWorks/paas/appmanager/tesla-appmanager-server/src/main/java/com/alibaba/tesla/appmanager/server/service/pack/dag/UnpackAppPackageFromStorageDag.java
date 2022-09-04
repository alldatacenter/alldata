package com.alibaba.tesla.appmanager.server.service.pack.dag;


import com.alibaba.tesla.dag.local.AbstractLocalDagBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 将 AppPackage 从 Storage 存储中获取并解包为各个 Component Package，存储到 DB 中并再次上传 Component Package 到 Storage
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@Service
public class UnpackAppPackageFromStorageDag extends AbstractLocalDagBase {

    public static String name = "unpack_app_package_from_storage";

    @Override
    public void draw() throws Exception {
        node("UnpackAppPackageFromStorageNode");
    }
}
