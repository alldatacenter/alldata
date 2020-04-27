package com.platform.devops.autogen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Created by wulinhao on 2020/04/12.
 */
public class UpdaterMain {
    static public RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000)
            .setConnectTimeout(30000).setConnectionRequestTimeout(30000).build();


    public static void main(String[] args) {
        try {
            String currDir = args[0];
            String re = getJson("http://120.77.155.220:8085/BigDataPlatform/system-devops");
            JsonParser jp = new JsonParser();
            JsonObject jo = jp.parse(re).getAsJsonObject();
            JsonArray ja = jo.get("data").getAsJsonArray();
            List<String> lists = new ArrayList<>();
            for (int i = 0; i < ja.size(); i++) {
                JsonObject one = ja.get(i).getAsJsonObject();
                String text = one.get("text").getAsString();
                if (text.startsWith("v")) {
                    lists.add(text);
                }
            }
            Collections.sort(lists);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("avaliable version:");
                for (String one : lists) {
                    System.out.println(one);
                }
                System.out.println("please enter the version name you want:");
                String versionName = scanner.next();
                if (lists.contains(versionName)) {
                    System.out.println("update to " + versionName);
                    boolean ok = downloadJar(currDir, versionName);
                    if (ok) System.out.println("update finish " + versionName);
                    else System.out.println("update failed try to download and replace handle");
                    break;
                } else {
                    System.out.println("input is error!not found " + versionName + ".");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean downloadJar(String currDir, String version) {
        FileOutputStream os = null;
        try {
            String libDir = currDir + "/lib";
            File d = new File(libDir);
            if (d.exists()) {
                for (File one : d.listFiles()) {
                    System.out.println("delete " + one.getPath());
                    one.delete();
                }
            } else {
                System.out.println("build dir");
                d.mkdirs();
            }
            String url = String.format("http://120.77.155.220:8085/BigDataPlatform/system-devops/%s/system-devops-autogen-%s-jar-with-dependencies.jar", version, version);
            String md5 = String.format("http://120.77.155.220:8085/BigDataPlatform/system-devops/%s/system-devops-autogen-%s-jar-with-dependencies.jar.md5", version, version);

            String file = String.format("%s/system-devops-autogen-%s-jar-with-dependencies.jar", libDir, version);


            os = new FileOutputStream(file);
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                IOUtils.copy(response.getEntity().getContent(), os);
                os.close();
                return checkMd5(file, md5);
            } else {
                System.out.println("down from " + url + " is not 200");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                os.close();
            } catch (IOException ee) {
                //ignore
            }
            return false;
        }
    }

    public static boolean checkMd5(String file, String md5Url) {
        try {
            file = file.replaceAll( "\\\\",   "/");
            FileInputStream fis = new FileInputStream(new File(file));
            String fmd5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
            fis.close();
            String hmd5 = getString(md5Url);
            if (fmd5.equals(hmd5)) {
                System.out.println("md5 check is ok");
                return true;
            } else {
                System.out.println("md5 is diff. local is " + fmd5 + " remote is " + hmd5);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getJson(String url) {
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                byte[] b = IOUtils.toByteArray(response.getEntity().getContent());
                String str = new String(b);
                return str;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public static String getString(String url) {
        try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                byte[] b = IOUtils.toByteArray(response.getEntity().getContent());
                String str = new String(b);
                return str;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}
