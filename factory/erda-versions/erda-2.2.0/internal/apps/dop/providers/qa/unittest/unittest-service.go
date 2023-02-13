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

package unittest

import (
	"context"
	"strconv"

	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/dop/qa/unittest/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/pkg/qaparser/types"
)

type UnitTestService struct {
	logger logs.Logger

	db *dao.DBClient
}

func (s *UnitTestService) Callback(ctx context.Context, req *pb.TestCallBackRequest) (*pb.TestCallBackResponse, error) {
	tpRecord := &dbclient.TPRecordDO{
		Suites: req.Suites,
		Totals: req.Totals,
	}
	if req.Results != nil {
		tpRecord.ParserType = req.Results.Type
		tpRecord.ApplicationID = req.Results.ApplicationId
		tpRecord.ProjectID = req.Results.ProjectId
		tpRecord.BuildID = req.Results.BuildId
		tpRecord.Name = req.Results.Name
		tpRecord.Branch = req.Results.Branch
		tpRecord.GitRepo = req.Results.GitRepo
		tpRecord.OperatorID = req.Results.OperatorId
		tpRecord.TType = req.Results.Type
		tpRecord.Workspace = apistructs.DiceWorkspace(req.Results.Workspace)
		tpRecord.CommitID = req.Results.CommitId
		tpRecord.OperatorName = req.Results.OperatorName
		tpRecord.ApplicationName = req.Results.ApplicationName
		tpRecord.Extra = req.Results.Extra
		tpRecord.UUID = req.Results.Uuid
		tpRecord.CoverageReport = req.CoverageReport
	}
	_, err := dbclient.InsertTPRecord(tpRecord)
	if err != nil {
		return nil, err
	}
	return &pb.TestCallBackResponse{Data: strconv.FormatUint(tpRecord.ID, 10)}, nil
}

func (s *UnitTestService) GetTestTypes(ctx context.Context, req *pb.TestTypeRequest) (*pb.TestTypeResponse, error) {
	testTypes := types.TestTypeValues()
	pbRes := &pb.TestTypeResponse{
		Data: make([]string, 0),
	}
	for i := range testTypes {
		pbRes.Data = append(pbRes.Data, string(testTypes[i]))
	}
	return pbRes, nil
}

func (s *UnitTestService) ListRecords(ctx context.Context, req *pb.TestRecordPagingRequest) (*pb.TestRecordPagingResponse, error) {
	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 15
	}
	pagingResult, err := dbclient.FindTPRecordPagingByAppID(req)
	if err != nil {
		return nil, err
	}
	records := make([]*pb.TestRecord, 0)
	for _, r := range pagingResult.List.([]*dbclient.TPRecordDO) {
		// erase sensitive information
		r.EraseSensitiveInfo()
		// only return the parent code coverage data when list unittest records
		for _, coverageReport := range r.CoverageReport {
			coverageReport.Children = nil
		}
		records = append(records, convertRecordToPB(r))
	}
	return &pb.TestRecordPagingResponse{
		Data: &pb.TestRecordPagingResult{
			Total: pagingResult.Total,
			List:  records,
		},
	}, nil
}

func (s *UnitTestService) GetRecord(ctx context.Context, req *pb.TestRecordGetRequest) (*pb.TestRecordGetResponse, error) {
	record, err := dbclient.FindTPRecordById(req.Id)
	if err != nil {
		return nil, err
	}
	return &pb.TestRecordGetResponse{
		Data: convertRecordToPB(record),
	}, nil
}

func convertRecordToPB(r *dbclient.TPRecordDO) *pb.TestRecord {
	return &pb.TestRecord{
		Id: r.ID,
		CreatedAt: func() *timestamppb.Timestamp {
			if r.CreatedAt.IsZero() {
				return nil
			}
			return timestamppb.New(r.CreatedAt)
		}(),
		UpdatedAt: func() *timestamppb.Timestamp {
			if r.UpdatedAt.IsZero() {
				return nil
			}
			return timestamppb.New(r.UpdatedAt)
		}(),
		ApplicationId:   r.ApplicationID,
		ProjectId:       r.ProjectID,
		BuildId:         r.BuildID,
		Name:            r.Name,
		Uuid:            r.UUID,
		ApplicationName: r.ApplicationName,
		Output:          r.Output,
		Desc:            r.Desc,
		OperatorId:      r.OperatorID,
		OperatorName:    r.OperatorName,
		CommitId:        r.CommitID,
		Branch:          r.Branch,
		GitRepo:         r.GitRepo,
		CaseDir:         r.CaseDir,
		Application:     r.Application,
		Totals:          r.Totals,
		Type:            r.TType,
		ParserType:      r.ParserType,
		Extra:           r.Extra,
		Envs:            r.Envs,
		Workspace:       r.Workspace.String(),
		Suites:          r.Suites,
		CoverageReport:  r.CoverageReport,
	}
}
