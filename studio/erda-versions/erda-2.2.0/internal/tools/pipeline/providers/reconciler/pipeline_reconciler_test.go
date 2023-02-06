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
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func Test_defaultPipelineReconciler_IsReconcileDone(t *testing.T) {
	p := &spec.Pipeline{
		PipelineBase: spec.PipelineBase{
			Status: apistructs.PipelineStatusRunning,
		},
	}
	pr := &defaultPipelineReconciler{}
	ctx := context.TODO()

	// pipeline is canceled
	// tasks: s1c1, s2c1
	pr.flagCanceling = true
	pr.calculatedStatusForTaskUse = apistructs.PipelineStatusStopByUser
	pr.totalTaskNumber = &[]int{2}[0]
	pr.processedTasks.Store("s1c1", struct{}{})
	pr.processingTasks.Store("s2c1", struct{}{})
	done := pr.IsReconcileDone(ctx, p)
	if !done {
		t.Fatalf("should done")
	}

	// pipeline running
	// tasks: s1c1(done), s2c1(done), s2c2(analyzed)
	pr = &defaultPipelineReconciler{}
	pr.flagCanceling = false
	pr.totalTaskNumber = &[]int{3}[0]
	pr.processedTasks.Store("s1c1", struct{}{})
	pr.processedTasks.Store("s2c1", struct{}{})
	done = pr.IsReconcileDone(ctx, p)
	if done {
		t.Fatalf("should running")
	}

	// pipeline all tasks done
	// tasks: s1c1(done), s2c1(done), s2c2(failed)
	pr = &defaultPipelineReconciler{}
	pr.flagCanceling = false
	pr.totalTaskNumber = &[]int{3}[0]
	pr.processedTasks.Store("s1c1", struct{}{})
	pr.processedTasks.Store("s2c1", struct{}{})
	pr.processedTasks.Store("s2c2", struct{}{})
	done = pr.IsReconcileDone(ctx, p)
	if !done {
		t.Fatalf("should done")
	}
}

func Test_defaultPipelineReconciler_PrepareBeforeReconcile(t *testing.T) {
	ctx := context.TODO()
	r := &provider{}
	pr := &defaultPipelineReconciler{
		r:                     r,
		chanToTriggerNextLoop: make(chan struct{}),
		log:                   logrusx.New(),
	}
	go func() {
		for {
			select {
			case <-pr.chanToTriggerNextLoop:
			}
		}
	}()
	monkey.PatchInstanceMethod(reflect.TypeOf(pr), "UpdatePipelineToRunning",
		func(_ *defaultPipelineReconciler, ctx context.Context, p *spec.Pipeline) {
			p.Status = apistructs.PipelineStatusRunning
		})

	// pipeline in queue status
	// two tasks
	p := &spec.Pipeline{
		PipelineBase: spec.PipelineBase{
			Status: apistructs.PipelineStatusQueue,
		},
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(r), "YmlTaskMergeDBTasks",
		func(_ *provider, pipeline *spec.Pipeline) ([]*spec.PipelineTask, error) {
			// two tasks
			tasks := []*spec.PipelineTask{
				{Name: "s1c1"},
				{Name: "s2c1"},
			}
			return tasks, nil
		})
	pr.PrepareBeforeReconcile(ctx, p)
	if pr.totalTaskNumber == nil {
		t.Fatalf("task num can not be nil")
	}
	if *pr.totalTaskNumber != 2 {
		t.Fatalf("task num should be 2")
	}
	if !p.Status.IsRunningStatus() {
		t.Fatalf("should be running")
	}

	// pipeline already in running status
	// two tasks
	p = &spec.Pipeline{
		PipelineBase: spec.PipelineBase{
			Status: apistructs.PipelineStatusRunning,
		},
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(r), "YmlTaskMergeDBTasks",
		func(_ *provider, pipeline *spec.Pipeline) ([]*spec.PipelineTask, error) {
			// two tasks
			tasks := []*spec.PipelineTask{
				{Name: "s1c1"},
				{Name: "s2c1"},
			}
			return tasks, nil
		})
	pr.PrepareBeforeReconcile(ctx, p)
	if pr.totalTaskNumber == nil {
		t.Fatalf("task num can not be nil")
	}
	if *pr.totalTaskNumber != 2 {
		t.Fatalf("task num should be 2")
	}
	if !p.Status.IsRunningStatus() {
		t.Fatalf("should be running")
	}
}
