package com.aliyun.oss.common.parser;

import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.internal.ResponseParsers;
import com.aliyun.oss.internal.model.OSSErrorResult;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zhoufeng.chen on 2018/1/10.
 */
public class ResponseParsersTest {
    @Test
    public void testParseGetBucketReplicationWithCloudLocation() {
        String respBody = "<ReplicationConfiguration>\n" +
                " <Rule>\n" +
                "    <ID>12345678</ID>\n" +
                "        <Destination>\n" +
                "            <Bucket>testBucketName</Bucket>\n" +
                "            <Cloud>testCloud</Cloud>\n" +
                "            <CloudLocation>testCloudLocation</CloudLocation>\n" +
                "        </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                " </Rule>\n" +
                "</ReplicationConfiguration>\n";
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<ReplicationRule> rules = null;
        try {
            rules = ResponseParsers.parseGetBucketReplication(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }
        Assert.assertTrue(rules.size() > 0);

        ReplicationRule rule = rules.get(0);
        Assert.assertEquals("12345678", rule.getReplicationRuleID());
        Assert.assertEquals("testBucketName", rule.getTargetBucketName());
        Assert.assertNull(rule.getTargetBucketLocation());
        Assert.assertEquals("testCloud", rule.getTargetCloud());
        Assert.assertEquals("testCloudLocation", rule.getTargetCloudLocation());
        Assert.assertEquals(true, rule.isEnableHistoricalObjectReplication());
    }

    @Test
    public void testParseGetBucketReplicationWithoutCloudLocation() {
        String respBody = "<ReplicationConfiguration>\n" +
                " <Rule>\n" +
                "    <ID>12345678</ID>\n" +
                "        <Destination>\n" +
                "            <Bucket>testBucketName</Bucket>\n" +
                "            <Location>testLocation</Location>\n" +
                "        </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>disabled</HistoricalObjectReplication>\n" +
                " </Rule>\n" +
                "</ReplicationConfiguration>\n";
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<ReplicationRule> rules = null;
        try {
            rules = ResponseParsers.parseGetBucketReplication(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }
        Assert.assertTrue(rules.size() > 0);

        ReplicationRule rule = rules.get(0);
        Assert.assertEquals("12345678", rule.getReplicationRuleID());
        Assert.assertEquals("testBucketName", rule.getTargetBucketName());
        Assert.assertEquals("testLocation", rule.getTargetBucketLocation());
        Assert.assertNull(rule.getTargetCloud());
        Assert.assertNull(rule.getTargetCloudLocation());
        Assert.assertEquals(false, rule.isEnableHistoricalObjectReplication());
        Assert.assertNull(rule.getSourceBucketLocation());

    }

    @Test
    public void testParseGetBucketReplicationWithSourceLocation() {
        String respBody = "<ReplicationConfiguration>\n" +
                " <Rule>\n" +
                "    <ID>12345678</ID>\n" +
                "    <Destination>\n" +
                "        <Bucket>testBucketName</Bucket>\n" +
                "        <Cloud>testCloud</Cloud>\n" +
                "        <CloudLocation>testCloudLocation</CloudLocation>\n" +
                "    </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "    <Source>\n" +
                "        <Location>sourceLocation</Location>\n" +
                "    </Source>\n" +
                " </Rule>\n" +
                "</ReplicationConfiguration>\n";
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<ReplicationRule> rules = null;
        try {
            rules = ResponseParsers.parseGetBucketReplication(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }
        Assert.assertTrue(rules.size() > 0);

        ReplicationRule rule = rules.get(0);
        Assert.assertEquals("12345678", rule.getReplicationRuleID());
        Assert.assertEquals("testBucketName", rule.getTargetBucketName());
        Assert.assertNull(rule.getTargetBucketLocation());
        Assert.assertEquals("testCloud", rule.getTargetCloud());
        Assert.assertEquals("testCloudLocation", rule.getTargetCloudLocation());
        Assert.assertEquals(true, rule.isEnableHistoricalObjectReplication());
        Assert.assertEquals("sourceLocation", rule.getSourceBucketLocation());
    }

    @Test
    public void testParseGetBucketReplicationProgressWithCloudLocation() {
        Date dt = new Date();
        String respBody = "<ReplicationProgress>\n" +
                " <Rule>\n" +
                "     <ID>12345678</ID>\n" +
                "     <Destination>\n" +
                "         <Bucket>testBucketName</Bucket>\n" +
                "         <Cloud>testCloud</Cloud>\n" +
                "         <CloudLocation>testCloudLocation</CloudLocation>\n" +
                "     </Destination>\n" +
                "     <PrefixSet>\n" +
                "         <Prefix>aaa</Prefix>\n" +
                "         <Prefix>bbb</Prefix>\n" +
                "     </PrefixSet>\n" +
                "     <Action>xxx,xxx,xxx</Action>\n" +
                "     <Status>doing</Status>\n" +
                "     <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "     <Progress>\n" +
                "         <HistoricalObject>0.8</HistoricalObject>\n" +
                "         <NewObject>" + DateUtil.formatIso8601Date(dt) + "</NewObject>\n" +
                "     </Progress>\n" +
                " </Rule>\n" +
                "</ReplicationProgress>";


        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        BucketReplicationProgress progress = null;
        try {
            progress = ResponseParsers.parseGetBucketReplicationProgress(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication process response body fail!");
        }

        Assert.assertEquals("12345678", progress.getReplicationRuleID());
        Assert.assertEquals("testBucketName", progress.getTargetBucketName());
        Assert.assertNull(progress.getTargetBucketLocation());
        Assert.assertEquals("testCloud", progress.getTargetCloud());
        Assert.assertEquals("testCloudLocation", progress.getTargetCloudLocation());
        Assert.assertEquals(0.8f, progress.getHistoricalObjectProgress());
        Assert.assertEquals(dt, progress.getNewObjectProgress());

    }

    @Test
    public void testParseGetBucketReplicationProgressWithoutCloudLocation() {
        Date dt = new Date();
        String respBody = "<ReplicationProgress>\n" +
                " <Rule>\n" +
                "     <ID>12345678</ID>\n" +
                "     <Destination>\n" +
                "         <Bucket>testBucketName</Bucket>\n" +
                "         <Location>testLocation</Location>\n" +
                "     </Destination>\n" +
                "     <PrefixSet>\n" +
                "         <Prefix>aaa</Prefix>\n" +
                "         <Prefix>bbb</Prefix>\n" +
                "     </PrefixSet>\n" +
                "     <Action>xxx,xxx,xxx</Action>\n" +
                "     <Status>doing</Status>\n" +
                "     <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "     <Progress>\n" +
                "         <HistoricalObject>0.9</HistoricalObject>\n" +
                "         <NewObject>" + DateUtil.formatIso8601Date(dt) + "</NewObject>\n" +
                "     </Progress>\n" +
                " </Rule>\n" +
                "</ReplicationProgress>";


        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        BucketReplicationProgress progress = null;
        try {
            progress = ResponseParsers.parseGetBucketReplicationProgress(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication process response body fail!");
        }

        Assert.assertEquals("12345678", progress.getReplicationRuleID());
        Assert.assertEquals("testBucketName", progress.getTargetBucketName());
        Assert.assertEquals("testLocation", progress.getTargetBucketLocation());
        Assert.assertNull(progress.getTargetCloud());
        Assert.assertNull(progress.getTargetCloudLocation());
        Assert.assertEquals(0.9f, progress.getHistoricalObjectProgress());
        Assert.assertEquals(dt, progress.getNewObjectProgress());
    }

    @Test
    public void testParseGetBucketReplicationProgressWithUnnormalParam() {
        Date dt = new Date();
        // <HistoricalObjectReplication>disabled
        String respBody1 = "<ReplicationProgress>\n" +
                " <Rule>\n" +
                "     <ID>12345678</ID>\n" +
                "     <Destination>\n" +
                "         <Bucket>testBucketName</Bucket>\n" +
                "         <Location>testLocation</Location>\n" +
                "     </Destination>\n" +
                "     <PrefixSet>\n" +
                "         <Prefix>aaa</Prefix>\n" +
                "         <Prefix>bbb</Prefix>\n" +
                "     </PrefixSet>\n" +
                "     <Action>xxx,xxx,xxx</Action>\n" +
                "     <Status>doing</Status>\n" +
                "     <HistoricalObjectReplication>disabled</HistoricalObjectReplication>\n" +
                "     <Progress>\n" +
                "         <HistoricalObject>0.9</HistoricalObject>\n" +
                "         <NewObject>" + DateUtil.formatIso8601Date(dt) + "</NewObject>\n" +
                "     </Progress>\n" +
                " </Rule>\n" +
                "</ReplicationProgress>";

        // none <Progress>
        String respBody2 = "<ReplicationProgress>\n" +
                " <Rule>\n" +
                "     <ID>12345678</ID>\n" +
                "     <Destination>\n" +
                "         <Bucket>testBucketName</Bucket>\n" +
                "         <Location>testLocation</Location>\n" +
                "     </Destination>\n" +
                "     <PrefixSet>\n" +
                "         <Prefix>aaa</Prefix>\n" +
                "         <Prefix>bbb</Prefix>\n" +
                "     </PrefixSet>\n" +
                "     <Action>xxx,xxx,xxx</Action>\n" +
                "     <Status>doing</Status>\n" +
                "     <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                " </Rule>\n" +
                "</ReplicationProgress>";

        // none <HistoricalObject>
        String respBody3 = "<ReplicationProgress>\n" +
                " <Rule>\n" +
                "     <ID>12345678</ID>\n" +
                "     <Destination>\n" +
                "         <Bucket>testBucketName</Bucket>\n" +
                "         <Location>testLocation</Location>\n" +
                "     </Destination>\n" +
                "     <PrefixSet>\n" +
                "         <Prefix>aaa</Prefix>\n" +
                "         <Prefix>bbb</Prefix>\n" +
                "     </PrefixSet>\n" +
                "     <Action>xxx,xxx,xxx</Action>\n" +
                "     <Status>doing</Status>\n" +
                "     <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "     <Progress>\n" +
                "         <NewObject>" + DateUtil.formatIso8601Date(dt) + "</NewObject>\n" +
                "     </Progress>\n" +
                " </Rule>\n" +
                "</ReplicationProgress>";


        InputStream instream1 = null;
        InputStream instream2 = null;
        InputStream instream3 = null;
        try {
            instream1 = new ByteArrayInputStream(respBody1.getBytes("utf-8"));
            instream2 = new ByteArrayInputStream(respBody2.getBytes("utf-8"));
            instream3 = new ByteArrayInputStream(respBody3.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        BucketReplicationProgress progress1 = null;
        BucketReplicationProgress progress2 = null;
        BucketReplicationProgress progress3 = null;
        try {
            progress1 = ResponseParsers.parseGetBucketReplicationProgress(instream1);
            progress2 = ResponseParsers.parseGetBucketReplicationProgress(instream2);
            progress3 = ResponseParsers.parseGetBucketReplicationProgress(instream3);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication process response body fail!");
        }

        Assert.assertFalse(progress1.isEnableHistoricalObjectReplication());
        Assert.assertEquals(Float.parseFloat("0"), progress2.getHistoricalObjectProgress());
        Assert.assertNull(progress2.getNewObjectProgress());
        Assert.assertEquals(Float.parseFloat("0"), progress3.getHistoricalObjectProgress());
        Assert.assertNotNull(progress3.getNewObjectProgress());

        // test parse error
        String respBody4 = respBody1 + "-error-body";

        InputStream instream4 = null;
        try {
            instream4 = new ByteArrayInputStream(respBody4.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketReplicationProgress(instream4);
        } catch (ResponseParseException e) {
            // expected exception.
        }
    }

    @Test
    public void testParseGetBucketReplicationWithSyncRole() {
        String respBody = "<ReplicationConfiguration>\n" +
                " <Rule>\n" +
                "    <ID>12345678</ID>\n" +
                "        <Destination>\n" +
                "            <Bucket>testBucketName</Bucket>\n" +
                "            <Cloud>testCloud</Cloud>\n" +
                "            <CloudLocation>testCloudLocation</CloudLocation>\n" +
                "        </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "    <SyncRole>ft-sync-role</SyncRole>\n" +
                " </Rule>\n" +
                "</ReplicationConfiguration>\n";
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<ReplicationRule> rules = null;
        try {
            rules = ResponseParsers.parseGetBucketReplication(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }
        Assert.assertTrue(rules.size() > 0);

        ReplicationRule rule = rules.get(0);
        Assert.assertEquals("12345678", rule.getReplicationRuleID());
        Assert.assertEquals("testBucketName", rule.getTargetBucketName());
        Assert.assertNull(rule.getTargetBucketLocation());
        Assert.assertEquals("testCloud", rule.getTargetCloud());
        Assert.assertEquals("testCloudLocation", rule.getTargetCloudLocation());
        Assert.assertEquals(true, rule.isEnableHistoricalObjectReplication());
        Assert.assertEquals("ft-sync-role", rule.getSyncRole());
        Assert.assertNull(rule.getSseKmsEncryptedObjectsStatus());
        Assert.assertNull(rule.getReplicaKmsKeyID());
    }

    @Test
    public void testParseGetBucketReplicationWithReplicaKmsKeyID() {
        String respBody = "<ReplicationConfiguration>\n" +
                " <Rule>\n" +
                "    <ID>12345678</ID>\n" +
                "        <Destination>\n" +
                "            <Bucket>testBucketName</Bucket>\n" +
                "            <Cloud>testCloud</Cloud>\n" +
                "            <CloudLocation>testCloudLocation</CloudLocation>\n" +
                "        </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "    <EncryptionConfiguration>\n" +
                "        <ReplicaKmsKeyID>12345</ReplicaKmsKeyID>\n" +
                "    </EncryptionConfiguration>\n" +
                " </Rule>\n" +
                "</ReplicationConfiguration>\n";
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<ReplicationRule> rules = null;
        try {
            rules = ResponseParsers.parseGetBucketReplication(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }
        Assert.assertTrue(rules.size() > 0);

        ReplicationRule rule = rules.get(0);
        Assert.assertEquals("12345678", rule.getReplicationRuleID());
        Assert.assertEquals("testBucketName", rule.getTargetBucketName());
        Assert.assertNull(rule.getTargetBucketLocation());
        Assert.assertEquals("testCloud", rule.getTargetCloud());
        Assert.assertEquals("testCloudLocation", rule.getTargetCloudLocation());
        Assert.assertEquals(true, rule.isEnableHistoricalObjectReplication());
        Assert.assertNull(rule.getSyncRole());
        Assert.assertNull(rule.getSseKmsEncryptedObjectsStatus());
        Assert.assertEquals("12345", rule.getReplicaKmsKeyID());
    }

    @Test
    public void testParseGetBucketReplicationWithSseKmsEncryptedObjectsStatus() {
        String respBody = "<ReplicationConfiguration>\n" +
                " <Rule>\n" +
                "    <ID>12345678</ID>\n" +
                "        <Destination>\n" +
                "            <Bucket>testBucketName</Bucket>\n" +
                "            <Cloud>testCloud</Cloud>\n" +
                "            <CloudLocation>testCloudLocation</CloudLocation>\n" +
                "        </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "    <SourceSelectionCriteria>\n" +
                "         <SseKmsEncryptedObjects>\n" +
                "             <Status>Enabled</Status>\n" +
                "         </SseKmsEncryptedObjects>\n" +
                "    </SourceSelectionCriteria>\n" +
                " </Rule>\n" +
                "</ReplicationConfiguration>\n";
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<ReplicationRule> rules = null;
        try {
            rules = ResponseParsers.parseGetBucketReplication(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }
        Assert.assertTrue(rules.size() > 0);

        ReplicationRule rule = rules.get(0);
        Assert.assertEquals("12345678", rule.getReplicationRuleID());
        Assert.assertEquals("testBucketName", rule.getTargetBucketName());
        Assert.assertNull(rule.getTargetBucketLocation());
        Assert.assertEquals("testCloud", rule.getTargetCloud());
        Assert.assertEquals("testCloudLocation", rule.getTargetCloudLocation());
        Assert.assertEquals(true, rule.isEnableHistoricalObjectReplication());
        Assert.assertNull(rule.getSyncRole());
        Assert.assertEquals("Enabled", rule.getSseKmsEncryptedObjectsStatus());
        Assert.assertNull(rule.getReplicaKmsKeyID());
    }

    @Test
    public void testParseGetBucketReplicationWithPrefixAndAction() {
        String respBody = "<ReplicationConfiguration>\n" +
                " <Rule>\n" +
                "    <ID>12345678</ID>\n" +
                "        <Destination>\n" +
                "            <Bucket>testBucketName</Bucket>\n" +
                "            <Cloud>testCloud</Cloud>\n" +
                "            <CloudLocation>testCloudLocation</CloudLocation>\n" +
                "        </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "    <PrefixSet>\n" +
                "        <Prefix>test-prefix-1</Prefix>\n" +
                "        <Prefix>test-prefix-2</Prefix>\n" +
                "        <Prefix>test-prefix-3</Prefix>\n" +
                "    </PrefixSet>\n" +
                "    <Action>PUT,DELETE</Action>\n" +
                "    <EncryptionConfiguration>\n" +
                "        <ReplicaKmsKeyID>12345</ReplicaKmsKeyID>\n" +
                "    </EncryptionConfiguration>\n" +
                " </Rule>\n" +
                "</ReplicationConfiguration>\n";
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<ReplicationRule> rules = null;
        try {
            rules = ResponseParsers.parseGetBucketReplication(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }
        Assert.assertTrue(rules.size() > 0);

        ReplicationRule rule = rules.get(0);
        Assert.assertEquals("12345678", rule.getReplicationRuleID());
        Assert.assertEquals("testBucketName", rule.getTargetBucketName());
        Assert.assertNull(rule.getTargetBucketLocation());
        Assert.assertEquals("testCloud", rule.getTargetCloud());
        Assert.assertEquals("testCloudLocation", rule.getTargetCloudLocation());
        Assert.assertEquals(true, rule.isEnableHistoricalObjectReplication());
        Assert.assertEquals(3, rule.getObjectPrefixList().size());
        for (String o : rule.getObjectPrefixList()) {
            Assert.assertTrue(o.startsWith("test-prefix-"));
        }
        Assert.assertEquals(2, rule.getReplicationActionList().size());
        Assert.assertNull(rule.getSyncRole());
        Assert.assertNull(rule.getSseKmsEncryptedObjectsStatus());
        Assert.assertEquals("12345", rule.getReplicaKmsKeyID());
    }

    @Test
    public void parseGetBucketReplicationLocation() {
        String respBody = "<ReplicationLocation>\n" +
                "    <Location>test-location-1</Location>\n" +
                "    <Location>test-location-2</Location>\n" +
                "</ReplicationLocation>\n";
        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<String> locations = null;
        try {
            locations = ResponseParsers.parseGetBucketReplicationLocation(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }
        Assert.assertEquals(2, locations.size());
        for (String o : locations) {
            Assert.assertTrue(o.startsWith("test-location-"));
        }


        // test parse error
        respBody  += "-error-body";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketReplicationLocation(instream);
            Assert.fail("should be failed here.");
        } catch (Exception e) {
            // expected exception.
        }
    }

    @Test
    public void testParseGetLiveChannelStat() {
        String respBody = "" +
                "<LiveChannelStat>\n" +
                "  <Status>Live</Status>\n" +
                "  <ConnectedTime>2016-08-25T06:25:15.000Z</ConnectedTime>\n" +
                "  <RemoteAddr>10.1.2.3:47745</RemoteAddr>\n" +
                "  <Video>\n" +
                "    <Width>1280</Width>\n" +
                "    <Height>536</Height>\n" +
                "    <FrameRate>24</FrameRate>\n" +
                "    <Bandwidth>0</Bandwidth>\n" +
                "    <Codec>H264</Codec>\n" +
                "  </Video>\n" +
                "  <Audio>\n" +
                "    <Bandwidth>0</Bandwidth>\n" +
                "    <SampleRate>44100</SampleRate>\n" +
                "    <Codec>ADPCM</Codec>\n" +
                "  </Audio>\n" +
                "</LiveChannelStat>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        LiveChannelStat stat = null;
        try {
            stat = ResponseParsers.parseGetLiveChannelStat(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals(PushflowStatus.Live, stat.getPushflowStatus());
        Assert.assertEquals("10.1.2.3:47745", stat.getRemoteAddress());
        Assert.assertEquals(1280, stat.getVideoStat().getWidth());
        Assert.assertEquals(44100, stat.getAudioStat().getSampleRate());
    }

    @Test
    public void testParseGetLiveChannelHistory() {
        String respBody = "" +
                "<LiveChannelHistory>\n" +
                "  <LiveRecord>\n" +
                "    <StartTime>2016-07-30T01:53:21.000Z</StartTime>\n" +
                "    <EndTime>2016-07-30T01:53:31.000Z</EndTime>\n" +
                "    <RemoteAddr>10.101.194.148:56861</RemoteAddr>\n" +
                "  </LiveRecord>\n" +
                "  <LiveRecord>\n" +
                "    <StartTime>2016-07-30T01:53:35.000Z</StartTime>\n" +
                "    <EndTime>2016-07-30T01:53:45.000Z</EndTime>\n" +
                "    <RemoteAddr>10.101.194.148:57126</RemoteAddr>\n" +
                "  </LiveRecord>\n" +
                "</LiveChannelHistory>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<LiveRecord> records = null;
        try {
            records = ResponseParsers.parseGetLiveChannelHistory(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals(2, records.size());
        Assert.assertEquals("10.101.194.148:56861", records.get(0).getRemoteAddress());
    }

    @Test
    public void testParseListImageStyle() {
        String respBody = "" +
                "<ImageStyle>\n" +
                "  <Style>\n" +
                "    <Name>Name1</Name>\n" +
                "    <Content>Style1</Content>\n" +
                "    <LastModifyTime>Wed, 02 Oct 2019 14:30:18 GMT</LastModifyTime>\n" +
                "    <CreateTime>Wed, 02 Oct 2019 14:30:18 GMT</CreateTime>\n" +
                "  </Style>\n" +
                "  <Style>\n" +
                "    <Name>Name2</Name>\n" +
                "    <Content>Style2</Content>\n" +
                "    <LastModifyTime>Wed, 02 Oct 2019 14:30:18 GMT</LastModifyTime>\n" +
                "    <CreateTime>Wed, 02 Oct 2019 14:30:18 GMT</CreateTime>\n" +
                "  </Style>\n" +
                "</ImageStyle>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<Style> records = null;
        try {
            records = ResponseParsers.parseListImageStyle(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals(2, records.size());
        Assert.assertEquals("Name1", records.get(0).GetStyleName());
    }

    @Test
    public void testParseBucketImage() {
        String respBody = "" +
                "  <Style>\n" +
                "    <Name>Name</Name>\n" +
                "    <Default404Pic>404Pic</Default404Pic>\n" +
                "    <StyleDelimiters>#</StyleDelimiters>\n" +
                "    <Status>Enable</Status>\n" +
                "    <AutoSetContentType>True</AutoSetContentType>\n" +
                "    <OrigPicForbidden>True</OrigPicForbidden>\n" +
                "    <SetAttachName>True</SetAttachName>\n" +
                "    <UseStyleOnly>True</UseStyleOnly>\n" +
                "    <UseSrcFormat>True</UseSrcFormat>\n" +
                "  </Style>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        GetBucketImageResult result = null;
        try {
            result = ResponseParsers.parseBucketImage(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("Enable", result.GetStatus());
    }

    @Test
    public void testParseImageStyle() {
        String respBody = "" +
                "  <Style>\n" +
                "    <Name>Name1</Name>\n" +
                "    <Content>Style1</Content>\n" +
                "    <LastModifyTime>Wed, 02 Oct 2019 14:30:18 GMT</LastModifyTime>\n" +
                "    <CreateTime>Wed, 02 Oct 2019 14:30:18 GMT</CreateTime>\n" +
                "  </Style>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        GetImageStyleResult result = null;
        try {
            result = ResponseParsers.parseImageStyle(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("Name1", result.GetStyleName());
    }

    @Test
    public void testParseGetBucketCname() throws Exception {
        String respBody = "" +
                "<CnameConfiguration>\n" +
                "  <Cname>\n" +
                "    <Domain>Domain1</Domain>\n" +
                "    <Status>Enabled</Status>\n" +
                "    <LastModified>2019-09-30T01:53:45.000Z</LastModified>\n" +
                "    <IsPurgeCdnCache>True</IsPurgeCdnCache>\n" +
                "  </Cname>\n " +
                "  <Cname>\n" +
                "    <Domain>Domain2</Domain>\n" +
                "    <Status>Disabled</Status>\n" +
                "    <LastModified>2019-09-30T01:53:45.000Z</LastModified>\n" +
                "  </Cname>\n" +
                "  <Cname>\n" +
                "    <Domain>Domain3</Domain>\n" +
                "    <Status>Enabled</Status>\n" +
                "    <LastModified>2019-09-30T01:53:45.000Z</LastModified>\n" +
                "    <Certificate>\n" +
                "        <Type>CAS</Type>\n" +
                "        <CertId>hangzhou-01</CertId>\n" +
                "        <Status>Enabled</Status>\n" +
                "    </Certificate>\n" +
                "  </Cname>\n" +
                "  <Cname>\n" +
                "    <Domain>Domain4</Domain>\n" +
                "    <Status>Enabled</Status>\n" +
                "    <LastModified>2019-09-30T01:53:45.000Z</LastModified>\n" +
                "    <Certificate>\n" +
                "        <Type>Upload</Type>\n" +
                "        <Status>Disabled</Status>\n" +
                "    </Certificate>\n" +
                "  </Cname>\n" +
                "</CnameConfiguration>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<CnameConfiguration> result = null;
        try {
            result = ResponseParsers.parseGetBucketCname(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Date date = DateUtil.parseIso8601Date("2019-09-30T01:53:45.000Z");

        Assert.assertEquals(4, result.size());

        Assert.assertEquals("Domain1", result.get(0).getDomain());
        Assert.assertEquals(CnameConfiguration.CnameStatus.Enabled, result.get(0).getStatus());
        Assert.assertEquals(date, result.get(0).getLastMofiedTime());
        Assert.assertEquals(new Boolean(true), result.get(0).getPurgeCdnCache());
        Assert.assertNull(result.get(0).getCertType());

        Assert.assertEquals("Domain2", result.get(1).getDomain());
        Assert.assertEquals(CnameConfiguration.CnameStatus.Disabled, result.get(1).getStatus());
        Assert.assertEquals(date, result.get(1).getLastMofiedTime());
        Assert.assertNull(result.get(1).getPurgeCdnCache());
        Assert.assertNull(result.get(1).getCertType());

        Assert.assertEquals("Domain3", result.get(2).getDomain());
        Assert.assertEquals(CnameConfiguration.CnameStatus.Enabled, result.get(2).getStatus());
        Assert.assertEquals(date, result.get(2).getLastMofiedTime());
        Assert.assertNull(result.get(2).getPurgeCdnCache());
        Assert.assertEquals(CnameConfiguration.CertType.CAS, result.get(2).getCertType());
        Assert.assertEquals("hangzhou-01", result.get(2).getCertId());
        Assert.assertEquals(CnameConfiguration.CertStatus.Enabled, result.get(2).getCertStatus());

        Assert.assertEquals("Domain4", result.get(3).getDomain());
        Assert.assertEquals(CnameConfiguration.CnameStatus.Enabled, result.get(3).getStatus());
        Assert.assertEquals(date, result.get(3).getLastMofiedTime());
        Assert.assertNull(result.get(3).getPurgeCdnCache());
        Assert.assertEquals(CnameConfiguration.CertType.Upload, result.get(3).getCertType());
        Assert.assertNull(result.get(3).getCertId());
        Assert.assertEquals(CnameConfiguration.CertStatus.Disabled, result.get(3).getCertStatus());
    }

    @Test
    public void testParseCreateBucketCnameToken() throws Exception {
        String respBody = "" +
                "<CnameToken>\n" +
                "  <Bucket>bucket-name</Bucket>\n" +
                "  <Cname>domain</Cname>\n" +
                "  <Token>token-1234</Token>\n" +
                "  <ExpireTime>Wed, 23 Feb 2022 21:16:37 GMT</ExpireTime>\n" +
                 "</CnameToken>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        CreateBucketCnameTokenResult result = null;
        try {
            result = ResponseParsers.parseCreateBucketCnameToken(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("bucket-name", result.getBucket());
        Assert.assertEquals("domain", result.getCname());
        Assert.assertEquals("token-1234", result.getToken());
        Assert.assertEquals("Wed, 23 Feb 2022 21:16:37 GMT", result.getExpireTime());
    }

    @Test
    public void testParseGetBucketCnameToken() throws Exception {
        String respBody = "" +
                "<CnameToken>\n" +
                "  <Bucket>bucket-name</Bucket>\n" +
                "  <Cname>domain</Cname>\n" +
                "  <Token>token-1234</Token>\n" +
                "  <ExpireTime>Wed, 23 Feb 2022 21:16:37 GMT</ExpireTime>\n" +
                "</CnameToken>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        GetBucketCnameTokenResult result = null;
        try {
            result = ResponseParsers.parseGetBucketCnameToken(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("bucket-name", result.getBucket());
        Assert.assertEquals("domain", result.getCname());
        Assert.assertEquals("token-1234", result.getToken());
        Assert.assertEquals("Wed, 23 Feb 2022 21:16:37 GMT", result.getExpireTime());
    }

    @Test
    public void testParseGetBucketInfoBasic() {
        String respBody = "" +
                "<BucketInfo>\n" +
                "  <Bucket>\n" +
                "           <CreationDate>2013-07-31T10:56:21.000Z</CreationDate>\n" +
                "            <ExtranetEndpoint>oss-cn-hangzhou.aliyuncs.com</ExtranetEndpoint>\n" +
                "            <IntranetEndpoint>oss-cn-hangzhou-internal.aliyuncs.com</IntranetEndpoint>\n" +
                "            <Location>oss-cn-hangzhou</Location>\n" +
                "            <Name>oss-example</Name>\n" +
                "            <Owner>\n" +
                "              <DisplayName>username</DisplayName>\n" +
                "              <ID>27183473914****</ID>\n" +
                "            </Owner>\n" +
                "            <AccessControlList>\n" +
                "              <Grant>private</Grant>\n" +
                "            </AccessControlList>\n" +
                "          </Bucket>\n" +
                " </BucketInfo>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        BucketInfo result = null;
        try {
            result = ResponseParsers.parseGetBucketInfo(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals(CannedAccessControlList.Private, result.getCannedACL());
        Assert.assertEquals("oss-cn-hangzhou", result.getBucket().getLocation());
        Assert.assertEquals("oss-example", result.getBucket().getName());
    }

    @Test
    public void testParseGetBucketInfo() {
        String respBody = "" +
                "<BucketInfo>\n" +
                "  <Bucket>\n" +
                "           <CreationDate>2013-07-31T10:56:21.000Z</CreationDate>\n" +
                "            <ExtranetEndpoint>oss-cn-hangzhou.aliyuncs.com</ExtranetEndpoint>\n" +
                "            <IntranetEndpoint>oss-cn-hangzhou-internal.aliyuncs.com</IntranetEndpoint>\n" +
                "            <Location>oss-cn-hangzhou</Location>\n" +
                "            <Name>oss-example</Name>\n" +
                "            <Owner>\n" +
                "              <DisplayName>username</DisplayName>\n" +
                "              <ID>27183473914****</ID>\n" +
                "            </Owner>\n" +
                "            <AccessControlList>\n" +
                "              <Grant>private</Grant>\n" +
                "            </AccessControlList>\n" +
                "            <Comment>test</Comment>\n" +
                "            <DataRedundancyType>LRS</DataRedundancyType>\n" +
                "          </Bucket>\n" +
                " </BucketInfo>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        BucketInfo result = null;
        try {
            result = ResponseParsers.parseGetBucketInfo(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("test", result.getComment());
        Assert.assertEquals(DataRedundancyType.LRS, result.getDataRedundancyType());
        Assert.assertEquals(CannedAccessControlList.Private, result.getCannedACL());
        Assert.assertEquals("oss-cn-hangzhou", result.getBucket().getLocation());
        Assert.assertEquals("oss-example", result.getBucket().getName());
        Assert.assertEquals(null, result.getBucket().getHnsStatus());
        Assert.assertEquals(null, result.getBucket().getResourceGroupId());

        respBody = "" +
                "<BucketInfo>\n" +
                "  <Bucket>\n" +
                "           <CreationDate>2013-07-31T10:56:21.000Z</CreationDate>\n" +
                "            <ExtranetEndpoint>oss-cn-hangzhou.aliyuncs.com</ExtranetEndpoint>\n" +
                "            <HierarchicalNamespace>Enabled</HierarchicalNamespace>\n" +
                "            <IntranetEndpoint>oss-cn-hangzhou-internal.aliyuncs.com</IntranetEndpoint>\n" +
                "            <Location>oss-cn-hangzhou</Location>\n" +
                "            <ResourceGroupId>xxx-id-123</ResourceGroupId>\n" +
                "            <Name>oss-example</Name>\n" +
                "            <Owner>\n" +
                "              <DisplayName>username</DisplayName>\n" +
                "              <ID>27183473914****</ID>\n" +
                "            </Owner>\n" +
                "            <AccessControlList>\n" +
                "              <Grant>private</Grant>\n" +
                "            </AccessControlList>\n" +
                "            <Comment>test</Comment>\n" +
                "            <DataRedundancyType>LRS</DataRedundancyType>\n" +
                "          </Bucket>\n" +
                " </BucketInfo>";

        instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        result = null;
        try {
            result = ResponseParsers.parseGetBucketInfo(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("test", result.getComment());
        Assert.assertEquals(DataRedundancyType.LRS, result.getDataRedundancyType());
        Assert.assertEquals(CannedAccessControlList.Private, result.getCannedACL());
        Assert.assertEquals("oss-cn-hangzhou", result.getBucket().getLocation());
        Assert.assertEquals("oss-example", result.getBucket().getName());
        Assert.assertEquals(HnsStatus.Enabled.toString(), result.getBucket().getHnsStatus());
        Assert.assertEquals("xxx-id-123", result.getBucket().getResourceGroupId());
    }

    @Test
    public void testParseListVpcip() {
        String respBody = "" +
                "<ListVpcipResult>\n" +
                "  <Vpcip>\n" +
                "           <Region>test-region-1</Region>\n" +
                "            <VpcId>test-vpcid-1</VpcId>\n" +
                "            <Vip>test-vip-1</Vip>\n" +
                "            <Label>test-label-1</Label>\n" +
                " </Vpcip>\n" +
                "  <Vpcip>\n" +
                "           <Region>test-region-2</Region>\n" +
                "            <VpcId>test-vpcid-2</VpcId>\n" +
                "            <Vip>test-vip-2</Vip>\n" +
                "            <Label>test-label-2</Label>\n" +
                " </Vpcip>\n" +
                "</ListVpcipResult>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<Vpcip>  result = null;
        try {
            result = ResponseParsers.parseListVpcipResult(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals(2, result.size());
        for (Vpcip v: result) {
            Assert.assertTrue(v.getRegion().startsWith("test-region-"));
            Assert.assertTrue(v.getVpcId().startsWith("test-vpcid-"));
            Assert.assertTrue(v.getVip().startsWith("test-vip-"));
            Assert.assertTrue(v.getLabel().startsWith("test-label-"));
            Assert.assertTrue(v.toString().contains("test-region-"));
        }
    }

    @Test
    public void testParseGetBucketVpcip() {
        String respBody = "" +
                "<ListVpcPolicyResult>\n" +
                "  <Vpcip>\n" +
                "           <Region>test-region-1</Region>\n" +
                "            <VpcId>test-vpcid-1</VpcId>\n" +
                "            <Vip>test-vip-1</Vip>\n" +
                " </Vpcip>\n" +
                "  <Vpcip>\n" +
                "           <Region>test-region-2</Region>\n" +
                "            <VpcId>test-vpcid-2</VpcId>\n" +
                "            <Vip>test-vip-2</Vip>\n" +
                " </Vpcip>\n" +
                "</ListVpcPolicyResult>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<VpcPolicy>  result = null;
        try {
            result = ResponseParsers.parseListVpcPolicyResult(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals(2, result.size());
        for (VpcPolicy v: result) {
            Assert.assertTrue(v.getRegion().startsWith("test-region-"));
            Assert.assertTrue(v.getVpcId().startsWith("test-vpcid-"));
            Assert.assertTrue(v.getVip().startsWith("test-vip-"));
        }
    }

    @Test
    public void testparseGetCreateVpcipResult() {
        String respBody = "" +
                "<Vpcip>\n" +
                "     <Region>test-region-1</Region>\n" +
                "     <VpcId>test-vpcid-1</VpcId>\n" +
                "     <Vip>test-vip-1</Vip>\n" +
                "     <Label>test-label-1</Label>\n" +
                "</Vpcip>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        Vpcip vpcip = null;
        try {
            vpcip = ResponseParsers.parseGetCreateVpcipResult(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertTrue(vpcip.getRegion().startsWith("test-region-"));
        Assert.assertTrue(vpcip.getVpcId().startsWith("test-vpcid-"));
        Assert.assertTrue(vpcip.getVip().startsWith("test-vip-"));
        Assert.assertTrue(vpcip.getLabel().startsWith("test-label-"));
        Assert.assertTrue(vpcip.toString().contains("test-region-"));

        // test error body
        try {
            instream = new ByteArrayInputStream((respBody + "error-body").getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetCreateVpcipResult(instream);
        } catch (ResponseParseException e) {
           // expected exception.
        }
    }

    @Test
    public void testListImageStyleResponseParser() {
        String respBody = null;
        InputStream instream = null;

        Date dt = new Date();
        DateUtil.formatIso8601Date(dt);

        respBody = "" +
            "<ListImageStyleResult>\n" +
            "  <Style>\n" +
            "    <Name>name1</Name>\n" +
            "    <Content>content1</Content>\n" +
            "    <LastModifyTime>"+ DateUtil.formatRfc822Date(dt) + "</LastModifyTime>\n" +
            "    <CreateTime>"+ DateUtil.formatRfc822Date(dt) + "</CreateTime>\n" +
            "  </Style>\n" +
            "  <Style>\n" +
            "    <Name>name2</Name>\n" +
            "    <Content>content2</Content>\n" +
            "    <LastModifyTime>" + DateUtil.formatRfc822Date(dt) + "</LastModifyTime>\n" +
            "    <CreateTime>"+ DateUtil.formatRfc822Date(dt) + "</CreateTime>\n" +
            "  </Style>\n" +
            "</ListImageStyleResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.ListImageStyleResponseParser parser = new ResponseParsers.ListImageStyleResponseParser();
            List<Style> value = parser.parse(responseMessage);
            Assert.assertEquals(value.size(), 2);
            Assert.assertEquals(value.get(0).GetStyleName(), "name1");
        } catch (ResponseParseException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        respBody = "invalid xml";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.ListImageStyleResponseParser parser = new ResponseParsers.ListImageStyleResponseParser();
            List<Style> value = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }

        respBody = "" +
                "<ListImageStyleResult>\n" +
                "  <Style>\n" +
                "    <Name>name1</Name>\n" +
                "    <Content>content1</Content>\n" +
                "    <LastModifyTime>invalid</LastModifyTime>\n" +
                "    <CreateTime>"+ DateUtil.formatRfc822Date(dt) + "</CreateTime>\n" +
                "  </Style>\n" +
                "</ListImageStyleResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.ListImageStyleResponseParser parser = new ResponseParsers.ListImageStyleResponseParser();
            List<Style> value = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetBucketImageResponseParser() {
        String respBody = null;
        InputStream instream = null;

        respBody = "" +
                "<ListImageStyleResult>\n" +
                "    <Name>name</Name>\n" +
                "    <Default404Pic>default</Default404Pic>\n" +
                "    <StyleDelimiters>value</StyleDelimiters>\n" +
                "    <Status>Status</Status>\n" +
                "    <AutoSetContentType>True</AutoSetContentType>\n" +
                "    <OrigPicForbidden>True</OrigPicForbidden>\n" +
                "    <SetAttachName>True</SetAttachName>\n" +
                "    <UseStyleOnly>True</UseStyleOnly>\n" +
                "    <UseSrcFormat>True</UseSrcFormat>\n" +
                "</ListImageStyleResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketImageResponseParser parser = new ResponseParsers.GetBucketImageResponseParser();
            GetBucketImageResult result = parser.parse(responseMessage);
            Assert.assertEquals(result.GetBucketName(), "name");
        } catch (ResponseParseException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        respBody = "invalid xml";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketImageResponseParser parser = new ResponseParsers.GetBucketImageResponseParser();
            GetBucketImageResult result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }

        respBody = "" +
                "<ListImageStyleResult>\n" +
                "    <Name>name</Name>\n" +
                "    <Default404Pic>default</Default404Pic>\n" +
                "    <StyleDelimiters>value</StyleDelimiters>\n" +
                "    <Status>Status</Status>\n" +
                "    <OrigPicForbidden>True</OrigPicForbidden>\n" +
                "    <SetAttachName>True</SetAttachName>\n" +
                "    <UseStyleOnly>True</UseStyleOnly>\n" +
                "    <UseSrcFormat>True</UseSrcFormat>\n" +
                "</ListImageStyleResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketImageResponseParser parser = new ResponseParsers.GetBucketImageResponseParser();
            GetBucketImageResult result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetImageStyleResponseParser() {
        String respBody = null;
        InputStream instream = null;
        Date dt = new Date();
        DateUtil.formatIso8601Date(dt);

        respBody = "" +
                "  <Style>\n" +
                "    <Name>name</Name>\n" +
                "    <Content>content</Content>\n" +
                "    <LastModifyTime>" + DateUtil.formatRfc822Date(dt) + "</LastModifyTime>\n" +
                "    <CreateTime>"+ DateUtil.formatRfc822Date(dt) + "</CreateTime>\n" +
                "  </Style>\n";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetImageStyleResponseParser parser = new ResponseParsers.GetImageStyleResponseParser();
            GetImageStyleResult result = parser.parse(responseMessage);
            Assert.assertEquals(result.GetStyleName(), "name");
        } catch (ResponseParseException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        respBody = "invalid xml";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetImageStyleResponseParser parser = new ResponseParsers.GetImageStyleResponseParser();
            GetImageStyleResult result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }

        respBody = "" +
                "  <Style>\n" +
                "    <Name>name1</Name>\n" +
                "    <Content>content1</Content>\n" +
                "    <LastModifyTime>invalid</LastModifyTime>\n" +
                "    <CreateTime>"+ DateUtil.formatIso8601Date(dt) + "</CreateTime>\n" +
                "  </Style>\n";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetImageStyleResponseParser parser = new ResponseParsers.GetImageStyleResponseParser();
            GetImageStyleResult result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetBucketCnameResponseParser() {
        String respBody = null;
        InputStream instream = null;
        Date dt = new Date();
        DateUtil.formatIso8601Date(dt);

        respBody = "" +
                "<CnameConfigurationListResult>\n" +
                "  <Cname>\n" +
                "    <Domain>name</Domain>\n" +
                "    <Status>Enabled</Status>\n" +
                "    <LastModified>" + DateUtil.formatIso8601Date(dt) + "</LastModified>\n" +
                "    <IsPurgeCdnCache>True</IsPurgeCdnCache>\n" +
                "  </Cname>\n" +
                "  <Cname>\n" +
                "    <Domain>name1</Domain>\n" +
                "    <Status>Disabled</Status>\n" +
                "    <LastModified>" + DateUtil.formatIso8601Date(dt) + "</LastModified>\n" +
                "  </Cname>\n" +
                "</CnameConfigurationListResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketCnameResponseParser parser = new ResponseParsers.GetBucketCnameResponseParser();
            List<CnameConfiguration> result = parser.parse(responseMessage);
            Assert.assertEquals(result.size(), 2);
            Assert.assertEquals(result.get(0).getDomain(), "name");
        } catch (ResponseParseException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        respBody = "invalid xml";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketCnameResponseParser parser = new ResponseParsers.GetBucketCnameResponseParser();
            List<CnameConfiguration> result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }

        respBody = "" +
                "<CnameConfigurationListResult>\n" +
                "  <Cname>\n" +
                "    <Domain>name</Domain>\n" +
                "    <Status>Enabled</Status>\n" +
                "    <LastModified>" + DateUtil.formatRfc822Date(dt) + "</LastModified>\n" +
                "    <IsPurgeCdnCache>True</IsPurgeCdnCache>\n" +
                "  </Cname>\n" +
                "</CnameConfigurationListResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketCnameResponseParser parser = new ResponseParsers.GetBucketCnameResponseParser();
            List<CnameConfiguration> result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetBucketReplicationResponseParser() {
        String respBody = null;
        InputStream instream = null;

        respBody = "" +
                "<ReplicationConfiguration>\n" +
                "  <Rule>\n" +
                "    <ID>name</ID>\n" +
                "    <PrefixSet>\n" +
                "       <Prefix>source_image</Prefix>\n" +
                "       <Prefix>video</Prefix>\n" +
                "    </PrefixSet>\n" +
                "    <Action>PUT</Action>\n" +
                "    <Destination>\n" +
                "       <Bucket>target-bucket</Bucket>\n" +
                "       <Location>oss-cn-beijing</Location>\n" +
                "    </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "    <EncryptionConfiguration>\n" +
                "       <ReplicaKmsKeyID>kmsid</ReplicaKmsKeyID>\n" +
                "    </EncryptionConfiguration>\n" +
                "    <SourceSelectionCriteria>\n" +
                "        <SseKmsEncryptedObjects>\n" +
                "           <Status>status</Status>\n" +
                "        </SseKmsEncryptedObjects>\n" +
                "    </SourceSelectionCriteria>\n" +
                "  </Rule>\n" +
                "  <Rule>\n" +
                "    <ID>name2</ID>\n" +
                "    <Destination>\n" +
                "       <Bucket>target-bucket</Bucket>\n" +
                "       <Location>oss-cn-beijing</Location>\n" +
                "    </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>disable</HistoricalObjectReplication>\n" +
                "    <SourceSelectionCriteria>\n" +
                "    </SourceSelectionCriteria>\n" +
                "  </Rule>\n" +
                "  <Rule>\n" +
                "    <ID>name3</ID>\n" +
                "    <Destination>\n" +
                "       <Bucket>target-bucket</Bucket>\n" +
                "       <Location>oss-cn-beijing</Location>\n" +
                "    </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>disable</HistoricalObjectReplication>\n" +
                "  </Rule>\n" +
                "</ReplicationConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationResponseParser parser = new ResponseParsers.GetBucketReplicationResponseParser();
            List<ReplicationRule> result = parser.parse(responseMessage);
            Assert.assertEquals(result.size(), 3);
            Assert.assertEquals(result.get(0).getReplicationRuleID(), "name");
        } catch (ResponseParseException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        respBody = "invalid xml";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationResponseParser parser = new ResponseParsers.GetBucketReplicationResponseParser();
            List<ReplicationRule> result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }

        respBody = "" +
                "<ReplicationConfiguration>\n" +
                "  <Rule>\n" +
                "    <ID>name3</ID>\n" +
                "    <Destination>\n" +
                "       <Bucket>target-bucket</Bucket>\n" +
                "       <Location>oss-cn-beijing</Location>\n" +
                "    </Destination>\n" +
                "    <Status>invalid</Status>\n" +
                "    <HistoricalObjectReplication>disable</HistoricalObjectReplication>\n" +
                "  </Rule>\n" +
                "</ReplicationConfiguration>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationResponseParser parser = new ResponseParsers.GetBucketReplicationResponseParser();
            List<ReplicationRule> result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetBucketReplicationProgressResponseParser() {
        String respBody = null;
        InputStream instream = null;

        respBody = "" +
                "<ReplicationProgress>\n" +
                "  <Rule>\n" +
                "    <ID>name</ID>\n" +
                "    <Destination>\n" +
                "       <Bucket>target-bucket</Bucket>\n" +
                "       <Location>oss-cn-beijing</Location>\n" +
                "    </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>disable</HistoricalObjectReplication>\n" +
                "    <Progress>\n" +
                "       <HistoricalObject>0.85</HistoricalObject>\n" +
                "       <NewObject>2015-09-24T15:28:14.000Z</NewObject>\n" +
                "    </Progress>\n" +
                "  </Rule>\n" +
                "</ReplicationProgress>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationProgressResponseParser parser = new ResponseParsers.GetBucketReplicationProgressResponseParser();
            BucketReplicationProgress result = parser.parse(responseMessage);
            Assert.assertEquals(result.getReplicationRuleID(), "name");
        } catch (ResponseParseException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        respBody = "" +
                "<ReplicationProgress>\n" +
                "  <Rule>\n" +
                "    <ID>name</ID>\n" +
                "    <Destination>\n" +
                "       <Bucket>target-bucket</Bucket>\n" +
                "       <Location>oss-cn-beijing</Location>\n" +
                "    </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>enabled</HistoricalObjectReplication>\n" +
                "  </Rule>\n" +
                "</ReplicationProgress>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationProgressResponseParser parser = new ResponseParsers.GetBucketReplicationProgressResponseParser();
            BucketReplicationProgress result = parser.parse(responseMessage);
            Assert.assertEquals(result.getReplicationRuleID(), "name");
        } catch (ResponseParseException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        respBody = "invalid xml";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationProgressResponseParser parser = new ResponseParsers.GetBucketReplicationProgressResponseParser();
            BucketReplicationProgress result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }

        respBody = "" +
                "<ReplicationProgress>\n" +
                "  <Rule>\n" +
                "    <ID>name</ID>\n" +
                "    <Destination>\n" +
                "       <Bucket>target-bucket</Bucket>\n" +
                "       <Location>oss-cn-beijing</Location>\n" +
                "    </Destination>\n" +
                "    <Status>doing</Status>\n" +
                "    <HistoricalObjectReplication>disable</HistoricalObjectReplication>\n" +
                "    <Progress>\n" +
                "       <NewObject>invalid</NewObject>\n" +
                "    </Progress>\n" +
                "  </Rule>\n" +
                "</ReplicationProgress>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationProgressResponseParser parser = new ResponseParsers.GetBucketReplicationProgressResponseParser();
            BucketReplicationProgress result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetBucketReplicationLocationResponseParser() {
        String respBody = null;
        InputStream instream = null;

        respBody = "" +
                "<ReplicationLocation>\n" +
                "  <Location>oss-cn-beijing</Location>\n" +
                "  <Location>oss-cn-shenzhen</Location>\n" +
                "</ReplicationLocation>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationLocationResponseParser parser = new ResponseParsers.GetBucketReplicationLocationResponseParser();
            List<String> result = parser.parse(responseMessage);
            Assert.assertEquals(result.size(), 2);
            Assert.assertEquals(result.get(0), "oss-cn-beijing");
        } catch (ResponseParseException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        respBody = "invalid xml";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(instream);
            ResponseParsers.GetBucketReplicationLocationResponseParser parser = new ResponseParsers.GetBucketReplicationLocationResponseParser();
            List<String> result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            responseMessage.setContent(null);
            ResponseParsers.GetBucketReplicationLocationResponseParser parser = new ResponseParsers.GetBucketReplicationLocationResponseParser();
            List<String> result = parser.parse(responseMessage);
            Assert.fail("should not here");
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testAppendObjectResponseParser() {
        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            Map<String, String> headers = new HashMap<String, String>();
            responseMessage.setHeaders(headers);
            ResponseParsers.AppendObjectResponseParser parser = new ResponseParsers.AppendObjectResponseParser();
            AppendObjectResult result = parser.parse(responseMessage);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testGetObjectResponseParser() {
        try {
            ResponseMessage responseMessage = new ResponseMessage(null);;
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Last-Modified", "invalid");
            responseMessage.setHeaders(headers);
            ResponseParsers.GetObjectResponseParser parser = new ResponseParsers.GetObjectResponseParser("bucket", "key");
            OSSObject result = parser.parse(responseMessage);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testDeleteVersionsResponseParser() {
        InputStream instream = null;

        try {
            instream = new ByteArrayInputStream("".getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage responseMessage = new ResponseMessage(null);
            responseMessage.setContent(null);
            responseMessage.setContentLength(0);
            ResponseParsers.DeleteVersionsResponseParser parser = new ResponseParsers.DeleteVersionsResponseParser();
            DeleteVersionsResult result = parser.parse(responseMessage);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseListObjects() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListBucketResult>\n" +
                "  <Name>oss-example</Name>\n" +
                "  <MaxKeys>100</MaxKeys>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "  <Prefix></Prefix>\n" +
                "  <Marker></Marker>\n" +
                "  <Delimiter></Delimiter>\n" +
                "  <NextMarker></NextMarker>\n" +
                "  <EncodingType></EncodingType>\n" +
                "  <CommonPrefixes>\n" +
                "    <Prefix></Prefix>\n" +
                "  </CommonPrefixes>\n" +
                "</ListBucketResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ObjectListing result = ResponseParsers.parseListObjects(instream);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<ListBucketResult>\n" +
                "  <Name>oss-example</Name>\n" +
                "  <MaxKeys>100</MaxKeys>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "</ListBucketResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ObjectListing result = ResponseParsers.parseListObjects(instream);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ObjectListing result = ResponseParsers.parseListObjects(null);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testparseListVersions() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListBucketResult>\n" +
                "  <Name>oss-example</Name>\n" +
                "  <MaxKeys>100</MaxKeys>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "  <Prefix></Prefix>\n" +
                "  <Marker></Marker>\n" +
                "  <Delimiter></Delimiter>\n" +
                "  <NextMarker></NextMarker>\n" +
                "  <NextKeyMarker></NextKeyMarker>\n" +
                "  <EncodingType>invalid</EncodingType>\n" +
                "  <VersionIdMarker></VersionIdMarker>\n" +
                "  <NextVersionIdMarker></NextVersionIdMarker>\n" +
                "  <CommonPrefixes>\n" +
                "    <Prefix></Prefix>\n" +
                "  </CommonPrefixes>\n" +
                "</ListBucketResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            VersionListing result = ResponseParsers.parseListVersions(instream);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<ListBucketResult>\n" +
                "  <Name>oss-example</Name>\n" +
                "  <MaxKeys>100</MaxKeys>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "  <Prefix></Prefix>\n" +
                "  <Marker></Marker>\n" +
                "  <Delimiter></Delimiter>\n" +
                "  <NextMarker></NextMarker>\n" +
                "  <NextKeyMarker></NextKeyMarker>\n" +
                "  <EncodingType></EncodingType>\n" +
                "  <VersionIdMarker></VersionIdMarker>\n" +
                "  <NextVersionIdMarker></NextVersionIdMarker>\n" +
                "  <CommonPrefixes>\n" +
                "    <Prefix></Prefix>\n" +
                "  </CommonPrefixes>\n" +
                "</ListBucketResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            VersionListing result = ResponseParsers.parseListVersions(instream);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<ListBucketResult>\n" +
                "  <Name>oss-example</Name>\n" +
                "  <MaxKeys>100</MaxKeys>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "</ListBucketResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            VersionListing result = ResponseParsers.parseListVersions(instream);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            VersionListing result = ResponseParsers.parseListVersions(null);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testparseGetBucketAcl() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            AccessControlList result = ResponseParsers.parseGetBucketAcl(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            AccessControlList result = ResponseParsers.parseGetBucketAcl(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetObjectAcl() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ObjectAcl result = ResponseParsers.parseGetObjectAcl(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ObjectAcl result = ResponseParsers.parseGetObjectAcl(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketReferer() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<RefererConfiguration>\n" +
                "  <AllowEmptyReferer>true</AllowEmptyReferer>\n" +
                "  <RefererList>\n" +
                "  </RefererList>\n" +
                "</RefererConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            BucketReferer result = ResponseParsers.parseGetBucketReferer(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<RefererConfiguration>\n" +
                "  <AllowEmptyReferer>true</AllowEmptyReferer>\n" +
                "</RefererConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            BucketReferer result = ResponseParsers.parseGetBucketReferer(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            BucketReferer result = ResponseParsers.parseGetBucketReferer(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            BucketReferer result = ResponseParsers.parseGetBucketReferer(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }
    @Test
    public void testparseUploadPartCopy() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseUploadPartCopy(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseUploadPartCopy(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseListBucket() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListAllMyBucketsResult>\n" +
                "  <MaxKeys></MaxKeys>\n" +
                "  <IsTruncated></IsTruncated>\n" +
                "  <AllowEmptyReferer>true</AllowEmptyReferer>\n" +
                "  <Owner>\n" +
                "    <ID></ID>\n" +
                "    <DisplayName></DisplayName>\n" +
                "  </Owner>\n" +
                "</ListAllMyBucketsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListBucket(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<ListAllMyBucketsResult>\n" +
                "  <MaxKeys>10</MaxKeys>\n" +
                "  <IsTruncated>true</IsTruncated>\n" +
                "  <AllowEmptyReferer>true</AllowEmptyReferer>\n" +
                "  <Owner>\n" +
                "    <ID></ID>\n" +
                "    <DisplayName></DisplayName>\n" +
                "  </Owner>\n" +
                "</ListAllMyBucketsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListBucket(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListBucket(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseListBucket(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        //Parser Region
        respBody = "" +
                "<ListAllMyBucketsResult>\n" +
                "  <MaxKeys>10</MaxKeys>\n" +
                "  <IsTruncated>true</IsTruncated>\n" +
                "  <AllowEmptyReferer>true</AllowEmptyReferer>\n" +
                "  <Owner>\n" +
                "    <ID>id</ID>\n" +
                "    <DisplayName>name</DisplayName>\n" +
                "  </Owner>\n" +
                "  <Buckets>\n" +
                "    <Bucket>\n" +
                "      <Comment></Comment>\n" +
                "      <CreationDate>2020-03-12T02:18:25.000Z</CreationDate>\n" +
                "      <ExtranetEndpoint>oss-cn-hangzhou.aliyuncs.com</ExtranetEndpoint>\n" +
                "      <IntranetEndpoint>oss-cn-hangzhou-internal.aliyuncs.com</IntranetEndpoint>\n" +
                "      <Location>oss-cn-hangzhou</Location>\n" +
                "      <Name>oss-bucket</Name>\n" +
                "      <Region>cn-hangzhou</Region>\n" +
                "      <StorageClass>Standard</StorageClass>\n" +
                "    </Bucket>\n" +
                "  </Buckets>\n" +
                "</ListAllMyBucketsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            BucketList result = ResponseParsers.parseListBucket(instream);
            Assert.assertEquals(result.getBucketList().size(), 1);
            Assert.assertEquals(result.getBucketList().get(0).getLocation(), "oss-cn-hangzhou");
            Assert.assertEquals(result.getBucketList().get(0).getRegion(), "cn-hangzhou");
            Assert.assertEquals(result.getBucketList().get(0).getHnsStatus(), null);
            Assert.assertEquals(result.getBucketList().get(0).getResourceGroupId(), null);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        //Parser Region
        respBody = "" +
                "<ListAllMyBucketsResult>\n" +
                "  <MaxKeys>10</MaxKeys>\n" +
                "  <IsTruncated>true</IsTruncated>\n" +
                "  <AllowEmptyReferer>true</AllowEmptyReferer>\n" +
                "  <Owner>\n" +
                "    <ID>id</ID>\n" +
                "    <DisplayName>name</DisplayName>\n" +
                "  </Owner>\n" +
                "  <Buckets>\n" +
                "    <Bucket>\n" +
                "      <Comment></Comment>\n" +
                "      <CreationDate>2020-03-12T02:18:25.000Z</CreationDate>\n" +
                "      <ExtranetEndpoint>oss-cn-hangzhou.aliyuncs.com</ExtranetEndpoint>\n" +
                "      <IntranetEndpoint>oss-cn-hangzhou-internal.aliyuncs.com</IntranetEndpoint>\n" +
                "      <Location>oss-cn-hangzhou</Location>\n" +
                "      <Name>oss-bucket</Name>\n" +
                "      <HierarchicalNamespace>status</HierarchicalNamespace>\n" +
                "      <ResourceGroupId>xxx-id-123</ResourceGroupId>\n" +
                "      <StorageClass>Standard</StorageClass>\n" +
                "    </Bucket>\n" +
                "  </Buckets>\n" +
                "</ListAllMyBucketsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            BucketList result = ResponseParsers.parseListBucket(instream);
            Assert.assertEquals(result.getBucketList().size(), 1);
            Assert.assertEquals(result.getBucketList().get(0).getLocation(), "oss-cn-hangzhou");
            Assert.assertEquals(result.getBucketList().get(0).getRegion(), null);
            Assert.assertEquals(result.getBucketList().get(0).getHnsStatus(), "status");
            Assert.assertEquals(result.getBucketList().get(0).getResourceGroupId(), "xxx-id-123");
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListBucket(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseListImageStyle() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListImageStyle(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseListImageStyle(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketLocation() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketLocation(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketLocation(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseBucketMetadata() {

        try {
            ResponseParsers.parseBucketMetadata(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseSimplifiedObjectMeta() {

        try {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Last-Modified", "invalid");
            ResponseParsers.parseSimplifiedObjectMeta(headers);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseSimplifiedObjectMeta(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseSymbolicLink() {

        try {
            ResponseMessage response = new ResponseMessage(null);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Last-Modified", "invalid");
            response.setHeaders(headers);
            ResponseParsers.parseSymbolicLink(response);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setHeaders(null);
            ResponseParsers.parseSymbolicLink(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseInitiateMultipartUpload() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<InitiateMultipartUploadResult>\n" +
                "</InitiateMultipartUploadResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseInitiateMultipartUpload(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseInitiateMultipartUpload(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseInitiateMultipartUpload(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseListMultipartUploads() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListMultipartUploadsResult>\n" +
                "  <Bucket></Bucket>\n" +
                "  <MaxUploads>100</MaxUploads>\n" +
                "  <IsTruncated>true</IsTruncated>\n" +
                "  <KeyMarker>marker</KeyMarker>\n" +
                "  <UploadIdMarker>idmarker</UploadIdMarker>\n" +
                "  <Upload>\n" +
                "    <Key></Key>\n" +
                "  </Upload>\n" +
                "  <CommonPrefixes>\n" +
                "    <Prefix></Prefix>\n" +
                "  </CommonPrefixes>\n" +
                "</ListMultipartUploadsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListMultipartUploads(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<ListMultipartUploadsResult>\n" +
                "  <Bucket></Bucket>\n" +
                "  <MaxUploads>100</MaxUploads>\n" +
                "  <IsTruncated>true</IsTruncated>\n" +
                "</ListMultipartUploadsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListMultipartUploads(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListMultipartUploads(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseListMultipartUploads(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseListParts() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListPartsResult>\n" +
                "<StorageClass>Standard</StorageClass>\n" +
                "<MaxParts>100</MaxParts>\n" +
                "<IsTruncated>true</IsTruncated>\n" +
                "<PartNumberMarker></PartNumberMarker>\n" +
                "<PartNumberMarker></PartNumberMarker>\n" +
                "</ListPartsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListParts(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListParts(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseListParts(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseCompleteMultipartUpload() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseCompleteMultipartUpload(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseCompleteMultipartUpload(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseBucketLogging() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<BucketLogging>\n" +
                "</BucketLogging>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseBucketLogging(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }


        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseBucketLogging(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseBucketLogging(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }


    @Test
    public void testparseGetBucketImageProcessConf() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<BucketImageProcessConf>\n" +
                "<SourceFileProtect>Enabled</SourceFileProtect>\n" +
                "</BucketImageProcessConf>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketImageProcessConf(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }


        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketImageProcessConf(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketImageProcessConf(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseBucketWebsite() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<WebsiteConfiguration>\n" +
                "  <RoutingRules>\n" +
                "    <RoutingRule>\n" +
                "      <RuleNumber>1</RuleNumber>\n" +
                "      <Condition>\n" +
                "        <KeyPrefixEquals>prefix</KeyPrefixEquals>\n" +
                "      </Condition>\n" +
                "      <Redirect>\n" +
                "      </Redirect>\n" +
                "      <HostName>HostName</HostName>\n" +
                "      <ReplaceKeyPrefixWith>prefix</ReplaceKeyPrefixWith>\n" +
                "      <ReplaceKeyWith>prefix</ReplaceKeyWith>\n" +
                "      <MirrorURL>prefix</MirrorURL>\n" +
                "      <MirrorURLSlave>prefix</MirrorURLSlave>\n" +
                "      <MirrorURLProbe>prefix</MirrorURLProbe>\n" +
                "      <MirrorPassQueryString>prefix</MirrorPassQueryString>\n" +
                "      <MirrorPassOriginalSlashes>prefix</MirrorPassOriginalSlashes>\n" +
                "    </RoutingRule>\n" +
                "    <RoutingRule>\n" +
                "      <RuleNumber>2</RuleNumber>\n" +
                "      <Redirect>\n" +
                "      </Redirect>\n" +
                "      <HostName>HostName</HostName>\n" +
                "      <ReplaceKeyPrefixWith>prefix</ReplaceKeyPrefixWith>\n" +
                "      <ReplaceKeyWith>prefix</ReplaceKeyWith>\n" +
                "      <MirrorURL>prefix</MirrorURL>\n" +
                "      <MirrorURLSlave>prefix</MirrorURLSlave>\n" +
                "      <MirrorURLProbe>prefix</MirrorURLProbe>\n" +
                "      <MirrorPassQueryString>prefix</MirrorPassQueryString>\n" +
                "      <MirrorPassOriginalSlashes>prefix</MirrorPassOriginalSlashes>\n" +
                "    </RoutingRule>\n" +
                "  </RoutingRules>\n" +
                "</WebsiteConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseBucketWebsite(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<WebsiteConfiguration>\n" +
                "  <RoutingRules>\n" +
                "    <RoutingRule>\n" +
                "      <RuleNumber>2</RuleNumber>\n" +
                "      <Redirect>\n" +
                "      </Redirect>\n" +
                "      <HostName>HostName</HostName>\n" +
                "      <ReplaceKeyPrefixWith>prefix</ReplaceKeyPrefixWith>\n" +
                "      <ReplaceKeyWith>prefix</ReplaceKeyWith>\n" +
                "      <MirrorURL>prefix</MirrorURL>\n" +
                "      <MirrorURLSlave>prefix</MirrorURLSlave>\n" +
                "      <MirrorURLProbe>prefix</MirrorURLProbe>\n" +
                "      <MirrorPassQueryString>prefix</MirrorPassQueryString>\n" +
                "      <MirrorPassOriginalSlashes>prefix</MirrorPassOriginalSlashes>\n" +
                "    </RoutingRule>\n" +
                "  </RoutingRules>\n" +
                "</WebsiteConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseBucketWebsite(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseBucketWebsite(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseBucketWebsite(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseCopyObjectResult() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseCopyObjectResult(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseCopyObjectResult(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseDeleteObjectsResult() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<DeleteObjectsResult>\n" +
                "  <EncodingType>invalid</EncodingType>\n" +
                "</DeleteObjectsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseDeleteObjectsResult(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<DeleteObjectsResult>\n" +
                "  <EncodingType></EncodingType>\n" +
                "</DeleteObjectsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseDeleteObjectsResult(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseDeleteObjectsResult(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseDeleteObjectsResult(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseDeleteVersionsResult() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<DeleteObjectsResult>\n" +
                "  <Deleted>\n" +
                "    <Key>key</Key>\n" +
                "  </Deleted>\n" +
                "</DeleteObjectsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseDeleteVersionsResult(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }


        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseDeleteVersionsResult(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseDeleteVersionsResult(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseListBucketCORS() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListBucketCORS(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseListBucketCORS(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketTagging() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<BucketTagging>\n" +
                "  <TagSet>\n" +
                "    <Tag></Tag>\n" +
                "  </TagSet>\n" +
                "</BucketTagging>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketTagging(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketTagging(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketTagging(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketInfo() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketInfo(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketInfo(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketStat() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketStat(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketStat(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testParseGetBucketStat() {
        InputStream instream = null;

        String respBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<BucketStat>\n" +
                "  <Storage>1600</Storage>\n" +
                "  <ObjectCount>230</ObjectCount>\n" +
                "  <MultipartUploadCount>40</MultipartUploadCount>\n" +
                "  <LiveChannelCount>4</LiveChannelCount>\n" +
                "  <LastModifiedTime>0</LastModifiedTime>\n" +
                "  <StandardStorage>430</StandardStorage>\n" +
                "  <StandardObjectCount>66</StandardObjectCount>\n" +
                "  <InfrequentAccessStorage>2359296</InfrequentAccessStorage>\n" +
                "  <InfrequentAccessRealStorage>360</InfrequentAccessRealStorage>\n" +
                "  <InfrequentAccessObjectCount>54</InfrequentAccessObjectCount>\n" +
                "  <ArchiveStorage>2949120</ArchiveStorage>\n" +
                "  <ArchiveRealStorage>450</ArchiveRealStorage>\n" +
                "  <ArchiveObjectCount>74</ArchiveObjectCount>\n" +
                "  <ColdArchiveStorage>2359296</ColdArchiveStorage>\n" +
                "  <ColdArchiveRealStorage>360</ColdArchiveRealStorage>\n" +
                "  <ColdArchiveObjectCount>36</ColdArchiveObjectCount>\n" +
                "</BucketStat>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
            BucketStat result = ResponseParsers.parseGetBucketStat(instream);
            Assert.assertEquals(Long.valueOf(1600), result.getStorageSize());
            Assert.assertEquals(Long.valueOf(230), result.getObjectCount());
            Assert.assertEquals(Long.valueOf(40), result.getMultipartUploadCount());
            Assert.assertEquals(Long.valueOf(4), result.getLiveChannelCount());
            Assert.assertEquals(Long.valueOf(0), result.getLastModifiedTime());
            Assert.assertEquals(Long.valueOf(430), result.getStandardStorage());
            Assert.assertEquals(Long.valueOf(66), result.getStandardObjectCount());
            Assert.assertEquals(Long.valueOf(2359296), result.getInfrequentAccessStorage());
            Assert.assertEquals(Long.valueOf(360), result.getInfrequentAccessRealStorage());
            Assert.assertEquals(Long.valueOf(54), result.getInfrequentAccessObjectCount());
            Assert.assertEquals(Long.valueOf(2949120), result.getArchiveStorage());
            Assert.assertEquals(Long.valueOf(450), result.getArchiveRealStorage());
            Assert.assertEquals(Long.valueOf(74), result.getArchiveObjectCount());
            Assert.assertEquals(Long.valueOf(2359296), result.getColdArchiveStorage());
            Assert.assertEquals(Long.valueOf(360), result.getColdArchiveRealStorage());
            Assert.assertEquals(Long.valueOf(36), result.getColdArchiveObjectCount());
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testParseGetBucketStatReturnEmpty() {
        InputStream instream = null;

        String respBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<BucketStat>\n" +
                "  <Storage>1600</Storage>\n" +
                "  <ObjectCount>230</ObjectCount>\n" +
                "  <MultipartUploadCount>40</MultipartUploadCount>\n" +
                "  <LiveChannelCount></LiveChannelCount>\n" +
                "</BucketStat>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
            BucketStat result = ResponseParsers.parseGetBucketStat(instream);
            Assert.assertEquals(Long.valueOf(1600), result.getStorageSize());
            Assert.assertEquals(Long.valueOf(230), result.getObjectCount());
            Assert.assertEquals(Long.valueOf(40), result.getMultipartUploadCount());
            Assert.assertEquals(Long.valueOf(0), result.getLiveChannelCount());
            Assert.assertEquals(Long.valueOf(0), result.getLastModifiedTime());
            Assert.assertEquals(Long.valueOf(0), result.getStandardStorage());
            Assert.assertEquals(Long.valueOf(0), result.getStandardObjectCount());
            Assert.assertEquals(Long.valueOf(0), result.getInfrequentAccessStorage());
            Assert.assertEquals(Long.valueOf(0), result.getInfrequentAccessRealStorage());
            Assert.assertEquals(Long.valueOf(0), result.getInfrequentAccessObjectCount());
            Assert.assertEquals(Long.valueOf(0), result.getArchiveStorage());
            Assert.assertEquals(Long.valueOf(0), result.getArchiveRealStorage());
            Assert.assertEquals(Long.valueOf(0), result.getArchiveObjectCount());
            Assert.assertEquals(Long.valueOf(0), result.getColdArchiveStorage());
            Assert.assertEquals(Long.valueOf(0), result.getColdArchiveRealStorage());
            Assert.assertEquals(Long.valueOf(0), result.getColdArchiveObjectCount());
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseCreateLiveChannel() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseCreateLiveChannel(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseCreateLiveChannel(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetLiveChannelInfo() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetLiveChannelInfo(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetLiveChannelInfo(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetLiveChannelStat() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetLiveChannelStat(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetLiveChannelStat(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetLiveChannelHistory() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetLiveChannelHistory(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetLiveChannelHistory(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseListLiveChannels() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListLiveChannelsResult>\n" +
                "  <IsTruncated>true</IsTruncated>\n" +
                "</ListLiveChannelsResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListLiveChannels(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListLiveChannels(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseListLiveChannels(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetUserQos() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<UserQos>\n" +
                "</UserQos>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetUserQos(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetUserQos(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetUserQos(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketVersioning() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketVersioning(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketVersioning(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketEncryption() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketEncryption(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketEncryption(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketPolicy() {

        try {
            ResponseParsers.parseGetBucketPolicy(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketRequestPayment() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<RequestPayment>\n" +
                "</RequestPayment>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketRequestPayment(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketRequestPayment(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketRequestPayment(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetUserQosInfo() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetUserQosInfo(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetUserQosInfo(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketQosInfo() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketQosInfo(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketQosInfo(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketLifecycle() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<LifecycleConfiguration>\n" +
                "  <Rule>\n" +
                "    <Tag>\n" +
                "    </Tag>\n" +
                "  </Rule>\n" +
                "  <Rule>\n" +
                "  </Rule>\n" +
                "  <Rule>\n" +
                "    <Expiration>\n" +
                "    </Expiration>\n" +
                "    <Transition>\n" +
                "      <Days>1</Days>\n" +
                "    </Transition>\n" +
                "    <NoncurrentVersionExpiration>\n" +
                "    </NoncurrentVersionExpiration>\n" +
                "    <NoncurrentVersionTransition>\n" +
                "    </NoncurrentVersionTransition>\n" +
                "  </Rule>\n" +
                "</LifecycleConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketLifecycle(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }


        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketLifecycle(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketLifecycle(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<LifecycleConfiguration>\n" +
                "  <Rule>\n" +
                "    <ID>RuleID</ID>\n" +
                "    <Prefix>Prefix</Prefix>\n" +
                "    <Status>Enabled</Status>\n" +
                "    <Filter>\n" +
                "      <ObjectSizeGreaterThan>500</ObjectSizeGreaterThan>\n" +
                "      <ObjectSizeLessThan>64000</ObjectSizeLessThan>\n" +
                "      <Not>\n" +
                "        <Prefix>abc/not1/</Prefix>\n" +
                "        <Tag>\n" +
                "          <Key>notkey1</Key>\n" +
                "          <Value>notvalue1</Value>\n" +
                "        </Tag>\n" +
                "      </Not>\n" +
                "      <Not>\n" +
                "        <Prefix>abc/not2/</Prefix>\n" +
                "        <Tag>\n" +
                "          <Key>notkey2</Key>\n" +
                "          <Value>notvalue2</Value>\n" +
                "        </Tag>\n" +
                "      </Not>\n" +
                "    </Filter>" +
                "  </Rule>\n" +
                "</LifecycleConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        List<LifecycleRule> rules = null;
        try {
            rules = ResponseParsers.parseGetBucketLifecycle(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse response body fail!");
        }

        Assert.assertEquals("abc/not1/", rules.get(0).getFilter().getNotList().get(0).getPrefix());
        Assert.assertEquals("notkey1", rules.get(0).getFilter().getNotList().get(0).getTag().getKey());
        Assert.assertEquals("notvalue1", rules.get(0).getFilter().getNotList().get(0).getTag().getValue());
        Assert.assertEquals("abc/not2/", rules.get(0).getFilter().getNotList().get(1).getPrefix());
        Assert.assertEquals("notkey2", rules.get(0).getFilter().getNotList().get(1).getTag().getKey());
        Assert.assertEquals("notvalue2", rules.get(0).getFilter().getNotList().get(1).getTag().getValue());
    }

    @Test
    public void testparseSetAsyncFetchTaskResult() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseSetAsyncFetchTaskResult(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseSetAsyncFetchTaskResult(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetAsyncFetchTaskResult() {
        InputStream instream = null;
        String respBody;

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetAsyncFetchTaskResult(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetAsyncFetchTaskResult(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testCreateVpcipResultResponseParser() {
        InputStream instream = null;
        String respBody;

        respBody = "<Vpcip></Vpcip>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.CreateVpcipResultResponseParser parser = new ResponseParsers.CreateVpcipResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.CreateVpcipResultResponseParser parser = new ResponseParsers.CreateVpcipResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(null);
            ResponseParsers.CreateVpcipResultResponseParser parser = new ResponseParsers.CreateVpcipResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testListVpcipResultResponseParser() {
        InputStream instream = null;
        String respBody;

        respBody = "<Vpcip></Vpcip>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.ListVpcipResultResponseParser parser = new ResponseParsers.ListVpcipResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.ListVpcipResultResponseParser parser = new ResponseParsers.ListVpcipResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(null);
            ResponseParsers.ListVpcipResultResponseParser parser = new ResponseParsers.ListVpcipResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testListVpcPolicyResultResponseParser() {
        InputStream instream = null;
        String respBody;

        respBody = "<Vpcip></Vpcip>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.ListVpcPolicyResultResponseParser parser = new ResponseParsers.ListVpcPolicyResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.ListVpcPolicyResultResponseParser parser = new ResponseParsers.ListVpcPolicyResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(null);
            ResponseParsers.ListVpcPolicyResultResponseParser parser = new ResponseParsers.ListVpcPolicyResultResponseParser();
            parser.parse(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseGetBucketInventoryConfig() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<InventoryConfiguration>\n" +
                "</InventoryConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketInventoryConfig(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<InventoryConfiguration>\n" +
                "  <Filter>\n" +
                "  </Filter>\n" +
                "  <Schedule>\n" +
                "  </Schedule>\n" +
                "  <Destination>\n" +
                "  </Destination>\n" +
                "</InventoryConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketInventoryConfig(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<InventoryConfiguration>\n" +
                "  <Filter>\n" +
                "  </Filter>\n" +
                "  <Schedule>\n" +
                "  </Schedule>\n" +
                "  <Destination>\n" +
                "    <OSSBucketDestination>\n" +
                "    </OSSBucketDestination>\n" +
                "  </Destination>\n" +
                "</InventoryConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketInventoryConfig(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<InventoryConfiguration>\n" +
                "  <Filter>\n" +
                "  </Filter>\n" +
                "  <Schedule>\n" +
                "  </Schedule>\n" +
                "  <Destination>\n" +
                "    <OSSBucketDestination>\n" +
                "      <Encryption>\n" +
                "      </Encryption>\n" +
                "    </OSSBucketDestination>\n" +
                "  </Destination>\n" +
                "</InventoryConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketInventoryConfig(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseGetBucketInventoryConfig(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseGetBucketInventoryConfig(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testparseListBucketInventoryConfigurations() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListInventoryConfiguration>\n" +
                "</ListInventoryConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListBucketInventoryConfigurations(instream);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseParsers.parseListBucketInventoryConfigurations(instream);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseParsers.parseListBucketInventoryConfigurations(null);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testGetBucketInventoryConfigWithFilter() {
        String respBody = "" +
                "<InventoryConfiguration>\n" +
                "     <Id>report1</Id>\n" +
                "     <IsEnabled>true</IsEnabled>\n" +
                "     <Filter>\n" +
                "        <Prefix>filterPrefix/</Prefix>\n" +
                "       \t<LastModifyBeginTimeStamp>1637883649</LastModifyBeginTimeStamp>\n" +
                "     \t  <LastModifyEndTimeStamp>1638347592</LastModifyEndTimeStamp>\n" +
                "     \t  <LowerSizeBound>1024</LowerSizeBound>\n" +
                "     \t  <UpperSizeBound>5365000000000</UpperSizeBound>\n" +
                "        <StorageClass>Standard,IA</StorageClass>\n" +
                "     </Filter>\n" +
                "     <Destination>\n" +
                "        <OSSBucketDestination>\n" +
                "           <Format>CSV</Format>\n" +
                "           <AccountId>1000000000000000</AccountId>\n" +
                "           <RoleArn>acs:ram::1000000000000000:role/AliyunOSSRole</RoleArn>\n" +
                "           <Bucket>acs:oss:::destination-bucket</Bucket>\n" +
                "           <Prefix>prefix1</Prefix>\n" +
                "           <Encryption>\n" +
                "              <SSE-KMS>\n" +
                "                 <KeyId>keyId</KeyId>\n" +
                "              </SSE-KMS>\n" +
                "           </Encryption>\n" +
                "        </OSSBucketDestination>\n" +
                "     </Destination>\n" +
                "     <Schedule>\n" +
                "        <Frequency>Daily</Frequency>\n" +
                "     </Schedule>\n" +
                "     <IncludedObjectVersions>All</IncludedObjectVersions>\n" +
                "     <OptionalFields>\n" +
                "        <Field>Size</Field>\n" +
                "        <Field>LastModifiedDate</Field>\n" +
                "        <Field>ETag</Field>\n" +
                "        <Field>StorageClass</Field>\n" +
                "        <Field>IsMultipartUploaded</Field>\n" +
                "        <Field>EncryptionStatus</Field>\n" +
                "     </OptionalFields>\n" +
                "  </InventoryConfiguration>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        GetBucketInventoryConfigurationResult result = null;

        try {
            result = ResponseParsers.parseGetBucketInventoryConfig(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("report1", result.getInventoryConfiguration().getInventoryId());
        Assert.assertEquals("filterPrefix/", result.getInventoryConfiguration().getInventoryFilter().getPrefix());
        Assert.assertEquals(Long.valueOf(1637883649), result.getInventoryConfiguration().getInventoryFilter().getLastModifyBeginTimeStamp());
        Assert.assertEquals(Long.valueOf(1638347592), result.getInventoryConfiguration().getInventoryFilter().getLastModifyEndTimeStamp());
        Assert.assertEquals(Long.valueOf(1024L), result.getInventoryConfiguration().getInventoryFilter().getLowerSizeBound());
        Assert.assertEquals(Long.valueOf(5365000000000L), result.getInventoryConfiguration().getInventoryFilter().getUpperSizeBound());
        Assert.assertEquals("Standard,IA", result.getInventoryConfiguration().getInventoryFilter().getStorageClass());
    }

    @Test
    public void testGetBucketEncryptionResponseParser() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ServerSideEncryptionRule>\n" +
                "  <ApplyServerSideEncryptionByDefault>\n" +
                "    <SSEAlgorithm>KMS</SSEAlgorithm>\n" +
                "    <KMSMasterKeyID>id</KMSMasterKeyID>\n" +
                "    <KMSDataEncryption>SM4</KMSDataEncryption>\n" +
                "  </ApplyServerSideEncryptionByDefault>\n" +
                "</ServerSideEncryptionRule>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketEncryptionResponseParser parser = new ResponseParsers.GetBucketEncryptionResponseParser();
            ServerSideEncryptionConfiguration config = parser.parse(response);
            Assert.assertEquals(config.getApplyServerSideEncryptionByDefault().getSSEAlgorithm(), "KMS");
            Assert.assertEquals(config.getApplyServerSideEncryptionByDefault().getKMSMasterKeyID(), "id");
            Assert.assertEquals(config.getApplyServerSideEncryptionByDefault().getKMSDataEncryption(), "SM4");
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "" +
                "<ServerSideEncryptionRule>\n" +
                "  <ApplyServerSideEncryptionByDefault>\n" +
                "    <SSEAlgorithm>KMS</SSEAlgorithm>\n" +
                "    <KMSMasterKeyID>id</KMSMasterKeyID>\n" +
                "  </ApplyServerSideEncryptionByDefault>\n" +
                "</ServerSideEncryptionRule>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketEncryptionResponseParser parser = new ResponseParsers.GetBucketEncryptionResponseParser();
            ServerSideEncryptionConfiguration config = parser.parse(response);
            Assert.assertEquals(config.getApplyServerSideEncryptionByDefault().getSSEAlgorithm(), "KMS");
            Assert.assertEquals(config.getApplyServerSideEncryptionByDefault().getKMSMasterKeyID(), "id");
            Assert.assertEquals(config.getApplyServerSideEncryptionByDefault().getKMSDataEncryption(), null);
            Assert.assertTrue(true);
        } catch (ResponseParseException e) {
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        respBody = "invalid";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketEncryptionResponseParser parser = new ResponseParsers.GetBucketEncryptionResponseParser();
            parser.parse(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(null);
            ResponseParsers.GetBucketEncryptionResponseParser parser = new ResponseParsers.GetBucketEncryptionResponseParser();
            parser.parse(response);
            Assert.assertTrue(false);
        } catch (ResponseParseException e) {
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testParseDeleteDirectory() {
        String respBody = "" +
                "<DeleteDirectoryResult>\n" +
                "    <DirectoryName>a/b/c</DirectoryName>\n" +
                "    <DeleteNumber>1</DeleteNumber>\n" +
                "</DeleteDirectoryResult>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        DeleteDirectoryResult result = null;
        try {
            result = ResponseParsers.parseDeleteDirectoryResult(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals("a/b/c", result.getDirectoryName());
        Assert.assertEquals(1, result.getDeleteNumber());
        Assert.assertNull(result.getNextDeleteToken());

        respBody = "" +
                "<DeleteDirectoryResult>\n" +
                "    <DirectoryName>a/b/c</DirectoryName>\n" +
                "    <DeleteNumber>1</DeleteNumber>\n" +
                "    <NextDeleteToken>CgJiYw--</NextDeleteToken>\n" +
                "</DeleteDirectoryResult>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        result = null;
        try {
            result = ResponseParsers.parseDeleteDirectoryResult(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals("a/b/c", result.getDirectoryName());
        Assert.assertEquals(1, result.getDeleteNumber());
        Assert.assertEquals("CgJiYw--", result.getNextDeleteToken());
    }

    @Test
    public void testParseBucketResourceGroup() {
        String respBody = "" +
                "<BucketResourceGroupConfiguration>\n" +
                "    <ResourceGroupId>xxx-id-123</ResourceGroupId>\n" +
                "</BucketResourceGroupConfiguration>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        GetBucketResourceGroupResult result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketResourceGroupResponseParser parser = new ResponseParsers.GetBucketResourceGroupResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals("xxx-id-123", result.getResourceGroupId());


        respBody = "" +
                "<BucketResourceGroupConfiguration>\n" +
                "</BucketResourceGroupConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketResourceGroupResponseParser parser = new ResponseParsers.GetBucketResourceGroupResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals(null, result.getResourceGroupId());
    }

    @Test
    public void testGetBucketTransferAccelerationResponseParser() {
        String respBody = "" +
                "<TransferAccelerationConfiguration>\n" +
                "    <Enabled>true</Enabled>\n" +
                "</TransferAccelerationConfiguration>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        TransferAcceleration result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketTransferAccelerationResponseParser parser = new ResponseParsers.GetBucketTransferAccelerationResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals(true, result.isEnabled());


        respBody = "" +
                "<TransferAccelerationConfiguration>\n" +
                "    <Enabled>false</Enabled>\n" +
                "</TransferAccelerationConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketTransferAccelerationResponseParser parser = new ResponseParsers.GetBucketTransferAccelerationResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }
        Assert.assertEquals(false, result.isEnabled());



        respBody = "" +
                "<TransferAccelerationConfiguration>\n" +
                "</TransferAccelerationConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketTransferAccelerationResponseParser parser = new ResponseParsers.GetBucketTransferAccelerationResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }
        Assert.assertEquals(false, result.isEnabled());
    }


    @Test
    public void testGetBucketAccessMonitorResponseParser() {
        String respBody = "" +
                "<AccessMonitorConfiguration>\n" +
                "    <Status>Enabled</Status>\n" +
                "</AccessMonitorConfiguration>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        AccessMonitor result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketAccessMonitorResponseParser parser = new ResponseParsers.GetBucketAccessMonitorResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals("Enabled", result.getStatus());


        respBody = "" +
                "<AccessMonitorConfiguration>\n" +
                "    <Status>Disabled</Status>\n" +
                "</AccessMonitorConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketAccessMonitorResponseParser parser = new ResponseParsers.GetBucketAccessMonitorResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }
        Assert.assertEquals("Disabled", result.getStatus());



        respBody = "" +
                "<AccessMonitorConfiguration>\n" +
                "</AccessMonitorConfiguration>";

        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetBucketAccessMonitorResponseParser parser = new ResponseParsers.GetBucketAccessMonitorResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }
        Assert.assertEquals("Disabled", result.getStatus());
    }
    @Test
    public void testParseGetBucketInfoWithAccessMonitor() {
        String respBody = "" +
                "<BucketInfo>\n" +
                "  <Bucket>\n" +
                "           <CreationDate>2013-07-31T10:56:21.000Z</CreationDate>\n" +
                "            <ExtranetEndpoint>oss-cn-hangzhou.aliyuncs.com</ExtranetEndpoint>\n" +
                "            <IntranetEndpoint>oss-cn-hangzhou-internal.aliyuncs.com</IntranetEndpoint>\n" +
                "            <Location>oss-cn-hangzhou</Location>\n" +
                "            <Name>oss-example</Name>\n" +
                "            <AccessMonitor>Enabled</AccessMonitor>\n" +
                "            <Owner>\n" +
                "              <DisplayName>username</DisplayName>\n" +
                "              <ID>27183473914****</ID>\n" +
                "            </Owner>\n" +
                "            <AccessControlList>\n" +
                "              <Grant>private</Grant>\n" +
                "            </AccessControlList>\n" +
                "            <Comment>test</Comment>\n" +
                "            <DataRedundancyType>LRS</DataRedundancyType>\n" +
                "          </Bucket>\n" +
                " </BucketInfo>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        BucketInfo result = null;
        try {
            result = ResponseParsers.parseGetBucketInfo(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("test", result.getComment());
        Assert.assertEquals(DataRedundancyType.LRS, result.getDataRedundancyType());
        Assert.assertEquals(CannedAccessControlList.Private, result.getCannedACL());
        Assert.assertEquals("oss-cn-hangzhou", result.getBucket().getLocation());
        Assert.assertEquals("oss-example", result.getBucket().getName());
        Assert.assertEquals(null, result.getBucket().getHnsStatus());
        Assert.assertEquals(null, result.getBucket().getResourceGroupId());
        Assert.assertEquals("Enabled", result.getBucket().getAccessMonitor());

        respBody = "" +
                "<BucketInfo>\n" +
                "  <Bucket>\n" +
                "           <CreationDate>2013-07-31T10:56:21.000Z</CreationDate>\n" +
                "            <ExtranetEndpoint>oss-cn-hangzhou.aliyuncs.com</ExtranetEndpoint>\n" +
                "            <HierarchicalNamespace>Enabled</HierarchicalNamespace>\n" +
                "            <IntranetEndpoint>oss-cn-hangzhou-internal.aliyuncs.com</IntranetEndpoint>\n" +
                "            <Location>oss-cn-hangzhou</Location>\n" +
                "            <ResourceGroupId>xxx-id-123</ResourceGroupId>\n" +
                "            <Name>oss-example</Name>\n" +
                "            <AccessMonitor>Disabled</AccessMonitor>\n" +
                "            <Owner>\n" +
                "              <DisplayName>username</DisplayName>\n" +
                "              <ID>27183473914****</ID>\n" +
                "            </Owner>\n" +
                "            <AccessControlList>\n" +
                "              <Grant>private</Grant>\n" +
                "            </AccessControlList>\n" +
                "            <Comment>test</Comment>\n" +
                "            <DataRedundancyType>LRS</DataRedundancyType>\n" +
                "          </Bucket>\n" +
                " </BucketInfo>";

        instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        result = null;
        try {
            result = ResponseParsers.parseGetBucketInfo(instream);
        } catch (ResponseParseException e) {
            Assert.fail("parse bucket replication response body fail!");
        }

        Assert.assertEquals("test", result.getComment());
        Assert.assertEquals(DataRedundancyType.LRS, result.getDataRedundancyType());
        Assert.assertEquals(CannedAccessControlList.Private, result.getCannedACL());
        Assert.assertEquals("oss-cn-hangzhou", result.getBucket().getLocation());
        Assert.assertEquals("oss-example", result.getBucket().getName());
        Assert.assertEquals(HnsStatus.Enabled.toString(), result.getBucket().getHnsStatus());
        Assert.assertEquals("xxx-id-123", result.getBucket().getResourceGroupId());
        Assert.assertEquals("Disabled", result.getBucket().getAccessMonitor());
    }

    @Test
    public void testGetMetaQueryStatusResponseParser() {
        String respBody = "" +
                "<MetaQuery>\n" +
                "  <State>Running</State>\n" +
                "  <Phase>FullScanning</Phase>\n" +
                "  <CreateTime>2021-08-02T10:49:17.289372919+08:00</CreateTime>\n" +
                "  <UpdateTime>2021-08-02T10:49:17.289372919+08:00</UpdateTime>\n" +
                "</MetaQuery>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        GetMetaQueryStatusResult result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.GetMetaQueryStatusResponseParser parser = new ResponseParsers.GetMetaQueryStatusResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals("Running", result.getState());
        Assert.assertEquals("FullScanning", result.getPhase());
        Assert.assertEquals("2021-08-02T10:49:17.289372919+08:00", result.getCreateTime());
        Assert.assertEquals("2021-08-02T10:49:17.289372919+08:00", result.getUpdateTime());
    }

    @Test
    public void testDoMetaQueryResponseParser() {
        String respBody = "" +
                "<MetaQuery>\n" +
                "    <NextToken>MTIzNDU2NzgnV9zYW1wbGVvYmplY3QxLmpwZw==</NextToken>\n" +
                "    <Files>    \n" +
                "        <File>     \n" +
                "            <Filename>exampleobject.txt</Filename>\n" +
                "            <Size>120</Size>\n" +
                "            <FileModifiedTime>2021-06-29T14:50:13.011643661+08:00</FileModifiedTime>\n" +
                "            <FileCreateTime>2021-06-28T14:50:13.011643661+08:00</FileCreateTime>\n" +
                "            <FileAccessTime>2021-06-27T14:50:13.011643661+08:00</FileAccessTime>\n" +
                "            <OSSObjectType>Normal</OSSObjectType>\n" +
                "            <OSSStorageClass>Standard</OSSStorageClass>\n" +
                "            <ObjectACL>defalut</ObjectACL>\n" +
                "            <ETag>fba9dede5f27731c9771645a3986****</ETag>\n" +
                "            <OSSCRC64>4858A48BD1466884</OSSCRC64>\n" +
                "            <OSSTaggingCount>2</OSSTaggingCount>\n" +
                "            <OSSTagging>\n" +
                "                <Tagging>\n" +
                "                    <Key>owner</Key>\n" +
                "                    <Value>John</Value>\n" +
                "                </Tagging>\n" +
                "                <Tagging>\n" +
                "                    <Key>type</Key>\n" +
                "                    <Value>document</Value>\n" +
                "                </Tagging>\n" +
                "            </OSSTagging>\n" +
                "            <OSSUserMeta>\n" +
                "                <UserMeta>\n" +
                "                    <Key>x-oss-meta-location</Key>\n" +
                "                    <Value>hangzhou</Value>\n" +
                "                </UserMeta>\n" +
                "            </OSSUserMeta>\n" +
                "        </File>\n" +
                "        <File>\n" +
                "          <Filename>file2</Filename>\n" +
                "          <Size>5168828111</Size>\n" +
                "          <ObjectACL>private</ObjectACL>\n" +
                "          <OSSObjectType>Appendable</OSSObjectType>\n" +
                "          <OSSStorageClass>Standard</OSSStorageClass>\n" +
                "          <ETag>etag</ETag>\n" +
                "          <OSSCRC64>crc</OSSCRC64>\n" +
                "          <OSSTaggingCount>2</OSSTaggingCount>\n" +
                "          <OSSTagging>\n" +
                "            <Tagging>\n" +
                "              <Key>t3</Key>\n" +
                "              <Value>v3</Value>\n" +
                "            </Tagging>\n" +
                "            <Tagging>\n" +
                "              <Key>t4</Key>\n" +
                "              <Value>v4</Value>\n" +
                "            </Tagging>\n" +
                "          </OSSTagging>\n" +
                "          <OSSUserMeta>\n" +
                "            <UserMeta>\n" +
                "              <Key>u3</Key>\n" +
                "              <Value>v3</Value>\n" +
                "            </UserMeta>\n" +
                "            <UserMeta>\n" +
                "              <Key>u4</Key>\n" +
                "              <Value>v4</Value>\n" +
                "            </UserMeta>\n" +
                "          </OSSUserMeta>\n" +
                "        </File>\n" +
                "    </Files>\n" +
                "    <Aggregations>\n" +
                "            <Aggregation>\n" +
                "              <Field>Size</Field>\n" +
                "              <Operation>sum</Operation>\n" +
                "              <Value>200</Value>\n" +
                "              <Groups>\n" +
                "                <Group>\n" +
                "                    <Value>100</Value>\n" +
                "                    <Count>5</Count>\n" +
                "                </Group>\n" +
                "                <Group>\n" +
                "                    <Value>300</Value>\n" +
                "                    <Count>6</Count>\n" +
                "                </Group>\n" +
                "              </Groups>\n" +
                "            </Aggregation>\n" +
                "            <Aggregation>\n" +
                "              <Field>Size</Field>\n" +
                "              <Operation>max</Operation>\n" +
                "              <Value>200.2</Value>\n" +
                "            </Aggregation>\n" +
                "            <Aggregation>\n" +
                "              <Field>field1</Field>\n" +
                "              <Operation>operation1</Operation>\n" +
                "              <Groups>\n" +
                "                <Group>\n" +
                "                  <Value>value1</Value>\n" +
                "                  <Count>10</Count>\n" +
                "                </Group>\n" +
                "                <Group>\n" +
                "                  <Value>value2</Value>\n" +
                "                  <Count>20</Count>\n" +
                "                </Group>\n" +
                "              </Groups>\n" +
                "            </Aggregation>\n" +
                "        </Aggregations>\n" +
                "</MetaQuery>";

        InputStream instream = null;
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        DoMetaQueryResult result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.DoMetaQueryResponseParser parser = new ResponseParsers.DoMetaQueryResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals("MTIzNDU2NzgnV9zYW1wbGVvYmplY3QxLmpwZw==", result.getNextToken());

        Assert.assertEquals("exampleobject.txt", result.getFiles().getFile().get(0).getFilename());
        Assert.assertEquals(120L, result.getFiles().getFile().get(0).getSize());
        Assert.assertEquals("2021-06-29T14:50:13.011643661+08:00", result.getFiles().getFile().get(0).getFileModifiedTime());
        Assert.assertEquals("2021-06-28T14:50:13.011643661+08:00", result.getFiles().getFile().get(0).getFileCreateTime());
        Assert.assertEquals("2021-06-27T14:50:13.011643661+08:00", result.getFiles().getFile().get(0).getFileAccessTime());
        Assert.assertEquals("Normal", result.getFiles().getFile().get(0).getOssObjectType());
        Assert.assertEquals("Standard", result.getFiles().getFile().get(0).getOssStorageClass());
        Assert.assertEquals("defalut", result.getFiles().getFile().get(0).getObjectACL());
        Assert.assertEquals("fba9dede5f27731c9771645a3986****", result.getFiles().getFile().get(0).getETag());
        Assert.assertEquals("4858A48BD1466884", result.getFiles().getFile().get(0).getOssCRC64());
        Assert.assertEquals(2, result.getFiles().getFile().get(0).getOssTaggingCount());
        Assert.assertEquals("owner", result.getFiles().getFile().get(0).getOssTagging().getTagging().get(0).getKey());
        Assert.assertEquals("John", result.getFiles().getFile().get(0).getOssTagging().getTagging().get(0).getValue());
        Assert.assertEquals("type", result.getFiles().getFile().get(0).getOssTagging().getTagging().get(1).getKey());
        Assert.assertEquals("document", result.getFiles().getFile().get(0).getOssTagging().getTagging().get(1).getValue());
        Assert.assertEquals("x-oss-meta-location", result.getFiles().getFile().get(0).getOssUserMeta().getUserMeta().get(0).getKey());
        Assert.assertEquals("hangzhou", result.getFiles().getFile().get(0).getOssUserMeta().getUserMeta().get(0).getValue());
        Assert.assertEquals("file2", result.getFiles().getFile().get(1).getFilename());
        Assert.assertEquals(5168828111L, result.getFiles().getFile().get(1).getSize());
        Assert.assertEquals("Appendable", result.getFiles().getFile().get(1).getOssObjectType());
        Assert.assertEquals("Standard", result.getFiles().getFile().get(1).getOssStorageClass());
        Assert.assertEquals("private", result.getFiles().getFile().get(1).getObjectACL());
        Assert.assertEquals("etag", result.getFiles().getFile().get(1).getETag());
        Assert.assertEquals("crc", result.getFiles().getFile().get(1).getOssCRC64());
        Assert.assertEquals(2, result.getFiles().getFile().get(1).getOssTaggingCount());
        Assert.assertEquals("t3", result.getFiles().getFile().get(1).getOssTagging().getTagging().get(0).getKey());
        Assert.assertEquals("v3", result.getFiles().getFile().get(1).getOssTagging().getTagging().get(0).getValue());
        Assert.assertEquals("t4", result.getFiles().getFile().get(1).getOssTagging().getTagging().get(1).getKey());
        Assert.assertEquals("v4", result.getFiles().getFile().get(1).getOssTagging().getTagging().get(1).getValue());
        Assert.assertEquals("u3", result.getFiles().getFile().get(1).getOssUserMeta().getUserMeta().get(0).getKey());
        Assert.assertEquals("v3", result.getFiles().getFile().get(1).getOssUserMeta().getUserMeta().get(0).getValue());
        Assert.assertEquals("u4", result.getFiles().getFile().get(1).getOssUserMeta().getUserMeta().get(1).getKey());
        Assert.assertEquals("v4", result.getFiles().getFile().get(1).getOssUserMeta().getUserMeta().get(1).getValue());
        Assert.assertEquals("Size", result.getAggregations().getAggregation().get(0).getField());
        Assert.assertEquals("sum", result.getAggregations().getAggregation().get(0).getOperation());
        Assert.assertEquals(200.0, result.getAggregations().getAggregation().get(0).getValue());
        Assert.assertEquals("100", result.getAggregations().getAggregation().get(0).getGroups().getGroup().get(0).getValue());
        Assert.assertEquals(5, result.getAggregations().getAggregation().get(0).getGroups().getGroup().get(0).getCount());
        Assert.assertEquals("300", result.getAggregations().getAggregation().get(0).getGroups().getGroup().get(1).getValue());
        Assert.assertEquals(6, result.getAggregations().getAggregation().get(0).getGroups().getGroup().get(1).getCount());
        Assert.assertEquals("Size", result.getAggregations().getAggregation().get(1).getField());
        Assert.assertEquals("max", result.getAggregations().getAggregation().get(1).getOperation());
        Assert.assertEquals(200.2, result.getAggregations().getAggregation().get(1).getValue());
        Assert.assertEquals("field1", result.getAggregations().getAggregation().get(2).getField());
        Assert.assertEquals("operation1", result.getAggregations().getAggregation().get(2).getOperation());
        Assert.assertEquals("value1", result.getAggregations().getAggregation().get(2).getGroups().getGroup().get(0).getValue());
        Assert.assertEquals(10, result.getAggregations().getAggregation().get(2).getGroups().getGroup().get(0).getCount());
        Assert.assertEquals("value2", result.getAggregations().getAggregation().get(2).getGroups().getGroup().get(1).getValue());
        Assert.assertEquals(20, result.getAggregations().getAggregation().get(2).getGroups().getGroup().get(1).getCount());
    }

    @Test
    public void testParseErrorResponse() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<Error>\n" +
                "  <Code>MethodNotAllowed</Code>\n" +
                "  <Message>The specified method is not allowed against this resource.</Message>\n" +
                "  <RequestId>5CAC0CF8DE0170*****</RequestId>\n" +
                "  <HostId>versioning-get.oss-cn-hangzhou.aliyunc*****</HostId>\n" +
                "  <ResourceType>DeleteMarker</ResourceType>\n" +
                "  <Method>GET</Method>\n" +
                "  <Header>If-Modified-Since</Header>\n" +
                "</Error>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }


        OSSErrorResult result = null;
        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.ErrorResponseParser parser = new ResponseParsers.ErrorResponseParser();
            result = parser.parse(response);
        } catch (ResponseParseException e) {
            Assert.fail("parse delete directory response body fail!");
        }

        Assert.assertEquals("MethodNotAllowed", result.Code);
        Assert.assertEquals("The specified method is not allowed against this resource.", result.Message);
        Assert.assertEquals("5CAC0CF8DE0170*****", result.RequestId);
        Assert.assertEquals("versioning-get.oss-cn-hangzhou.aliyunc*****", result.HostId);
        Assert.assertEquals("DeleteMarker", result.ResourceType);
        Assert.assertEquals("GET", result.Method);
        Assert.assertEquals("If-Modified-Since", result.Header);

        respBody = "" +
                "<Error>\n" +
                "  <Code></Code>\n" +
                "  <Message></Message>\n" +
                "  <RequestId></RequestId>\n" +
                "  <HostId></HostId>\n" +
                "  <ResourceType></ResourceType>\n" +
                "  <Method></Method>\n" +
                "  <Header></Header>\n" +
                "</Error>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        try {
            ResponseMessage response = new ResponseMessage(null);
            response.setContent(instream);
            ResponseParsers.ErrorResponseParser parser = new ResponseParsers.ErrorResponseParser();
            result = parser.parse(response);
            Assert.assertTrue(true);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testParseListObjectsWithRestoreInfo() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListBucketResult>\n" +
                "  <Name>oss-java-sdk-1667542813</Name>\n" +
                "  <Prefix></Prefix>\n" +
                "  <Marker></Marker>\n" +
                "  <MaxKeys>100</MaxKeys>\n" +
                "  <Delimiter></Delimiter>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "  <Contents>\n" +
                "    <Key>object-with-special-restore</Key>\n" +
                "    <LastModified>2022-11-04T05:23:18.000Z</LastModified>\n" +
                "    <ETag>\"AB56B4D92B40713ACC5AF89985D4B786\"</ETag>\n" +
                "    <Type>Normal</Type>\n" +
                "    <Size>5</Size>\n" +
                "    <StorageClass>Archive</StorageClass>\n" +
                "    <Owner>\n" +
                "      <ID>1283641064516515</ID>\n" +
                "      <DisplayName>1283641064516515</DisplayName>\n" +
                "    </Owner>\n" +
                "    <RestoreInfo>ongoing-request=\"true\"</RestoreInfo>\n" +
                "  </Contents>\n" +
                "</ListBucketResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        ObjectListing result = null;
        try {
            result = ResponseParsers.parseListObjects(instream);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
        Assert.assertEquals("object-with-special-restore", result.getObjectSummaries().get(0).getKey());
        Assert.assertEquals("ongoing-request=\"true\"", result.getObjectSummaries().get(0).getRestoreInfo());
    }

    @Test
    public void testParseListObjectsV2WithRestoreInfo() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListBucketResult>\n" +
                "  <Name>oss-java-sdk-1667548362-list-v2</Name>\n" +
                "  <Prefix></Prefix>\n" +
                "  <MaxKeys>100</MaxKeys>\n" +
                "  <Delimiter></Delimiter>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "  <Contents>\n" +
                "    <Key>object-with-special-restore</Key>\n" +
                "    <LastModified>2022-11-04T07:37:25.000Z</LastModified>\n" +
                "    <ETag>\"AB56B4D92B40713ACC5AF89985D4B786\"</ETag>\n" +
                "    <Type>Normal</Type>\n" +
                "    <Size>5</Size>\n" +
                "    <StorageClass>Archive</StorageClass>\n" +
                "    <RestoreInfo>ongoing-request=\"false\", expiry-date=\"Sat, 05 Nov 2022 07:38:08 GMT\"</RestoreInfo>\n" +
                "  </Contents>\n" +
                "  <KeyCount>1</KeyCount>\n" +
                "</ListBucketResult>\n";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        ListObjectsV2Result result = null;
        try {
            result = ResponseParsers.parseListObjectsV2(instream);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
        Assert.assertEquals("object-with-special-restore", result.getObjectSummaries().get(0).getKey());
        Assert.assertEquals("ongoing-request=\"false\", expiry-date=\"Sat, 05 Nov 2022 07:38:08 GMT\"", result.getObjectSummaries().get(0).getRestoreInfo());
    }

    @Test
    public void testParseListVersionsWithRestoreInfo() {
        InputStream instream = null;
        String respBody;

        respBody = "" +
                "<ListVersionsResult>\n" +
                "  <Name>oss-java-sdk-1667549556-list-versions</Name>\n" +
                "  <Prefix></Prefix>\n" +
                "  <KeyMarker></KeyMarker>\n" +
                "  <VersionIdMarker></VersionIdMarker>\n" +
                "  <MaxKeys>100</MaxKeys>\n" +
                "  <Delimiter></Delimiter>\n" +
                "  <IsTruncated>false</IsTruncated>\n" +
                "  <Version>\n" +
                "    <Key>object-with-special-restore</Key>\n" +
                "    <VersionId>CAEQDxiBgID78pyGohgiIDFhNWM0ODYxMDcyNTQ0ODJiZDJjZDlmNjRhZmU5MWEy</VersionId>\n" +
                "    <IsLatest>true</IsLatest>\n" +
                "    <LastModified>2022-11-04T07:32:45.000Z</LastModified>\n" +
                "    <ETag>\"AB56B4D92B40713ACC5AF89985D4B786\"</ETag>\n" +
                "    <Type>Normal</Type>\n" +
                "    <Size>5</Size>\n" +
                "    <StorageClass>Archive</StorageClass>\n" +
                "    <RestoreInfo>ongoing-request=\"true\"</RestoreInfo>\n" +
                "    <Owner>\n" +
                "      <ID>1283641064516515</ID>\n" +
                "      <DisplayName>1283641064516515</DisplayName>\n" +
                "    </Owner>\n" +
                "  </Version>\n" +
                "</ListVersionsResult>";
        try {
            instream = new ByteArrayInputStream(respBody.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            Assert.fail("UnsupportedEncodingException");
        }

        VersionListing result = null;
        try {
            result = ResponseParsers.parseListVersions(instream);
            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.assertTrue(false);
        }
        Assert.assertEquals("object-with-special-restore", result.getVersionSummaries().get(0).getKey());
        Assert.assertEquals("ongoing-request=\"true\"", result.getVersionSummaries().get(0).getRestoreInfo());
    }
}
