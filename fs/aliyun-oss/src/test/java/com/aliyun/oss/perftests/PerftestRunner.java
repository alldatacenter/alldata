/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.perftests;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;

import com.aliyun.oss.perftests.TestScenario.Type;

public class PerftestRunner {

    private static final Log log = LogFactory.getLog(PerftestRunner.class);

    private TestScenario scenario;
    private OSS ossClient;
    private Date startTime;
    private Date endTime;
    private int putErrNum = 0;
    private int getErrNum = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private static String fileName = "perftest.txt";

    private byte[] byteArray1KB;
    private byte[] byteArray100KB;
    private byte[] byteArray1MB;
    private byte[] byteArray4MB;

    private static TestScenario.Type determineScenarioType(
            final String scenarioTypeString) {
        TestScenario.Type t = Type.UNKNOWN;
        if (scenarioTypeString.equals("onlyput-1MB")) {
            t = Type.ONLY_PUT_1MB;
        } else if (scenarioTypeString.equals("onlyput-4MB")) {
            t = Type.ONLY_PUT_4MB;
        } else if (scenarioTypeString.equals("get-and-put-1vs1-1KB")) {
            t = Type.GET_AND_PUT_1VS1_1KB;
        } else if (scenarioTypeString.equals("get-and-put-1vs1-100KB")) {
            t = Type.GET_AND_PUT_1VS1_100KB;
        } else if (scenarioTypeString.equals("get-and-put-1vs1-1MB")) {
            t = Type.GET_AND_PUT_1VS1_1MB;
        } else if (scenarioTypeString.equals("get-and-put-1vs1-4MB")) {
            t = Type.GET_AND_PUT_1VS1_4MB;
        } else if (scenarioTypeString.equals("get-and-put-1vs1-1MB")) {
            t = Type.GET_AND_PUT_1VS1_1MB;
        } else if (scenarioTypeString.equals("get-and-put-1vs4-1MB")) {
            t = Type.GET_AND_PUT_1VS4_1MB;
        } else if (scenarioTypeString.equals("get-and-put-1vs4-4MB")) {
            t = Type.GET_AND_PUT_1VS4_4MB;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported test sconario type " + scenarioTypeString);
        }

        return t;
    }

