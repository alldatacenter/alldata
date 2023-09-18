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
package scala.tools.scala_maven_executions;

import java.io.File;

/**
 * This interface is used to create a call on a main method of a java class.
 * The important implementations are JavaCommand and ReflectionJavaCaller
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface JavaMainCaller {

    /**
     * Adds a JVM arg. Note: This is not available for in-process "forks"
     */
    void addJvmArgs(String... args);

    /**
     * Adds arguments for the process
     */
    void addArgs(String... args);

    /**
     * Adds option (basically two arguments)
     */
    void addOption(String key, String value);

    /**
     * Adds an option (key-file pair). This will pull the absolute path of the file
     */
    void addOption(String key, File value);

    /**
     * Adds the key if the value is true
     */
    void addOption(String key, boolean value);

    /**
     * request run to be redirected to maven/requester logger
     */
    void redirectToLog();

    // TODO: avoid to have several Thread to pipe stream
    // TODO: add support to inject startup command and shutdown command (on :quit)
    void run(boolean displayCmd) throws Exception;

    /**
     * Runs the JavaMain with all the built up arguments/options
     */
    boolean run(boolean displayCmd, boolean throwFailure) throws Exception;
    // /**
    // * run the command without stream redirection nor waiting for exit
    // *
    // * @param displayCmd
    // * @return the spawn Process (or null if no process was spawned)
    // * @throws Exception
    // */
    // SpawnMonitor spawn(boolean displayCmd) throws Exception;
}
