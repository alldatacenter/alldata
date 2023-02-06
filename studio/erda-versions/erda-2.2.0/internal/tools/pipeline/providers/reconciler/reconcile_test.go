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
	"time"

	"bou.ke/monkey"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func Test_defaultPipelineReconciler_internalNextLoopLogic(t *testing.T) {
	ctx := context.TODO()
	r := &provider{}
	num := 2
	pr := &defaultPipelineReconciler{
		r:               r,
		log:             logrusx.New(),
		totalTaskNumber: &num,
		doneChan:        make(chan struct{}),
	}

	pr.processingTasks.Store("task-2", struct{}{})
	pr.processedTasks.Store("task-1", struct{}{})

	monkey.PatchInstanceMethod(reflect.TypeOf(pr), "UpdateCalculatedPipelineStatusForTaskUseField",
		func(pr *defaultPipelineReconciler, ctx context.Context, p *spec.Pipeline) error {
			time.Sleep(500 * time.Millisecond)
			pr.calculatedStatusForTaskUse = apistructs.PipelineStatusRunning
			return nil
		})
	defer monkey.UnpatchAll()

	monkey.PatchInstanceMethod(reflect.TypeOf(pr), "GetTasksCanBeConcurrentlyScheduled",
		func(pr *defaultPipelineReconciler, ctx context.Context, p *spec.Pipeline) ([]*spec.PipelineTask, error) {
			return nil, nil
		})

	monkey.PatchInstanceMethod(reflect.TypeOf(pr), "UpdateCurrentReconcileStatusIfNecessary",
		func(pr *defaultPipelineReconciler, ctx context.Context, p *spec.Pipeline) error {
			return nil
		})

	go func() {
		time.Sleep(500 * time.Millisecond)
		pr.releaseTaskAfterReconciled(ctx, nil, &spec.PipelineTask{
			Name: "task-2",
		})
	}()
	go func() {
		err := pr.internalNextLoopLogic(ctx, &spec.Pipeline{})
		if err != nil {
			t.Error(err)
			return
		}
	}()

	for {
		select {
		case <-time.After(1500 * time.Millisecond):
			return
		case <-pr.doneChan:
			t.Fatal("fail")
		}
	}
}
