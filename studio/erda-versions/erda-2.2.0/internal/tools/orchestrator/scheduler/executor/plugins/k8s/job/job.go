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

// Package deployment manipulates the k8s api of deployment object
package job

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/url"
	"strings"

	"github.com/pkg/errors"
	batchv1 "k8s.io/api/batch/v1"

	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8sapi"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

// Deployment is the object to manipulate k8s api of deployment
type Job struct {
	addr   string
	client *httpclient.HTTPClient
}

// Option configures a Deployment
type Option func(*Job)

// New news a Deployment
func New(options ...Option) *Job {
	ns := &Job{}

	for _, op := range options {
		op(ns)
	}

	return ns
}

// WithCompleteParams provides an Option
func WithCompleteParams(addr string, client *httpclient.HTTPClient) Option {
	return func(d *Job) {
		d.addr = addr
		d.client = client
	}
}

// Create creates a k8s job object
func (j *Job) Create(job *batchv1.Job) error {
	var b bytes.Buffer

	resp, err := j.client.Post(j.addr).
		Path("/apis/batch/v1/namespaces/" + job.Namespace + "/jobs").
		JSONBody(job).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to create job, name: %s", job.Name)
	}

	if !resp.IsOK() {
		errMsg := fmt.Sprintf("failed to create k8s job statuscode: %v, body: %v",
			resp.StatusCode(), b.String())
		return errors.Errorf(errMsg)
	}
	return nil
}

// Get gets a k8s job object
func (j *Job) Get(namespace, name string) (*batchv1.Job, error) {
	var b bytes.Buffer
	resp, err := j.client.Get(j.addr).
		Path("/apis/batch/v1/namespaces/" + namespace + "/jobs/" + name).
		Do().
		Body(&b)

	if err != nil {
		return nil, errors.Errorf("failed to get job info, name: %s", name)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return nil, k8serror.ErrNotFound
		}
		return nil, errors.Errorf("failed to get job info, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}

	job := &batchv1.Job{}
	if err := json.NewDecoder(&b).Decode(job); err != nil {
		return nil, err
	}
	return job, nil
}

// List lists job under specific namespace
func (j *Job) List(namespace string, labelSelector map[string]string) (batchv1.JobList, error) {
	var jobList batchv1.JobList
	var params url.Values
	if len(labelSelector) > 0 {
		var kvs []string
		params = make(url.Values, 0)

		for key, value := range labelSelector {
			kvs = append(kvs, fmt.Sprintf("%s=%s", key, value))
		}
		params.Add("labelSelector", strings.Join(kvs, ","))
	}
	var b bytes.Buffer
	resp, err := j.client.Get(j.addr).
		Path("/apis/batch/v1/namespaces/" + namespace + "/jobs").
		Params(params).
		Do().
		Body(&b)

	if err != nil {
		return jobList, errors.Errorf("failed to get job list, ns: %s, (%v)", namespace, err)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return jobList, k8serror.ErrNotFound
		}
		return jobList, errors.Errorf("failed to get job list, ns: %s, statuscode: %v, body: %v",
			namespace, resp.StatusCode(), b.String())
	}

	if err := json.NewDecoder(&b).Decode(&jobList); err != nil {
		return jobList, err
	}
	return jobList, nil
}

// Delete deletes a k8s job
func (j *Job) Delete(namespace, name string) error {
	var b bytes.Buffer
	resp, err := j.client.Delete(j.addr).
		Path("/apis/batch/v1/namespaces/" + namespace + "/jobs/" + name).
		JSONBody(k8sapi.DeleteOptions).
		Do().
		Body(&b)

	if err != nil {
		return errors.Errorf("failed to delete job, name: %s, (%v)", name, err)
	}

	if !resp.IsOK() {
		if resp.IsNotfound() {
			return k8serror.ErrNotFound
		}
		return errors.Errorf("failed to delete job, name: %s, statuscode: %v, body: %v",
			name, resp.StatusCode(), b.String())
	}
	return nil
}
