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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (h *HTTPEndpoints) JobCreate(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	//h.metric.TotalCounter.WithLabelValues(metric.JobCreateTotal).Add(1)
	req := apistructs.JobCreateRequest{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("decode create volume request fail: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusBadRequest,
			Content: apistructs.JobCreateResponse{
				Error: errstr,
			},
		}, nil
	}

	// specify namespace from scheduler ENV 'ENABLE_SPECIFIED_K8S_NAMESPACE'
	if os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE) != "" {
		req.Namespace = os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
	}

	job, err := h.Job.Create(req)
	if err != nil {
		//h.metric.ErrorCounter.WithLabelValues(metric.JobCreateError).Add(1)
		errstr := fmt.Sprintf("failed to create job: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusBadRequest,
			Content: apistructs.JobCreateResponse{
				Error: errstr,
			},
		}, nil
	}
	return mkResponse(apistructs.JobCreateResponse{
		Name: job.Name,
		Job:  job,
	})
}

func (h *HTTPEndpoints) JobStart(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	name := vars["name"]
	namespace := vars["namespace"]
	if name == "" || namespace == "" {
		errstr := "failed to start job, empty name or namespace"
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusBadRequest,
			Content: apistructs.JobStartResponse{
				Error: errstr,
			},
		}, nil
	}
	if os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE) != "" {
		namespace = os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
	}

	resultJob, err := h.Job.Start(namespace, name, map[string]string{})
	if err != nil {
		errstr := fmt.Sprintf("failed to start job, err: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusInternalServerError,
			Content: apistructs.JobStartResponse{
				Error: errstr,
			},
		}, nil
	}
	return mkResponse(apistructs.JobStartResponse{
		Name: name,
		Job:  resultJob,
	})
}

func (h *HTTPEndpoints) JobStop(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	name := vars["name"]
	namespace := vars["namespace"]

	if name == "" || namespace == "" {
		errstr := "failed to stop job, empty name or namespace"
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusBadRequest,
			Content: apistructs.JobStopResponse{
				Error: errstr,
			},
		}, nil
	}

	if os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE) != "" {
		namespace = os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
	}

	if err := h.Job.Stop(namespace, name); err != nil {
		errstr := fmt.Sprintf("failed to stop job, err: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusBadRequest,
			Content: apistructs.JobStopResponse{
				Error: errstr,
			},
		}, nil
	}
	return mkResponse(apistructs.JobStopResponse{Name: name})
}

func (h *HTTPEndpoints) JobDelete(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	var job apistructs.Job
	if err := json.NewDecoder(r.Body).Decode(&job); err != nil && r.ContentLength != 0 {
		errstr := fmt.Sprintf("failed to decode jobStart body, err: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusBadRequest,
			Content: apistructs.JobDeleteResponse{
				Error: errstr,
			},
		}, nil
	}

	if os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE) != "" {
		job.Namespace = os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
	}

	if job.Env == nil {
		job.Env = make(map[string]string, 0)
	}
	job.Env[RetainNamespace] = "true"

	if err := h.Job.Delete(job); err != nil {
		errstr := fmt.Sprintf("failed to delete job, err: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusBadRequest,
			Content: apistructs.JobDeleteResponse{
				Name:      job.Name,
				Namespace: job.Namespace,
				Error:     errstr,
			},
		}, nil
	}
	return mkResponse(apistructs.JobDeleteResponse{Name: job.Name, Namespace: job.Namespace})
}

// batch Delete Jobs will set retainNamespace is false
// so that the namespace will be deleted when the
// job count is zero
func (h *HTTPEndpoints) DeleteJobs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var jobs []apistructs.Job
	if err := json.NewDecoder(r.Body).Decode(&jobs); err != nil && r.ContentLength != 0 {
		errstr := fmt.Sprintf("failed to decode jobStart body, err: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status: http.StatusBadRequest,
			Content: []apistructs.JobDeleteResponse{
				{
					Error: errstr,
				},
			},
		}, nil
	}

	deleteResponseList := apistructs.JobsDeleteResponse{}
	logrus.Infof("batch delete %d jobs", len(jobs))

	for _, job := range jobs {
		if job.Env == nil {
			job.Env = make(map[string]string, 0)
		}
		job.Env[RetainNamespace] = "false"

		if os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE) != "" {
			job.Namespace = os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
		}

		if err := h.Job.Delete(job); err != nil {
			errstr := fmt.Sprintf("failed to delete job %s in ns %s, err: %v", job.Name, job.Namespace, err)
			logrus.Error(errstr)
			deleteResponseList = append(deleteResponseList, apistructs.JobDeleteResponse{
				Name:      job.Name,
				Namespace: job.Namespace,
				Error:     errstr,
			})
		}
	}

	if len(deleteResponseList) != 0 {
		return httpserver.HTTPResponse{
			Status:  http.StatusBadRequest,
			Content: deleteResponseList,
		}, nil
	}
	return mkResponse(apistructs.JobsDeleteResponse{})
}

func (h *HTTPEndpoints) JobInspect(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	name := vars["name"]
	namespace := vars["namespace"]

	if name == "" || namespace == "" {
		errstr := "failed to inspect job, empty name or namespace"
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status:  http.StatusBadRequest,
			Content: errstr,
		}, nil
	}

	if os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE) != "" {
		namespace = os.Getenv(apistructs.ENABLE_SPECIFIED_K8S_NAMESPACE)
	}

	job, err := h.Job.Inspect(namespace, name)
	if err != nil {
		errstr := fmt.Sprintf("failed to inspect job, err: %v", err)
		logrus.Error(errstr)
		return httpserver.HTTPResponse{
			Status:  http.StatusInternalServerError,
			Content: errstr,
		}, nil
	}
	return mkResponse(job)
}
