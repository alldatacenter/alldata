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

package guide

import (
	"context"
	"encoding/json"
	"fmt"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/dop/guide/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/guide/db"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/limit_sync_group"
)

const (
	DicePipelinePath string = ".dice/pipelines"
	ErdaPipelinePath string = ".erda/pipelines"
	InitCommitID     string = "0000000000000000000000000000000000000000"
	BranchPrefix     string = "refs/heads/"
)

type GuideService struct {
	bdl *bundle.Bundle
	db  *db.GuideDB

	branchRuleSve *branchrule.BranchRule
}

func (g *GuideService) WithBranchRuleSve(svc *branchrule.BranchRule) {
	g.branchRuleSve = svc
}

type Service interface {
	CreateGuideByGittarPushHook(context.Context, *pb.GittarPushPayloadEvent) (*pb.CreateGuideResponse, error)
	ListGuide(context.Context, *pb.ListGuideRequest) (*pb.ListGuideResponse, error)
	ProcessGuide(context.Context, *pb.ProcessGuideRequest) (*pb.ProcessGuideResponse, error)
}

func (g *GuideService) CreateGuideByGittarPushHook(ctx context.Context, req *pb.GittarPushPayloadEvent) (*pb.CreateGuideResponse, error) {
	if req.Content.Before != InitCommitID {
		return &pb.CreateGuideResponse{}, nil
	}

	orgID, err := strconv.ParseUint(req.OrgID, 10, 64)
	if err != nil {
		return nil, apierrors.ErrCreateGuide.InvalidParameter(err)
	}
	projectID, err := strconv.ParseUint(req.ProjectID, 10, 64)
	if err != nil {
		return nil, apierrors.ErrCreateGuide.InvalidParameter(err)
	}
	appID, err := strconv.ParseUint(req.ApplicationID, 10, 64)
	if err != nil {
		return nil, apierrors.ErrCreateGuide.InvalidParameter(err)
	}
	appDto, err := g.bdl.GetApp(appID)
	if err != nil {
		return nil, apierrors.ErrCreateGuide.InvalidParameter(err)
	}
	// Check branch rules
	ok, err := g.checkBranchRule(getBranchFromRef(req.Content.Ref), int64(projectID))
	if err != nil {
		return nil, apierrors.ErrCreateGuide.InvalidParameter(err)
	}
	if !ok {
		return &pb.CreateGuideResponse{}, nil
	}

	// Find pipeline yml list
	ymls, err := g.ListPipelineYml(appDto, getBranchFromRef(req.Content.Ref), req.Content.Pusher.ID)
	if err != nil {
		return nil, apierrors.ErrCreateGuide.InternalError(err)
	}
	if len(ymls) == 0 {
		return &pb.CreateGuideResponse{}, nil
	}
	pipelineYmls := make([]string, 0, len(ymls))
	for i := range ymls {
		pipelineYmls = append(pipelineYmls, filepath.Join(ymls[i].YmlPath, ymls[i].YmlName))
	}

	b, err := json.Marshal(&db.PipelineContent{
		Branch:       req.Content.Ref[len(BranchPrefix):],
		PipelineYmls: pipelineYmls,
	})
	if err != nil {
		return nil, apierrors.ErrCreateGuide.InternalError(err)
	}

	guide := db.Guide{
		Status:        db.InitStatus.String(),
		Kind:          db.PipelineGuide.String(),
		Creator:       req.Content.Pusher.ID,
		OrgID:         orgID,
		OrgName:       appDto.OrgName,
		ProjectID:     projectID,
		AppID:         appID,
		AppName:       appDto.Name,
		Branch:        req.Content.Ref[len(BranchPrefix):],
		Content:       db.JSON(b),
		SoftDeletedAt: 0,
	}
	// Check uniqueness
	has, err := g.db.CheckUniqueByAppIDAndBranch(guide.AppID, guide.Branch, guide.Kind)
	if err != nil {
		return nil, apierrors.ErrCreateGuide.InvalidParameter(err)
	}
	if has {
		return &pb.CreateGuideResponse{}, nil
	}

	if err = g.db.CreateGuide(&guide); err != nil {
		return nil, apierrors.ErrCreateGuide.InternalError(err)
	}
	return &pb.CreateGuideResponse{Data: guide.Convert()}, nil
}

func (g *GuideService) ListGuide(ctx context.Context, req *pb.ListGuideRequest) (*pb.ListGuideResponse, error) {
	if err := req.Validate(); err != nil {
		return nil, apierrors.ErrListGuide.InvalidParameter(err)
	}
	guidesDB, err := g.db.ListGuide(req, apis.GetUserID(ctx))
	if err != nil {
		return nil, apierrors.ErrListGuide.InternalError(err)
	}
	guides := make([]*pb.Guide, 0, len(guidesDB))
	for _, v := range guidesDB {
		guides = append(guides, v.Convert())
	}
	return &pb.ListGuideResponse{Data: guides}, nil
}

