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

package bundle

import (
	fmt "fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (b *Bundle) CreateJob(job apistructs.JobFromUser) (jsonResp apistructs.JobCreateResponse, err error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return
	}

	resp, err := b.hc.Put(host).
		Path("/v1/job/create").
		Header(httputil.InternalHeader, "bundle").
		JSONBody(apistructs.JobCreateRequest(job)).Do().JSON(&jsonResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || jsonResp.Error != "" {
		err = fmt.Errorf("fail to invoke: %s", jsonResp.Error)
		return
	}

	return jsonResp, nil
}

func (b *Bundle) StartJob(namespace string, name string) (jsonResp apistructs.JobStartResponse, err error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return
	}

	resp, err := b.hc.Post(host).
		Path(fmt.Sprintf("/v1/job/%s/%s/start", namespace, name)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&jsonResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || jsonResp.Error != "" {
		err = fmt.Errorf("fail to invoke: %s", jsonResp.Error)
		return
	}

	return jsonResp, nil
}

func (b *Bundle) GetJobStatus(namespace string, name string) (status apistructs.StatusCode, err error) {
	status = apistructs.StatusUnknown

	host, err := b.urls.Orchestrator()
	if err != nil {
		return
	}

	var statusResult struct {
		Status      string `json:"status"`
		LastMessage string `json:"last_message"`
	}

	resp, err := b.hc.Get(host).
		Path(fmt.Sprintf("/v1/job/%s/%s", namespace, name)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&statusResult)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() {
		err = fmt.Errorf("response http code not ok: %d", resp.StatusCode())
		return
	}

	status = transferStatus(statusResult.Status)
	return status, nil
}

func (b *Bundle) DeleteJob(namespace string, name string) (jsonResp apistructs.JobDeleteResponse, err error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return
	}

	resp, err := b.hc.Delete(host).
		Path(fmt.Sprintf("/v1/job/%s/%s/delete", namespace, name)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&jsonResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || jsonResp.Error != "" {
		err = fmt.Errorf("fail to invoke: %s", jsonResp.Error)
		return
	}

	return jsonResp, nil
}

func (b *Bundle) StopJob(namespace string, name string) (jsonResp apistructs.JobStopResponse, err error) {
	host, err := b.urls.Orchestrator()
	if err != nil {
		return
	}

	resp, err := b.hc.Post(host).
		Path(fmt.Sprintf("/v1/job/%s/%s/stop", namespace, name)).
		Header(httputil.InternalHeader, "bundle").
		Do().JSON(&jsonResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || jsonResp.Error != "" {
		err = fmt.Errorf("fail to invoke: %s", jsonResp.Error)
		return
	}

	return jsonResp, nil
}

func transferStatus(status string) apistructs.StatusCode {
	switch status {

	case string(apistructs.StatusError):
		return apistructs.StatusError

	case string(apistructs.StatusUnknown):
		return apistructs.StatusUnknown

	case string(apistructs.StatusCreated):
		return apistructs.StatusCreated

	case string(apistructs.StatusUnschedulable), "INITIAL":
		return apistructs.StatusUnschedulable

	case string(apistructs.StatusRunning), "ACTIVE":
		return apistructs.StatusRunning

	case string(apistructs.StatusStoppedOnOK), string(apistructs.StatusFinished):
		return apistructs.StatusStoppedOnOK

	case string(apistructs.StatusStoppedOnFailed), string(apistructs.StatusFailed):
		return apistructs.StatusStoppedOnFailed

	case string(apistructs.StatusStoppedByKilled):
		return apistructs.StatusStoppedByKilled

	case string(apistructs.StatusNotFoundInCluster):
		// scheduler 返回 job 在 cluster 中不存在 (在 etcd 中存在)，对应为 启动错误
		// 典型场景：created 成功，env key 为数字，导致 start job 时真正去创建 k8s job 时失败，即启动失败
		return apistructs.StatusNotFoundInCluster
	}

	return apistructs.StatusUnknown
}
