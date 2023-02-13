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

package action_runner_scheduler

import (
	"bytes"
	"context"

	"github.com/erda-project/erda-proto-go/core/pipeline/action_runner_scheduler/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/action_runner_scheduler/db"
	"github.com/erda-project/erda/internal/tools/pipeline/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type runnerTaskService struct {
	p *provider

	bdl      *bundle.Bundle
	dbClient *db.DBClient
}

func (s *runnerTaskService) CreateRunnerTask(ctx context.Context, req *pb.RunnerTaskCreateRequest) (*pb.RunnerTaskCreateResponse, error) {
	id, err := s.dbClient.CreateRunnerTask(req)
	if err != nil {
		return nil, apierrors.ErrCreateRunnerTask.InternalError(err)
	}
	return &pb.RunnerTaskCreateResponse{Data: int64(id)}, nil
}

func (s *runnerTaskService) UpdateRunnerTask(ctx context.Context, req *pb.RunnerTaskUpdateRequest) (*pb.RunnerTaskUpdateResponse, error) {
	task, err := s.dbClient.GetRunnerTask(req.Id)
	if err != nil {
		return nil, apierrors.ErrUpdateRunnerTask.InternalError(err)
	}
	if task.Status != apistructs.RunnerTaskStatusRunning && task.Status != apistructs.RunnerTaskStatusPending {
		return nil, apierrors.ErrUpdateRunnerTask.InvalidState("invalid task status")
	}
	task.Status = req.Status
	task.ResultDataUrl = req.ResultDataUrl
	if err := s.dbClient.UpdateRunnerTask(task); err != nil {
		return nil, apierrors.ErrUpdateRunnerTask.InternalError(err)
	}
	return &pb.RunnerTaskUpdateResponse{}, nil
}

func (s *runnerTaskService) GetRunnerTask(ctx context.Context, req *pb.RunnerTaskQueryRequest) (*pb.RunnerTaskQueryResponse, error) {
	task, err := s.dbClient.GetRunnerTask(req.Id)
	if err != nil {
		return nil, apierrors.ErrGetRunnerTask.InternalError(err)
	}
	return &pb.RunnerTaskQueryResponse{Data: task.ToPbData()}, nil
}

func (s *runnerTaskService) FetchRunnerTask(ctx context.Context, req *pb.RunnerTaskFetchRequest) (*pb.RunnerTaskFetchResponse, error) {
	task, err := s.dbClient.GetFirstPendingTask()
	if err != nil {
		return nil, apierrors.ErrFetchRunnerTask.InternalError(err)
	}
	if task == nil {
		return &pb.RunnerTaskFetchResponse{}, nil
	}
	token, err := s.bdl.GetOAuth2Token(apistructs.OAuth2TokenGetRequest{
		ClientID:     s.p.Cfg.ClientID,
		ClientSecret: s.p.Cfg.ClientSecret,
		Payload: apistructs.OAuth2TokenPayload{
			AccessTokenExpiredIn: "3630s",
			AccessibleAPIs: []apistructs.AccessibleAPI{
				{Path: "/api/files", Method: "POST", Schema: "http"},
				{Path: "/api/runner/tasks", Method: "POST", Schema: "http"},
				{Path: "/api/runner/tasks/<id>", Method: "GET", Schema: "http"},
				{Path: "/api/runner/collect/logs/<source>", Method: "POST", Schema: "http"},
			},
			Metadata: map[string]string{
				httputil.UserHeader: s.p.Cfg.RunnerUserID,
			},
		},
	})
	if err != nil {
		return nil, apierrors.ErrFetchRunnerTask.InternalError(err)
	}
	task.OpenApiToken = token.AccessToken
	task.Status = apistructs.RunnerTaskStatusRunning
	if err := s.dbClient.UpdateRunnerTask(task); err != nil {
		return nil, apierrors.ErrFetchRunnerTask.InternalError(err)
	}
	return &pb.RunnerTaskFetchResponse{Data: []*pb.RunnerTask{task.ToPbData()}}, nil
}

func (s *runnerTaskService) CollectLogs(ctx context.Context, req *pb.LogCollectRequest) (*pb.LogCollectResponse, error) {
	logReader := bytes.NewReader(req.Content)
	err := s.bdl.CollectLogs(req.Source, logReader)
	if err != nil {
		return nil, apierrors.ErrCollectRunnerLogs.InternalError(err)
	}
	return &pb.LogCollectResponse{}, nil
}
