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

package definition

import (
	"context"
	"encoding/json"
	"strings"
	"unicode/utf8"

	"github.com/pkg/errors"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/definition/db"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
	"github.com/erda-project/erda/pkg/time/mysql_time"
)

type pipelineDefinition struct {
	dbClient *db.Client
}

func GetExtraValue(definition *pb.PipelineDefinition) (*apistructs.PipelineDefinitionExtraValue, error) {
	var extraValue = apistructs.PipelineDefinitionExtraValue{}
	err := json.Unmarshal([]byte(definition.Extra.Extra), &extraValue)
	if err != nil {
		return nil, err
	}
	return &extraValue, nil
}

func (p pipelineDefinition) Create(ctx context.Context, request *pb.PipelineDefinitionCreateRequest) (*pb.PipelineDefinitionCreateResponse, error) {
	if err := createPreCheck(request); err != nil {
		return nil, err
	}

	var pipelineDefinition db.PipelineDefinition
	pipelineDefinition.Location = request.Location
	pipelineDefinition.Name = request.Name
	pipelineDefinition.PipelineSourceId = request.PipelineSourceID
	pipelineDefinition.Category = request.Category
	pipelineDefinition.Creator = request.Creator
	pipelineDefinition.ID = uuid.New()
	pipelineDefinition.StartedAt = *mysql_time.GetMysqlDefaultTime()
	pipelineDefinition.EndedAt = *mysql_time.GetMysqlDefaultTime()
	pipelineDefinition.CostTime = -1
	pipelineDefinition.Ref = request.Ref
	err := p.dbClient.CreatePipelineDefinition(&pipelineDefinition)
	if err != nil {
		return nil, err
	}

	var pipelineDefinitionExtra db.PipelineDefinitionExtra
	pipelineDefinitionExtra.ID = uuid.New()
	var extra apistructs.PipelineDefinitionExtraValue
	err = json.Unmarshal([]byte(request.Extra.Extra), &extra)
	if err != nil {
		return nil, err
	}
	pipelineDefinitionExtra.PipelineDefinitionID = pipelineDefinition.ID
	pipelineDefinitionExtra.Extra = extra
	err = p.dbClient.CreatePipelineDefinitionExtra(&pipelineDefinitionExtra)
	if err != nil {
		return nil, err
	}

	pbPipelineDefinition := PipelineDefinitionToPb(&pipelineDefinition)
	pbPipelineDefinitionExtra := PipelineDefinitionExtraToPb(&pipelineDefinitionExtra)
	pbPipelineDefinition.Extra = pbPipelineDefinitionExtra
	return &pb.PipelineDefinitionCreateResponse{
		PipelineDefinition: pbPipelineDefinition,
	}, nil
}

func createPreCheck(request *pb.PipelineDefinitionCreateRequest) error {
	if request.Name == "" || utf8.RuneCountInString(request.Name) > 30 {
		return apierrors.ErrCreatePipelineDefinition.InvalidParameter(errors.Errorf("name: %s", request.Name))
	}
	if request.Creator == "" {
		return apierrors.ErrCreatePipelineDefinition.InvalidParameter(errors.Errorf("creator: %s", request.Creator))
	}
	if request.Category == "" {
		return apierrors.ErrCreatePipelineDefinition.InvalidParameter(errors.Errorf("category: %s", request.Category))
	}
	if request.PipelineSourceID == "" {
		return apierrors.ErrCreatePipelineDefinition.InvalidParameter(errors.Errorf("pipelineSourceId: %s", request.PipelineSourceID))
	}
	if request.Extra == nil || request.Extra.Extra == "" {
		return apierrors.ErrCreatePipelineDefinition.InvalidParameter(errors.Errorf("extra: %s", request.Extra))
	}
	return nil
}

