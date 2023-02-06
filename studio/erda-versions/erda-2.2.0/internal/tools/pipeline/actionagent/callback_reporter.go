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

package actionagent

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/filehelper"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

var (
	defaultClientTimeout     = 5 * time.Second
	defaultFileUploadTimeout = 30 * time.Second
)

type CallbackReporter interface {
	CallbackToPipelinePlatform(cbReq apistructs.PipelineCallbackRequest) error
	UploadFile(pipelineID, taskID uint64, file *os.File) (apistructs.FileUploadResponse, error)
	GetBootstrapInfo(pipelineID, taskID uint64) (apistructs.PipelineTaskGetBootstrapInfoResponse, error)
	GetCmsFile(uuid string, absPath string) error
	SetOpenApiToken(token string)
	SetCollectorAddress(address string)
	PushCollectorLog(logLines *[]apistructs.LogPushLine) error
}

type CenterCallbackReporter struct {
	OpenAPIAddr          string
	OpenAPIToken         string
	TokenForBootstrap    string
	CollectorAddr        string
	FileStreamTimeoutSec time.Duration
}

func (cr *CenterCallbackReporter) CallbackToPipelinePlatform(cbReq apistructs.PipelineCallbackRequest) error {
	var resp apistructs.PipelineCallbackResponse

	r, err := httpclient.New(httpclient.WithCompleteRedirect(), httpclient.WithTimeout(defaultClientTimeout, defaultClientTimeout)).
		Post(cr.OpenAPIAddr).
		Path("/api/pipelines/actions/callback").
		Header("Authorization", cr.OpenAPIToken).
		JSONBody(&cbReq).
		Do().
		JSON(&resp)
	if err != nil {
		return err
	}
	if !r.IsOK() || !resp.Success {
		return errors.Errorf("status-code %d, resp %#v", r.StatusCode(), resp)
	}
	return nil
}

func (cr *CenterCallbackReporter) UploadFile(pipelineID, taskID uint64, file *os.File) (apistructs.FileUploadResponse, error) {
	var uploadResp apistructs.FileUploadResponse
	resp, err := httpclient.New(httpclient.WithCompleteRedirect(), httpclient.WithTimeout(cr.FileStreamTimeoutSec, cr.FileStreamTimeoutSec)).
		Post(cr.OpenAPIAddr).
		Path("/api/files").
		Param("fileFrom", fmt.Sprintf("action-upload-%d-%d", pipelineID, taskID)).
		Param("expiredIn", "168h").
		Header("Authorization", cr.TokenForBootstrap).
		MultipartFormDataBody(map[string]httpclient.MultipartItem{
			"file": {Reader: file},
		}).
		Do().
		JSON(&uploadResp)
	if err != nil {
		return uploadResp, err
	}
	if !resp.IsOK() || !uploadResp.Success {
		return uploadResp, fmt.Errorf("statusCode: %d, respError: %s", resp.StatusCode(), uploadResp.Error)
	}
	return uploadResp, nil
}

func (cr *CenterCallbackReporter) GetBootstrapInfo(pipelineID, taskID uint64) (apistructs.PipelineTaskGetBootstrapInfoResponse, error) {
	var body bytes.Buffer
	var getResp apistructs.PipelineTaskGetBootstrapInfoResponse
	r, err := httpclient.New(httpclient.WithCompleteRedirect(), httpclient.WithTimeout(defaultClientTimeout, defaultClientTimeout)).
		Get(cr.OpenAPIAddr).
		Path(fmt.Sprintf("/api/pipelines/%d/tasks/%d/actions/get-bootstrap-info", pipelineID, taskID)).
		Header("Authorization", cr.TokenForBootstrap).
		Do().
		Body(&body)
	if err != nil {
		return getResp, err
	}
	if !r.IsOK() {
		return getResp, errors.Errorf("status-code: %d, resp body: %s", r.StatusCode(), body.String())
	}
	if err := json.NewDecoder(&body).Decode(&getResp); err != nil {
		return getResp, errors.Errorf("status-code: %d, failed to json unmarshal get-bootstrap-resp, err: %v", r.StatusCode(), err)
	}
	return getResp, nil
}

func (cr *CenterCallbackReporter) SetOpenApiToken(token string) {
	cr.OpenAPIToken = token
}

func (cr *CenterCallbackReporter) SetCollectorAddress(address string) {
	cr.CollectorAddr = address
}

