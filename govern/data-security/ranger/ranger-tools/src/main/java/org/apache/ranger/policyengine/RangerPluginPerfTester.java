/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.policyengine;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.ranger.plugin.policyevaluator.RangerPolicyEvaluator;
import org.apache.ranger.plugin.service.RangerBasePlugin;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.File;

public class RangerPluginPerfTester {

	static RangerBasePlugin plugin = null;

	private static String serviceType;
	private static String serviceName;
	private static String appId;
	private static String rangerHostName;
	private static int socketReadTimeout = 30*1000;
	private static int pollingInterval = 30*1000;
	private static String policyCacheDir;
	private static boolean useCachedPolicyEvaluator = false;

	private static Options options = new Options();

	public static void main(String[] args) {

		if (!parseArguments(args)) {
			System.err.println("Exiting.. ");
			System.exit(-1);
		}



		System.out.println("Arguments:");
		System.out.println("\t\tservice-type:\t\t\t" + serviceType);
		System.out.println("\t\tservice-name:\t\t\t" + serviceName);
		System.out.println("\t\tapp-id:\t\t\t\t" + appId);
		System.out.println("\t\tranger-host:\t\t\t" + rangerHostName);
		System.out.println("\t\tsocket-read-timeout:\t\t" + socketReadTimeout);
		System.out.println("\t\tpolling-interval:\t\t" + pollingInterval);
		System.out.println("\t\tpolicy-cache-dir:\t\t" + policyCacheDir);
		System.out.println("\t\tuse-cached-policy-evaluator:\t" + useCachedPolicyEvaluator);
		System.out.println("\n\n");


		Path filePath = buildConfigurationFile();

		if (filePath != null) {
			plugin = new RangerBasePlugin(serviceType, appId);

			plugin.getConfig().addResource(filePath);

			Runtime runtime = Runtime.getRuntime();
			runtime.gc();

			long totalMemory = runtime.totalMemory();
			long freeMemory = runtime.freeMemory();

			System.out.println("Initial Memory Statistics:");
			System.out.println("\t\tMaximum Memory available for the process:\t" + runtime.maxMemory());
			System.out.println("\t\tInitial In-Use memory:\t\t\t\t" + (totalMemory-freeMemory));
			System.out.println("\t\tInitial Free memory:\t\t\t\t" + freeMemory);

			System.out.println("\n\n");

			plugin.init();

			while (true) {

				runtime.gc();

				freeMemory = runtime.freeMemory();
				totalMemory = runtime.totalMemory();

				System.out.println("Memory Statistics:");
				System.out.println("\t\tCurrently In-Use memory:\t" + (totalMemory-freeMemory));
				System.out.println("\t\tCurrently Free memory:\t\t" + freeMemory);

				System.out.println("\n\n");

				try {
					Thread.sleep(60 * 1000);
				} catch (InterruptedException e) {

					System.err.println("Main thread interrupted..., exiting...");
					break;
				}
			}
		} else {
			System.err.println("Failed to build configuration file");
		}
	}

