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

package pipelinesvc

import (
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

func (s *PipelineSvc) BatchCreateLabels(createReq *apistructs.PipelineLabelBatchInsertRequest) error {
	var insertLabels []spec.PipelineLabel
	for _, label := range createReq.Labels {
		insertLabels = append(insertLabels, spec.PipelineLabel{
			Type:            label.Type,
			Key:             label.Key,
			Value:           label.Value,
			TargetID:        label.TargetID,
			PipelineYmlName: label.PipelineYmlName,
			PipelineSource:  label.PipelineSource,
			TimeCreated:     time.Now(),
			TimeUpdated:     time.Now(),
			ID:              uuid.SnowFlakeIDUint64(),
		})
	}
	err := s.dbClient.BatchInsertLabels(insertLabels)
	if err != nil {
		return err
	}
	return nil
}

func (s *PipelineSvc) ListLabels(req *apistructs.PipelineLabelListRequest) (*apistructs.PipelineLabelPageListData, error) {

	// if UnMatchKeys and TargetIDS not empty, find targetID by MatchKeys from db then filter targetID not in TargetIDs
	if len(req.UnMatchKeys) > 0 && len(req.TargetIDs) > 0 {
		req.MatchKeys = req.UnMatchKeys
	}

	labels, _, err := s.dbClient.ListPipelineLabels(req)
	if err != nil {
		return nil, apierrors.ErrListPipelineLabel.InternalError(err)
	}

	var listLabel []apistructs.PipelineLabel
	// filter targetID not in TargetIDS
	if len(req.UnMatchKeys) > 0 && len(req.TargetIDs) > 0 {
		var labelMap = make(map[uint64]spec.PipelineLabel)
		for _, value := range labels {
			labelMap[value.TargetID] = value
		}

		for _, value := range req.TargetIDs {
			if _, ok := labelMap[value]; ok {
				continue
			}

			listLabel = append(listLabel, apistructs.PipelineLabel{
				TargetID: value,
			})
		}
	} else {
		for _, value := range labels {
			listLabel = append(listLabel, apistructs.PipelineLabel{
				ID:              value.ID,
				Type:            value.Type,
				TargetID:        value.TargetID,
				PipelineSource:  value.PipelineSource,
				PipelineYmlName: value.PipelineYmlName,
				Key:             value.Key,
				Value:           value.Value,
				TimeCreated:     value.TimeCreated,
				TimeUpdated:     value.TimeUpdated,
			})
		}
	}

	var result apistructs.PipelineLabelPageListData
	result.Labels = listLabel
	return &result, nil
}
