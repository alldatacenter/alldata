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

package reconciler

import (
	"context"
	"reflect"
	"sync"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func Test_defaultPipelineReconciler_setTotalTaskNumberBeforeReconcilePipeline(t *testing.T) {
	p := &spec.Pipeline{
		PipelineBase: spec.PipelineBase{
			Status: apistructs.PipelineStatusRunning,
		},
	}
	ctx := context.TODO()
	r := &provider{}
	pr := &defaultPipelineReconciler{r: r}

	// two tasks
	monkey.PatchInstanceMethod(reflect.TypeOf(r), "YmlTaskMergeDBTasks",
		func(_ *provider, pipeline *spec.Pipeline) ([]*spec.PipelineTask, error) {
			// two tasks
			tasks := []*spec.PipelineTask{
				{Name: "s1c1"},
				{Name: "s2c1"},
			}
			return tasks, nil
		})
	err := pr.setTotalTaskNumberBeforeReconcilePipeline(ctx, p)
	if err != nil {
		t.Fatalf("should no err, err: %v", err)
	}
	if *pr.totalTaskNumber != 2 {
		t.Fatalf("should have two tasks, actually %v", *pr.totalTaskNumber)
	}

	// none tasks
	monkey.PatchInstanceMethod(reflect.TypeOf(r), "YmlTaskMergeDBTasks",
		func(_ *provider, pipeline *spec.Pipeline) ([]*spec.PipelineTask, error) {
			// none tasks
			return nil, nil
		})
	err = pr.setTotalTaskNumberBeforeReconcilePipeline(ctx, p)
	if err != nil {
		t.Fatalf("should no err, err: %v", err)
	}
	if *pr.totalTaskNumber != 0 {
		t.Fatalf("should have none tasks, actually %v", *pr.totalTaskNumber)
	}

	// one disabled task and one running task
	monkey.PatchInstanceMethod(reflect.TypeOf(r), "YmlTaskMergeDBTasks",
		func(_ *provider, pipeline *spec.Pipeline) ([]*spec.PipelineTask, error) {
			// none tasks
			return []*spec.PipelineTask{
				{
					Name:   "disabled task",
					ID:     0,
					Status: apistructs.PipelineStatusDisabled,
				},
				{
					Name:   "running task",
					ID:     1,
					Status: apistructs.PipelineStatusRunning,
				},
			}, nil
		})
	err = pr.setTotalTaskNumberBeforeReconcilePipeline(ctx, p)
	if err != nil {
		t.Fatalf("should no err, err: %v", err)
	}
	if *pr.totalTaskNumber != 2 {
		t.Fatalf("should have two tasks, actually %v", *pr.totalTaskNumber)
	}
	if _, ok := pr.processedTasks.Load("disabled task"); !ok {
		t.Fatalf("should have disabled task, actually %v", ok)
	}
}

func Test_defaultPipelineReconciler_updateCalculatedPipelineStatusForTaskUseField(t *testing.T) {
	p := &spec.Pipeline{
		PipelineBase: spec.PipelineBase{
			Status: apistructs.PipelineStatusRunning,
		},
	}
	ctx := context.TODO()
	r := &provider{}
	pr := &defaultPipelineReconciler{r: r}

	// already end status
	p.Status = apistructs.PipelineStatusFailed
	err := pr.UpdateCalculatedPipelineStatusForTaskUseField(ctx, p)
	if err != nil {
		t.Fatalf("should no err, err: %v", err)
	}
	if pr.calculatedStatusForTaskUse != apistructs.PipelineStatusFailed {
		t.Fatalf("should be failed")
	}

	// canceled
	pr = &defaultPipelineReconciler{r: r}
	p.Status = apistructs.PipelineStatusRunning
	pr.flagCanceling = true
	err = pr.UpdateCalculatedPipelineStatusForTaskUseField(ctx, p)
	if err != nil {
		t.Fatalf("should no err, err: %v", err)
	}
	if pr.calculatedStatusForTaskUse != apistructs.PipelineStatusStopByUser {
		t.Fatalf("should be stopByUser")
	}

	// running
	pr = &defaultPipelineReconciler{r: r}
	pr.processedTasks.Store("s1c1", &spec.PipelineTask{ID: 1, Name: "s1c1", Status: apistructs.PipelineStatusSuccess})
	p.Status = apistructs.PipelineStatusRunning
	err = pr.UpdateCalculatedPipelineStatusForTaskUseField(ctx, p)
	if err != nil {
		t.Fatalf("should no err, err: %v", err)
	}
	if pr.calculatedStatusForTaskUse != apistructs.PipelineStatusSuccess {
		t.Fatalf("should be stopByUser")
	}
}

func Test_defaultPipelineReconciler_calculateNewStatusByReconciledTasks(t *testing.T) {
	p := &spec.Pipeline{
		PipelineBase: spec.PipelineBase{
			Status: apistructs.PipelineStatusRunning,
		},
	}
	ctx := context.TODO()
	r := &provider{}
	pr := &defaultPipelineReconciler{r: r}

	// no tasks
	pr.totalTaskNumber = &[]int{0}[0]
	newStatus := pr.calculatePipelineStatusForTaskUseField(ctx, p)
	if newStatus != apistructs.PipelineStatusSuccess {
		t.Fatalf("should be success if no task")
	}

	// one task but nothing scheduled (first loop)
	pr.totalTaskNumber = &[]int{1}[0]
	newStatus = pr.calculatePipelineStatusForTaskUseField(ctx, p)
	if newStatus != apistructs.PipelineStatusRunning {
		t.Fatalf("should be running")
	}

	// three task and two scheduled (one success, one running) => running
	pr.processedTasks.Store("s1c1", &spec.PipelineTask{ID: 1, Name: "s1c1", Status: apistructs.PipelineStatusSuccess})
	pr.processedTasks.Store("s2c1", &spec.PipelineTask{ID: 2, Name: "s2c1", Status: apistructs.PipelineStatusRunning})
	pr.totalTaskNumber = &[]int{3}[0]
	newStatus = pr.calculatePipelineStatusForTaskUseField(ctx, p)
	if newStatus != apistructs.PipelineStatusRunning {
		t.Fatalf("should be running")
	}

	// three task and two scheduled (one success, one failed) => failed
	pr.processedTasks = sync.Map{}
	pr.processedTasks.Store("s1c1", &spec.PipelineTask{ID: 1, Name: "s1c1", Status: apistructs.PipelineStatusSuccess})
	pr.processedTasks.Store("s2c1", &spec.PipelineTask{ID: 2, Name: "s2c1", Status: apistructs.PipelineStatusFailed})
	pr.totalTaskNumber = &[]int{3}[0]
	newStatus = pr.calculatePipelineStatusForTaskUseField(ctx, p)
	if newStatus != apistructs.PipelineStatusFailed {
		t.Fatalf("should be failed")
	}

	// three task and two scheduled (one running, one failed) => running
	pr.processedTasks = sync.Map{}
	pr.processedTasks.Store("s1c1", &spec.PipelineTask{ID: 1, Name: "s1c1", Status: apistructs.PipelineStatusRunning})
	pr.processingTasks.Store("s2c1", &spec.PipelineTask{ID: 1, Name: "s1c1", Status: apistructs.PipelineStatusFailed})
	pr.totalTaskNumber = &[]int{3}[0]
	newStatus = pr.calculatePipelineStatusForTaskUseField(ctx, p)
	if newStatus != apistructs.PipelineStatusRunning {
		t.Fatalf("should be running")
	}

	// three tasks are all disabled => success
	pr.totalTaskNumber = &[]int{3}[0]
	pr.processedTasks = sync.Map{}
	pr.processedTasks.Store("task-1", &spec.PipelineTask{ID: 1, Name: "s1c1", Status: apistructs.PipelineStatusDisabled})
	pr.processedTasks.Store("task-2", &spec.PipelineTask{ID: 2, Name: "s1c1", Status: apistructs.PipelineStatusDisabled})
	pr.processedTasks.Store("task-3", &spec.PipelineTask{ID: 1, Name: "s1c1", Status: apistructs.PipelineStatusDisabled})
	newStatus = pr.calculatePipelineStatusForTaskUseField(ctx, p)
	if newStatus != apistructs.PipelineStatusSuccess {
		t.Fatalf("should be success")
	}
}
