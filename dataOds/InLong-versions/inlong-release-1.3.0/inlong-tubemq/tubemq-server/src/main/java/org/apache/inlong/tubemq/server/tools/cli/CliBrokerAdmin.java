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

package org.apache.inlong.tubemq.server.tools.cli;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.fielddef.CliArgDef;
import org.apache.inlong.tubemq.server.common.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is use to process CLI Broker Admin process for script #{bin/tubemq-broker-admin.sh}.
 *
 *
 */
public class CliBrokerAdmin extends CliAbstractBase {

    private static final Logger logger =
            LoggerFactory.getLogger(CliBrokerAdmin.class);

    private static final String defBrokerPortal = "127.0.0.1:8081";

    public CliBrokerAdmin() {
        super("tubemq-broker-admin.sh");
        initCommandOptions();
    }

    /**
     * Init command options
     */
    protected void initCommandOptions() {
        // add the cli required parameters
        addCommandOption(CliArgDef.BROKERPORTAL);
        addCommandOption(CliArgDef.ADMINMETHOD);
        addCommandOption(CliArgDef.METHOD);

    }

    /**
     * Call the broker's HTTP API by the tubemq-broker-admin.sh script
     * @param args     Call parameter array,
     *                 the relevant parameters are dynamic mode, which is parsed by CommandLine.
     */
    public boolean processParams(String[] args) throws Exception {
        // parse parameters and check value
        CommandLine cli = parser.parse(options, args);
        if (cli == null) {
            throw new ParseException("Parse args failure");
        }
        if (cli.hasOption(CliArgDef.VERSION.longOpt)) {
            version();
        }
        if (cli.hasOption(CliArgDef.HELP.longOpt)) {
            help();
        }
        String brokerAddr = defBrokerPortal;
        if (cli.hasOption(CliArgDef.BROKERPORTAL.longOpt)) {
            brokerAddr = cli.getOptionValue(CliArgDef.BROKERPORTAL.longOpt);
            if (TStringUtils.isBlank(brokerAddr)) {
                throw new Exception(CliArgDef.BROKERPORTAL.longOpt + " is required!");
            }
        }
        JsonObject result = null;
        Map<String, String> inParamMap = new HashMap<>();
        String brokerUrl = "http://" + brokerAddr + "/broker.htm";
        if (cli.hasOption(CliArgDef.ADMINMETHOD.longOpt)) {
            inParamMap.put(CliArgDef.METHOD.longOpt, "admin_get_methods");
            result = HttpUtils.requestWebService(brokerUrl, inParamMap);
            System.out.println(result.toString());
            System.exit(0);
        }
        String methodStr = cli.getOptionValue(CliArgDef.METHOD.longOpt);
        if (TStringUtils.isBlank(methodStr)) {
            throw new Exception(CliArgDef.METHOD.longOpt + " is required!");
        }
        inParamMap.put(CliArgDef.METHOD.longOpt, methodStr);
        result = HttpUtils.requestWebService(brokerUrl, inParamMap);
        System.out.println(result.toString());
        return true;
    }

    public static void main(String[] args) {
        CliBrokerAdmin cliBrokerAdmin = new CliBrokerAdmin();
        try {
            cliBrokerAdmin.processParams(args);
        } catch (Throwable ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            cliBrokerAdmin.help();
        }

    }

}
