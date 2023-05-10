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

package flink

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/util"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

const (
	kind = "FLINK"
)

// Flink API, please refer: https://ci.apache.org/projects/flink/flink-docs-stable/monitoring/rest_api.html
type Flink struct {
	name      executortypes.Name
	addr      string
	options   map[string]string
	client    *httpclient.HTTPClient
	enableTag bool // Whether to enable label scheduling
}

func init() {
	executortypes.Register(kind, func(name executortypes.Name, clustername string, options map[string]string, optionsPlus interface{}) (executortypes.Executor, error) {
		addr, ok := options["ADDR"]
		if !ok {
			return nil, errors.Errorf("not found flink address in env variables")
		}

		client := httpclient.New()

		if _, ok := options["CA_CRT"]; ok {
			logrus.Infof("flink executor(%s) addr for https: %v", name, addr)
			client = httpclient.New(httpclient.WithHttpsCertFromJSON([]byte(options["CLIENT_CRT"]),
				[]byte(options["CLIENT_KEY"]),
				[]byte(options["CA_CRT"])))
		}

		enableTag, err := util.ParseEnableTagOption(options, "ENABLETAG", true)
		if err != nil {
			return nil, err
		}

		return &Flink{
			name:      name,
			addr:      addr,
			options:   options,
			client:    client,
			enableTag: enableTag,
		}, nil
	})
}

func (f *Flink) Kind() executortypes.Kind {
	return kind
}

func (f *Flink) Name() executortypes.Name {
	return f.name
}

type FlinkCreateRequest struct {
	EntryClass  string `json:"entryClass"`
	ProgramArgs string `json:"programArgs"`
}

func (f *Flink) Create(ctx context.Context, specObj interface{}) (interface{}, error) {
	job, ok := specObj.(apistructs.Job)
	if !ok {
		return nil, errors.New("invalid job spec")
	}

	flinkCreateRequest := FlinkCreateRequest{
		EntryClass:  job.MainClass,
		ProgramArgs: strings.Join(job.MainArgs, " "),
	}

	var buffer bytes.Buffer
	resp, err := f.client.Post(f.addr).Path(fmt.Sprintf("/jars/%s/run", job.Resource)).
		Header("Content-Type", "application/json").
		JSONBody(flinkCreateRequest).Do().Body(&buffer)
	if err != nil {
		return nil, errors.Errorf("run flink job(%s) error: %v", job.Resource, err)
	}
	if !resp.IsOK() {
		return nil, errors.Errorf("run flink job(%s) error, statusCode: %d, body: %v", job.Resource, resp.StatusCode(), buffer.String())
	}
	// Return JobId
	var flinkCreateResponse FlinkCreateResponse
	r := bytes.NewReader(buffer.Bytes())
	if err := json.NewDecoder(r).Decode(&flinkCreateResponse); err != nil {
		return nil, err
	}
	logrus.Debugf("job: %s, flink response: %+v", job.Name, flinkCreateResponse)
	return flinkCreateResponse.JobId, nil
}

// Flink does not provide a job deletion API, and temporarily uses the cancellation API used on the Flink UI
func (f *Flink) Destroy(ctx context.Context, specObj interface{}) error {
	job, ok := specObj.(apistructs.Job)
	if !ok {
		return errors.New("invalid job spec")
	}

	if job.ID == "" {
		return nil
	}

	resp, err := f.client.Get(f.addr).
		Path(fmt.Sprintf("/jobs/%s/yarn-cancel", job.ID)).
		Header("Content-Type", "application/json; charset=UTF-8").
		Do().
		DiscardBody()

	if err != nil {
		return errors.Errorf("delete flink job(%s) error: %v", job.ID, err)
	}
	if resp.IsNotfound() {
		return nil
	}
	if !resp.IsOK() {
		return errors.Errorf("run flink job(%s) error, statusCode: %d", job.ID, resp.StatusCode())
	}
	return nil
}

