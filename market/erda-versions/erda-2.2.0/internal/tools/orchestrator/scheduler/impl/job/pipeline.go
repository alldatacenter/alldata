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
	"fmt"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/task"
	"github.com/erda-project/erda/pkg/strutil"
)

func (j *JobImpl) Pipeline(namespace string, names []string) ([]apistructs.Job, error) {
	// max job number is 10 by supported.
	if len(names) > 10 {
		return nil, errors.Errorf("too many jobs, max number is 10.")
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

	// start all jobs one by one.
	for i := range jobs {
		job := &jobs[i]
		if err := j.startOneJob(ctx, job); err != nil {
			job.LastMessage = err.Error()
			return nil, fmt.Errorf("failed to pipeline jobs: names: %v", names)
		}
	}
	return jobs, nil
}

func (j *JobImpl) startOneJob(ctx context.Context, job *apistructs.Job) error {
	job.LastStartTime = time.Now().Unix()
	job.Status = apistructs.StatusRunning

	if err := j.js.Put(ctx, makeJobKey(job.Namespace, job.Name), job); err != nil {
		logrus.Warnf("failed to update job status: %s (%v)", job.Name, err)
	}

	// build job match tags & exclude tags
	job.Labels = appendJobTags(job.Labels)

	_, err := j.handleJobTask(ctx, job, task.TaskCreate)
	return err
}