func (cr *CenterCallbackReporter) PushCollectorLog(logLines *[]apistructs.LogPushLine) error {
	var respBody bytes.Buffer
	b, _ := json.Marshal(logLines)
	logrus.Debugf("push collector log data: %s", string(b))
	resp, err := httpclient.New(httpclient.WithCompleteRedirect(), httpclient.WithTimeout(defaultClientTimeout, defaultClientTimeout)).
		Post(cr.CollectorAddr).
		Path("/collect/logs/job").
		JSONBody(logLines).
		Header("Content-Type", "application/json").
		Do().
		Body(&respBody)
	if err != nil {
		return fmt.Errorf("failed to push log to collector, err: %v", err)
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to push log to collector, resp body: %s", respBody.String())
	}
	return nil
}

func (cr *CenterCallbackReporter) GetCmsFile(uuid string, absPath string) error {
	respBody, resp, err := httpclient.New(httpclient.WithCompleteRedirect(), httpclient.WithTimeout(cr.FileStreamTimeoutSec, cr.FileStreamTimeoutSec)).
		Get(cr.OpenAPIAddr).
		Path("/api/files").
		Param("file", uuid).
		Header("Authorization", cr.TokenForBootstrap).
		Do().StreamBody()
	if err != nil {
		return errors.Errorf("failed to download cms file, uuid: %s, err: %v",
			uuid, err)
	}
	if !resp.IsOK() {
		bodyBytes, _ := ioutil.ReadAll(respBody)
		return errors.Errorf("failed to download cms file, uuid: %s, err: %v",
			uuid, string(bodyBytes))
	}
	if err := filehelper.CreateFile2(absPath, respBody, 0755); err != nil {
		return err
	}
	return nil
}

type EdgeCallbackReporter struct {
	PipelineAddr         string
	OpenAPIToken         string
	TokenForBootstrap    string
	CollectorAddr        string
	FileStreamTimeoutSec time.Duration
}

func (er *EdgeCallbackReporter) CallbackToPipelinePlatform(cbReq apistructs.PipelineCallbackRequest) error {
	var resp apistructs.PipelineCallbackResponse

	r, err := httpclient.New(httpclient.WithCompleteRedirect(), httpclient.WithTimeout(defaultClientTimeout, defaultClientTimeout)).
		Post(er.PipelineAddr).
		Path("/api/pipelines/actions/callback").
		Header(httputil.InternalHeader, "edge-pipeline").
		Header("Authorization", er.OpenAPIToken).
		JSONBody(&cbReq).
		Do().
		JSON(&resp)
	if err != nil {
		return err
	}
	if !r.IsOK() || !resp.Success {
		return errors.Errorf("status-code %d, resp %#v", r.StatusCode(), resp)
	}
	return nil
}

func (er *EdgeCallbackReporter) UploadFile(pipelineID, taskID uint64, file *os.File) (apistructs.FileUploadResponse, error) {
	return apistructs.FileUploadResponse{}, fmt.Errorf("edge pipeline doesn't support upload file")
}

func (er *EdgeCallbackReporter) GetBootstrapInfo(pipelineID, taskID uint64) (apistructs.PipelineTaskGetBootstrapInfoResponse, error) {
	var body bytes.Buffer
	var getResp apistructs.PipelineTaskGetBootstrapInfoResponse
	r, err := httpclient.New(httpclient.WithCompleteRedirect(), httpclient.WithTimeout(defaultClientTimeout, defaultClientTimeout)).
		Get(er.PipelineAddr).
		Path(fmt.Sprintf("/api/pipelines/%d/tasks/%d/actions/get-bootstrap-info", pipelineID, taskID)).
		Header("Authorization", er.TokenForBootstrap).
		Header(httputil.InternalHeader, "edge-pipeline").
		Do().
		Body(&body)
	if err != nil {
		return getResp, err
	}
	if !r.IsOK() {
		return getResp, errors.Errorf("status-code: %d, resp body: %s", r.StatusCode(), body.String())
	}
	if err := json.NewDecoder(&body).Decode(&getResp); err != nil {
		return getResp, errors.Errorf("status-code: %d, failed to json unmarshal get-bootstrap-resp, err: %v", r.StatusCode(), err)
	}
	return getResp, nil
}

func (er *EdgeCallbackReporter) PushCollectorLog(logLines *[]apistructs.LogPushLine) error {
	return fmt.Errorf("edge pipeline doesn't support push collector log")
}

func (er *EdgeCallbackReporter) GetCmsFile(uuid string, absPath string) error {
	return fmt.Errorf("edge pipeline doesn't support get cms file")
}

func (er *EdgeCallbackReporter) SetOpenApiToken(token string) {
	er.OpenAPIToken = token
}

func (er *EdgeCallbackReporter) SetCollectorAddress(address string) {
	er.CollectorAddr = address
}
