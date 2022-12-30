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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.inlong.sdk.dataproxy.DefaultMessageSender;
import org.apache.inlong.sdk.dataproxy.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMsgThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(SendMsgThread.class);
    private DefaultMessageSender messageSender = null;

    public SendMsgThread(DefaultMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void run() {
        FileReader reader = null;
        try {
            reader = new FileReader("/data/work/jessey/d5.txt");

            BufferedReader br = new BufferedReader(reader);
            String line = null;
            while ((line = br.readLine()) != null) {

                long startTime = System.currentTimeMillis();
                SendResult result = messageSender.sendMessage("hhhh".getBytes("utf8"),
                        "b_test", "n_test1", 0, String.valueOf(System.currentTimeMillis()), 1,
                        TimeUnit.MILLISECONDS);
                long endTime = System.currentTimeMillis();
                if (result == result.OK) {
                    logger.info("this msg is ok time {}", endTime - startTime);
                } else {
                    logger.info("this msg is error ,{}", result);
                }
            }
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            e.printStackTrace();

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