func (p pipelineDefinition) Update(ctx context.Context, request *pb.PipelineDefinitionUpdateRequest) (*pb.PipelineDefinitionUpdateResponse, error) {
	if request.PipelineDefinitionID == "" {
		return nil, apierrors.ErrUpdatePipelineDefinition.InvalidParameter(errors.Errorf("pipelineDefinitionID: %s", request.PipelineDefinitionID))
	}
	pipelineDefinition, err := p.dbClient.GetPipelineDefinition(request.PipelineDefinitionID)
	if err != nil {
		return nil, err
	}
	if request.Category != "" {
		pipelineDefinition.Category = request.Category
	}
	if request.Name != "" {
		pipelineDefinition.Name = request.Name
	}
	if request.CostTime != 0 {
		pipelineDefinition.CostTime = request.CostTime
	}
	if request.PipelineSourceID != "" {
		pipelineDefinition.PipelineSourceId = request.PipelineSourceID
	}
	if request.StartedAt != nil {
		var startAt = request.StartedAt.AsTime()
		pipelineDefinition.StartedAt = startAt
	}
	if request.EndedAt != nil {
		var endAt = request.EndedAt.AsTime()
		pipelineDefinition.EndedAt = endAt
	}
	if request.Executor != "" {
		pipelineDefinition.Executor = request.Executor
	}
	if request.Status != "" {
		pipelineDefinition.Status = request.Status
	}
	if request.PipelineID > 0 {
		pipelineDefinition.PipelineID = uint64(request.PipelineID)
	}
	if request.TotalActionNum != 0 {
		pipelineDefinition.TotalActionNum = request.TotalActionNum
	}
	if request.ExecutedActionNum != 0 {
		pipelineDefinition.ExecutedActionNum = request.ExecutedActionNum
	}
	err = p.dbClient.UpdatePipelineDefinition(request.PipelineDefinitionID, pipelineDefinition)
	if err != nil {
		return nil, err
	}

	pbPipelineDefinition := PipelineDefinitionToPb(pipelineDefinition)
	return &pb.PipelineDefinitionUpdateResponse{
		PipelineDefinition: pbPipelineDefinition,
	}, nil
}

func (p pipelineDefinition) Delete(ctx context.Context, request *pb.PipelineDefinitionDeleteRequest) (*pb.PipelineDefinitionDeleteResponse, error) {
	err := p.dbClient.DeletePipelineDefinition(request.PipelineDefinitionID)
	if err != nil {
		return nil, err
	}

	return &pb.PipelineDefinitionDeleteResponse{}, nil
}

func (p pipelineDefinition) Get(ctx context.Context, request *pb.PipelineDefinitionGetRequest) (*pb.PipelineDefinitionGetResponse, error) {
	pipelineDefinition, err := p.dbClient.GetPipelineDefinition(request.PipelineDefinitionID)
	if err != nil {
		return nil, err
	}

	pipelineDefinitionExtra, err := p.dbClient.GetPipelineDefinitionExtraByDefinitionID(pipelineDefinition.ID)
	if err != nil {
		return nil, err
	}

	pbPipelineDefinition := PipelineDefinitionToPb(pipelineDefinition)
	pbPipelineDefinitionExtra := PipelineDefinitionExtraToPb(pipelineDefinitionExtra)
	pbPipelineDefinition.Extra = pbPipelineDefinitionExtra
	return &pb.PipelineDefinitionGetResponse{
		PipelineDefinition: pbPipelineDefinition,
	}, nil
}

func (p pipelineDefinition) List(ctx context.Context, request *pb.PipelineDefinitionListRequest) (*pb.PipelineDefinitionListResponse, error) {
	definitions, total, err := p.dbClient.ListPipelineDefinition(request)
	if err != nil {
		return nil, err
	}

	data := make([]*pb.PipelineDefinition, 0, len(definitions))
	var definitionIDList []string
	for _, v := range definitions {
		definitionIDList = append(definitionIDList, v.ID)
		data = append(data, v.Convert())
	}

	var extrasMap = map[string]db.PipelineDefinitionExtra{}
	extras, err := p.dbClient.ListPipelineDefinitionExtraByDefinitionIDList(definitionIDList)
	if err != nil {
		return nil, err
	}
	for _, extra := range extras {
		extrasMap[extra.PipelineDefinitionID] = extra
	}

	for _, definition := range data {
		definition.Extra = &pb.PipelineDefinitionExtra{
			ID:    extrasMap[definition.ID].ID,
			Extra: jsonparse.JsonOneLine(extrasMap[definition.ID].Extra),
		}
	}

	return &pb.PipelineDefinitionListResponse{
		Total: total,
		Data:  data,
	}, nil
}

func PipelineDefinitionToPb(pipelineDefinition *db.PipelineDefinition) *pb.PipelineDefinition {
	de := &pb.PipelineDefinition{
		ID:               pipelineDefinition.ID,
		Location:         pipelineDefinition.Location,
		Name:             pipelineDefinition.Name,
		Creator:          pipelineDefinition.Creator,
		Executor:         pipelineDefinition.Executor,
		CostTime:         pipelineDefinition.CostTime,
		Category:         pipelineDefinition.Category,
		PipelineSourceID: pipelineDefinition.PipelineSourceId,
		Status:           pipelineDefinition.Status,
		TimeCreated:      timestamppb.New(pipelineDefinition.TimeCreated),
		TimeUpdated:      timestamppb.New(pipelineDefinition.TimeUpdated),
		StartedAt:        timestamppb.New(pipelineDefinition.StartedAt),
		EndedAt:          timestamppb.New(pipelineDefinition.EndedAt),
		PipelineID:       int64(pipelineDefinition.PipelineID),
	}
	return de
}

