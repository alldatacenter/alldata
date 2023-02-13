package com.alibaba.tesla.tkgone.server.consumer;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.domain.dto.ConsumerDto;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yangjinghua
 */
@Log4j
@Service
public class GitBookMarkdownDocumentConsumer extends AbstractConsumer implements InitializingBean {

    private static Pattern pattern = Pattern.compile("\\[([^\\[\\]]+)\\]\\(([^\\(\\)]*)\\)");

    @Override
    public void afterPropertiesSet() {
        afterPropertiesSet(10, 10, ConsumerSourceType.gitBookMarkdownDocument);
    }

    @Override
    int consumerDataByConsumerDto(ConsumerDto consumerDto) throws Exception {

        String path = consumerDto.getSourceInfoJson().getString("path");
        boolean isPartition = consumerDto.getSourceInfoJson().getBooleanValue("isPartition");

        List<JSONObject> jsonObjectList = readGitBookMarkdown(path);
        String partition = isPartition ? Tools.currentDataString() : Constant.DEFAULT_PARTITION;
        return saveToBackendStore(consumerDto, jsonObjectList, consumerDto.getImportConfigArray(), partition);
    }

    private List<JSONObject> readGitBookMarkdown(String path) throws Exception {
        JSONArray retArray = new JSONArray();
        JSONArray summaryPathArray = parseSummary(path);

        for (JSONObject summaryJson : summaryPathArray.toJavaList(JSONObject.class)) {
            String relativeFilePath = summaryJson.getString("relativeFilePath");
            String name = summaryJson.getString("name");
            String namePath = summaryJson.getString("namePath");
            String filePath = path + "/" + relativeFilePath;
            if (filePath.endsWith(".md")) {
                for (JSONObject tmpJson : readMarkdownFileToArray(filePath).toJavaList(JSONObject.class)) {
                    tmpJson.put("name", name);
                    tmpJson.put("namePath", namePath);
                    tmpJson.put("relativeFilePath", relativeFilePath);
                    retArray.add(tmpJson);
                }
            }
        }
        return retArray.toJavaList(JSONObject.class);
    }

    private JSONArray parseSummary(String path) throws Exception {
        String summaryPath = path + "/SUMMARY.md";
        String content = Tools.readFromFile(summaryPath);
        JSONArray allPathInfo = new JSONArray();
        JSONObject spaceNumJson = new JSONObject();
        for (String line : content.split("\n")) {
            if (line.trim().startsWith("*")) {
                try {
                    int spaceNum = line.split("\\*")[0].length();
                    String name = line.split("\\[")[1].split("]")[0];
                    spaceNumJson.put(String.valueOf(spaceNum), name);
                    String relativeFilePath = line.split("\\(")[1].split("\\)")[0];
                    StringBuilder namePathSb = new StringBuilder("/");
                    for (int tmpInt = 0; tmpInt < spaceNum; tmpInt++) {
                        String tmpString = String.valueOf(tmpInt);
                        if (spaceNumJson.containsKey(tmpString)) {
                            namePathSb.append(spaceNumJson.getString(tmpString)).append("/");
                        }
                    }
                    JSONObject tmpJson = new JSONObject();
                    tmpJson.put("name", name);
                    tmpJson.put("namePath", namePathSb.toString() + name);
                    tmpJson.put("relativeFilePath", relativeFilePath);
                    allPathInfo.add(tmpJson);
                } catch (Exception e) {
                    log.error(String.format("解析失败: %s", line));
                }
            }
        }
        return allPathInfo;
    }

    private boolean isLineTitle(String line) {
        line = StringUtils.strip(line, "* ");
        List<String> titleStartStringList = new ArrayList<>(
            Arrays.asList("# ", "## ", "### ", "#### ", "##### ", "###### "));

        for (String titleStartString : titleStartStringList) {
            if (line.startsWith(titleStartString)) {
                return true;
            }
        }
        return false;
    }

    private String getLinkTitle(Map<String, Integer> linkTitleNumMap, String title) {
        Matcher m = pattern.matcher(title);
        if (m.find() && m.groupCount() == 2) {
            title = title.replace(m.group(0), m.group(1));
        }
        List<String> strings = Arrays.asList(".", "\\", "_", "%", "!", "@", "(", ")", "'", ",", "/", "[", "]"
            , ":", ";", "\"", "?", "$", "^", "&", "*", "{", "}"
            , "‘", "’", "，", "“", "”");
        for (String string : strings) {
            title = title.replace(string, "");
        }
        title = title.replace(" ", "-");
        title = title.toLowerCase();
        if (!linkTitleNumMap.containsKey(title)) {
            linkTitleNumMap.put(title, -1);
        }
        linkTitleNumMap.put(title, linkTitleNumMap.get(title) + 1);
        if (linkTitleNumMap.get(title) > 0) {
            title += "_" + linkTitleNumMap.get(title);
        }
        return title;
    }

    private JSONArray readMarkdownFileToArray(String filePath) {
        Map<String, Integer> linkTitleNumMap = new HashMap<>();
        JSONArray retArray = new JSONArray();
        String title = "";
        String linkTitle = "";
        StringBuilder contentSb = new StringBuilder();
        List<String> lines;
        try {
            lines = Tools.readLinesFromFile(filePath);
        } catch (Exception e) {
            log.error(String.format("filePath %s 文件不存在", filePath));
            return retArray;
        }
        if (CollectionUtils.isEmpty(lines)) {
            return retArray;
        }
        int lineNum = 0;
        for (; lineNum < lines.size(); lineNum++) {
            String line = lines.get(lineNum);
            if (isLineTitle(line)) {
                if (!StringUtils.isEmpty(title)) {
                    JSONObject eachJson = new JSONObject();
                    eachJson.put("filePath", filePath);
                    eachJson.put("lineNum", lineNum);
                    eachJson.put("title", title);
                    eachJson.put("linkTitle", linkTitle);
                    eachJson.put("content", contentSb.toString());
                    retArray.add(eachJson);
                }
                title = StringUtils.strip(line, "*# ");
                linkTitle = getLinkTitle(linkTitleNumMap, title);
                contentSb = new StringBuilder();
            } else {
                contentSb.append(line).append("\n");
            }
        }
        JSONObject eachJson = new JSONObject();
        eachJson.put("filePath", filePath);
        eachJson.put("lineNum", lineNum);
        eachJson.put("title", title);
        eachJson.put("linkTitle", linkTitle);
        eachJson.put("content", contentSb.toString());
        retArray.add(eachJson);
        return retArray;
    }

}
