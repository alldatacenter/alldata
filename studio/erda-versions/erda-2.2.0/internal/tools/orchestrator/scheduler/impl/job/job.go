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
	"regexp"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/task"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// TODO: eliminate it, use jsonstore.ErrNotFound
	NotFoundSuffix = "not found"
)

const jobNameNamespaceFormat = `^[a-zA-Z0-9_\-]+$`

var jobFormater *regexp.Regexp = regexp.MustCompile(jobNameNamespaceFormat)

type Job interface {
	Create(apistructs.JobCreateRequest) (apistructs.Job, error)
	Start(namespace, name string, env map[string]string) (apistructs.Job, error)
	Stop(namespace, name string) error
	Delete(job apistructs.Job) error
	Inspect(namespace, name string) (apistructs.Job, error)
	List(namespace string) ([]apistructs.Job, error)

	Pipeline(namespace string, names []string) ([]apistructs.Job, error)
	Concurrent(namespace string, names []string) ([]apistructs.Job, error)

	CreateJobVolume(apistructs.JobVolume) (string, error)
}

type JobImpl struct {
	js    jsonstore.JsonStore
	sched *task.Sched
}

func NewJobImpl(js jsonstore.JsonStore, sched *task.Sched) Job {
	return &JobImpl{js, sched}
}

func (j *JobImpl) fetchJobStatus(ctx context.Context, job *apistructs.Job) (bool, error) {
	result, err := j.handleJobTask(ctx, job, task.TaskStatus)
	if err != nil {
		return false, err
	}

	if result.Status().Status == apistructs.StatusCode("") {
		return false, nil
	}

	if job.StatusDesc.Status != result.Status().Status || job.StatusDesc.LastMessage != result.Status().LastMessage {
		job.LastModify = time.Now().String()
		job.StatusDesc = result.Status()
		return true, nil
	}
	return false, nil
}

func (j *JobImpl) handleJobVolumeTask(ctx context.Context, jobvolume *apistructs.JobVolume, action task.Action) (task.TaskResponse, error) {
	var (
		result task.TaskResponse
		err    error
	)

	if err = clusterutil.SetJobVolumeExecutorByCluster(jobvolume); err != nil {
		return result, err
	}
	tsk, err := j.sched.Send(ctx, task.TaskRequest{
		ExecutorKind: executor.GetJobExecutorKindByName(jobvolume.Executor),
		ExecutorName: jobvolume.Executor,
		Action:       action,
		ID:           jobvolume.Name,
		Spec:         *jobvolume,
	})
	if err != nil {
		return result, err
	}

	result = tsk.Wait(ctx)
	return result, result.Err()
}

func (j *JobImpl) handleJobTask(ctx context.Context, job *apistructs.Job, action task.Action) (task.TaskResponse, error) {
	var (
		result task.TaskResponse
		err    error
	)

	if err = clusterutil.SetJobExecutorByCluster(job); err != nil {
		return result, err
	}

	tsk, err := j.sched.Send(ctx, task.TaskRequest{
		ExecutorKind: executor.GetJobExecutorKindByName(job.Executor),
		ExecutorName: job.Executor,
		Action:       action,
		ID:           job.Name,
		Spec:         *job,
	})
	if err != nil {
		return result, err
	}

	result = tsk.Wait(ctx)
	return result, result.Err()
}

func appendJobTags(labels map[string]string) map[string]string {
	if labels == nil {
		labels = make(map[string]string)
	}
	matchTags := make([]string, 0)
	if kind, ok := labels[apistructs.LabelJobKind]; !ok || kind != apistructs.TagBigdata {
		matchTags = append(matchTags, apistructs.TagJob)
	}
	if labels[apistructs.LabelPack] == "true" {
		matchTags = append(matchTags, apistructs.TagPack)
	}

	if len(matchTags) != 0 {
		labels[apistructs.LabelMatchTags] = strutil.Join(matchTags, ",")
	}

	if _, ok := labels[apistructs.LabelExcludeTags]; ok {
		labels[apistructs.LabelExcludeTags] = labels[apistructs.LabelExcludeTags] + "," + apistructs.TagLocked + "," + apistructs.TagPlatform
	} else {
		labels[apistructs.LabelExcludeTags] = apistructs.TagLocked + "," + apistructs.TagPlatform
	}

	modifyLabelsWithJobKind(labels)
	labels[apistructs.LabelMatchTags] = strutil.Trim(labels[apistructs.LabelMatchTags], ",")

	return labels
}

// add job kind to label based on which, scheduler can schedule different kind of job resources.
func modifyLabelsWithJobKind(labels map[string]string) {
	if kind, ok := labels[apistructs.LabelJobKind]; ok {
		matchTagsStr := labels[apistructs.LabelMatchTags]
		matchTagsSlice := strutil.Split(matchTagsStr, ",")
		matchTagsSlice = append(matchTagsSlice, kind)
		matchTags := strutil.Join(matchTagsSlice, ",")
		labels[apistructs.LabelMatchTags] = matchTags
	}
}

func validateJobName(name string) bool {
	return jobFormater.MatchString(name)
}

func validateJobNamespace(namespace string) bool {
	return jobFormater.MatchString(namespace)
}

func validateJobFlowID(id string) bool {
	return jobFormater.MatchString(id)
}

func makeJobKey(namespce, name string) string {
	return "/dice/job/" + namespce + "/" + name
}

func prepareJob(job *apistructs.JobFromUser) {
	// job ID may be specified by job owner, e.g. jobflow
	if job.ID == "" {
		job.ID = uuid.UUID()
	}

	// Do not overwrite environment
	if job.Env != nil && job.Env[conf.TraceLogEnv()] == "" && job.ID != "" {
		job.Env[conf.TraceLogEnv()] = job.ID
	}
}
