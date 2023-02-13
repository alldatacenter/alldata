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
package scala.tools;

//import com.qlangtech.tis.sql.parser.DBNode;
//import com.google.common.collect.Sets;
//import com.qlangtech.tis.manage.common.Config;
//import com.qlangtech.tis.manage.common.incr.StreamContextConstant;
//import com.qlangtech.tis.sql.parser.IDBNodeMeta;
//import junit.framework.TestCase;
//import org.apache.commons.io.FileUtils;
//import scala.tools.scala_maven_executions.LogProcessorUtils;
//import java.io.File;
//import java.io.InputStream;
//import java.util.List;
//import java.util.Set;

import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestScalaCompilerSupport extends TestCase {

//    public void testStreamCodeCompile() throws Exception {
//        // 测试增量脚本编译
//        String collection = "search4totalpay";
//        long timestamp = 20190820171040l;
//        File sourceRoot = StreamContextConstant.getStreamScriptRootDir(collection, timestamp);
//        System.out.println("source root:" + sourceRoot);
//        List<DBNode> dbsMeta = null;
//        try (InputStream input = FileUtils.openInputStream(new File(Config.getMetaCfgDir(), StreamContextConstant.getDbDependencyConfigFilePath(collection, timestamp)))) {
//            dbsMeta = DBNode.load(input);
//        }
//        assertNotNull(dbsMeta);
//        // File sourceRoot = null;
//        LogProcessorUtils.LoggerListener loggerListener = new LogProcessorUtils.LoggerListener() {
//
//            @Override
//            public void receiveLog(LogProcessorUtils.Level level, String line) {
//                System.out.println("=========================" + level + "," + line);
//            }
//        };
//        assertFalse("compile must be success", streamScriptCompile(sourceRoot, Sets.newHashSet(dbsMeta), loggerListener));
//    }
//
//    private boolean streamScriptCompile(File sourceRoot, Set<DBNode> dependencyDBNodes, LogProcessorUtils.LoggerListener loggerListener) throws Exception {
//        Set<String> dbDependenciesClasspath = IDBNodeMeta.appendDBDependenciesClasspath(dependencyDBNodes);
//        return ScalaCompilerSupport.streamScriptCompile(sourceRoot, dbDependenciesClasspath, loggerListener);
//    }
}
