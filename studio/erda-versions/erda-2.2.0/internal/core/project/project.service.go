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

package project

import (
	"context"

	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/project/pb"
	"github.com/erda-project/erda/internal/core/project/dao"
	"github.com/erda-project/erda/pkg/common/errors"
)

// project implements pb.ProjectServer
type project struct {
	l *logrus.Entry
}

func (p *project) CheckProjectExist(ctx context.Context, req *pb.CheckProjectExistReq) (*pb.CheckProjectExistResp, error) {
	l := p.l.WithField("projectID", req.GetId())
	_, ok, err := dao.GetProject(dao.Q(), dao.Col("id").Is(req.GetId()))
	if err != nil {
		l.WithError(err).Errorln("failed to dao.GetProject")
		return nil, err
	}
	return &pb.CheckProjectExistResp{Ok: ok}, nil
}

func (p *project) GetProjectByID(ctx context.Context, req *pb.GetProjectByIDReq) (*pb.ProjectDto, error) {
	l := p.l.WithField("projectID", req.GetId())
	proj, ok, err := dao.GetProject(dao.Q(), dao.Col("id").Is(req.GetId()))
	if err != nil {
		l.Errorf("failed to dao.GetProject: %v", err)
		return nil, err
	}
	if !ok {
		l.Warnln("project not found")
		return nil, errors.NewNotFoundError("project")
	}

	var dto = pb.ProjectDto{
		Id:          uint64(proj.ID),
		Name:        proj.Name,
		DisplayName: proj.DisplayName,
		OrgID:       uint64(proj.OrgID),
		CreatorID:   proj.Creator,
		Logo:        proj.Logo,
		Desc:        proj.Desc,
		ActiveTime:  timestamppb.New(proj.ActiveTime),
		IsPublic:    proj.IsPublic,
		CreatedTime: timestamppb.New(proj.CreatedAt),
		UpdatedTime: timestamppb.New(proj.UpdatedAt),
		Type:        proj.Type,
	}
	return &dto, nil
}
