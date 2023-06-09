///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.fullbuild.taskflow.odps;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FilenameFilter;
//import java.io.InputStreamReader;
//import java.io.OutputStreamWriter;
//import java.io.Reader;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.apache.velocity.VelocityContext;
//import org.apache.velocity.app.Velocity;
//import com.qlangtech.tis.fullbuild.taskflow.TemplateContext;
//import com.qlangtech.tis.manage.common.TisUTF8;
//
///**
// * 解析odps sql模板脚本，将模板脚本解析成可执行脚本
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2014年7月21日下午2:54:18
// */
//public class ODPSTaskCreator {
//
//    // private static final String ENCODE = "utf8";
//    private final File outputDir;
//
//    private final String tplDir;
//
//    private final TemplateContext context;
//
//    private String odpsProject;
//
//    private String odpsHome;
//
//    private static final Logger log = LoggerFactory.getLogger(ODPSTaskCreator.class);
//
//    /**
//     * @param outputDir
//     * @param tplDir
//     */
//    public ODPSTaskCreator(String outputDir, String tplDir, TemplateContext templateContext) {
//        super();
//        this.outputDir = new File(outputDir);
//        this.tplDir = tplDir;
//        this.context = templateContext;
//    }
//
//    /**
//     * @param arg
//     * @throws Exception
//     */
//    public static void main(String[] arg) throws Exception {
//        final String outputDir = System.getProperty("tploutdir");
//        final String tplDir = System.getProperty("tplindir");
//        final String odpsHome = System.getProperty("odpsHome");
//        final String odpsProject = System.getProperty("odpsProject");
//        // 导入数据
//        TemplateContext templateContext = new TemplateContext(null);
//        // 在odps上整理数据
//        ODPSTaskCreator creator = new ODPSTaskCreator(outputDir, tplDir, templateContext);
//        creator.setOdpsHome(odpsHome);
//        creator.setOdpsProject(odpsProject);
//        creator.parseTemplate();
//        // 数据表整理
//        boolean success = creator.commitTask();
//        System.out.println("over");
//    }
//
//    /**
//     * 解析模板
//     */
//    private void parseTemplate() {
//        File dir = new File(tplDir);
//        if (!dir.isDirectory()) {
//            throw new IllegalArgumentException("dir:" + dir.getAbsolutePath() + " is not directory");
//        }
//        VelocityContext velocityContext = new VelocityContext();
//        velocityContext.put("odps", context);
//        File tpl = null;
//        for (String f : dir.list()) {
//            tpl = new File(dir, f);
//            mergeSystemParameter(velocityContext, new File(this.outputDir, context.getDate() + "_" + StringUtils.substringBeforeLast(f, ".tpl")), tpl);
//        }
//    }
//
//    /**
//     * 提交odps处理单表和打宽表的任务
//     */
//    private boolean commitTask() throws Exception {
//        final String[] scripts = getSortScriptsName();
//        File sqlFile = null;
//        for (String f : scripts) {
//            sqlFile = new File(outputDir, f);
//            log.info(sqlFile.getPath());
//            String cmd = this.getOdpsHome() + "/bin/odpscmd  -f " + sqlFile.getAbsolutePath() + " --project=" + this.getOdpsProject();
//            Process process = Runtime.getRuntime().exec(cmd);
//            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "gbk"));
//            String line = null;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//                if (StringUtils.contains(line, "FAILED:")) {
//                    // odps 执行失败
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
//
//    private static final Pattern PREFIX_PATTERN = Pattern.compile("^20\\d+_\\d+_.+");
//
//    /**
//     * @return
//     */
//    private String[] getSortScriptsName() {
//        final String[] scripts = outputDir.list(new FilenameFilter() {
//
//            @Override
//            public boolean accept(File dir, String name) {
//                Matcher match = PREFIX_PATTERN.matcher(name);
//                return match.find();
//            }
//        });
//        Arrays.sort(scripts, 0, scripts.length, new Comparator<String>() {
//
//            @Override
//            public int compare(String str1, String str2) {
//                int val1 = Integer.parseInt(StringUtils.split(str1, "_")[1]);
//                int val2 = Integer.parseInt(StringUtils.split(str2, "_")[1]);
//                return val1 - val2;
//            }
//        });
//        return scripts;
//    }
//
//    private void mergeSystemParameter(VelocityContext context, File output, File tplfile) {
//        OutputStreamWriter writer = null;
//        Reader reader = null;
//        try {
//            writer = new OutputStreamWriter(FileUtils.openOutputStream(output, false), TisUTF8.get());
//            reader = new InputStreamReader(FileUtils.openInputStream(tplfile), TisUTF8.get());
//            Velocity.evaluate(context, writer, "odps", reader);
//            writer.flush();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        } finally {
//            IOUtils.closeQuietly(writer);
//            IOUtils.closeQuietly(reader);
//        }
//    }
//
//    public String getOdpsProject() {
//        if (StringUtils.isBlank(odpsProject)) {
//            throw new IllegalStateException("odpsProject can not be blank");
//        }
//        return odpsProject;
//    }
//
//    public void setOdpsProject(String odpsProject) {
//        this.odpsProject = odpsProject;
//    }
//
//    public String getOdpsHome() {
//        if (StringUtils.isBlank(odpsHome)) {
//            throw new IllegalStateException("odpsHome can not be blank");
//        }
//        return odpsHome;
//    }
//
//    public void setOdpsHome(String odpsHome) {
//        this.odpsHome = odpsHome;
//    }
//}
