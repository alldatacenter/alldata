package com.platform.devops.autogen.impl;

import com.platform.devops.autogen.InputParams;
import com.platform.devops.autogen.TableInfo;
import com.platform.devops.autogen.TmpBuilder;
import com.platform.devops.autogen.TmpUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wulinhao on 2020/04/13.
 */
public class ScalaPlayImpl extends TmpBuilder {
    String newDir = "/scala-play/new_project";
    String crudDir = "/scala-play/crud";

    public ScalaPlayImpl(InputParams ip) {
        super(ip);
    }

    @Override
    protected void buildWithDb(List<TableInfo> tableInfos) throws IOException {
        //copy all file to target
        {
            List<Path> paths = getResourceFolderFiles(newDir);
            for (Path one : paths) {
                String path = one.toAbsolutePath().toString();
                String fromPath = findFromPath(path, newDir);
                String targetPath = findTargetPath(path, newDir);

                File targetFile = new File(targetPath);
                if (targetFile.exists()) {
                    System.out.println(targetPath + " is exist and skip copy");
                } else {
                    if (one.getFileName().toString().endsWith(".vm")) {
                        System.out.println("build vm from " + fromPath + " to " + targetPath);
                        VelocityContext ctx = getContext();
                        String content = buildVm(fromPath, ctx);
                        writeFile(targetPath, content);
                    } else {
                        System.out.println("copy " + path + " to " + targetPath);
                        String newContent = StringUtils.join(Files.readAllLines(one), "\n");
                        writeFile(targetPath, newContent);
                    }
                }
            }
        }
        if (tableInfos != null && tableInfos.size() > 0) {
            //build crud
            List<Path> paths = getResourceFolderFiles(crudDir);

            for (TableInfo tInfo : tableInfos) {

                String table = tInfo.name;
                String lowerTable = TmpUtils.underlineToLowerUpperFirst(table);
                String upperTable = TmpUtils.underlineToUpperFirst(table);
                VelocityContext ctx = getContext();
                ctx.put("table", table);
                ctx.put("lowerTable", lowerTable);
                ctx.put("upperTable", upperTable);

                ctx.put("tableInfo", tInfo);
                for (Path one : paths) {
                    String path = one.toAbsolutePath().toString();
                    String fromPath = findFromPath(path, crudDir);
                    String lastPath = findTargetPath(path, crudDir);
                    String targetPath = StringUtils.replace(lastPath, "Test", upperTable);
                    if (one.getFileName().toString().equals("routes.vm")) {
                        String content = buildVm(fromPath, ctx);
                        //找到routes
                        File targetFile = new File(targetPath);
                        String startMark = "#autorouters-start-" + lowerTable;
                        String endMark = "#autorouters-end-" + lowerTable;
                        List<String> readIns = FileUtils.readLines(targetFile, StandardCharsets.UTF_8);
                        TmpUtils.SplitLines parts = TmpUtils.splitLinesWithMark(readIns, startMark, endMark);
                        List<String> lastPrint = new ArrayList<>();
                        lastPrint.addAll(parts.headPart);
                        lastPrint.add(content);
                        if (parts.lastPart.size() > 0) {
                            System.out.println("replace " + startMark + " into " + targetPath);
                            lastPrint.addAll(parts.lastPart);
                        } else {
                            System.out.println("insert " + startMark + " into " + targetPath);
                        }
                        String newContent = StringUtils.join(lastPrint, "\n");
                        writeFile(targetPath, newContent);
                    } else {
                        if (shouldSkip(targetPath, upperTable)) {
                            System.out.println(targetPath + " is exist and skip copy");
                        } else {
                            String content = buildVm(fromPath, ctx);
                            System.out.println("build vm from " + fromPath + " to " + targetPath);
                            writeFile(targetPath, content);
                        }
                    }
                }
            }
        }
    }

    protected boolean shouldSkip(String targetPath, String upperTable) {
        if (!targetPath.contains("app/auto/" + upperTable)) {
            //检测是否存在覆盖
            File targetFile = new File(targetPath);
            if (targetFile.exists()) {
                return true;
            }
        }
        return false;
    }

    protected String findFromPath(String path, String dir) {
        int start = path.indexOf(dir);
        String subPath = path.substring(start + dir.length());
        return dir + subPath;
    }

    protected String findTargetPath(String path, String dir) {
        int start = path.indexOf(dir);
        String subPath = path.substring(start + dir.length());
        String endPath = subPath;
        if (subPath.endsWith(".vm")) {
            endPath = subPath.substring(0, subPath.length() - 3);
        }
        String targetPath = ip.dir + endPath;
        String[] temp = targetPath.split("/");
        String name = temp[temp.length - 1];
        if (name.startsWith("_")) {
            String newName = "." + name.substring(1);
            temp[temp.length - 1] = newName;
        }
        return StringUtils.join(temp, "/");
    }
}
