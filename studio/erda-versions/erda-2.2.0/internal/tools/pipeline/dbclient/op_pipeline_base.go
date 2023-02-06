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

	"github.com/erda-project/erda/apistructs"
	definitiondb "github.com/erda-project/erda/internal/tools/pipeline/providers/definition/db"
	sourcedb "github.com/erda-project/erda/internal/tools/pipeline/providers/source/db"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

func (client *Client) ListPipelineBasesByIDs(pipelineIDs []uint64, ops ...SessionOption) (map[uint64]spec.PipelineBase, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var bases []spec.PipelineBase
	if err := session.In("id", pipelineIDs).Find(&bases); err != nil {
		return nil, err
	}
	basesMap := make(map[uint64]spec.PipelineBase, len(bases))
	for _, base := range bases {
		basesMap[base.ID] = base
	}
	return basesMap, nil
}

func (client *Client) ListPipelineBaseWithDefinitionByIDs(pipelineIDs []uint64, ops ...SessionOption) (map[uint64]spec.PipelineBaseWithDefinition, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var bases []spec.PipelineBaseWithDefinition
	if err := session.In(tableFieldName((&spec.PipelineBaseWithDefinition{}).TableName(), "id"), pipelineIDs).
		Join("INNER", definitiondb.PipelineDefinition{}.TableName(), fmt.Sprintf("%v.id = %v.pipeline_definition_id", definitiondb.PipelineDefinition{}.TableName(), (&spec.PipelineBaseWithDefinition{}).TableName())).
		Join("INNER", sourcedb.PipelineSource{}.TableName(), fmt.Sprintf("%v.id = %v.pipeline_source_id", sourcedb.PipelineSource{}.TableName(), definitiondb.PipelineDefinition{}.TableName())).
		Find(&bases); err != nil {
		return nil, err
	}
	basesMap := make(map[uint64]spec.PipelineBaseWithDefinition, len(bases))
	for _, base := range bases {
		basesMap[base.PipelineBase.ID] = base
	}
	return basesMap, nil
}

func (client *Client) CreatePipelineBase(base *spec.PipelineBase, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	if base.ID == 0 {
		base.ID = uuid.SnowFlakeIDUint64()
	}
	_, err := session.InsertOne(base)
	return err
}

func (client *Client) UpdatePipelineBase(id uint64, base *spec.PipelineBase, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).AllCols().Update(base)
	return err
}

func (client *Client) GetPipelineBase(id uint64, ops ...SessionOption) (spec.PipelineBase, bool, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var base spec.PipelineBase
	found, err := session.ID(id).Get(&base)
	if err != nil {
		return spec.PipelineBase{}, false, err
	}
	return base, found, nil
}

func (client *Client) GetPipelineStatus(id uint64, ops ...SessionOption) (apistructs.PipelineStatus, error) {
	session := client.NewSession(ops...)
	defer session.Close()

	var base spec.PipelineBase
	exist, err := session.ID(id).Cols("status").Get(&base)
	if err != nil {
		return "", err
	}
	if !exist {
		return "", fmt.Errorf("pipeline base not found")
	}
	return base.Status, nil
}

func (client *Client) UpdatePipelineBaseStatus(id uint64, status apistructs.PipelineStatus, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	_, err := session.ID(id).Cols("status").Update(&spec.PipelineBase{Status: status})
	return err
}
