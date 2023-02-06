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

package job

import (
	"context"
	"encoding/json"
	"fmt"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/task"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	RetainNamespace = "RETAIN_NAMESPACE"
)

func (j *JobImpl) Create(create apistructs.JobCreateRequest) (apistructs.Job, error) {
	// check job kind
	if create.Kind == "" {
		create.Kind = string(apistructs.Metronome) // FIXME Compatible with the untransmitted situation of the existing job kind, the business side must be forced to transmit the kind
	} else {
		if create.Kind != string(apistructs.Metronome) && create.Kind != string(apistructs.Flink) &&
			create.Kind != string(apistructs.K8SFlink) && create.Kind != string(apistructs.Spark) &&
			create.Kind != string(apistructs.K8SSpark) && create.Kind != string(apistructs.LocalDocker) &&
			create.Kind != string(apistructs.Kubernetes) && create.Kind != string(apistructs.Swarm) &&
			create.Kind != string(apistructs.LocalJob) {
			return apistructs.Job{}, errors.Errorf("param [kind:%s] is invalid", create.Kind)
		}

		// check job resource
		if create.Kind != string(apistructs.Metronome) && create.Resource == "" {
			return apistructs.Job{}, errors.Errorf("param[resource] is invalid")
		}
	}

	// TODO: Mandatory verification of clusterName must be added in the follow-up
	logrus.Infof("epCreateJob job: %+v", create)
	job := apistructs.Job{
		JobFromUser: apistructs.JobFromUser(create),
		CreatedTime: time.Now().Unix(),
		LastModify:  time.Now().String(),
	}

	// Flink & Spark doesn't need generate jobId, it will be generated on flink/spark server
	if job.Kind == string(apistructs.Metronome) {
		prepareJob(&job.JobFromUser)
	}

	if job.Namespace == "" {
		job.Namespace = "default"
	}
	job.Status = apistructs.StatusCreated

	if !validateJobName(job.Name) && !validateJobNamespace(job.Namespace) {
		return apistructs.Job{}, errors.New("param[name] or param[namespace] is invalid")
	}

	// transform job kind to k8sflink
	if value, ok := job.Params["bigDataConf"]; ok {
		job.BigdataConf = apistructs.BigdataConf{
			BigdataMetadata: apistructs.BigdataMetadata{
				Name:      job.Name,
				Namespace: job.Namespace,
			},
			Spec: apistructs.BigdataSpec{},
		}
		err := json.Unmarshal([]byte(value.(string)), &job.BigdataConf.Spec)
		if err != nil {
			return apistructs.Job{}, fmt.Errorf("unmarshal bigdata config error: %s", err.Error())
		}
		if job.BigdataConf.Spec.FlinkConf != nil {
			job.Kind = string(apistructs.K8SFlink)
		}
		if job.BigdataConf.Spec.SparkConf != nil {
			job.Kind = string(apistructs.K8SSpark)
		}
	}

	// Get jobStatus, determine whether it is Running
	// If you are in Running, do not update the job to the store
	ctx := context.Background()
	update, err := j.fetchJobStatus(ctx, &job)
	if err != nil {
		return apistructs.Job{}, errors.Wrapf(err, "failed to fetch job status: %s", job.Name)
	}
	if update {
		if err := j.js.Put(ctx, makeJobKey(job.Namespace, job.Name), &job); err != nil {
			return apistructs.Job{}, err
		}
	}
	if job.Status == apistructs.StatusRunning {
		return apistructs.Job{}, apistructs.ErrJobIsRunning
	}

	if err := j.js.Put(ctx, makeJobKey(job.Namespace, job.Name), job); err != nil {
		return apistructs.Job{}, err
	}
	jobsinfo := apistructs.Job{}
	if err := j.js.Get(ctx, makeJobKey(job.Namespace, ""), &jobsinfo); err != nil {
		if err == jsonstore.NotFoundErr {
			if err := j.js.Put(ctx, makeJobKey(job.Namespace, ""), job); err != nil {
				return apistructs.Job{}, err
			}
		} else {
			return apistructs.Job{}, err
		}
	}

	return job, nil
}

func (j *JobImpl) List(namespace string) ([]apistructs.Job, error) {
	jobs := make([]apistructs.Job, 0, 100)
	ctx := context.Background()
	err := j.js.ForEach(ctx, makeJobKey(namespace, ""), apistructs.Job{}, func(key string, v interface{}) error {
		job := v.(*apistructs.Job)
		update, err := j.fetchJobStatus(ctx, job)
		if err != nil {
			return err
		}
		if update {
			if err := j.js.Put(ctx, makeJobKey(job.Namespace, job.Name), job); err != nil {
				return err
			}
		}
		jobs = append(jobs, *job)
		return nil
	})
	if err != nil {
		return nil, err
	}
	return jobs, nil
}