	static Path buildConfigurationFile() {

		Path ret = null;

		String propertyPrefix    = "ranger.plugin." + serviceType;
		String policyEvaluatorType = useCachedPolicyEvaluator ? RangerPolicyEvaluator.EVALUATOR_TYPE_CACHED : RangerPolicyEvaluator.EVALUATOR_TYPE_OPTIMIZED;

		try {

			File file = File.createTempFile("ranger-plugin-test-site", ".xml");
			file.deleteOnExit();

			String filePathStr =  file.getAbsolutePath();

			Path filePath = new Path(filePathStr);

			FileSystem fs = filePath.getFileSystem(new Configuration());

			FSDataOutputStream outStream = fs.create(filePath, true);

			OutputStreamWriter writer = new OutputStreamWriter(outStream);

			writer.write("<configuration>\n" +
					"        <property>\n" +
					"                <name>" + propertyPrefix + ".policy.pollIntervalMs</name>\n" +
					"                <value>" + pollingInterval + "</value>\n" +
					"        </property>\n" +
					"        <property>\n" +
					"                <name>" + propertyPrefix + ".policy.cache.dir</name>\n" +
					"                <value>" + policyCacheDir + "</value>\n" +
					"        </property>\n" +
					"        <property>\n" +
					"                <name>" + propertyPrefix + ".policy.rest.url</name>\n" +
					"                <value>" + rangerHostName + ":6080" + "</value>\n" +
					"        </property>\n" +
					"        <property>\n" +
					"                <name>" + propertyPrefix + ".policy.source.impl</name>\n" +
					"                <value>org.apache.ranger.admin.client.RangerAdminRESTClient</value>\n" +
					"        </property>\n" +
					"        <property>\n" +
					"                <name>" + propertyPrefix + ".policy.rest.client.read.timeoutMs</name>\n" +
					"                <value>" + socketReadTimeout + "</value>\n" +
					"        </property>\n" +
					"        <property>\n" +
					"                <name>" + propertyPrefix + ".policyengine.option.evaluator.type</name>\n" +
					"                <value>" + policyEvaluatorType + "</value>\n" +
					"        </property>\n" +
					"        <property>\n" +
					"                <name>" + propertyPrefix + ".service.name</name>\n" +
					"                <value>" + serviceName + "</value>\n" +
					"        </property>\n" +

					"        <property>\n" +
					"                <name>xasecure.audit.is.enabled</name>\n" +
					"                <value>false</value>\n" +
					"        </property>\n" +
					"</configuration>\n");

			writer.close();
			ret = filePath;

		} catch (IOException exception) {
			//Ignore
		}

		return ret;

	}


	static boolean parseArguments(final String[] args) {

		boolean ret = false;

		options.addOption("h", "help", false, "show help.");
		options.addOption("s", "service-type", true, "Service-Type");
		options.addOption("n", "service-name", true, "Ranger service-name ");
		options.addOption("a", "app-id", true, "Application-Id");
		options.addOption("r", "ranger-host", true, "Ranger host-name");
		options.addOption("t", "socket-read-timeout", true, "Read timeout on socket in milliseconds");
		options.addOption("p", "polling-interval", true, "Polling Interval in milliseconds");
		options.addOption("c", "policy-cache-dir", true, "Policy-Cache directory ");
		options.addOption("e", "policy-evaluator-type", true, "Policy-Evaluator-Type (Cached/Other");

		DefaultParser commandLineParser = new DefaultParser();

		try {
			CommandLine commandLine = commandLineParser.parse(options, args);

			if (commandLine.hasOption("h")) {
				showUsage();
				return false;
			}

			serviceType = commandLine.getOptionValue("s");
			serviceName = commandLine.getOptionValue("n");
			appId = commandLine.getOptionValue("a");
			rangerHostName = commandLine.getOptionValue("r");
			policyCacheDir = commandLine.getOptionValue("c");

			try {
				String pollingIntervalStr = commandLine.getOptionValue("p");
				pollingInterval = Integer.parseInt(pollingIntervalStr);
			} catch (NumberFormatException exception) {
				// Ignore
			}

			String useCachedPolicyEvaluatorStr = commandLine.getOptionValue("e");
			if (StringUtils.equalsIgnoreCase(useCachedPolicyEvaluatorStr, "cache")) {
				useCachedPolicyEvaluator = true;
			}

			try {
				String socketReadTimeoutStr = commandLine.getOptionValue("t");
				socketReadTimeout = Integer.parseInt(socketReadTimeoutStr);
			} catch (NumberFormatException exception) {
				// Ignore
			}

			ret = true;

		} catch (ParseException exception) {
			System.err.println("Failed to parse arguments:" + exception);
		}

		return ret;
	}

    static void showUsage() {
        HelpFormatter formater = new HelpFormatter();
        formater.printHelp("plugin-tester", options);
    }

}

