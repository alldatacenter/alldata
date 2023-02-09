package com.obs.test.tools;

import com.obs.services.ObsClient;
import com.obs.services.model.*;

import com.obs.test.UserInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ObjectTools {
    private static final Logger logger = LogManager.getLogger(ObjectTools.class);
    /**
     * 列举桶内所有对象
     * @param obsClient
     * @param bucketName
     * @return
     */
    public static List<ObsObject> listAllObject(ObsClient obsClient, String bucketName) {
        ListObjectsRequest request = new ListObjectsRequest(bucketName);
        // 设置每页1000个对象
        request.setMaxKeys(1000);

        List<ObsObject> objects = new ArrayList<>();
        ObjectListing result;
        do {
            result = obsClient.listObjects(request);
            for (ObsObject obsObject : result.getObjects()) {
                objects.add(obsObject);
            }
            request.setMarker(result.getNextMarker());
        } while (result.isTruncated());

        return objects;
    }

    /**
     * 列举所有多版本对象
     * @param obsClient
     * @param bucketName
     * @return
     */
    public static List<VersionOrDeleteMarker> listAllVersions(ObsClient obsClient, String bucketName) {
        ListVersionsRequest request = new ListVersionsRequest (bucketName);
        // 设置每页1000个对象
        request.setMaxKeys(1000);

        List<VersionOrDeleteMarker> objects = new ArrayList<>();
        ListVersionsResult result;
        do {
            result = obsClient.listVersions(bucketName);
            for (VersionOrDeleteMarker versionOrDeleteMarker : result.getVersions()) {
                objects.add(versionOrDeleteMarker);
            }
            request.setKeyMarker(result.getNextKeyMarker());
            request.setVersionIdMarker(result.getNextVersionIdMarker());
        } while (result.isTruncated());

        return objects;
    }

    /**
     * 删除指定的对象
     * @param obsClient
     * @param bucketName
     * @param objectKey
     */
    public static void deleteObject(ObsClient obsClient, String bucketName, String objectKey) {
        logger.warn("delete object : " + bucketName + "   " + objectKey);
        obsClient.deleteObject(bucketName, objectKey);
    }

    /**
     * 删除指定版本的对象
     * @param obsClient
     * @param bucketName
     * @param objectKey
     * @param versionid
     */
    public static void deleteObject(ObsClient obsClient, String bucketName, String objectKey, String versionid) {
        logger.warn("delete object : " + bucketName + "   " + objectKey + "    " + versionid);
        obsClient.deleteObject(bucketName, objectKey, versionid);
    }

    /**
     * 删除桶内的所有对象
     * @param obsClient
     * @param bucketName
     */
    public static void deleteAllObjects(ObsClient obsClient, String bucketName) {
        List<ObsObject> obsObjects = ObjectTools.listAllObject(obsClient, bucketName);
        if(null == obsObjects) {
            return;
        }
        for(ObsObject obsObject : obsObjects) {
            ObjectTools.deleteObject(obsClient, bucketName, obsObject.getObjectKey());
        }
    }

    /**
     * 删除桶内的所有历史版本对象
     * @param obsClient
     * @param bucketName
     */
    public static void deleteAllVersions(ObsClient obsClient, String bucketName) {
        List<VersionOrDeleteMarker> versions = ObjectTools.listAllVersions(obsClient, bucketName);
        if(null == versions) {
            return;
        }
        for(VersionOrDeleteMarker version : versions) {
            ObjectTools.deleteObject(obsClient, bucketName, version.getObjectKey(), version.getVersionId());
        }
    }
    
    /**
     * 设置对象的ACL
     * @param obsClient
     * @param bucketName
     * @param objectKey
     */
    public static void setObjectAcl(UserInfo srcUser, UserInfo destUser, String bucketName, String objectKey) {
        AccessControlList accessControlList = srcUser.getObsClient().getObjectAcl(bucketName, objectKey);
        accessControlList.grantPermission(new CanonicalGrantee(destUser.getDomainId()), Permission.PERMISSION_FULL_CONTROL, true);
        
        srcUser.getObsClient().setObjectAcl(bucketName, objectKey, accessControlList);
    }
}