func (f *Flink) Status(ctx context.Context, specObj interface{}) (apistructs.StatusDesc, error) {
	jobStatus := apistructs.StatusDesc{Status: apistructs.StatusUnknown}

	job, ok := specObj.(apistructs.Job)
	if !ok {
		return jobStatus, errors.New("invalid job spec")
	}

	if job.ID == "" {
		jobStatus.Status = apistructs.StatusCreated
		return jobStatus, nil
	}

	var flinkResp FlinkGetResponse
	resp, err := f.client.Get(f.addr).
		Path(fmt.Sprintf("/jobs/%s", job.ID)).
		Header("Content-Type", "application/json; charset=UTF-8").
		Do().
		JSON(&flinkResp)

	if err != nil {
		return jobStatus, errors.Errorf("get flink job(%+v) error: %v", job, err)
	}
	if !resp.IsOK() {
		if resp.IsNotfound() {
			logrus.Warnf("can't find job: %+v", job)
			return jobStatus, nil
		}
		return jobStatus, errors.Errorf("get flink job(%+v) error, statusCode: %d", job, resp.StatusCode())
	}

	jobStatus.Status = convertStatus(flinkResp.State)

	return jobStatus, nil
}

func (f *Flink) Remove(ctx context.Context, specObj interface{}) error {
	return f.Destroy(ctx, specObj)
}

func (f *Flink) Update(ctx context.Context, specObj interface{}) (interface{}, error) {
	return nil, errors.Errorf("job(flink) not support destroy action")
}

func (f *Flink) Inspect(ctx context.Context, specObj interface{}) (interface{}, error) {
	job, ok := specObj.(apistructs.Job)
	if !ok {
		return nil, errors.New("invalid job spec")
	}

	if job.ID == "" {
		return job, nil
	}

	var flinkResp FlinkGetResponse
	resp, err := f.client.Get(f.addr).
		Path(fmt.Sprintf("/jobs/%s", job.ID)).
		Header("Content-Type", "application/json; charset=UTF-8").
		Do().
		JSON(&flinkResp)

	if err != nil {
		return nil, errors.Errorf("get flink job(%s) error: %v", job.ID, err)
	}
	if !resp.IsOK() {
		return nil, errors.Errorf("get flink job(%s) error, statusCode: %d", job.ID, resp.StatusCode())
	}

	job.Status = convertStatus(flinkResp.State)

	return job, nil
}

func (f *Flink) Cancel(ctx context.Context, spec interface{}) (interface{}, error) {
	return nil, errors.New("job(flink) not support Cancel action")
}
func (f *Flink) Precheck(ctx context.Context, specObj interface{}) (apistructs.ServiceGroupPrecheckData, error) {
	return apistructs.ServiceGroupPrecheckData{Status: "ok"}, nil
}
func convertStatus(flinkStatus string) apistructs.StatusCode {
	switch flinkStatus {
	case "RUNNING":
		return apistructs.StatusRunning
	case "FINISHED":
		return apistructs.StatusFinished
	case "FAILED", "FAILING", "RESTARTING":
		return apistructs.StatusFailed
	case "CANCELED":
		return apistructs.StatusStopped
	default:
		logrus.Infof("flinkStatus: %s", flinkStatus)
		return apistructs.StatusUnknown
	}
}
func (f *Flink) SetNodeLabels(setting executortypes.NodeLabelSetting, hosts []string, labels map[string]string) error {
	return errors.New("SetNodeLabels not implemented in Flink")
}

func (f *Flink) CapacityInfo() apistructs.CapacityInfoData {
	return apistructs.CapacityInfoData{}
}

func (f *Flink) ResourceInfo(brief bool) (apistructs.ClusterResourceInfoData, error) {
	return apistructs.ClusterResourceInfoData{}, fmt.Errorf("resourceinfo not support for flink")
}
func (*Flink) CleanUpBeforeDelete() {}
func (*Flink) JobVolumeCreate(ctx context.Context, spec interface{}) (string, error) {
	return "", fmt.Errorf("not support for flink")
}
func (*Flink) KillPod(podname string) error {
	return fmt.Errorf("not support for flink")
}

func (f *Flink) Scale(ctx context.Context, spec interface{}) (interface{}, error) {
	return apistructs.ServiceGroup{}, fmt.Errorf("scale not support for flink")
}