    public void buildScenario(final String scenarioTypeString) {
        File confFile = new File(System.getProperty("user.dir")
                + File.separator + "runner_conf.xml");
        InputStream input = null;
        try {
            input = new FileInputStream(confFile);
        } catch (FileNotFoundException e) {
            log.error(e);
            Assert.fail(e.getMessage());
        }
        SAXBuilder builder = new SAXBuilder();
        try {
            Document doc = builder.build(input);
            Element root = doc.getRootElement();
            scenario = new TestScenario();
            scenario.setHost(root.getChildText("host"));
            scenario.setAccessId(root.getChildText("accessid"));
            scenario.setAccessKey(root.getChildText("accesskey"));
            scenario.setBucketName(root.getChildText("bucket"));

            scenario.setType(determineScenarioType(scenarioTypeString));

            Element target = root.getChild(scenarioTypeString);
            if (target != null) {
                scenario.setContentLength(Long.parseLong(target
                        .getChildText("size")));
                scenario.setPutThreadNumber(Integer.parseInt(target
                        .getChildText("putthread")));
                scenario.setGetThreadNumber(Integer.parseInt(target
                        .getChildText("getthread")));
                scenario.setDurationInSeconds(Integer.parseInt(target
                        .getChildText("time")));
                scenario.setGetQPS(Integer.parseInt(target.getChildText("getqps")));
                scenario.setPutQPS(Integer.parseInt(target.getChildText("putqps")));
            } else {
                log.error("Unable to locate XML element "
                        + scenarioTypeString);
                Assert.fail("Unable to locate XML element " + scenarioTypeString);
            }
        } catch (JDOMException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    public void prepareEnv() {
        String endpoint = scenario.getHost();
        String accessId = scenario.getAccessId();
        String accessKey = scenario.getAccessKey();
        ossClient = new OSSClientBuilder().build(endpoint, accessId, accessKey);

        try {
            byteArray1KB = createFixedLengthBytes(1024);
            byteArray100KB = createFixedLengthBytes(102400);
            byteArray1MB = createFixedLengthBytes(1 * 1024 * 1024);
            byteArray4MB = createFixedLengthBytes(4 * 1024 * 1024);
        } catch (Exception e) {
            log.error(e.getMessage());
            Assert.fail(e.getMessage());
        }
    }

    public void createBucket() {
        String bucketName = scenario.getBucketName();
        try {
            ossClient.createBucket(bucketName);
        } catch (OSSException e) {
            log.error("Put bucket " + bucketName + " error " + e.getMessage());
            Assert.fail(e.getMessage());
        } catch (ClientException e) {
            log.error("Put bucket " + bucketName + " error " + e.getMessage());
            Assert.fail(e.getMessage());
        } catch (Exception e) {
            log.error("Put bucket " + bucketName + " error " + e.getMessage());
            Assert.fail(e.getMessage());
        }
    }
    
    private static byte[] createFixedLengthBytes(int fixedLength) {
        byte[] data = new byte[fixedLength];
        for (int i = 0; i < fixedLength; i++) {
            data[i] = 'a';
        }
        return data;
    }

    private byte[] chooseByteArray(long contentLength) {
        if (contentLength == 1024) {
            return byteArray1KB;
        } else if (contentLength == 102400) {
            return byteArray100KB;
        } else if (contentLength == 1 * 1024 * 1024) {
            return byteArray1MB;
        } else if (contentLength == 4 * 1024 * 1024) {
            return byteArray4MB;
        } else {
            throw new IllegalArgumentException(
                    "Illegal content length, only support 1KB & 100KB & 1MB & 4MB");
        }
    }

    private boolean hasExpired() {
        Date now = new Date();
        long interval = (now.getTime() - startTime.getTime()) / 1000;
        return interval > scenario.getDurationInSeconds();
    }

    private String buildObjectKey(String keyPrefix) {
        int n = new Random().nextInt(100000);
        StringBuilder sb = new StringBuilder();
        sb.append(keyPrefix);
        sb.append(Integer.toString(n));
        return sb.toString();
    }

    private static Thread[] joinThreads(Thread[]... threadArrays) {
        List<Thread> threadList = new ArrayList<Thread>();
        for (Thread[] tarr : threadArrays) {
            for (Thread t : tarr) {
                threadList.add(t);
            }
        }
        Thread[] joined = new Thread[threadList.size()];
        return threadList.toArray(joined);
    }

    public static void waitAll(Thread[] targets) {
        for (int i = 0; i < targets.length; i++) {
            targets[i].start();
        }

        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].join();
            } catch (InterruptedException e) {
            }
        }
    }

    private String getTestName(OperationType type) {
        switch (type) {
        case PUT:
            return "PutObject";
        case GET:
            return "GetObject";
        default:
            return "UnknownTest";
        }
    }
    
    private String getLengthString(long contentLength) {
        if (contentLength > 0) {
            if (contentLength % (1024 * 1024 * 1024) == 0) {
                return "" + contentLength / (1024 * 1024 * 1024) + "G";
            } else if (contentLength % (1024 * 1024) == 0) {
                return "" + contentLength / (1024 * 1024) + "M";
            } else if (contentLength % 1024 == 0) {
                return "" + contentLength / 1024 + "K";
            }
        }
        return "" + contentLength + "B";
    }
    
    private String getTimeSpanString(long passTime) {
        if (passTime > 0) {
            if (passTime % 3600 == 0) {
                return "" + passTime / 3600 + "hour";
            } else if (passTime % 60 ==0){
                return "" + passTime /60 + "min";
            }
        }
        return "" + passTime + "s";
    }
    
    private void calculateResult(final List<Long> latencyArray, OperationType type, int threadNum) {
        try {
            int errNum = type == OperationType.PUT ? putErrNum : getErrNum;
            int totalNum = latencyArray.size() + errNum;
            if (totalNum <= 0) {
                return;
            }
            
            long thresholds[] = {10, 50, 100, 1000, 3000, Long.MAX_VALUE};
            long thresholdNum[] = new long[6];
            long avgLatency = 0;
            Collections.sort(latencyArray);
            int j = 0;
            for (int i = 0; i < thresholds.length; i++) {
                for (; j < latencyArray.size() && latencyArray.get(j) <= thresholds[i]; j++) {
                    thresholdNum[i]++;
                    avgLatency += latencyArray.get(j);        
                }
            }
            long passTime = endTime.getTime() - startTime.getTime();
            double qps = latencyArray.size() * 1000.0 / passTime;
            double errRate = errNum * 1.0 / totalNum;
            avgLatency = avgLatency / latencyArray.size();
            double throughput = qps * scenario.getContentLength() / 1048576;
            String contentStr = null;
            contentStr = String.format("TestType:%s\n", getTestName(type));
            contentStr += String.format("threads:%d, %s, %s\n", threadNum, getLengthString(scenario.getContentLength()), getTimeSpanString(scenario.getDurationInSeconds()));
            contentStr += String.format("Qps:%.1f Throughput:%.2fM AvgLatency:%dms\n", qps, throughput, avgLatency);
            contentStr += String.format("ErrorRate:%.2f\n", errRate);
            contentStr += "Latency Range:\n";
            contentStr += String.format("%d-%dms\t%.2f%s\n", 0, thresholds[0], thresholdNum[0] * 100.0 / totalNum, "%");
            for (int i = 0; i < thresholds.length - 2; i++) {
                contentStr += String.format("%d-%dms\t%.2f%s\n", thresholds[i], thresholds[i + 1], thresholdNum[i + 1] * 100.0 / totalNum, "%");
            }
            contentStr += String.format("%dms+\t\t%.2f%s\n", thresholds[thresholds.length - 2], thresholdNum[thresholdNum.length - 1] * 100.0 / totalNum, "%");
            //print result
            System.out.printf("%s", contentStr);
            // write file
            writeResult(contentStr);
        } catch (Exception e) {
            log.error("Unexpected exception occurs when calculate result " + getTestName(type));
            Assert.fail(e.getMessage());
        }
    }
    
    public void writeResult(final String contentStr) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName, true);
            writer.write(contentStr);
        } catch (IOException e) {
            // TODO: handle exception
            log.error(e.getMessage());
            Assert.fail(e.getMessage());
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e2) {
                // TODO: handle exception
                log.error(e2.getMessage());
                Assert.fail(e2.getMessage());
            }
        }
    }
    
    private void recordError(OperationType type) {
        try {
            lock.lock();
            if (type == OperationType.PUT) {
                putErrNum++;
            } else if (type == OperationType.GET) {
                getErrNum++;
            }
        } finally {
            lock.unlock();
        }
    }

    public void testRun() {
        int putThreadNumber = scenario.getPutThreadNumber();
        int getThreadNumber = scenario.getGetThreadNumber();
        int getQPS = scenario.getGetQPS();
        int putQPS = scenario.getPutQPS();
        
        final int getInterval = getThreadNumber == 0 ? 0: getThreadNumber * (1000 / getQPS);
        final int putInterval = putThreadNumber * (1000 / putQPS);

        final long contentLength = scenario.getContentLength();
        final String bucketName = scenario.getBucketName();
        final byte[] byteArray = chooseByteArray(contentLength);
        final String keyPrefix = "xiao-perf-test-";
        final List<String> uploadedObjects = new ArrayList<String>();
        final List<Long> putLatencyArray = new ArrayList<Long>();
        final List<Long> getLatencyArray = new ArrayList<Long>();
        final PerftestRunner self = this;

        Thread[] putThreads = new Thread[putThreadNumber];
        try {
            for (int i = 0; i < putThreadNumber; i++) {
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        while (!self.hasExpired()) {
                            InputStream input = null;
                            try {
                                input = new ByteArrayInputStream(byteArray);
                                String objectKey = buildObjectKey(keyPrefix);

                                log.info("Begin put " + objectKey);
                                Date from = new Date();
                                ObjectMetadata metadata = new ObjectMetadata();
                                metadata.setContentLength(contentLength);
                                ossClient.putObject(bucketName, objectKey,
                                        input, metadata);
                                Date to = new Date();
                                long latency = to.getTime() - from.getTime();
                                if (latency < putInterval) {
                                    Thread.sleep(putInterval - latency);
                                } else {
                                    log.warn("Put object " + objectKey + " latency " + latency
                                        + ", exceed max interval " + putInterval);
                                }
                                log.info("Put object " + objectKey
                                        + " finished, elapsed " + latency
                                        + "ms");

                                try {
                                    lock.lock();
                                    uploadedObjects.add(objectKey);
                                    putLatencyArray.add(latency);
                                } finally {
                                    lock.unlock();
                                }
                            } catch (OSSException oe) {
                                log.warn("Unexpected oss exception occurs when putting object "
                                        + oe.getMessage());
                                recordError(OperationType.PUT);
                            } catch (ClientException ce) {
                                log.warn("Unexpected client exception occurs when putting object "
                                        + ce.getMessage());
                                recordError(OperationType.PUT);
                            } catch (Exception e) {
                                log.warn("Other unexpected exception "
                                        + e.getMessage());
                                recordError(OperationType.PUT);
                            } finally {
                                if (input != null) {
                                    try {
                                        input.close();
                                        input = null;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                };

                putThreads[i] = new Thread(r);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            Assert.fail(ex.getMessage());
        }

        Thread[] getThreads = new Thread[getThreadNumber];
        try {
            for (int i = 0; i < getThreadNumber; i++) {
                Runnable r = new Runnable() {

                    @Override
                    public void run() {
                        OSSObject o = null;
                        while (!hasExpired()) {
                            try {
                                String objectKey = null;
                                if (uploadedObjects.size() > 0) {
                                    try {
                                        lock.lock();
                                        if (uploadedObjects.size() > 0) {
                                            int index = new Random()
                                                    .nextInt(uploadedObjects
                                                            .size());
                                            objectKey = uploadedObjects
                                                    .get(index);
                                        }
                                    } finally {
                                        lock.unlock();
                                    }
                                } else {
                                    Thread.sleep(50);
                                    continue;
                                }

                                GetObjectRequest request = new GetObjectRequest(
                                        bucketName, objectKey);
                                log.info("Begin get " + objectKey);
                                Date from = new Date();
                                o = ossClient.getObject(request);

                                // read data
                                byte[] content = new byte[8196];
                                InputStream in = o.getObjectContent();
                                while (in.read(content) >= 0)
                                    ;

                                Date to = new Date();
                                long latency = to.getTime() - from.getTime();
                                if (latency < getInterval) {
                                    Thread.sleep(getInterval - latency);
                                } else {
                                    log.warn("Get object " + objectKey + " latency " + latency
                                        + ", exceed max interval " + getInterval);
                                }
                                log.info("Get object " + objectKey
                                        + " finished, elapsed " + latency
                                        + "ms");
                                try {
                                    lock.lock();
                                    getLatencyArray.add(latency);
                                } finally {
                                    lock.unlock();
                                }
                            } catch (OSSException oe) {
                                log.warn("Unexpected oss exception occurs when getting object "
                                        + oe.getMessage());
                                recordError(OperationType.GET);
                            } catch (ClientException ce) {
                                log.warn("Unexpected oss exception occurs when getting object "
                                        + ce.getMessage());
                                recordError(OperationType.GET);
                            } catch (Exception e) {
                                log.warn("Other unexpected exception "
                                        + e.getMessage());
                                recordError(OperationType.GET);
                            } finally {
                                if (o != null) {
                                    try {
                                        o.getObjectContent().close();
                                        o = null;
                                    } catch (IOException e) {
                                        log.error("IO Exception "
                                                + e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                };

                getThreads[i] = new Thread(r);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
            Assert.fail(ex.getMessage());
        }

        startTime = new Date();
        Thread[] allThreads = joinThreads(putThreads, getThreads);
        waitAll(allThreads);
        endTime = new Date();
        
        calculateResult(putLatencyArray, OperationType.PUT, putThreadNumber);
        calculateResult(getLatencyArray, OperationType.GET, getThreadNumber);
    }

    public static void deleteFile() {
        File file = new File(fileName);
        if (file.isFile() && file.exists()) {
            file.delete();
        }
    }
    
    public static void main(String[] args) {
        String realArgs[];
        if (args.length >= 1) {
            realArgs = Arrays.copyOf(args, args.length);
        } else {
            realArgs = new String[]{"get-and-put-1vs1-1KB","get-and-put-1vs1-100KB","get-and-put-1vs1-1MB","get-and-put-1vs1-4MB"};
        }

        deleteFile();
        for (int i = 0; i < realArgs.length; i++) {
            PerftestRunner runner = new PerftestRunner();
            runner.buildScenario(realArgs[i]);
            runner.prepareEnv();
            runner.createBucket();
            runner.writeResult("TestCycle:\n");
            System.out.printf("TestCycle:\n");
            runner.testRun();                
        }
    }
}
