package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.log4j.Log4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author yangjinghua
 */
@Log4j
public class LarkDocumentHelper {

    public static String larkUrl = "https://yuque-api.antfin-inc.com";
    private String larkApiUrl = larkUrl + "/api/v2";
    private String xAuthToken;
    private String userLogin;
    private String groupName;
    private String repoName;
    private String tocPath;
    private Iterator<JSONObject> repoIterator;

    public LarkDocumentHelper(String xAuthToken, String groupName, String repoName, String tocPath) throws IOException {
        this.xAuthToken = xAuthToken;
        this.userLogin = getUserLogin();
        this.groupName = groupName;
        this.repoName = repoName;
        this.tocPath = tocPath;
        this.repoIterator = getRepoList().iterator();
    }

    public List<JSONObject> fetchData() throws IOException {
        List<JSONObject> retArray = new ArrayList<>();
        if (!repoIterator.hasNext()) {
            return retArray;
        }
        JSONObject repoJson = repoIterator.next();
        String repoId = repoJson.getString("id");
        JSONArray tocArray = getTocArray(repoId);
        for (JSONObject tocJson : tocArray.toJavaList(JSONObject.class)) {
            List<JSONObject> tmpArray = getTocBodyNodes(repoJson, tocJson).toJavaList(JSONObject.class);
            retArray.addAll(tmpArray);
        }
        return retArray;
    }

    private JSONObject getHeaders() {
        JSONObject headers = new JSONObject();
        headers.put("X-Auth-Token", xAuthToken);
        return headers;
    }

    private String getUserLogin() throws IOException {
        String url = String.format("%s/user", larkApiUrl);
        JSONObject retJson = Requests.get(url, new JSONObject(), getHeaders());
        log.info(String.format("url is: %s,\nretJson is \n%s", url, retJson));
        return retJson.getJSONObject("data").getString("login");
    }

    private List<JSONObject> getRepoList() throws IOException {
        List<JSONObject> repoList = new ArrayList<>();
        List<JSONObject> allRepoList = new ArrayList<>();

        String getGroupsUrl = String.format("%s/users/%s/groups", larkApiUrl, userLogin);
        JSONArray groups = Requests.get(getGroupsUrl, null, getHeaders()).getJSONArray("data");
        for (JSONObject groupJson : groups.toJavaList(JSONObject.class)) {
            String groupLogin = groupJson.getString("login");
            String url = String.format("%s/groups/%s/repos", larkApiUrl, groupLogin);
            JSONObject params = new JSONObject();
            params.put("type", "all");
            params.put("offset", 0);

            JSONArray tmpRepoList;
            do {
                tmpRepoList = Requests.get(url, params, getHeaders()).getJSONArray("data");
                allRepoList.addAll(tmpRepoList.toJavaList(JSONObject.class));
                params.put("offset", params.getIntValue("offset") + 20);
            } while (tmpRepoList.size() > 0);
        }

        String url = String.format("%s/users/%s/repos", larkApiUrl, userLogin);
        JSONObject params = new JSONObject();
        params.put("type", "all");
        params.put("offset", 0);
        JSONArray tmpRepoList;
        do {
            tmpRepoList = Requests.get(url, params, getHeaders()).getJSONArray("data");
            allRepoList.addAll(tmpRepoList.toJavaList(JSONObject.class));
            params.put("offset", params.getIntValue("offset") + 20);
        } while (tmpRepoList.size() > 0);

        allRepoList.forEach(x -> {
            Boolean checkGroupName = StringUtils.isEmpty(groupName)
                    || (x.getJSONObject("user").getString("name").equals(groupName));
            Boolean checkRepoName = StringUtils.isEmpty(repoName) || (x.getString("name").equals(repoName));
            if (checkGroupName && checkRepoName) {
                repoList.add(x);
            }
        });
        return repoList;
    }

    private JSONArray getTocArray(String repoId) throws IOException {
        List<String> pathList = new ArrayList<>(Arrays.asList(tocPath.split("\\.")));
        while (pathList.contains("")) {
            pathList.remove("");
        }
        while (pathList.contains(null)) {
            pathList.remove(null);
        }
        String url = String.format("%s/repos/%s/toc", larkApiUrl, repoId);
        JSONObject retJson = Requests.get(url, new JSONObject(), getHeaders());
        JSONArray allTocArray = retJson.getJSONArray("data");
        for (int index = 0; index < allTocArray.size(); index++) {
            JSONObject tmpJson = allTocArray.getJSONObject(index);
            StringBuilder pathSb = new StringBuilder();
            pathSb.append(tmpJson.getString("title"));
            int depth = tmpJson.getIntValue("depth");
            for (int depthIndex = depth - 1; depthIndex > 0; depthIndex--) {
                for (int innerIndex = index - 1; innerIndex >= 0; innerIndex--) {
                    tmpJson = allTocArray.getJSONObject(innerIndex);
                    if (tmpJson.getIntValue("depth") == depthIndex) {
                        pathSb.insert(0, ".").insert(0, tmpJson.getString("title"));
                        break;
                    }
                }
            }
            allTocArray.getJSONObject(index).put("path", pathSb.toString());
        }
        if (CollectionUtils.isEmpty(pathList)) {
            return allTocArray;
        }
        JSONArray retArray = new JSONArray();

        for (int index = 0; index < allTocArray.size(); index++) {
            if (allTocArray.size() < index + pathList.size()) {
                break;
            }
            Boolean check = true;
            for (int pathIndex = 0; pathIndex < pathList.size(); pathIndex++) {
                String title = allTocArray.getJSONObject(index + pathIndex).getString("title");
                String pathWord = pathList.get(pathIndex).trim();
                if (!title.equals(pathWord)) {
                    check = false;
                }
            }
            if (check) {
                retArray.add(allTocArray.getJSONObject(index + pathList.size() - 1));
                return retArray;
            }
        }
        return retArray;
    }

