package com.aliyun.oss.common.parser;

/**
 * Created by zhoufeng.chen on 2018/1/10.
 */

import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.comm.io.FixedLengthInputStream;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static com.aliyun.oss.common.parser.RequestMarshallers.*;
import static com.aliyun.oss.common.parser.RequestMarshallers.putBucketAccessMonitorRequestMarshaller;

public class RequestMarshallersTest {
    @Test
    public void testAddBucketReplicationRequestMarshallerWithCloudLocation() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);

        //without Cloud & CloudLocation
        //isEnableHistoricalObjectReplication = false
        //has getObjectPrefixList
        //has getReplicationActionList
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(null);
        addBucketReplicationRequest.setTargetCloudLocation(null);
        addBucketReplicationRequest.setEnableHistoricalObjectReplication(false);
        List<String> prefixList = new ArrayList<String>();
        prefixList.add("prefix");
        addBucketReplicationRequest.setObjectPrefixList(prefixList);
        List<AddBucketReplicationRequest.ReplicationAction> replicationActionList = new ArrayList<AddBucketReplicationRequest.ReplicationAction>();
        replicationActionList.add(AddBucketReplicationRequest.ReplicationAction.ALL);
        addBucketReplicationRequest.setReplicationActionList(replicationActionList);

        is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        ruleElems = root.getChild("Rule");
        destination = ruleElems.getChild("Destination");
        aTargetBucketName = destination.getChildText("Bucket");
        aTargetLocation = destination.getChildText("Location");
        aTargetCloud = destination.getChildText("Cloud");
        aTargetCloudLocation = destination.getChildText("CloudLocation");
        String aHistoricalObjectReplication = ruleElems.getChildText("HistoricalObjectReplication");
        Element aPrefixSet = ruleElems.getChild("PrefixSet");
        Element aAction = ruleElems.getChild("Action");
        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertNull(aTargetCloud);
        Assert.assertNull(aTargetCloudLocation);
        Assert.assertEquals("disabled", aHistoricalObjectReplication);
        Assert.assertNotNull(aPrefixSet);
        Assert.assertNotNull(aAction);
    }

    @Test
    public void testAddBucketReplicationRequestMarshallerWithoutCloudLocation() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetBucketLocation = "alicloud-targetBucketLocation";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetBucketLocation(targetBucketLocation);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertEquals(targetBucketLocation, aTargetLocation);
        Assert.assertNull(aTargetCloud);
        Assert.assertNull(aTargetCloudLocation);
    }

    @Test
    public void testAddBucketReplicationRequestMarshallerWithSyncRole() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        String syncRole = "syncRole";
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);
        addBucketReplicationRequest.setSyncRole(syncRole);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");
        String aSyncRole = ruleElems.getChildText("SyncRole");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertEquals(syncRole, aSyncRole);
    }

    @Test
    public void testAddBucketReplicationRequestMarshallerWithoutSyncRole() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");
        String aSyncRole = ruleElems.getChildText("SyncRole");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertNull(aSyncRole);
    }

    @Test
    public void testAddBucketReplicationRequestMarshallerWithReplicaKmsKeyID() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        String replicaKmsKeyID = "replicaKmsKeyID";
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);
        addBucketReplicationRequest.setReplicaKmsKeyID(replicaKmsKeyID);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");
        String aReplicaKmsKeyID = ruleElems.getChild("EncryptionConfiguration").getChildText("ReplicaKmsKeyID");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertEquals(replicaKmsKeyID, aReplicaKmsKeyID);
    }

    @Test
    public void testAddBucketReplicationRequestMarshallerWithoutReplicaKmsKeyID() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");
        Element encryptionConfiguration = ruleElems.getChild("EncryptionConfiguration");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertNull(encryptionConfiguration);
    }

    @Test
    public void testAddBucketReplicationRequestMarshallerWithSSEStatus() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        String SSEStatus = AddBucketReplicationRequest.ENABLED;
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);
        addBucketReplicationRequest.setSseKmsEncryptedObjectsStatus(SSEStatus);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");
        String aSSEStatus = ruleElems.getChild("SourceSelectionCriteria").
                getChild("SseKmsEncryptedObjects").getChildText("Status");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertEquals(SSEStatus, aSSEStatus);

        //set disable
        SSEStatus = AddBucketReplicationRequest.DISABLED;
        addBucketReplicationRequest.setSseKmsEncryptedObjectsStatus(SSEStatus);
        is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        ruleElems = root.getChild("Rule");
        destination = ruleElems.getChild("Destination");
        aTargetBucketName = destination.getChildText("Bucket");
        aTargetLocation = destination.getChildText("Location");
        aTargetCloud = destination.getChildText("Cloud");
        aTargetCloudLocation = destination.getChildText("CloudLocation");
        aSSEStatus = ruleElems.getChild("SourceSelectionCriteria").
                getChild("SseKmsEncryptedObjects").getChildText("Status");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertEquals(SSEStatus, aSSEStatus);

        //set other value
        addBucketReplicationRequest.setSseKmsEncryptedObjectsStatus("invalid");
        is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        ruleElems = root.getChild("Rule");
        destination = ruleElems.getChild("Destination");
        aTargetBucketName = destination.getChildText("Bucket");
        aTargetLocation = destination.getChildText("Location");
        aTargetCloud = destination.getChildText("Cloud");
        aTargetCloudLocation = destination.getChildText("CloudLocation");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertNull(ruleElems.getChild("SourceSelectionCriteria"));

    }

    @Test
    public void testAddBucketReplicationRequestMarshallerWithoutSSEStatus() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");
        Element sourceSelectionCriteria = ruleElems.getChild("SourceSelectionCriteria");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertNull(sourceSelectionCriteria);
    }

    @Test
    public void testAddBucketReplicationRequestMarshallerWithSourceLocation() {
        String bucketName = "alicloud-bucket";
        String targetBucketName = "alicloud-targetBucketName";
        String targetCloud = "testTargetCloud";
        String targetCloudLocation = "testTargetCloudLocation";
        String syncRole = "syncRole";
        String sourceLocation = "sourceLocation";
        AddBucketReplicationRequest addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);
        addBucketReplicationRequest.setSyncRole(syncRole);
        addBucketReplicationRequest.setSourceBucketLocation(sourceLocation);

        FixedLengthInputStream is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element ruleElems = root.getChild("Rule");
        Element destination = ruleElems.getChild("Destination");
        String aTargetBucketName = destination.getChildText("Bucket");
        String aTargetLocation = destination.getChildText("Location");
        String aTargetCloud = destination.getChildText("Cloud");
        String aTargetCloudLocation = destination.getChildText("CloudLocation");
        String aSyncRole = ruleElems.getChildText("SyncRole");
        String aSourceLocation = ruleElems.getChild("Source").getChildText("Location");

        Assert.assertEquals(targetBucketName, aTargetBucketName);
        Assert.assertNull(aTargetLocation);
        Assert.assertEquals(targetCloud, aTargetCloud);
        Assert.assertEquals(targetCloudLocation, aTargetCloudLocation);
        Assert.assertEquals(syncRole, aSyncRole);
        Assert.assertEquals(sourceLocation, aSourceLocation);


        addBucketReplicationRequest = new AddBucketReplicationRequest(bucketName);
        addBucketReplicationRequest.setTargetBucketName(targetBucketName);
        addBucketReplicationRequest.setTargetCloud(targetCloud);
        addBucketReplicationRequest.setTargetCloudLocation(targetCloudLocation);
        addBucketReplicationRequest.setSyncRole(syncRole);

        is = addBucketReplicationRequestMarshaller.marshall(addBucketReplicationRequest);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        ruleElems = root.getChild("Rule");
        Element sourceElems = ruleElems.getChild("Source");
        Assert.assertNull(sourceElems);
    }

    @Test
    public void testPutImageStyleRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        PutImageStyleRequest request = new PutImageStyleRequest();
        request.SetStyle("style");

        FixedLengthInputStream is = putImageStyleRequestMarshaller.marshall(request);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        String Content = root.getChildText("Content");
        Assert.assertEquals("style", Content);
    }

    @Test
    public void testBucketImageProcessConfMarshaller() {
        String bucketName = "alicloud-bucket";
        ImageProcess request = new ImageProcess("compliedHost", true, "sourceFileProtectSuffix",
                "styleDelimiters");
        request.setSourceFileProtect(null);
        request.setSupportAtStyle(null);
        FixedLengthInputStream is = bucketImageProcessConfMarshaller.marshall(request);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        String SourceFileProtect = root.getChildText("SourceFileProtect");
        String OssDomainSupportAtProcess = root.getChildText("OssDomainSupportAtProcess");
        Assert.assertEquals("Disabled", SourceFileProtect);
        Assert.assertEquals("Disabled", OssDomainSupportAtProcess);
    }

    @Test
    public void testPutBucketImageRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        PutBucketImageRequest request = new PutBucketImageRequest(bucketName);
        request.SetIsForbidOrigPicAccess(true);
        request.SetIsUseStyleOnly(true);
        request.SetIsAutoSetContentType(true);
        request.SetIsUseSrcFormat(true);
        request.SetIsSetAttachName(true);
        request.SetDefault404Pic("Default404Pic");
        request.SetStyleDelimiters("StyleDelimiters");

        FixedLengthInputStream is = putBucketImageRequestMarshaller.marshall(request);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        String OrigPicForbidden = root.getChildText("OrigPicForbidden");
        String UseStyleOnly = root.getChildText("UseStyleOnly");
        String AutoSetContentType = root.getChildText("AutoSetContentType");
        String UseSrcFormat = root.getChildText("UseSrcFormat");
        String SetAttachName = root.getChildText("SetAttachName");
        String Default404Pic = root.getChildText("Default404Pic");
        String StyleDelimiters = root.getChildText("StyleDelimiters");

        Assert.assertEquals("true", OrigPicForbidden);
        Assert.assertEquals("true", UseStyleOnly);
        Assert.assertEquals("true", AutoSetContentType);
        Assert.assertEquals("true", UseSrcFormat);
        Assert.assertEquals("true", SetAttachName);
        Assert.assertEquals("Default404Pic", Default404Pic);
        Assert.assertEquals("StyleDelimiters", StyleDelimiters);

        //
        request.SetIsForbidOrigPicAccess(false);
        request.SetIsUseStyleOnly(false);
        request.SetIsAutoSetContentType(false);
        request.SetIsUseSrcFormat(false);
        request.SetIsSetAttachName(false);
        request.SetDefault404Pic("Default404Pic");
        request.SetStyleDelimiters("StyleDelimiters");

        is = putBucketImageRequestMarshaller.marshall(request);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        OrigPicForbidden = root.getChildText("OrigPicForbidden");
        UseStyleOnly = root.getChildText("UseStyleOnly");
        AutoSetContentType = root.getChildText("AutoSetContentType");
        UseSrcFormat = root.getChildText("UseSrcFormat");
        SetAttachName = root.getChildText("SetAttachName");
        Default404Pic = root.getChildText("Default404Pic");
        StyleDelimiters = root.getChildText("StyleDelimiters");

        Assert.assertEquals("false", OrigPicForbidden);
        Assert.assertEquals("false", UseStyleOnly);
        Assert.assertEquals("false", AutoSetContentType);
        Assert.assertEquals("false", UseSrcFormat);
        Assert.assertEquals("false", SetAttachName);
        Assert.assertEquals("Default404Pic", Default404Pic);
        Assert.assertEquals("StyleDelimiters", StyleDelimiters);
    }

    @Test
    public void testSetBucketWebsiteRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        SetBucketWebsiteRequest request = new SetBucketWebsiteRequest(bucketName);
        request.setIndexDocument(null);
        request.setErrorDocument(null);

        RoutingRule rule = new RoutingRule();
        rule.setNumber(1);
        RoutingRule.Condition condition = new RoutingRule.Condition();
        condition.setHttpErrorCodeReturnedEquals(403);
        rule.setCondition(condition);

        rule.getRedirect().setRedirectType(RoutingRule.RedirectType.AliCDN);
        rule.getRedirect().setHostName("oss.aliyuncs.com");
        rule.getRedirect().setProtocol(RoutingRule.Protocol.Https);
        rule.getRedirect().setReplaceKeyWith("${key}.jpg");
        rule.getRedirect().setHttpRedirectCode(302);

        request.AddRoutingRule(rule);

        FixedLengthInputStream is = setBucketWebsiteRequestMarshaller.marshall(request);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element IndexDocument = root.getChild("IndexDocument");
        Element RoutingRules = root.getChild("RoutingRules");
        Element RoutingRule = RoutingRules.getChild("RoutingRule");
        Element Condition = RoutingRule.getChild("Condition");
        Element KeyPrefixEquals = Condition.getChild("KeyPrefixEquals");

        Assert.assertNull(IndexDocument);
        Assert.assertNotNull(RoutingRule);
        Assert.assertNotNull(Condition);
        Assert.assertNull(KeyPrefixEquals);
    }

    @Test
    public void testDeleteBucketReplicationRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        DeleteBucketReplicationRequest request = new DeleteBucketReplicationRequest(bucketName);
        request.setReplicationRuleID("ID");

        byte[] data = deleteBucketReplicationRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        String ID = root.getChildText("ID");
        Assert.assertEquals("ID", ID);
    }

    @Test
    public void testAddBucketCnameRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        AddBucketCnameRequest request = new AddBucketCnameRequest(bucketName);
        request.setDomain("domain");

        byte[] data = addBucketCnameRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element Cname = root.getChild("Cname");
        String Domain = Cname.getChildText("Domain");
        Assert.assertEquals("domain", Domain);
    }

    @Test
    public void testAddBucketCnameWithCertificateRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        AddBucketCnameRequest request = new AddBucketCnameRequest(bucketName);
        request.setDomain("domain");
        request.setCertificateConfiguration(
            new CertificateConfiguration()
                .withPublicKey("pubkey")
                .withPrivateKey("prikey")
                .withPreviousId("previd")
                .withId("id")
                .withForceOverwriteCert(true));

        byte[] data = addBucketCnameRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element cname = root.getChild("Cname");
        String domain = cname.getChildText("Domain");
        Assert.assertEquals("domain", domain);
        Element certificate = cname.getChild("CertificateConfiguration");
        Assert.assertEquals("pubkey", certificate.getChildText("Certificate"));
        Assert.assertEquals("prikey", certificate.getChildText("PrivateKey"));
        Assert.assertEquals("previd", certificate.getChildText("PreviousCertId"));
        Assert.assertEquals("id", certificate.getChildText("CertId"));
        Assert.assertEquals("true", certificate.getChildText("Force"));
        Assert.assertEquals(null, certificate.getChildText("DeleteCertificate"));

        request = new AddBucketCnameRequest(bucketName);
        request.setDomain("domain1");
        request.setCertificateConfiguration(
                new CertificateConfiguration()
                        .withPublicKey("pubkey")
                        .withPrivateKey("prikey")
                        .withPreviousId("previd")
                        .withId("id"));

        data = addBucketCnameRequestMarshaller.marshall(request);
        is = new ByteArrayInputStream(data);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        cname = root.getChild("Cname");
        domain = cname.getChildText("Domain");
        Assert.assertEquals("domain1", domain);
        certificate = cname.getChild("CertificateConfiguration");
        Assert.assertEquals("pubkey", certificate.getChildText("Certificate"));
        Assert.assertEquals("prikey", certificate.getChildText("PrivateKey"));
        Assert.assertEquals("previd", certificate.getChildText("PreviousCertId"));
        Assert.assertEquals("id", certificate.getChildText("CertId"));
        Assert.assertEquals(null, certificate.getChildText("Force"));

        request = new AddBucketCnameRequest(bucketName);
        request.setDomain("domain1");
        request.setCertificateConfiguration(
                new CertificateConfiguration()
                        .withDeleteCertificate(true));

        data = addBucketCnameRequestMarshaller.marshall(request);
        is = new ByteArrayInputStream(data);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        cname = root.getChild("Cname");
        domain = cname.getChildText("Domain");
        Assert.assertEquals("domain1", domain);
        certificate = cname.getChild("CertificateConfiguration");
        Assert.assertEquals(null, certificate.getChildText("Certificate"));
        Assert.assertEquals(null, certificate.getChildText("PrivateKey"));
        Assert.assertEquals(null, certificate.getChildText("PreviousCertId"));
        Assert.assertEquals(null, certificate.getChildText("CertId"));
        Assert.assertEquals(null, certificate.getChildText("Force"));
        Assert.assertEquals("true", certificate.getChildText("DeleteCertificate"));

        request = new AddBucketCnameRequest(bucketName);
        request.setDomain("domain1");
        request.setCertificateConfiguration(
                new CertificateConfiguration()
                        .withDeleteCertificate(false));

        data = addBucketCnameRequestMarshaller.marshall(request);
        is = new ByteArrayInputStream(data);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        cname = root.getChild("Cname");
        domain = cname.getChildText("Domain");
        Assert.assertEquals("domain1", domain);
        certificate = cname.getChild("CertificateConfiguration");
        Assert.assertEquals(null, certificate.getChildText("Certificate"));
        Assert.assertEquals(null, certificate.getChildText("PrivateKey"));
        Assert.assertEquals(null, certificate.getChildText("PreviousCertId"));
        Assert.assertEquals("false", certificate.getChildText("DeleteCertificate"));
    }

    @Test
    public void testDeleteBucketCnameRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        DeleteBucketCnameRequest request = new DeleteBucketCnameRequest(bucketName);
        request.setDomain("domain");

        byte[] data = deleteBucketCnameRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element Cname = root.getChild("Cname");
        String Domain = Cname.getChildText("Domain");
        Assert.assertEquals("domain", Domain);
    }

    @Test
    public void testCreateUdfRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        CreateUdfRequest request = new CreateUdfRequest(bucketName);
        request.setName("name");
        request.setId("id");
        request.setDesc("desc");

        byte[] data = createUdfRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        String Name = root.getChildText("Name");
        String ID = root.getChildText("ID");
        String Description = root.getChildText("Description");
        Assert.assertEquals("name", Name);
        Assert.assertEquals("id", ID);
        Assert.assertEquals("desc", Description);


        request.setId(null);
        request.setDesc(null);
        data = createUdfRequestMarshaller.marshall(request);
        is = new ByteArrayInputStream(data);

        builder = new SAXBuilder();
        doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        root = doc.getRootElement();
        Name = root.getChildText("Name");
        Element eID = root.getChild("ID");
        Element eDescription = root.getChild("Description");
        Assert.assertEquals("name", Name);
        Assert.assertEquals(null, eID);
        Assert.assertEquals(null, eDescription);
    }

    @Test
    public void testCreateUdfApplicationRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        UdfApplicationConfiguration udfApplicationConfiguration = new UdfApplicationConfiguration(1, 2);
        CreateUdfApplicationRequest request = new CreateUdfApplicationRequest(bucketName, udfApplicationConfiguration);

        byte[] data = createUdfApplicationRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        String ImageVersion = root.getChildText("ImageVersion");
        String InstanceNum = root.getChildText("InstanceNum");
        Assert.assertEquals("1", ImageVersion);
        Assert.assertEquals("2", InstanceNum);
    }

    @Test
    public void testUpgradeUdfApplicationRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        UpgradeUdfApplicationRequest request = new UpgradeUdfApplicationRequest("name", 1);

        byte[] data = upgradeUdfApplicationRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        String ImageVersion = root.getChildText("ImageVersion");
        Assert.assertEquals("1", ImageVersion);
    }

    @Test
    public void testResizeUdfApplicationRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        ResizeUdfApplicationRequest request = new ResizeUdfApplicationRequest("name", 1);

        byte[] data = resizeUdfApplicationRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        String InstanceNum = root.getChildText("InstanceNum");
        Assert.assertEquals("1", InstanceNum);
    }

    @Test
    public void testStringMarshaller() {
        try {
            FixedLengthInputStream is = stringMarshaller.marshall(null);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testSetBucketTaggingRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        SetTaggingRequest request = new SetTaggingRequest(bucketName, "key");
        FixedLengthInputStream is = setBucketTaggingRequestMarshaller.marshall(request);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element TagSet = root.getChild("TagSet");
        Element Tag = TagSet.getChild("Tag");
        Assert.assertNotNull(TagSet);
        Assert.assertNull(Tag);
    }

    @Test
    public void testDeleteVersionsRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        DeleteVersionsRequest request = new DeleteVersionsRequest(bucketName);

        List<DeleteVersionsRequest.KeyVersion> keys = new ArrayList<DeleteVersionsRequest.KeyVersion>();
        keys.add(new DeleteVersionsRequest.KeyVersion("key1", "version"));
        request.setKeys(keys);
        byte[] data = deleteVersionsRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element Object = root.getChild("Object");
        Element VersionId = Object.getChild("VersionId");
        Assert.assertNotNull(Object);
        Assert.assertNotNull(VersionId);
    }

    @Test
    public void testSelectObjectRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        SelectObjectRequest request = new SelectObjectRequest(bucketName, "key");
        request.setExpression("select * from table;");
        request.setMaxSkippedRecordsAllowed(10);

        byte[] data = selectObjectRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element Options = root.getChild("Options");
        Element allowed = Options.getChild("MaxSkippedRecordsAllowed");
        Assert.assertNotNull(allowed);
    }

    @Test
    public void testSetBucketCORSRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        SetBucketCORSRequest request = new SetBucketCORSRequest(bucketName);
        SetBucketCORSRequest.CORSRule corsRule = new SetBucketCORSRequest.CORSRule();
        corsRule.addAllowdOrigin("*");
        corsRule.addAllowedMethod("PUT");
        corsRule.addAllowedHeader("header1");
        corsRule.addAllowedHeader("header2");
        request.addCorsRule(corsRule);

        FixedLengthInputStream is = setBucketCORSRequestMarshaller.marshall(request);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Element root = doc.getRootElement();
        Element CORSRule = root.getChild("CORSRule");
        Element AllowedHeader = CORSRule.getChild("AllowedHeader");
        Assert.assertNotNull(AllowedHeader);
    }

    @Test
    public void testCreateVpcipRequestMarshaller() {
        CreateVpcipRequest createVpcipRequest = new CreateVpcipRequest();
        createVpcipRequest.setRegion("test-region");
        createVpcipRequest.setVSwitchId("test-vpcip-name");
        createVpcipRequest.setLabel("test-vpcip-label");
        createVpcipRequest.toString();

        FixedLengthInputStream is = createVpcipRequestMarshaller.marshall(createVpcipRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        String region = root.getChildText("Region");
        String switchid = root.getChildText("VSwitchId");
        String label = root.getChildText("Label");
        Assert.assertEquals("test-region", region);
        Assert.assertEquals("test-vpcip-name", switchid);
        Assert.assertEquals("test-vpcip-label", label);
    }

    @Test
    public void testCreateBucketVpcipRequestMarshaller() {
        String bucketName = "alicloud-bucket";
        CreateBucketVpcipRequest createBucketVpcipRequest = new CreateBucketVpcipRequest();
        createBucketVpcipRequest.setBucketName(bucketName);
        VpcPolicy vpcPolicy = new VpcPolicy();
        vpcPolicy.setRegion("test-region");
        vpcPolicy.setVpcId("test-vpc-id");
        vpcPolicy.setVip("test-vip");
        createBucketVpcipRequest.setVpcPolicy(vpcPolicy);
        createBucketVpcipRequest.toString();

        FixedLengthInputStream is = createBucketVpcipRequestMarshaller.marshall(createBucketVpcipRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        String region = root.getChildText("Region");
        String vpcid = root.getChildText("VpcId");
        String vip = root.getChildText("Vip");
        Assert.assertEquals("test-region", region);
        Assert.assertEquals("test-vpc-id", vpcid);
        Assert.assertEquals("test-vip", vip);
    }


    @Test
    public void testDeleteBucketVpcipRequestMarshaller() {
        String bucketName = "alicloud-bucket";

        DeleteBucketVpcipRequest deleteBucketVpcipRequest = new DeleteBucketVpcipRequest();
        deleteBucketVpcipRequest.setBucketName(bucketName);
        VpcPolicy vpcPolicy = new VpcPolicy();
        vpcPolicy.setRegion("test-region");
        vpcPolicy.setVpcId("test-vpc-id");
        vpcPolicy.setVip("test-vip");
        deleteBucketVpcipRequest.setVpcPolicy(vpcPolicy);
        deleteBucketVpcipRequest.toString();

        FixedLengthInputStream is = deleteBucketVpcipRequestMarshaller.marshall(deleteBucketVpcipRequest.getVpcPolicy());

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        String region = root.getChildText("Region");
        String vpcid = root.getChildText("VpcId");
        String vip = root.getChildText("Vip");
        Assert.assertEquals("test-region", region);
        Assert.assertEquals("test-vpc-id", vpcid);
        Assert.assertEquals("test-vip", vip);
    }

    @Test
    public void testDeleteVpcipRequestMarshaller() {

        DeleteVpcipRequest deleteVpcipRequest = new DeleteVpcipRequest();
        VpcPolicy vpcPolicy = new VpcPolicy();
        vpcPolicy.setRegion("test-region");
        vpcPolicy.setVpcId("test-vpc-id");
        vpcPolicy.setVip("test-vip");
        deleteVpcipRequest.setVpcPolicy(vpcPolicy);
        deleteVpcipRequest.toString();

        FixedLengthInputStream is = deleteVpcipRequestMarshaller.marshall(deleteVpcipRequest);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        String region = root.getChildText("Region");
        String vpcid = root.getChildText("VpcId");
        String vip = root.getChildText("Vip");
        Assert.assertEquals("test-region", region);
        Assert.assertEquals("test-vpc-id", vpcid);
        Assert.assertEquals("test-vip", vip);
    }

    @Test
    public void testSetBucketInventoryRequestMarshaller() {

        InventoryConfiguration  config = new InventoryConfiguration();

        byte[] data = setBucketInventoryRequestMarshaller.marshall(config);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        Assert.assertEquals(root.getChild("Id"), null);
        Assert.assertEquals(root.getChild("IsEnabled"), null);
        Assert.assertEquals(root.getChild("IncludedObjectVersions"), null);
        Assert.assertEquals(root.getChild("Filter"), null);
        Assert.assertEquals(root.getChild("Schedule"), null);
        Assert.assertEquals(root.getChild("OptionalFields"), null);
        Assert.assertEquals(root.getChild("Destination"), null);

        config = new InventoryConfiguration();
        config.setOptionalFields(new ArrayList<String>());
        config.setDestination(new InventoryDestination().withOSSBucketDestination(new InventoryOSSBucketDestination()));

        data = setBucketInventoryRequestMarshaller.marshall(config);
        is = new ByteArrayInputStream(data);

        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        root = doc.getRootElement();
        Assert.assertEquals(root.getChild("Id"), null);
        Assert.assertEquals(root.getChild("IsEnabled"), null);
        Assert.assertEquals(root.getChild("IncludedObjectVersions"), null);
        Assert.assertEquals(root.getChild("Filter"), null);
        Assert.assertEquals(root.getChild("Schedule"), null);
        Assert.assertEquals(root.getChild("OptionalFields"), null);
        Assert.assertNotNull(root.getChild("Destination"));

    }

    @Test
    public void testSetBucketResourceGroupRequestMarshaller() {

        String id = "xxx-id-123";

        byte[] data = setBucketResourceGroupRequestMarshaller.marshall(id);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        Assert.assertNotNull(root.getChild("ResourceGroupId"));
        Assert.assertEquals(root.getChildText("ResourceGroupId"), "xxx-id-123");
    }

    @Test
    public void testPutBucketTransferAccelerationRequestMarshaller() {

        SetBucketTransferAccelerationRequest request = new SetBucketTransferAccelerationRequest("bucket", true);
        Assert.assertEquals(request.isEnabled(), true);
        request.setEnabled(false);
        Assert.assertEquals(request.isEnabled(), false);

        request.setEnabled(true);
        Assert.assertEquals(request.isEnabled(), true);

        byte[] data = putBucketTransferAccelerationRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        String status = root.getChildText("Enabled");
        Assert.assertEquals("true", status);
    }

    @Test
    public void testPutBucketAccessMonitorRequestMarshaller() {

        PutBucketAccessMonitorRequest request = new PutBucketAccessMonitorRequest("bucket", "Enabled");
        Assert.assertEquals(request.getStatus(), "Enabled");
        request.setStatus("Disabled");
        Assert.assertEquals(request.getStatus(), "Disabled");

        request.setStatus("Enabled");
        Assert.assertEquals(request.getStatus(), "Enabled");

        byte[] data = putBucketAccessMonitorRequestMarshaller.marshall(request);
        ByteArrayInputStream is = new ByteArrayInputStream(data);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        String status = root.getChildText("Status");
        Assert.assertEquals("Enabled", status);
    }

    @Test
    public void testPutLifeCycleRequestMarshaller() {
        SetBucketLifecycleRequest request = new SetBucketLifecycleRequest("bucket");
        String ruleId0 = "rule0";
        String matchPrefix0 = "A0/";
        Map<String, String> matchTags0 = new HashMap<String, String>();
        matchTags0.put("key0", "value0");
        LifecycleRule rule = new LifecycleRule(ruleId0, matchPrefix0, LifecycleRule.RuleStatus.Enabled, 3);
        rule.setTags(matchTags0);
        LifecycleFilter filter = new LifecycleFilter();
        LifecycleNot not = new LifecycleNot();
        List<LifecycleNot> notList = new ArrayList<LifecycleNot>();
        Tag tag = new Tag("key","value");
        not.setPrefix("not-prefix");
        not.setTag(tag);
        notList.add(not);
        LifecycleNot not2 = new LifecycleNot();
        Tag tag2 = new Tag("key2","value2");
        not2.setPrefix("not2-prefix");
        not2.setTag(tag2);
        notList.add(not2);
        filter.setNotList(notList);
        rule.setFilter(filter);
        request.AddLifecycleRule(rule);

        FixedLengthInputStream is = setBucketLifecycleRequestMarshaller.marshall(request);

        SAXBuilder builder = new SAXBuilder();
        Document doc = null;
        try {
            doc = builder.build(is);
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getRootElement();
        Assert.assertEquals("A0/", root.getChild("Rule").getChildText("Prefix"));
        Assert.assertEquals("A0/not-prefix", root.getChild("Rule").getChild("Filter").getChildren("Not").get(0).getChildText("Prefix"));
        Assert.assertEquals("key", root.getChild("Rule").getChild("Filter").getChildren("Not").get(0).getChild("Tag").getChildText("Key"));
        Assert.assertEquals("value", root.getChild("Rule").getChild("Filter").getChildren("Not").get(0).getChild("Tag").getChildText("Value"));
        Assert.assertEquals("A0/not2-prefix", root.getChild("Rule").getChild("Filter").getChildren("Not").get(1).getChildText("Prefix"));
        Assert.assertEquals("key2", root.getChild("Rule").getChild("Filter").getChildren("Not").get(1).getChild("Tag").getChildText("Key"));
        Assert.assertEquals("value2", root.getChild("Rule").getChild("Filter").getChildren("Not").get(1).getChild("Tag").getChildText("Value"));
    }
}
