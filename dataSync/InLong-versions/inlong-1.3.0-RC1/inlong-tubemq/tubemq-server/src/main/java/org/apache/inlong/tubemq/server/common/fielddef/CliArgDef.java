/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common.fielddef;

public enum CliArgDef {

    // Note: Due to compatibility considerations,
    //      the defined fields in the scheme are forbidden to be modified,
    //      only new fields can be added

    HELP("h", "help", "Print usage information."),
    VERSION("v", "version", "Display TubeMQ version."),
    MASTERSERVER(null, "master-servers",
            "String: format is master1_ip:port[,master2_ip:port]",
            "The master address(es) to connect to."),
    MASTERPORTAL(null, "master-portal",
            "String: format is master_ip:master_webport",
            "Master Service portal to which to connect.(default: 127.0.0.1:8080)"),
    BROKERPORTAL(null, "broker-portal",
            "String: format is broker_ip:broker_webport",
            "Broker Service URL to which to connect.(default: 127.0.0.1:8081)"),
    MESSAGES(null, "messages",
            "Long: count",
            "The number of messages to send or consume, If not set, production or consumption is continual."),
    MSGDATASIZE(null, "message-data-size",
            "Int: message size,(0, 1024 * 1024)",
            "message's data size in bytes. Note that you must provide exactly"
                    + " one of --msg-data-size or --payload-file."),
    PAYLOADFILE(null, "payload-file",
            "String: payload file path",
            "file to read the message payloads from. This works only for"
                    + " UTF-8 encoded text files. Payloads will be read from this"
                    + " file and a payload will be randomly selected when sending"
                    + " messages. Note that you must provide exactly one"
                    + " of --msg-data-size or --payload-file."),
    PAYLOADDELIM(null, "payload-delimiter",
            "String: payload data's delimiter",
            "provides delimiter to be used when --payload-file is provided."
                    + " Defaults to new line. Note that this parameter will be"
                    + " ignored if --payload-file is not provided. (default: \\n)"),
    PRDTOPIC("topic", "topicName",
            "String: topic, format is topic_1[,topic_2[:filterCond_2.1[\\;filterCond_2.2]]]",
            "The topic(s) to produce messages to."),
    CNSTOPIC("topic", "topicName",
            "String: topic, format is topic_1[[:filterCond_1.1[\\;filterCond_1.2]][,topic_2]]",
            "The topic(s) to consume on."),
    RPCTIMEOUT(null, "rpc-timeout",
            "Long: milliseconds",
            "The maximum duration between request and response in milliseconds. (default: 10000)"),
    CONNREUSE(null, "conn-reuse",
            "bool: true or false",
            "Different clients reuse TCP connections. (default: true)"),
    GROUP("group", "groupName",
            "String: consumer group",
            "The consumer group name of the consumer. (default: test_consume)"),
    CLIENTCOUNT(null, "client-count",
            "Int: client count, [1, 100]",
            "Number of producers or consumers to started."),
    PUSHCONSUME(null, "consume-push",
            "Push consumption action.(default: pull)"),
    FETCHTHREADS(null, "num-fetch-threads",
            "Integer: count, [1,100]",
            "Number of fetch threads, default: num of cpu count."),
    SENDTHREADS(null, "num-send-threads",
            "Integer: count, [1,200]",
            "Number of send message threads, default: num of cpu count."),
    CONSUMEPOS(null, "consume-position",
            "Integer: [-1,0, 1]",
            "Set the start position of the consumer group. The value can be [-1, 0, 1]."
                    + " Default value is 0. -1: Start from 0 for the first time."
                    + " Otherwise start from last consume position."
                    + " 0: Start from the latest position for the first time."
                    + " Otherwise start from last consume position."
                    + " 1: Start from the latest consume position."),
    OUTPUTINTERVAL(null, "output-interval",
            "Integer: interval_ms, [5000, +)",
            "Interval in milliseconds at which to print progress info. (default: 5000)"),
    SYNCPRODUCE(null, "sync-produce",
            "Synchronous production. (default: false)"),
    WITHOUTDELAY(null, "without-delay",
            "Production without delay. (default: false)"),
    METHOD(null, "method",
            "String: http call method",
            "Http call method"),
    ADMINMETHOD(null, "show-methods",
            "Return http's methods."),
    FILEPATH("f", "file",
            "String: file path.",
            "File path."),
    METAFILEPATH("path", "meta-file-path",
            "String: backup or recovery file path.",
            "File path to backup or restore metadata."
                    + " Defaults value is the current path where the program is running."),
    OPERATIONTYPE("type", "operation-type",
            "String: operation type, include [backup, recovery]",
            "Types of operations on metadata"),
    AUTHTOKEN("token", "auth-token",
            "String: API operation authorization code",
            "API operation authorization code,"
                    + " required when adding or modifying, optional when querying");

    CliArgDef(String opt, String longOpt, String optDesc) {
        this(opt, longOpt, false, "", optDesc);
    }

    CliArgDef(String opt, String longOpt, String argDesc, String optDesc) {
        this(opt, longOpt, true, argDesc, optDesc);
    }

    CliArgDef(String opt, String longOpt, boolean hasArg, String argDesc, String optDesc) {
        this.opt = opt;
        this.longOpt = longOpt;
        this.hasArg = hasArg;
        this.argDesc = argDesc;
        this.optDesc = optDesc;
    }

    public final String opt;
    public final String longOpt;
    public final boolean hasArg;
    public final String argDesc;
    public final String optDesc;
}