    private JSONArray getTocBodyNodes(JSONObject repoJson, JSONObject tocJson) throws IOException {
        JSONArray retArray = new JSONArray();

        String repoId = repoJson.getString("id");
        String repoName = repoJson.getString("name");
        String repoNameSpace = repoJson.getString("namespace");
        String repoGroupName = repoJson.getJSONObject("user").getString("name");

        String tocTitle = tocJson.getString("title");
        String tocSlug = tocJson.getString("slug");
        String tocPath = tocJson.getString("path");
        if ("#".equals(tocSlug) || StringUtils.isEmpty(tocSlug)) {
            return retArray;
        }
        String tocId = String.format("%s.%s", repoId, tocSlug);

        String url = String.format("%s/repos/%s/docs/%s", larkApiUrl, repoId, tocSlug);
        JSONObject params = new JSONObject();
        params.put("raw", 0);
        // params.put("format", "markdown");

        JSONObject retJson = Requests.get(url, params, getHeaders());
        if (retJson.containsKey("status") && retJson.getInteger("status").equals(404)) {
            return retArray;
        }
        JSONObject dataJson = retJson.getJSONObject("data");
        dataJson = dataJson == null ? new JSONObject() : dataJson;
        String body = dataJson.getString("body_html");

        for (JSONObject bodyNode : analyzeBodyToNodes(body).toJavaList(JSONObject.class)) {
            bodyNode.put("repoName", repoName);
            bodyNode.put("repoNameSpace", repoNameSpace);
            bodyNode.put("repoGroupName", repoGroupName);
            bodyNode.put("tocTitle", tocTitle);
            bodyNode.put("tocSlug", tocSlug);
            bodyNode.put("tocPath", tocPath);
            String id = StringUtils.isEmpty(bodyNode.getString("bodyName"))
                    ? String.valueOf(bodyNode.getString("bodyTitle").hashCode())
                    : bodyNode.getString("bodyName");
            bodyNode.put("bodyId", tocId + "." + id);
            retArray.add(bodyNode);
        }
        return retArray;
    }

    private static JSONObject paramHeaderLink(String line) {
        JSONObject retJson = new JSONObject();
        retJson.put("isHeader", false);
        Document document = Jsoup.parse(line);
        List<String> headerStrings = Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8", "h9");
        for (String headerString : headerStrings) {
            Elements headers = document.getElementsByTag(headerString);
            if (headers.size() > 0) {
                retJson.put("isHeader", true);
                retJson.put("text", headers.get(0).text());
                retJson.put("id", headers.get(0).attr("id"));
                break;
            }
        }
        return retJson;
    }

    private static String formatBody(String body) {
        List<String> headerStrings = Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8", "h9");
        for (String headerString : headerStrings) {
            String startHeaderString = String.format("<%s ", headerString);
            String endHeaderString = String.format("</%s>", headerString);
            body = body.replace(startHeaderString, "\n" + startHeaderString);
            body = body.replace(endHeaderString, endHeaderString + "\n");
        }
        return body;
    }

    private static JSONArray analyzeBodyToNodes(String body) {
        if (StringUtils.isEmpty(body)) {
            body = "";
        }
        JSONArray retArray = new JSONArray();
        String bodyTitle = "";
        String bodyName = "";
        StringBuilder bodyContentSb = new StringBuilder();

        for (String line : formatBody(body).split("\n")) {
            JSONObject headerJson = paramHeaderLink(line);
            if (headerJson.getBooleanValue("isHeader")) {
                if (bodyContentSb.length() > 0) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("bodyTitle", bodyTitle);
                    jsonObject.put("bodyName", bodyName);
                    jsonObject.put("bodyContent", bodyContentSb.toString());
                    retArray.add(jsonObject);
                }
                bodyTitle = headerJson.getString("text");
                bodyName = headerJson.getString("id");
                bodyContentSb = new StringBuilder();
                bodyContentSb.append(line);
            } else {
                bodyContentSb.append(line).append("\n");
            }
        }
        if (bodyContentSb.length() > 0) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("bodyTitle", bodyTitle);
            jsonObject.put("bodyName", bodyName);
            jsonObject.put("bodyContent", bodyContentSb.toString());
            retArray.add(jsonObject);
        }
        return retArray;
    }

}
