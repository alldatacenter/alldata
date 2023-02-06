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

package dbclient

import (
	"fmt"

	"github.com/erda-project/erda/internal/tools/pipeline/providers/definition/db"
)

func (client *Client) GetPipelineDefinition(id string, ops ...SessionOption) (*db.PipelineDefinition, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var pipelineDefinition db.PipelineDefinition
	var has bool
	var err error
	if has, _, err = session.Where("id = ? and soft_deleted_at = 0", id).GetFirst(&pipelineDefinition).GetResult(); err != nil {
		return nil, err
	}

	if !has {
		return nil, fmt.Errorf("the record not fount")
	}

	return &pipelineDefinition, nil
}

func (client *Client) GetPipelineDefinitionByPipelineID(pipelineID uint64, ops ...SessionOption) (*db.PipelineDefinition, bool, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var pipelineDefinition db.PipelineDefinition
	var has bool
	var err error
	if has, _, err = session.Where("pipeline_id = ? and soft_deleted_at = 0", pipelineID).GetFirst(&pipelineDefinition).GetResult(); err != nil {
		return nil, false, err
	}

	if !has {
		return nil, false, nil
	}

	return &pipelineDefinition, true, nil
}

func (client *Client) UpdatePipelineDefinition(id string, pipelineDefinition *db.PipelineDefinition, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).AllCols().Update(pipelineDefinition)
	return err
}

func (client *Client) IncreaseExecutedActionNum(id string, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()
	sql := `update pipeline_definition set executed_action_num = IF(executed_action_num<0,1,executed_action_num + 1) where id = ? AND soft_deleted_at = 0`
	_, err := session.Exec(sql, id)
	return err
}
