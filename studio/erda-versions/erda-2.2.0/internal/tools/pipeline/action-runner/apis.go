// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package actionrunner

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

// TaskListResponse .
type TaskListResponse struct {
	apistructs.Header
	Data []*Task
}

// fetchTasks invoke HTTP API to fetch tasks.
func (r *Runner) fetchTasks() []*Task {
	request := httpclient.New(httpclient.WithCompleteRedirect()).Get(r.Conf.OpenAPI).
		Path("/api/runner/fetch-task").
		Header("Content-Type", "application/json").
		Header("Authorization", r.Conf.Token)
	var resp TaskListResponse
	httpResp, err := request.Do().JSON(&resp)
	if err != nil {
		logrus.Errorf("fail to fetch task: %s", err)
		return nil
	}
	if !httpResp.IsOK() {
		logrus.Errorf("fail to upload file, status code: %d, body: %s", httpResp.StatusCode(), string(httpResp.Body()))
		return nil
	}
	if !resp.Success {
		logrus.Errorf("fail to fetch task: %s", resp.Error.Msg)
		return nil
	}
	if len(resp.Data) > 0 {
		byts, _ := json.Marshal(resp.Data)
		logrus.Infof("fetch task: %s", string(byts))
	}
	return resp.Data
}

func (w *worker) taskResultCallback(id int, status, fileURL string) error {
	request := httpclient.New(httpclient.WithCompleteRedirect()).Put(w.r.Conf.OpenAPI).
		Path("/api/runner/tasks/"+strconv.Itoa(id)).
		Header("Content-Type", "application/json").
		Header("Authorization", w.r.Conf.Token)
	var resp apistructs.Header
	httpResp, err := request.JSONBody(map[string]interface{}{
		"status":          status,
		"result_data_url": fileURL,
	}).Do().JSON(&resp)
	if err != nil {
		return err
	}
	if !httpResp.IsOK() {
		return fmt.Errorf("fail to task %d callback, status code: %d, body: %s", id, httpResp.StatusCode(), string(httpResp.Body()))
	}
	if !resp.Success {
		return fmt.Errorf(resp.Error.Msg)
	}
	return nil
}

// uploadFile upload files using HTTP API: api/files
func (w *worker) uploadFile(filePath, token string) (string, error) {
	w.log.Infof("uploading file %s", filePath)
	f, err := os.Open(filePath)
	if err != nil {
		return "", err
	}
	stat, err := f.Stat()
	if err != nil {
		return "", err
	}
	w.log.Infof("upload file size %dMB", stat.Size()/1024/1024)
	fileName := filepath.Base(filePath)
	multiparts := map[string]httpclient.MultipartItem{
		"file": {
			Reader:   f,
			Filename: fileName,
		},
	}

	request := httpclient.New(httpclient.WithCompleteRedirect(),
		httpclient.WithTimeout(15*time.Second, 300*time.Second)).Post(w.r.Conf.OpenAPI).
		Path("/api/files").
		Param("fileFrom", "runner-cli").
		Param("expiredIn", "3600s").
		Param("public", "true").
		Header("Authorization", token).
		MultipartFormDataBody(multiparts)
	var resp apistructs.FileUploadResponse
	httpResp, err := request.Do().JSON(&resp)
	if err != nil {
		return "", err
	}
	if !httpResp.IsOK() {
		return "", fmt.Errorf("fail to upload file, status code: %d, body: %s", httpResp.StatusCode(), string(httpResp.Body()))
	}
	if !resp.Success {
		return "", fmt.Errorf(resp.Error.Msg)
	}
	if resp.Data == nil || len(resp.Data.DownloadURL) <= 0 {
		return "", fmt.Errorf("fail to upload file, no url in response")
	}
	return resp.Data.DownloadURL, nil
}

func (w *worker) getTaskStatus(id int, token string) (bool, error) {
	type Response struct {
		apistructs.Header
		Data *Task `json:"data"`
	}
	request := httpclient.New(httpclient.WithCompleteRedirect()).Get(w.r.Conf.OpenAPI).
		Path("/api/runner/tasks/"+strconv.Itoa(id)).
		Header("Content-Type", "application/json").
		Header("Authorization", token)
	var resp Response
	httpResp, err := request.Do().JSON(&resp)
	if err != nil {
		return false, err
	}
	if !httpResp.IsOK() {
		// if token is invalid, skip report and exit directly.
		if httpResp.StatusCode() == 403 {
			return false, nil
		}
		return false, fmt.Errorf("fail to get task %d, status code: %d, body: %s", id, httpResp.StatusCode(), string(httpResp.Body()))
	}
	if !resp.Success {
		return false, fmt.Errorf(resp.Error.Msg)
	}
	if resp.Data == nil {
		return false, fmt.Errorf("fail to get task, data is empty")
	}
	if resp.Data.Status != "running" {
		return false, nil
	}
	return true, nil
}

func (l *logger) collectLogs(list []*LogEntry) error {
	request := httpclient.New(httpclient.WithCompleteRedirect(),
		httpclient.WithTimeout(10*time.Second, 30*time.Second)).Post(l.url).
		Header("Content-Type", "application/json").
		Path("/api/runner/collect/logs/container")
	resp, err := request.JSONBody(list).Do().DiscardBody()
	if err != nil {
		return err
	}
	if resp.StatusCode() >= 300 {
		return fmt.Errorf("invalid http status code %d", resp.StatusCode())
	}
	return nil
}