func (j *JobImpl) Start(namespace, name string, env map[string]string) (apistructs.Job, error) {
	job := apistructs.Job{}
	ctx := context.Background()
	if err := j.js.Get(ctx, makeJobKey(namespace, name), &job); err != nil {
		if strutil.HasSuffixes(err.Error(), NotFoundSuffix) {
			return apistructs.Job{}, err
		}
		return apistructs.Job{}, err
	}

	// update job status
	job.LastModify = time.Now().String()
	job.LastStartTime = time.Now().Unix()
	job.Status = apistructs.StatusUnschedulable
	if err := j.js.Put(ctx, makeJobKey(job.Namespace, job.Name), job); err != nil {
		return apistructs.Job{}, err
	}

	// update job request content(currently only update env)
	if job.Env == nil {
		job.Env = make(map[string]string)
	}
	for k, v := range env {
		job.Env[k] = v
	}

	// build job match tags & exclude tags
	job.Labels = appendJobTags(job.Labels)

	result, err := j.handleJobTask(ctx, &job, task.TaskCreate)
	if err != nil {
		return apistructs.Job{}, err
	}
	// get job id from flink/spark server
	if job.Kind == string(apistructs.Flink) &&
		job.Kind == string(apistructs.Spark) {
		if result.Extra != nil {
			job.ID = result.Extra.(string)
			// update job id to etcd
			if err := j.js.Put(ctx, makeJobKey(job.Namespace, job.Name), job); err != nil {
				return apistructs.Job{}, err
			}
		} else {
			logrus.Errorf("[alert] can't get job id, %+v", job)
		}
	}

	if job, ok := result.Extra.(apistructs.Job); ok {
		return job, nil
	}

	return apistructs.Job{}, nil
}

func (j *JobImpl) Stop(namespace, name string) error {
	job := apistructs.Job{}
	ctx := context.Background()
	if err := j.js.Get(ctx, makeJobKey(namespace, name), &job); err != nil {
		if strutil.HasSuffixes(err.Error(), NotFoundSuffix) {
			return err
		}
		return err
	}
	if job.Env == nil {
		job.Env = make(map[string]string, 0)
	}
	job.Env[RetainNamespace] = "true"

	if _, err := j.handleJobTask(ctx, &job, task.TaskDestroy); err != nil {
		return err
	}
	return nil
}

func (j *JobImpl) Delete(job apistructs.Job) error {
	var (
		ok  bool
		err error
	)
	if _, err = j.handleJobTask(context.Background(), &job, task.TaskRemove); err != nil {
		return err
	}
	if ok, err = j.js.Notfound(context.Background(), makeJobKey(job.Namespace, job.Name)); err != nil {
		return err
	}
	if ok {
		if err = j.js.Remove(context.Background(), makeJobKey(job.Namespace, job.Name), &job); err != nil {
			return err
		}
	}
	return nil
}

func (j *JobImpl) Inspect(namespace, name string) (apistructs.Job, error) {
	job := apistructs.Job{}
	ctx := context.Background()

	if err := j.js.Get(ctx, makeJobKey(namespace, name), &job); err != nil {
		if strutil.HasSuffixes(err.Error(), NotFoundSuffix) {
			return apistructs.Job{}, err
		}
		return apistructs.Job{}, err
	}

	update, err := j.fetchJobStatus(ctx, &job)
	if err != nil {
		return apistructs.Job{}, err
	}
	if update {
		if err := j.js.Put(ctx, makeJobKey(namespace, name), &job); err != nil {
			return apistructs.Job{}, err
		}
	}
	return job, nil
}

func (j *JobImpl) Concurrent(namespace string, names []string) ([]apistructs.Job, error) {
	// max job number is 10 by supported.
	if len(names) > 10 {
		return nil, fmt.Errorf("too many jobs, max number is 10.")
	}

	// read all jobs from store.
	jobs := make([]apistructs.Job, len(names))
	ctx := context.Background()
	for i, name := range names {
		if err := j.js.Get(ctx, makeJobKey(namespace, name), &jobs[i]); err != nil {
			if strutil.HasSuffixes(err.Error(), NotFoundSuffix) {
				return nil, err
			}
			return nil, err
		}
	}

	var wg sync.WaitGroup

	for i := range jobs {
		wg.Add(1)

		go func(k int) {
			defer wg.Done()

			job := &jobs[k]
			if err := j.startOneJob(ctx, job); err != nil {
				job.LastMessage = err.Error()
			}
		}(i)
	}

	wg.Wait()

	return jobs, nil
}
