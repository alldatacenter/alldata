package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

@Slf4j
public class HttpUtil {
    public static File download(String url, String destFile) throws AppException {
        File dir = new File(ResourcesUtil.getURL() + File.separator + "templates");
        if (!dir.exists()) {
            dir.mkdir();
        }

        File file = new File(dir, destFile);
        log.info(">>>httpUtil|download|url={}, templates dir={}, destFile={}", url, dir.getAbsolutePath(),
                file.getAbsoluteFile());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        try {
            Response response = client.newCall(request).execute();
            log.info(">>>httpUtil|download|responseCode={}", response.code());
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (Objects.nonNull(body)) {
                    writeFile(body, file);
                    return file;
                }
            }

            throw new AppException(url + " download error");
        } catch (Exception e) {
            log.error(">>>httpUtil|download Err|url={}, destFile={}, Err={}", url, file.getAbsoluteFile(),
                    e.getMessage(), e);
            throw new AppException(e.getMessage());
        }
    }

    private static void writeFile(ResponseBody body, File file) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            IOUtils.write(body.bytes(), fos);

        } catch (Exception e) {
            log.error(">>>httpUtil|write Err|filePath={}, Err={}", file.getAbsolutePath(), e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }
}