func (g *GuideService) ProcessGuide(ctx context.Context, req *pb.ProcessGuideRequest) (*pb.ProcessGuideResponse, error) {
	if err := req.Validate(); err != nil {
		return nil, apierrors.ErrProcessGuide.InvalidParameter(err)
	}

	if req.Kind == db.PipelineGuide.String() {
		if err := g.db.UpdateGuideByAppIDAndBranch(req.AppID, req.Branch, req.Kind, map[string]interface{}{"status": db.ProcessedStatus}); err != nil {
			return nil, apierrors.ErrProcessGuide.InternalError(err)
		}
	}

	return &pb.ProcessGuideResponse{}, nil
}

func (g *GuideService) BatchUpdateGuideExpiryStatus() error {
	return g.db.BatchUpdateGuideExpiryStatus()
}

type PipelineYml struct {
	YmlName string
	YmlPath string
}

func (g *GuideService) ListPipelineYml(app *apistructs.ApplicationDTO, branch, userID string) ([]PipelineYml, error) {
	work := limit_sync_group.NewWorker(3)
	var list []PipelineYml
	var pathList = []string{"", ErdaPipelinePath}
	for _, path := range pathList {
		work.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			result, err := g.getPipelineYml(app, userID, branch, i[0].(string))
			if err != nil {
				return nil
			}

			locker.Lock()
			defer locker.Unlock()
			list = append(list, result...)
			return nil
		}, path)
	}
	if err := work.Do().Error(); err != nil {
		return nil, err
	}

	return list, nil
}

func (g *GuideService) getPipelineYml(app *apistructs.ApplicationDTO, userID string, branch string, findPath string) ([]PipelineYml, error) {
	var path string
	if findPath == "" {
		path = fmt.Sprintf("/wb/%v/%v/tree/%v", app.ProjectName, app.Name, branch)
	} else {
		path = fmt.Sprintf("/wb/%v/%v/tree/%v/%v", app.ProjectName, app.Name, branch, findPath)
	}

	treeData, err := g.bdl.GetGittarTreeNode(path, strconv.Itoa(int(app.OrgID)), true, userID)
	if err != nil {
		return nil, err
	}

	var list []PipelineYml
	for _, entry := range treeData.Entries {
		if !strings.HasSuffix(entry.Name, ".yml") &&
			!strings.HasSuffix(entry.Name, ".yaml") {
			continue
		}
		if findPath == "" && entry.Name != apistructs.DefaultPipelineYmlName {
			continue
		}
		list = append(list, PipelineYml{
			YmlName: entry.Name,
			YmlPath: findPath,
		})
	}
	return list, nil
}

func (g *GuideService) checkBranchRule(branch string, projectID int64) (bool, error) {
	branchRules, err := g.branchRuleSve.Query(apistructs.ProjectScope, projectID)
	if err != nil {
		return false, err
	}
	_, err = diceworkspace.GetByGitReference(branch, branchRules)
	if err != nil {
		return false, err
	}
	return true, nil
}

func (g *GuideService) checkPipelineYml(app *apistructs.ApplicationDTO, branch, userID string) (bool, error) {
	ymls, err := g.ListPipelineYml(app, branch, userID)
	if err != nil {
		return false, err
	}
	if len(ymls) == 0 {
		logrus.Info("the pipeline yml is not exists")
		return false, nil
	}
	return true, nil
}

func getBranchFromRef(ref string) string {
	return ref[len(BranchPrefix):]
}

func (g *GuideService) DeleteGuideByGittarPushHook(ctx context.Context, req *pb.GittarPushPayloadEvent) (*pb.DeleteGuideResponse, error) {
	if req.Content.After != InitCommitID {
		return &pb.DeleteGuideResponse{}, nil
	}

	appID, err := strconv.ParseUint(req.ApplicationID, 10, 64)
	if err != nil {
		return nil, apierrors.ErrDeleteGuide.InvalidParameter(err)
	}

	if err = g.db.UpdateGuideByAppIDAndBranch(appID, getBranchFromRef(req.Content.Ref), db.PipelineGuide.String(), map[string]interface{}{"soft_deleted_at": time.Now().UnixNano() / 1e6}); err != nil {
		return nil, apierrors.ErrDeleteGuide.InternalError(err)
	}
	return &pb.DeleteGuideResponse{}, nil
}

func (g *GuideService) CancelGuide(ctx context.Context, req *pb.CancelGuideRequest) (*pb.CancelGuideResponse, error) {
	if err := req.Validate(); err != nil {
		return nil, apierrors.ErrCancelGuide.InvalidParameter(err)
	}

	guide, err := g.db.GetGuide(req.ID)
	if err != nil {
		return nil, apierrors.ErrCancelGuide.InternalError(err)
	}
	if guide.Creator != apis.GetUserID(ctx) {
		return nil, fmt.Errorf("unauthorized operation")
	}

	if err = g.db.CancelGuide(req.ID); err != nil {
		return nil, apierrors.ErrCancelGuide.InternalError(err)
	}

	return &pb.CancelGuideResponse{}, nil
}
