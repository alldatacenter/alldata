/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.server.controller;

import datart.core.common.FileUtils;
import datart.core.entity.Download;
import datart.server.base.dto.ResponseData;
import datart.server.base.params.DownloadCreateParam;
import datart.server.service.DownloadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;


@Api
@RestController
@RequestMapping(value = "/download")
public class DownloadController extends BaseController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @ApiOperation(value = "get download tasks")
    @GetMapping(value = "/tasks")
    public ResponseData<List<Download>> listDownloadTasks() {
        ResponseData.ResponseDataBuilder<List<Download>> builder = ResponseData.builder();
        return ResponseData.success(downloadService.listDownloadTasks());
    }

    @ApiOperation(value = "submit a new download task")
    @PostMapping(value = "/submit/task")
    public ResponseData<Download> submitDownloadTask(@RequestBody @Validated DownloadCreateParam createParam) {
        return ResponseData.success(downloadService.submitDownloadTask(createParam));
    }

    @ApiOperation(value = "get download file")
    @GetMapping(value = "/files/{id}")
    public void downloadFile(@PathVariable String id,
                             HttpServletResponse response) throws IOException {
        Download download = downloadService.downloadFile(id);
        response.setHeader("Content-Type", "application/octet-stream");
        File file = new File(FileUtils.withBasePath(download.getPath()));
        response.setHeader("Content-Disposition", String.format("attachment;filename=\"%s\"", URLEncoder.encode(file.getName(), "utf-8")));
        try (InputStream inputStream = new FileInputStream(file)) {
            Streams.copy(inputStream, response.getOutputStream(), true);
        }
    }

}