func PipelineDefinitionExtraToPb(pipelineDefinitionExtra *db.PipelineDefinitionExtra) *pb.PipelineDefinitionExtra {
	de := &pb.PipelineDefinitionExtra{
		ID:    pipelineDefinitionExtra.ID,
		Extra: jsonparse.JsonOneLine(pipelineDefinitionExtra.Extra),
	}
	if pipelineDefinitionExtra.TimeCreated != nil {
		de.TimeCreated = timestamppb.New(*pipelineDefinitionExtra.TimeCreated)
	}
	if pipelineDefinitionExtra.TimeUpdated != nil {
		de.TimeUpdated = timestamppb.New(*pipelineDefinitionExtra.TimeUpdated)
	}
	return de
}

func (p pipelineDefinition) StatisticsGroupByRemote(ctx context.Context, request *pb.PipelineDefinitionStatisticsRequest) (*pb.PipelineDefinitionStatisticsResponse, error) {
	statics, err := p.dbClient.StatisticsGroupByRemote(request)
	if err != nil {
		return nil, err
	}

	pipelineDefinitionStatistics := make([]*pb.PipelineDefinitionStatistics, 0, len(statics))
	for _, v := range statics {
		pipelineDefinitionStatistics = append(pipelineDefinitionStatistics, &pb.PipelineDefinitionStatistics{
			Group:      v.Group,
			FailedNum:  v.FailedNum,
			RunningNum: v.RunningNum,
			TotalNum:   v.TotalNum,
		})
	}
	return &pb.PipelineDefinitionStatisticsResponse{PipelineDefinitionStatistics: pipelineDefinitionStatistics}, nil
}

func (p pipelineDefinition) ListUsedRefs(ctx context.Context, req *pb.PipelineDefinitionUsedRefListRequest) (*pb.PipelineDefinitionUsedRefListResponse, error) {
	if err := req.Validate(); err != nil {
		return nil, err
	}
	refs, err := p.dbClient.ListUsedRef(req)
	if err != nil {
		return nil, err
	}
	return &pb.PipelineDefinitionUsedRefListResponse{Ref: refs}, nil
}

func (p pipelineDefinition) StatisticsGroupByFilePath(ctx context.Context, request *pb.PipelineDefinitionStatisticsRequest) (*pb.PipelineDefinitionStatisticsResponse, error) {
	statics, err := p.dbClient.StatisticsGroupByFilePath(request)
	if err != nil {
		return nil, err
	}

	pipelineDefinitionStatistics := make([]*pb.PipelineDefinitionStatistics, 0, len(statics))
	for _, v := range statics {
		group := v.Group
		if strings.HasPrefix(v.Group, "/") {
			group = group[1:]
		}
		pipelineDefinitionStatistics = append(pipelineDefinitionStatistics, &pb.PipelineDefinitionStatistics{
			Group:      group,
			FailedNum:  v.FailedNum,
			RunningNum: v.RunningNum,
			TotalNum:   v.TotalNum,
		})
	}
	return &pb.PipelineDefinitionStatisticsResponse{PipelineDefinitionStatistics: pipelineDefinitionStatistics}, nil
}

func (p pipelineDefinition) UpdateExtra(ctx context.Context, request *pb.PipelineDefinitionExtraUpdateRequest) (*pb.PipelineDefinitionExtraUpdateResponse, error) {

	dbExtra, err := p.dbClient.GetPipelineDefinitionExtraByDefinitionID(request.PipelineDefinitionID)
	if err != nil {
		return nil, err
	}

	var extra apistructs.PipelineDefinitionExtraValue
	err = json.Unmarshal([]byte(request.Extra), &extra)
	if err != nil {
		return nil, err
	}
	dbExtra.Extra = extra

	err = p.dbClient.UpdatePipelineDefinitionExtra(dbExtra.ID, dbExtra)
	if err != nil {
		return nil, err
	}

	return &pb.PipelineDefinitionExtraUpdateResponse{
		Extra: PipelineDefinitionExtraToPb(dbExtra),
	}, nil
}
