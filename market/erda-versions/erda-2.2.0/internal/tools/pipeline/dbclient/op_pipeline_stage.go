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
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/retry"
)

func (client *Client) CreatePipelineStage(ps *spec.PipelineStage, ops ...SessionOption) (err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	if ps.ID == 0 {
		ps.ID = uuid.SnowFlakeIDUint64()
	}
	_, err = session.InsertOne(ps)
	return err
}

func (client *Client) GetPipelineStage(id interface{}, ops ...SessionOption) (spec.PipelineStage, error) {
	session := client.NewSession(ops...)
	defer session.Close()
	var stage spec.PipelineStage
	exist, err := session.ID(id).Get(&stage)
	if err != nil {
		return spec.PipelineStage{}, errors.Wrapf(err, "failed to get stage by id [%v]", id)
	}
	if !exist {
		return spec.PipelineStage{}, errors.Errorf("not found stage by id [%v]", id)
	}
	return stage, nil
}

func (client *Client) GetPipelineStageWithPreStatus(id interface{}, ops ...SessionOption) (spec.PipelineStage, error) {
	session := client.NewSession(ops...)
	defer session.Close()
	stage, err := client.GetPipelineStage(id, ops...)
	if err != nil {
		return spec.PipelineStage{}, err
	}
	if stage.Extra.PreStage != nil && stage.Extra.PreStage.ID > 0 {
		preStage, err := client.GetPipelineStage(stage.Extra.PreStage.ID, ops...)
		if err != nil {
			return spec.PipelineStage{}, errors.Wrap(err, "failed to get pre stage")
		}
		stage.Extra.PreStage.Status = preStage.Status
	}
	return stage, nil
}

func (client *Client) UpdatePipelineStage(id interface{}, stage *spec.PipelineStage) error {
	if _, err := client.ID(id).AllCols().Update(stage); err != nil {
		return errors.Wrapf(err, "failed to update stage, id [%v]", id)
	}
	return nil
}

func (client *Client) ListPipelineStageByPipelineID(pipelineID uint64, ops ...SessionOption) ([]spec.PipelineStage, error) {
	session := client.NewSession(ops...)
	defer session.Close()
	var stageList []spec.PipelineStage
	if err := session.Find(&stageList, spec.PipelineStage{PipelineID: pipelineID}); err != nil {
		return nil, err
	}
	return stageList, nil
}

func (client *Client) ListPipelineStageByStatuses(statuses ...apistructs.PipelineStatus) ([]spec.PipelineStage, error) {
	var stageList []spec.PipelineStage
	if err := client.In("status", statuses).Find(&stageList); err != nil {
		return nil, err
	}
	for i, stage := range stageList {
		tmp, err := client.GetPipelineStageWithPreStatus(stage.ID)
		if err != nil {
			return nil, err
		}
		stageList[i] = tmp
	}
	return stageList, nil
}

func (client *Client) DeletePipelineStagesByPipelineID(pipelineID uint64, ops ...SessionOption) error {
	session := client.NewSession(ops...)
	defer session.Close()

	return retry.DoWithInterval(func() error {
		_, err := session.Delete(&spec.PipelineStage{PipelineID: pipelineID})
		return err
	}, 3, time.Second)
}

func (client *Client) BatchCreatePipelineStages(stages []spec.PipelineStage, ops ...SessionOption) (err error) {
	session := client.NewSession(ops...)
	defer session.Close()

	stage := &spec.PipelineStage{}
	_, err = session.Table(stage.TableName()).InsertMulti(stages)
	return err
}
