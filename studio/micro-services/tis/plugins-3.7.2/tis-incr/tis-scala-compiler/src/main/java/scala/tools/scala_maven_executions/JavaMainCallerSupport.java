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

import org.apache.commons.lang.StringUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract helper implementation for JavaMainCaller interface.
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class JavaMainCallerSupport implements JavaMainCaller {

    // protected AbstractMojo requester;
    protected List<String> env = new ArrayList<String>();

    protected String mainClassName;

    protected List<String> jvmArgs = new ArrayList<String>();

    protected List<String> args = new ArrayList<String>();

    protected JavaMainCallerSupport(String mainClassName1, String classpath, String[] jvmArgs1, String[] args1) throws Exception {
        // this.requester = requester1;
        for (String key : System.getenv().keySet()) {
            env.add(key + "=" + System.getenv(key));
        }
        this.mainClassName = mainClassName1;
        if (StringUtils.isNotEmpty(classpath)) {
            addJvmArgs("-classpath", classpath);
        }
        addJvmArgs(jvmArgs1);
        addArgs(args1);
    }

    @Override
    public void addJvmArgs(String... args0) {
        if (args0 != null) {
            for (String arg : args0) {
                if (StringUtils.isNotEmpty(arg)) {
                    this.jvmArgs.add(arg);
                }
            }
        }
    }

    public void addToClasspath(File entry) throws Exception {
        if ((entry == null) || !entry.exists()) {
            return;
        }
        boolean found = false;
        boolean isClasspath = false;
        for (int i = 0; i < jvmArgs.size(); i++) {
            String item = jvmArgs.get(i);
            if (isClasspath) {
                item = item + File.pathSeparator + entry.getCanonicalPath();
                jvmArgs.set(i, item);
                isClasspath = false;
                found = true;
                break;
            }
            isClasspath = "-classpath".equals(item);
        }
        if (!found) {
            addJvmArgs("-classpath", entry.getCanonicalPath());
        }
    }

    @Override
    public void addOption(String key, String value) {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(key)) {
            return;
        }
        addArgs(key, value);
    }

    @Override
    public void addOption(String key, File value) {
        if ((value == null) || StringUtils.isEmpty(key)) {
            return;
        }
        addArgs(key, value.getAbsolutePath());
    }

    @Override
    public void addOption(String key, boolean value) {
        if ((!value) || StringUtils.isEmpty(key)) {
            return;
        }
        addArgs(key);
    }

    @Override
    public void addArgs(String... args1) {
        if (args1 != null) {
            for (String arg : args1) {
                if (StringUtils.isNotEmpty(arg)) {
                    this.args.add(arg);
                }
            }
        }
    }

    @Override
    public void run(boolean displayCmd) throws Exception {
        run(displayCmd, true);
    }
}
