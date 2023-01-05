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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * This class will call a java main method via reflection.
 * <p>
 * Note: a -classpath argument *must* be passed into the jvmargs.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class JavaMainCallerInProcess extends JavaMainCallerSupport {

    private ClassLoader _cl;

    public JavaMainCallerInProcess(String mainClassName, String classpath, String[] jvmArgs, String[] args) throws Exception {
        super(mainClassName, "", jvmArgs, args);
        // Pull out classpath and create class loader
        ArrayList<URL> urls = new ArrayList<>();
        for (String path : classpath.split(File.pathSeparator)) {
            try {
                urls.add(new File(path).toURI().toURL());
            } catch (MalformedURLException e) {
            // TODO - Do something usefull here...
            // requester.getLog().error(e);
            }
        }
        _cl = new URLClassLoader(urls.toArray(new URL[] {}), null);
    }

    @Override
    public void addJvmArgs(String... args0) {
        // TODO - Ignore classpath
        if (args0 != null) {
            for (String arg : args0) {
            // requester.getLog().warn("jvmArgs are ignored when run in process :" + arg);
            }
        }
    }

    @Override
    public boolean run(boolean displayCmd, boolean throwFailure) throws Exception {
        try {
            runInternal(displayCmd);
            return true;
        } catch (Exception e) {
            if (throwFailure) {
                throw e;
            }
            return false;
        }
    }

    // /**
    // * spawns a thread to run the method
    // */
    // @Override
    // public SpawnMonitor spawn(final boolean displayCmd) {
    // final Thread t = new Thread(() -> {
    // try {
    // runInternal(displayCmd);
    // } catch (Exception e) {
    // // Ignore
    // }
    // });
    // t.start();
    // return t::isAlive;
    // }
    /**
     * Runs the main method of a java class
     */
    private void runInternal(boolean displayCmd) throws Exception {
        String[] argArray = args.toArray(new String[] {});
        if (displayCmd) {
        // requester.getLog().info("cmd : " + mainClassName + "(" + StringUtils.join(argArray, ",") + ")");
        }
        MainHelper.runMain(mainClassName, args, _cl);
    }

    @Override
    public void redirectToLog() {
    // requester.getLog().warn("redirection to log is not supported for 'inProcess' mode");
    }
}
