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

package schedulabletask

import (
	"context"

	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/dag"
)

type DagImpl struct {
}

// GetSchedulableTasks return the list of schedulable tasks.
// tasks in list can be schedule concurrently.
func (d *DagImpl) GetSchedulableTasks(ctx context.Context, p *spec.Pipeline, tasks []*spec.PipelineTask) ([]*spec.PipelineTask, error) {

	// construct DAG
	dagNodes := make([]dag.NamedNode, 0, len(tasks))
	for _, task := range tasks {
		dagNodes = append(dagNodes, task)
	}
	_dag, err := dag.New(dagNodes,
		// pipeline DAG 中目前可以禁用任意节点，即 dag.WithAllowMarkArbitraryNodesAsDone=true
		dag.WithAllowMarkArbitraryNodesAsDone(true),
		// 不做 cycle check，因为 pipeline.yml 写法保证一定无环，即 dag.WithAllowNotCheckCycle=true
		dag.WithAllowNotCheckCycle(true),
	)
	if err != nil {
		return nil, err
	}

	// calculate schedulable nodes according to dag and current done tasks
	schedulableNodeFromDAG, err := _dag.GetSchedulable((&spec.PipelineWithTasks{Tasks: tasks}).DoneTasks()...)
	if err != nil {
		return nil, err
	}

	// transfer schedulable nodes to tasks
	taskMap := make(map[string]*spec.PipelineTask)
	for _, task := range tasks {
		taskMap[task.NodeName()] = task
	}
	var schedulableTasks []*spec.PipelineTask
	for nodeName := range schedulableNodeFromDAG {
		// get task by nodeName
		task := taskMap[nodeName]
		schedulableTasks = append(schedulableTasks, task)
	}

	// nothing can be schedule
	if len(schedulableTasks) == 0 {
		return nil, nil
	}

	return schedulableTasks, nil
}
