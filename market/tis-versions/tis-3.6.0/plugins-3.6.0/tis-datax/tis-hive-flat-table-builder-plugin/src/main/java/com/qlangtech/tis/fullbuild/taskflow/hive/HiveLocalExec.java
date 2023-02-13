/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.fullbuild.taskflow.hive;

/* *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年2月24日
 */
public class HiveLocalExec {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
    // String line = "AcroRd32.exe /p /h " + file.getAbsolutePath();
    // CommandLine cmdLine = new CommandLine("hive");
    // cmdLine.addArgument("--database tis");
    // cmdLine.addArgument("-e");
    // cmdLine.addArgument(
    // "select count(1) from totalpay where pt='20160224001002';",
    // true);
    //
    // System.out.println("start===================");
    //
    // // CommandLine cmdLine = new CommandLine("/bin/sh");
    // // cmdLine.addArgument("./dumpcenter-daily.sh");
    //
    // CommandLine cmdLine = new CommandLine("hive");
    // cmdLine.addArgument("--database");
    // cmdLine.addArgument("tis");
    // cmdLine.addArgument("-e");
    // cmdLine.addArgument(
    // "select count(1) from instance;\n select count(1) from totalpay;",
    // true);
    //
    // System.out.println("getExecutable:" + cmdLine.getExecutable());
    // System.out.println(cmdLine.getArguments());
    // System.out.println("==============");
    // DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
    //
    // ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
    // DefaultExecutor executor = new DefaultExecutor();
    // executor.setWorkingDirectory(new File("/home/baisui/tis"));
    //
    // executor.setStreamHandler(new PumpStreamHandler(System.out));
    // executor.setExitValue(1);
    // executor.setWatchdog(watchdog);
    // executor.execute(cmdLine, resultHandler);
    //
    // // 等待5个小时
    // resultHandler.waitFor(5 * 60 * 60 * 1000);
    // System.out.println("exec over===================");
    //
    // System.out.println("exitCode:" + resultHandler.getExitValue());
    // if (resultHandler.getException() != null) {
    // resultHandler.getException().printStackTrace();
    // }
    }
}
