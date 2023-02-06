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

package projectpipeline

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"path/filepath"
	"sort"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	dpb "github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	common "github.com/erda-project/erda-proto-go/core/pipeline/pb"
	spb "github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	guidepb "github.com/erda-project/erda-proto-go/dop/guide/pb"
	"github.com/erda-project/erda-proto-go/dop/projectpipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline/deftype"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/utils"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	def "github.com/erda-project/erda/internal/tools/pipeline/providers/definition"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/limit_sync_group"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type CategoryType string

const (
	DefaultCategory CategoryType = "default"
	StarCategory    CategoryType = "primary"

	CreateProjectPipelineNamePreCheckLocaleKey   string = "ProjectPipelineCreateNamePreCheckNotPass"
	CreateProjectPipelineSourcePreCheckLocaleKey string = "ProjectPipelineCreateSourcePreCheckNotPass"

	InitCommitID string = "0000000000000000000000000000000000000000"
	BranchPrefix string = "refs/heads/"
)

type AutoRunParams struct {
	definition *dpb.PipelineDefinition
	source     *spb.PipelineSource
	runParams  []*pb.PipelineRunParam
}

func (c CategoryType) String() string {
	return string(c)
}

func (s *ProjectPipelineService) ListPipelineYml(ctx context.Context, req *pb.ListAppPipelineYmlRequest) (*pb.ListAppPipelineYmlResponse, error) {

	app, err := s.bundle.GetApp(req.AppID)
	if err != nil {
		return nil, err
	}
	list, err := s.ListPipelineYmlByApp(app, req.Branch, apis.GetUserID(ctx))
	if err != nil {
		return nil, err
	}

	return &pb.ListAppPipelineYmlResponse{
		Result: list,
	}, nil
}

func (s *ProjectPipelineService) ListPipelineYmlByApp(app *apistructs.ApplicationDTO, branch, userID string) ([]*pb.PipelineYmlList, error) {
	work := limit_sync_group.NewWorker(3)
	var list []*pb.PipelineYmlList
	var pathList = []string{apistructs.DefaultPipelinePath, apistructs.DicePipelinePath, apistructs.ErdaPipelinePath}
	for _, path := range pathList {
		work.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			result, err := s.GetPipelineYml(app, userID, branch, i[0].(string))
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

func (s *ProjectPipelineService) GetPipelineYml(app *apistructs.ApplicationDTO, userID string, branch string, findPath string) ([]*pb.PipelineYmlList, error) {
	var path string
	if findPath == "" {
		path = fmt.Sprintf("/wb/%v/%v/tree/%v", app.ProjectName, app.Name, branch)
	} else {
		path = fmt.Sprintf("/wb/%v/%v/tree/%v/%v", app.ProjectName, app.Name, branch, findPath)
	}

	treeData, err := s.bundle.GetGittarTreeNode(path, strconv.Itoa(int(app.OrgID)), true, userID)
	if err != nil {
		return nil, err
	}

	var list []*pb.PipelineYmlList
	for _, entry := range treeData.Entries {
		if !strings.HasSuffix(entry.Name, ".yml") {
			continue
		}
		if findPath == "" && entry.Name != apistructs.DefaultPipelineYmlName {
			continue
		}
		list = append(list, &pb.PipelineYmlList{
			YmlName: entry.Name,
			YmlPath: findPath,
		})
	}
	return list, nil
}

func (p *ProjectPipelineService) CreateSourcePreCheck(ctx context.Context, params *pb.CreateProjectPipelineSourcePreCheckRequest) (*pb.CreateProjectPipelineSourcePreCheckResponse, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrCreateProjectPipeline.InvalidParameter(err)
	}

	app, err := p.bundle.GetApp(params.AppID)
	if err != nil {
		return nil, err
	}

	resp, err := p.PipelineSource.List(ctx, &spb.PipelineSourceListRequest{
		SourceType: params.SourceType,
		Remote:     makeRemote(app),
		Ref:        params.Ref,
		Path:       params.Path,
		Name:       params.FileName,
	})
	if err != nil {
		return nil, apierrors.ErrCreateProjectPipeline.InvalidParameter(err)
	}
	if len(resp.Data) == 0 {
		return &pb.CreateProjectPipelineSourcePreCheckResponse{
			Pass: true,
		}, nil
	}

	definitionList, err := p.PipelineDefinition.List(ctx, &dpb.PipelineDefinitionListRequest{
		Location: apistructs.MakeLocation(&apistructs.ApplicationDTO{
			OrgName:     app.OrgName,
			ProjectName: app.ProjectName,
		}, apistructs.PipelineTypeCICD),
		SourceIDList: []string{resp.Data[0].ID},
	})
	if err != nil {
		return nil, apierrors.ErrCreateProjectPipeline.InvalidParameter(err)
	}

	if len(definitionList.Data) == 0 {
		return &pb.CreateProjectPipelineSourcePreCheckResponse{
			Pass: true,
		}, nil
	}
	definitionName := definitionList.Data[0].Name

	return &pb.CreateProjectPipelineSourcePreCheckResponse{
		Pass:    false,
		Message: fmt.Sprintf(p.trans.Text(apis.Language(ctx), CreateProjectPipelineSourcePreCheckLocaleKey), definitionName),
	}, nil
}

func (p *ProjectPipelineService) CreateNamePreCheck(ctx context.Context, req *pb.CreateProjectPipelineNamePreCheckRequest) (*pb.CreateProjectPipelineNamePreCheckResponse, error) {
	if err := req.Validate(); err != nil {
		return nil, apierrors.ErrCreateProjectPipeline.InvalidParameter(err)
	}

	haveSameNameDefinition, err := p.checkDefinitionRemoteSameName(req.ProjectID, "", req.Name)
	if err != nil {
		return nil, apierrors.ErrCreateProjectPipeline.InternalError(err)
	}
	if haveSameNameDefinition {
		return &pb.CreateProjectPipelineNamePreCheckResponse{
			Pass:    false,
			Message: p.trans.Text(apis.Language(ctx), CreateProjectPipelineNamePreCheckLocaleKey),
		}, nil
	}

	return &pb.CreateProjectPipelineNamePreCheckResponse{
		Pass: true,
	}, nil
}

func (p *ProjectPipelineService) Create(ctx context.Context, params *pb.CreateProjectPipelineRequest) (*pb.CreateProjectPipelineResponse, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrCreateProjectPipeline.InvalidParameter(err)
	}

	err := p.checkRolePermission(apistructs.IdentityInfo{
		UserID: apis.GetUserID(ctx),
	}, &apistructs.PipelineCreateRequestV2{
		Labels: map[string]string{
			apistructs.LabelAppID:  strconv.FormatUint(params.AppID, 10),
			apistructs.LabelBranch: params.Ref,
		},
	}, apierrors.ErrCreateProjectPipeline)
	if err != nil {
		return nil, err
	}
	pipeline, err := p.CreateOne(ctx, params)
	if err != nil {
		return nil, apierrors.ErrCreateProjectPipeline.InternalError(err)
	}
	return &pb.CreateProjectPipelineResponse{ProjectPipeline: pipeline}, nil
}

func (p *ProjectPipelineService) BatchCreateByGittarPushHook(ctx context.Context, params *pb.GittarPushPayloadEvent) (*pb.BatchCreateProjectPipelineResponse, error) {
	if params.Content.Before != InitCommitID {
		return &pb.BatchCreateProjectPipelineResponse{}, nil
	}

	projectID, err := strconv.ParseUint(params.ProjectID, 10, 64)
	if err != nil {
		return nil, err
	}
	appID, err := strconv.ParseUint(params.ApplicationID, 10, 64)
	if err != nil {
		return nil, err
	}
	appDto, err := p.bundle.GetApp(appID)
	if err != nil {
		return nil, err
	}
	// Check branch rules
	ok, err := p.CheckBranchRule(getBranchFromRef(params.Content.Ref), int64(projectID))
	if err != nil {
		return nil, err
	}
	if !ok {
		return &pb.BatchCreateProjectPipelineResponse{}, nil
	}

	// Find pipeline yml list
	ymls, err := p.ListPipelineYmlByApp(appDto, getBranchFromRef(params.Content.Ref), params.Content.Pusher.ID)
	if err != nil {
		return nil, err
	}
	if len(ymls) == 0 {
		return &pb.BatchCreateProjectPipelineResponse{}, nil
	}
	pipelineYmls := make([]string, 0, len(ymls))
	for i := range ymls {
		pipelineYmls = append(pipelineYmls, filepath.Join(ymls[i].YmlPath, ymls[i].YmlName))
	}

	for _, v := range pipelineYmls {
		req := pb.CreateProjectPipelineRequest{
			ProjectID:  projectID,
			AppID:      appID,
			SourceType: "erda",
			Ref:        getBranchFromRef(params.Content.Ref),
			Path:       getFilePath(v),
			FileName:   filepath.Base(v),
		}
		_, err = p.CreateOne(ctx, &req)
		if err != nil {
			p.logger.Errorf("failed to createOne, pipelineYml: %s, err: %v", v, err)
		}
	}

	return &pb.BatchCreateProjectPipelineResponse{}, nil
}

func getBranchFromRef(ref string) string {
	return ref[len(BranchPrefix):]
}

func (p *ProjectPipelineService) CheckBranchRule(branch string, projectID int64) (bool, error) {
	branchRules, err := p.branchRuleSve.Query(apistructs.ProjectScope, projectID)
	if err != nil {
		return false, err
	}
	_, err = diceworkspace.GetByGitReference(branch, branchRules)
	if err != nil {
		return false, err
	}
	return true, nil
}

func (p *ProjectPipelineService) CreateOne(ctx context.Context, params *pb.CreateProjectPipelineRequest) (*pb.ProjectPipeline, error) {
	sourceCheckResult, err := p.CreateSourcePreCheck(ctx, &pb.CreateProjectPipelineSourcePreCheckRequest{
		SourceType: params.SourceType,
		Ref:        params.Ref,
		Path:       params.Path,
		FileName:   params.FileName,
		AppID:      params.AppID,
	})
	if err != nil {
		return nil, err
	}
	if !sourceCheckResult.Pass {
		return nil, fmt.Errorf(sourceCheckResult.Message)
	}

	pipelineSourceType := NewProjectSourceType(params.SourceType)
	sourceReq, err := pipelineSourceType.GenerateReq(ctx, p, params)
	if err != nil {
		return nil, err
	}

	sourceRsp, err := p.PipelineSource.Create(ctx, sourceReq)
	if err != nil {
		return nil, err
	}

	location, err := p.makeLocationByAppID(params.AppID)
	if err != nil {
		return nil, err
	}

	definitionRsp, err := p.PipelineDefinition.Create(ctx, &dpb.PipelineDefinitionCreateRequest{
		Location:         location,
		Name:             makePipelineName(params, sourceReq.PipelineYml),
		Creator:          apis.GetUserID(ctx),
		PipelineSourceID: sourceRsp.PipelineSource.ID,
		Category:         DefaultCategory.String(),
		Extra: &dpb.PipelineDefinitionExtra{
			Extra: pipelineSourceType.GetPipelineCreateRequestV2(),
		},
		Ref: sourceRsp.PipelineSource.Ref,
	})
	if err != nil {
		return nil, err
	}

	// When creating a definition, a scheduled task is created
	err = p.createCronIfNotExist(definitionRsp.PipelineDefinition, pipelineSourceType)
	if err != nil {
		return nil, fmt.Errorf("failed to createCronIfNotExist error %v", err)
	}

	_, err = p.GuideSvc.ProcessGuide(ctx, &guidepb.ProcessGuideRequest{AppID: params.AppID, Branch: params.Ref, Kind: "pipeline"})
	if err != nil {
		p.logger.Errorf("failed to ProcessGuide, err: %v, appID: %d, branch: %s", err, params.AppID, params.Ref)
	}

	return &pb.ProjectPipeline{
		ID:               definitionRsp.PipelineDefinition.ID,
		Name:             definitionRsp.PipelineDefinition.Name,
		Creator:          definitionRsp.PipelineDefinition.Creator,
		Category:         definitionRsp.PipelineDefinition.Category,
		TimeCreated:      definitionRsp.PipelineDefinition.TimeCreated,
		TimeUpdated:      definitionRsp.PipelineDefinition.TimeUpdated,
		SourceType:       sourceRsp.PipelineSource.SourceType,
		Remote:           sourceRsp.PipelineSource.Remote,
		Ref:              sourceRsp.PipelineSource.Ref,
		Path:             sourceRsp.PipelineSource.Path,
		FileName:         sourceRsp.PipelineSource.Name,
		PipelineSourceID: sourceRsp.PipelineSource.ID,
	}, nil
}

func makePipelineName(params *pb.CreateProjectPipelineRequest, pipelineYml string) string {
	if params.Name != "" {
		return params.Name
	}
	yml, err := pipelineyml.GetNameByPipelineYml(pipelineYml)
	if err != nil {
		return ""
	}
	if yml != "" {
		return yml
	}
	return params.FileName
}

func (p *ProjectPipelineService) createCronIfNotExist(definition *dpb.PipelineDefinition, projectPipelineType ProjectSourceType) error {
	extraString := projectPipelineType.GetPipelineCreateRequestV2()
	var extra apistructs.PipelineDefinitionExtraValue
	err := json.Unmarshal([]byte(extraString), &extra)
	if err != nil {
		return err
	}

	crons, err := p.PipelineCron.CronPaging(context.Background(), &cronpb.CronPagingRequest{
		AllSources: false,
		Sources:    []string{extra.CreateRequest.PipelineSource.String()},
		YmlNames:   []string{extra.CreateRequest.PipelineYmlName},
		PageSize:   1,
		PageNo:     1,
	})
	if err != nil {
		return err
	}
	if len(crons.Data) == 1 && crons.Data[0].PipelineDefinitionID == definition.ID {
		return nil
	}

	createV2 := extra.CreateRequest
	createV2.DefinitionID = definition.ID
	_, err = p.bundle.CreatePipeline(createV2)
	if err != nil {
		return fmt.Errorf("failed to CreatePipeline, err: %v", err)
	}

	return nil
}

func (p *ProjectPipelineService) checkDefinitionRemoteSameName(projectID uint64, definitionID, name string) (bool, error) {
	projectDto, err := p.bundle.GetProject(projectID)
	if err != nil {
		return false, err
	}

	orgDto, err := p.bundle.GetOrg(projectDto.OrgID)
	if err != nil {
		return false, err
	}

	location := apistructs.MakeLocation(&apistructs.ApplicationDTO{
		OrgName:     orgDto.Name,
		ProjectName: projectDto.Name,
	}, apistructs.PipelineTypeCICD)

	resp, err := p.PipelineDefinition.List(context.Background(), &dpb.PipelineDefinitionListRequest{
		Location: location,
		Name:     name,
		PageNo:   1,
		PageSize: 1,
	})
	if err != nil {
		return false, err
	}
	if len(resp.Data) == 0 {
		return false, nil
	}
	if definitionID != "" && resp.Data[0].ID == definitionID {
		return false, nil
	}
	return true, nil
}

func (p *ProjectPipelineService) getYmlFromGittar(app *apistructs.ApplicationDTO, ref, filePath, userID string) (string, error) {
	commit, err := p.bundle.GetGittarCommit(app.GitRepoAbbrev, ref, userID)
	if err != nil {
		return "", err
	}

	yml, err := p.bundle.GetGittarFile(app.GitRepo, commit.ID, filePath, "", "", userID)
	return yml, err
}

func (p *ProjectPipelineService) List(ctx context.Context, params deftype.ProjectPipelineList) ([]*dpb.PipelineDefinition, int64, error) {
	if err := params.Validate(); err != nil {
		return nil, 0, apierrors.ErrListProjectPipeline.InvalidParameter(err)
	}

	project, err := p.bundle.GetProject(params.ProjectID)
	if err != nil {
		return nil, 0, apierrors.ErrListProjectPipeline.InternalError(err)
	}

	org, err := p.bundle.GetOrg(project.OrgID)
	if err != nil {
		return nil, 0, apierrors.ErrListProjectPipeline.InternalError(err)
	}

	list, err := p.PipelineDefinition.List(ctx, &dpb.PipelineDefinitionListRequest{
		PageSize: int64(params.PageSize),
		PageNo:   int64(params.PageNo),
		Location: apistructs.MakeLocation(&apistructs.ApplicationDTO{
			OrgName:     org.Name,
			ProjectName: project.Name,
		}, apistructs.PipelineTypeCICD),
		FuzzyName:         params.Name,
		Creator:           params.Creator,
		Executor:          params.Executor,
		Category:          params.Category,
		Ref:               params.Ref,
		Remote:            getRemotes(params.AppName, org.Name, project.Name),
		TimeCreated:       params.TimeCreated,
		TimeStarted:       params.TimeStarted,
		Status:            params.Status,
		AscCols:           params.AscCols,
		DescCols:          params.DescCols,
		IsOthers:          params.CategoryKey == apistructs.CategoryOthers.String(),
		FilePathWithNames: getRulesByCategoryKey(apistructs.PipelineCategory(params.CategoryKey)),
	})
	if err != nil {
		return nil, 0, apierrors.ErrListProjectPipeline.InternalError(err)
	}

	return list.Data, list.Total, nil
}

func getRulesByCategoryKey(categoryKey apistructs.PipelineCategory) []string {
	if categoryKey != apistructs.CategoryOthers {
		return apistructs.CategoryKeyRuleMap[categoryKey]
	}
	rules := make([]string, 0, len(apistructs.CategoryKeyRuleMap))
	for k := range apistructs.CategoryKeyRuleMap {
		rules = append(rules, apistructs.CategoryKeyRuleMap[k]...)
	}
	return rules
}

func (p *ProjectPipelineService) Delete(ctx context.Context, params deftype.ProjectPipelineDelete) (*deftype.ProjectPipelineDeleteResult, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrDeleteProjectPipeline.InvalidParameter(err)
	}

	definition, source, err := p.getPipelineDefinitionAndSource(params.ID)
	if err != nil {
		return nil, apierrors.ErrDeleteProjectPipeline.InvalidParameter(err)
	}
	if definition.Creator != apis.GetUserID(ctx) {
		return nil, apierrors.ErrDeleteProjectPipeline.AccessDenied()
	}
	err = p.checkDataPermissionByProjectID(params.ProjectID, source)
	if err != nil {
		return nil, apierrors.ErrDeleteProjectPipeline.AccessDenied()
	}
	if apistructs.PipelineStatus(definition.Status).IsRunningStatus() {
		return nil, apierrors.ErrDeleteProjectPipeline.InternalError(fmt.Errorf("pipeline wass running status"))
	}

	extraValue, err := def.GetExtraValue(definition)
	if err != nil {
		return nil, apierrors.ErrDeleteProjectPipeline.InternalError(fmt.Errorf("failed unmarshal pipeline extra error %v", err))
	}
	crons, err := p.PipelineCron.CronPaging(context.Background(), &cronpb.CronPagingRequest{
		Sources:  []string{extraValue.CreateRequest.PipelineSource.String()},
		YmlNames: []string{extraValue.CreateRequest.PipelineYmlName},
		PageSize: 1,
		PageNo:   1,
	})
	if err != nil {
		return nil, apierrors.ErrDeleteProjectPipeline.InternalError(err)
	}
	if len(crons.Data) > 0 && crons.Data[0].Enable != nil && crons.Data[0].Enable.Value == true {
		return nil, apierrors.ErrDeleteProjectPipeline.InternalError(fmt.Errorf("pipeline cron is running status"))
	}

	_, err = p.PipelineDefinition.Delete(ctx, &dpb.PipelineDefinitionDeleteRequest{PipelineDefinitionID: params.ID})
	return nil, err
}

func (p *ProjectPipelineService) Update(ctx context.Context, params *pb.UpdateProjectPipelineRequest) (*pb.UpdateProjectPipelineResponse, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrUpdateProjectPipeline.InvalidParameter(err)
	}

	_, source, err := p.getPipelineDefinitionAndSource(params.PipelineDefinitionID)
	if err != nil {
		return nil, apierrors.ErrUpdateProjectPipeline.InvalidParameter(err)
	}

	err = p.checkDataPermissionByProjectID(params.ProjectID, source)
	if err != nil {
		return nil, apierrors.ErrUpdateProjectPipeline.AccessDenied()
	}

	app, err := p.bundle.GetApp(params.ProjectPipelineSource.AppID)
	if err != nil {
		return nil, apierrors.ErrUpdateProjectPipeline.InvalidParameter(err)
	}
	if app.Name != getNameByRemote(source.Remote).AppName {
		return nil, apierrors.ErrUpdateProjectPipeline.InvalidParameter(fmt.Errorf("the application can not be updated"))
	}

	definitionUpdateReq := dpb.PipelineDefinitionUpdateRequest{
		PipelineDefinitionID: params.PipelineDefinitionID,
		Name:                 params.Name,
	}

	pipeline := pb.ProjectPipeline{
		SourceType:       source.SourceType,
		Remote:           source.Remote,
		Ref:              source.Ref,
		Path:             source.Path,
		FileName:         source.Name,
		PipelineSourceID: source.ID,
	}

	// update source
	if !isSameSourceInApp(source, params) {
		pipelineSourceType := NewProjectSourceType(params.ProjectPipelineSource.SourceType)
		sourceCreateReq, err := pipelineSourceType.GenerateReq(ctx, p, &pb.CreateProjectPipelineRequest{
			ProjectID:  params.ProjectID,
			AppID:      params.ProjectPipelineSource.AppID,
			SourceType: params.ProjectPipelineSource.SourceType,
			Ref:        params.ProjectPipelineSource.Ref,
			Path:       params.ProjectPipelineSource.Path,
			FileName:   params.ProjectPipelineSource.FileName,
		})
		if err != nil {
			return nil, apierrors.ErrUpdateProjectPipeline.InternalError(err)
		}
		sourceRsp, err := p.PipelineSource.Create(ctx, sourceCreateReq)
		if err != nil {
			return nil, apierrors.ErrUpdateProjectPipeline.InternalError(err)
		}
		definitionUpdateReq.PipelineSourceID = sourceRsp.PipelineSource.ID
		pipeline.SourceType = sourceRsp.PipelineSource.SourceType
		pipeline.Remote = sourceRsp.PipelineSource.Remote
		pipeline.Ref = sourceRsp.PipelineSource.Ref
		pipeline.Path = sourceRsp.PipelineSource.Path
		pipeline.FileName = sourceRsp.PipelineSource.Name
		pipeline.PipelineSourceID = sourceRsp.PipelineSource.ID
	}

	definitionRsp, err := p.PipelineDefinition.Update(ctx, &definitionUpdateReq)
	if err != nil {
		return nil, apierrors.ErrUpdateProjectPipeline.InternalError(err)
	}
	pipeline.ID = definitionRsp.PipelineDefinition.ID
	pipeline.Name = definitionRsp.PipelineDefinition.Name
	pipeline.Creator = definitionRsp.PipelineDefinition.Creator
	pipeline.Category = definitionRsp.PipelineDefinition.Category
	pipeline.TimeCreated = definitionRsp.PipelineDefinition.TimeCreated
	pipeline.TimeUpdated = definitionRsp.PipelineDefinition.TimeUpdated

	return &pb.UpdateProjectPipelineResponse{ProjectPipeline: &pipeline}, nil
}

func isSameSourceInApp(source *spb.PipelineSource, params *pb.UpdateProjectPipelineRequest) bool {
	if source.Ref != params.ProjectPipelineSource.Ref || source.Path != params.ProjectPipelineSource.Path ||
		source.Name != params.ProjectPipelineSource.FileName {
		return false
	}
	return true
}

func (p *ProjectPipelineService) SetPrimary(ctx context.Context, params deftype.ProjectPipelineCategory) (*dpb.PipelineDefinitionUpdateResponse, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrSetPrimaryProjectPipeline.InvalidParameter(err)
	}

	_, source, err := p.getPipelineDefinitionAndSource(params.PipelineDefinitionID)
	if err != nil {
		return nil, apierrors.ErrSetPrimaryProjectPipeline.InvalidParameter(err)
	}
	err = p.checkDataPermissionByProjectID(params.ProjectID, source)
	if err != nil {
		return nil, apierrors.ErrSetPrimaryProjectPipeline.AccessDenied()
	}

	definition, err := p.PipelineDefinition.Update(ctx, &dpb.PipelineDefinitionUpdateRequest{
		PipelineDefinitionID: params.PipelineDefinitionID,
		Category:             StarCategory.String(),
	})
	if err != nil {
		return nil, apierrors.ErrSetPrimaryProjectPipeline.InternalError(err)
	}

	return definition, nil
}

func (p *ProjectPipelineService) UnSetPrimary(ctx context.Context, params deftype.ProjectPipelineCategory) (*dpb.PipelineDefinitionUpdateResponse, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrUnSetPrimaryProjectPipeline.InvalidParameter(err)
	}

	_, source, err := p.getPipelineDefinitionAndSource(params.PipelineDefinitionID)
	if err != nil {
		return nil, apierrors.ErrUnSetPrimaryProjectPipeline.InvalidParameter(err)
	}
	err = p.checkDataPermissionByProjectID(params.ProjectID, source)
	if err != nil {
		return nil, apierrors.ErrUnSetPrimaryProjectPipeline.AccessDenied()
	}

	definition, err := p.PipelineDefinition.Update(ctx, &dpb.PipelineDefinitionUpdateRequest{
		PipelineDefinitionID: params.PipelineDefinitionID,
		Category:             DefaultCategory.String(),
	})
	if err != nil {
		return nil, apierrors.ErrUnSetPrimaryProjectPipeline.InternalError(err)
	}

	return definition, nil
}

func (p *ProjectPipelineService) Run(ctx context.Context, params *pb.RunProjectPipelineRequest) (*pb.RunProjectPipelineResponse, error) {
	if params.PipelineDefinitionID == "" {
		return nil, apierrors.ErrRunProjectPipeline.InvalidParameter(fmt.Errorf("pipelineDefinitionID：%s", params.PipelineDefinitionID))
	}

	definition, source, err := p.getPipelineDefinitionAndSource(params.PipelineDefinitionID)
	if err != nil {
		return nil, apierrors.ErrRunProjectPipeline.InternalError(err)
	}
	err = p.checkDataPermissionByProjectID(uint64(params.ProjectID), source)
	if err != nil {
		return nil, apierrors.ErrRunProjectPipeline.AccessDenied()
	}
	value, err := p.autoRunPipeline(apistructs.IdentityInfo{UserID: apis.GetUserID(ctx), InternalClient: apis.GetInternalClient(ctx)}, AutoRunParams{
		definition: definition,
		source:     source,
		runParams:  params.RunParams,
	})
	if err != nil {
		return nil, err
	}

	pipeline, err := pipelineDTOToStructPb(value)
	if err != nil {
		return nil, err
	}

	return &pb.RunProjectPipelineResponse{
		Pipeline: pipeline,
	}, nil
}

func pipelineDTOToStructPb(value *apistructs.PipelineDTO) (*structpb.Value, error) {
	valueJson, err := json.Marshal(value)
	if err != nil {
		return nil, err
	}

	var specValue map[string]interface{}
	err = json.Unmarshal(valueJson, &specValue)
	if err != nil {
		return nil, err
	}

	pipeline, err := structpb.NewValue(specValue)
	if err != nil {
		return nil, err
	}
	return pipeline, nil
}

func (p *ProjectPipelineService) StartCron(ctx context.Context, params deftype.ProjectPipelineStartCron) (*deftype.ProjectPipelineStartCronResult, error) {
	dto, err := p.startOrEndCron(params.IdentityInfo, params.PipelineDefinitionID, params.ProjectID, true, apierrors.ErrStartCronProjectPipeline)
	if err != nil {
		return nil, err
	}

	return &deftype.ProjectPipelineStartCronResult{
		Cron: dto,
	}, nil
}

func (p *ProjectPipelineService) EndCron(ctx context.Context, params deftype.ProjectPipelineEndCron) (*deftype.ProjectPipelineEndCronResult, error) {

	dto, err := p.startOrEndCron(params.IdentityInfo, params.PipelineDefinitionID, params.ProjectID, false, apierrors.ErrEndCronProjectPipeline)
	if err != nil {
		return nil, err
	}
	return &deftype.ProjectPipelineEndCronResult{
		Cron: dto,
	}, nil
}

func (p *ProjectPipelineService) ListExecHistory(ctx context.Context, params *pb.ListPipelineExecHistoryRequest) (*pb.ListPipelineExecHistoryResponse, error) {
	// Check project permission
	req := apistructs.PermissionCheckRequest{
		UserID:   apis.GetUserID(ctx),
		Scope:    apistructs.ProjectScope,
		ScopeID:  params.ProjectID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	}
	if access, err := p.bundle.CheckPermission(&req); err != nil || !access.Access {
		return nil, apierrors.ErrListExecHistoryProjectPipeline.AccessDenied()
	}
	var pipelineDefinition = apistructs.PipelineDefinitionRequest{}
	pipelineDefinition.Name = params.Name

	if params.ProjectID == 0 {
		return nil, apierrors.ErrListExecHistoryProjectPipeline.InternalError(fmt.Errorf("projectID can not empty"))
	}

	projectDto, err := p.bundle.GetProject(params.ProjectID)
	if err != nil {
		return nil, apierrors.ErrListExecHistoryProjectPipeline.InternalError(err)
	}

	orgDto, err := p.bundle.GetOrg(projectDto.OrgID)
	if err != nil {
		return nil, apierrors.ErrListExecHistoryProjectPipeline.InternalError(err)
	}

	pipelineDefinition.Location = apistructs.MakeLocation(&apistructs.ApplicationDTO{
		OrgName:     orgDto.Name,
		ProjectName: projectDto.Name,
	}, apistructs.PipelineTypeCICD)
	pipelineDefinition.SourceRemotes = getRemotes(params.AppNames, orgDto.Name, projectDto.Name)

	jsonValue, err := json.Marshal(pipelineDefinition)
	if err != nil {
		return nil, apierrors.ErrListExecHistoryProjectPipeline.InternalError(err)
	}

	pipelinePageListRequest := makePipelinePageListRequest(params, jsonValue)

	data, err := p.bundle.PageListPipeline(pipelinePageListRequest)
	if err != nil {
		return nil, apierrors.ErrListExecHistoryProjectPipeline.InternalError(err)
	}
	return makeListPipelineExecHistoryResponse(data), nil
}

func makePipelinePageListRequest(params *pb.ListPipelineExecHistoryRequest, jsonValue []byte) apistructs.PipelinePageListRequest {
	var startTime, endTime int64
	if params.StartTimeBegin != nil {
		startTime = (*params.StartTimeBegin).AsTime().Unix()
	}
	if params.StartTimeEnd != nil {
		endTime = (*params.StartTimeEnd).AsTime().Unix()
	}

	var pipelinePageListRequest = apistructs.PipelinePageListRequest{
		PageNum:                             int(params.PageNo),
		PageSize:                            int(params.PageSize),
		Statuses:                            params.Statuses,
		AllSources:                          true,
		StartTimeBeginTimestamp:             startTime,
		EndTimeBeginTimestamp:               endTime,
		PipelineDefinitionRequestJSONBase64: base64.StdEncoding.EncodeToString(jsonValue),
		DescCols:                            params.DescCols,
		AscCols:                             params.AscCols,
	}
	if len(params.Executors) > 0 {
		for _, v := range params.Executors {
			pipelinePageListRequest.MustMatchLabelsQueryParams = append(pipelinePageListRequest.MustMatchLabelsQueryParams, fmt.Sprintf("%v=%v", apistructs.LabelRunUserID, v))
		}
	}
	if len(params.Branches) > 0 {
		for _, v := range params.Branches {
			pipelinePageListRequest.MustMatchLabelsQueryParams = append(pipelinePageListRequest.MustMatchLabelsQueryParams, fmt.Sprintf("%v=%v", apistructs.LabelBranch, v))
		}
	}
	return pipelinePageListRequest
}

func makeListPipelineExecHistoryResponse(data *apistructs.PipelinePageListData) *pb.ListPipelineExecHistoryResponse {
	execHistories := make([]*pb.PipelineExecHistory, 0)
	for _, pipeline := range data.Pipelines {
		if pipeline.DefinitionPageInfo == nil {
			continue
		}
		var timeBegin *timestamppb.Timestamp
		if pipeline.TimeBegin != nil {
			timeBegin = timestamppb.New((*pipeline.TimeBegin).UTC())
		}
		if timeBegin != nil && timeBegin.AsTime().Unix() <= 0 {
			timeBegin = nil
		}

		execHistories = append(execHistories, &pb.PipelineExecHistory{
			PipelineName:   pipeline.DefinitionPageInfo.Name,
			PipelineStatus: pipeline.Status.String(),
			CostTimeSec: func() int64 {
				if !pipeline.Status.IsRunningStatus() &&
					!pipeline.Status.IsEndStatus() {
					return -1
				}
				return pipeline.CostTimeSec
			}(),
			AppName:    getApplicationNameFromDefinitionRemote(pipeline.DefinitionPageInfo.SourceRemote),
			Branch:     pipeline.DefinitionPageInfo.SourceRef,
			Executor:   pipeline.GetRunUserID(),
			TimeBegin:  timeBegin,
			PipelineID: pipeline.ID,
		})
	}
	return &pb.ListPipelineExecHistoryResponse{
		Total:           data.Total,
		CurrentPageSize: data.CurrentPageSize,
		ExecHistories:   execHistories,
	}
}

func getApplicationNameFromDefinitionRemote(remote string) string {
	values := strings.Split(remote, string(filepath.Separator))
	if len(values) != 3 {
		return remote
	}
	return values[2]
}

func (p *ProjectPipelineService) BatchRun(ctx context.Context, params deftype.ProjectPipelineBatchRun) (*deftype.ProjectPipelineBatchRunResult, error) {
	definitionMap, err := p.batchGetPipelineDefinition(params.PipelineDefinitionIDs, params.ProjectID)
	if err != nil {
		return nil, apierrors.ErrBatchRunProjectPipeline.InternalError(err)
	}

	var pipelineSourceIDArray []string
	for _, v := range definitionMap {
		if v.PipelineSourceID == "" {
			return nil, apierrors.ErrBatchRunProjectPipeline.InternalError(fmt.Errorf("definition %v pipeline source was empty", v.ID))
		}
		pipelineSourceIDArray = append(pipelineSourceIDArray, v.PipelineSourceID)
	}

	sourceMap, err := p.batchGetPipelineSources(pipelineSourceIDArray)
	if err != nil {
		return nil, apierrors.ErrRunProjectPipeline.InternalError(err)
	}

	project, err := p.bundle.GetProject(params.ProjectID)
	if err != nil {
		return nil, apierrors.ErrRunProjectPipeline.InternalError(err)
	}
	org, err := p.bundle.GetOrg(project.OrgID)
	if err != nil {
		return nil, apierrors.ErrRunProjectPipeline.InternalError(err)
	}
	for _, source := range sourceMap {
		err := p.checkDataPermission(project, org, source)
		if err != nil {
			return nil, err
		}
	}

	work := limit_sync_group.NewWorker(5)
	var result = map[string]*apistructs.PipelineDTO{}

	for _, v := range definitionMap {
		work.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			var definitionID = i[0].(string)
			var sourceID = i[1].(string)

			value, err := p.autoRunPipeline(params.IdentityInfo, AutoRunParams{
				definition: definitionMap[definitionID],
				source:     sourceMap[sourceID],
			})
			if err != nil {
				return err
			}

			locker.Lock()
			result[definitionID] = value
			locker.Unlock()
			return nil
		}, v.ID, v.PipelineSourceID)
	}
	if err := work.Do().Error(); err != nil {
		return nil, err
	}

	return &deftype.ProjectPipelineBatchRunResult{
		PipelineMap: result,
	}, nil
}

func (p *ProjectPipelineService) Cancel(ctx context.Context, params *pb.CancelProjectPipelineRequest) (*pb.CancelProjectPipelineResponse, error) {
	if params.PipelineDefinitionID == "" {
		return nil, apierrors.ErrCancelProjectPipeline.InvalidParameter(fmt.Errorf("pipelineDefinitionID：%s", params.PipelineDefinitionID))
	}

	definition, source, err := p.getPipelineDefinitionAndSource(params.PipelineDefinitionID)
	if err != nil {
		return nil, apierrors.ErrCancelProjectPipeline.InternalError(err)
	}
	err = p.checkDataPermissionByProjectID(uint64(params.ProjectID), source)
	if err != nil {
		return nil, apierrors.ErrCancelProjectPipeline.AccessDenied()
	}

	extraValue, err := def.GetExtraValue(definition)
	if err != nil {
		return nil, apierrors.ErrCancelProjectPipeline.InternalError(fmt.Errorf("failed unmarshal pipeline extra error %v", err))
	}

	identityInfo := apistructs.IdentityInfo{UserID: apis.GetUserID(ctx), InternalClient: apis.GetInternalClient(ctx)}

	if err := p.checkRolePermission(identityInfo, extraValue.CreateRequest, apierrors.ErrCancelProjectPipeline); err != nil {
		return nil, err
	}

	pipelineInfo, err := p.bundle.GetPipeline(uint64(definition.PipelineID))
	if err != nil {
		return nil, apierrors.ErrCancelProjectPipeline.InternalError(err)
	}

	if pipelineInfo.Status.CanCancel() {
		var req apistructs.PipelineCancelRequest
		req.PipelineID = uint64(definition.PipelineID)
		req.IdentityInfo = identityInfo
		err = p.bundle.CancelPipeline(req)
		if err != nil {
			return nil, apierrors.ErrCancelProjectPipeline.InternalError(err)
		}

		_, err = p.PipelineDefinition.Update(context.Background(), &dpb.PipelineDefinitionUpdateRequest{PipelineDefinitionID: definition.ID, Status: string(apistructs.PipelineStatusStopByUser), PipelineID: definition.PipelineID})
		if err != nil {
			return nil, apierrors.ErrCancelProjectPipeline.InternalError(err)
		}

		return &pb.CancelProjectPipelineResponse{}, nil
	}

	_, err = p.PipelineDefinition.Update(context.Background(), &dpb.PipelineDefinitionUpdateRequest{PipelineDefinitionID: definition.ID, Status: string(pipelineInfo.Status), PipelineID: definition.PipelineID})
	if err != nil {
		return nil, apierrors.ErrCancelProjectPipeline.InternalError(err)
	}

	return &pb.CancelProjectPipelineResponse{}, nil
}

func (p *ProjectPipelineService) Rerun(ctx context.Context, params *pb.RerunProjectPipelineRequest) (*pb.RerunProjectPipelineResponse, error) {

	identityInfo := apistructs.IdentityInfo{UserID: apis.GetUserID(ctx), InternalClient: apis.GetInternalClient(ctx)}

	dto, err := p.failRerunOrRerunPipeline(true, params.PipelineDefinitionID, uint64(params.ProjectID), identityInfo, apierrors.ErrRerunProjectPipeline)
	if err != nil {
		return nil, err
	}

	return &pb.RerunProjectPipelineResponse{
		Pipeline: dto,
	}, nil
}

func (p *ProjectPipelineService) RerunFailed(ctx context.Context, params *pb.RerunFailedProjectPipelineRequest) (*pb.RerunFailedProjectPipelineResponse, error) {

	identityInfo := apistructs.IdentityInfo{UserID: apis.GetUserID(ctx), InternalClient: apis.GetInternalClient(ctx)}

	dto, err := p.failRerunOrRerunPipeline(false, params.PipelineDefinitionID, uint64(params.ProjectID), identityInfo, apierrors.ErrFailRerunProjectPipeline)
	if err != nil {
		return nil, err
	}

	return &pb.RerunFailedProjectPipelineResponse{
		Pipeline: dto,
	}, nil
}

func (p *ProjectPipelineService) failRerunOrRerunPipeline(rerun bool, pipelineDefinitionID string, projectID uint64, identityInfo apistructs.IdentityInfo, apiError *errorresp.APIError) (*structpb.Value, error) {
	if pipelineDefinitionID == "" {
		return nil, apiError.InvalidParameter(fmt.Errorf("pipelineDefinitionID：%s", pipelineDefinitionID))
	}

	definition, source, err := p.getPipelineDefinitionAndSource(pipelineDefinitionID)
	if err != nil {
		return nil, apiError.InternalError(err)
	}
	err = p.checkDataPermissionByProjectID(projectID, source)
	if err != nil {
		return nil, apiError.AccessDenied()
	}

	extraValue, err := def.GetExtraValue(definition)
	if err != nil {
		return nil, apiError.InternalError(fmt.Errorf("failed unmarshal pipeline extra error %v", err))
	}

	if err = p.checkRolePermission(identityInfo, extraValue.CreateRequest, apiError); err != nil {
		return nil, err
	}

	pipeline, err := p.bundle.GetPipeline(uint64(definition.PipelineID))
	if err != nil {
		return nil, err
	}

	var dto *apistructs.PipelineDTO
	if rerun {
		var req apistructs.PipelineRerunRequest
		req.PipelineID = uint64(definition.PipelineID)
		req.AutoRunAtOnce = true
		req.IdentityInfo = identityInfo
		req.Secrets = utils.GetGittarSecrets(pipeline.ClusterName, pipeline.Branch, pipeline.CommitDetail)
		dto, err = p.bundle.RerunPipeline(req)
	} else {
		var req apistructs.PipelineRerunFailedRequest
		req.PipelineID = uint64(definition.PipelineID)
		req.AutoRunAtOnce = true
		req.IdentityInfo = identityInfo
		req.Secrets = utils.GetGittarSecrets(pipeline.ClusterName, pipeline.Branch, pipeline.CommitDetail)
		dto, err = p.bundle.RerunFailedPipeline(req)
	}
	if err != nil {
		return nil, apiError.InternalError(err)
	}

	pbValue, err := pipelineDTOToStructPb(dto)
	if err != nil {
		return nil, err
	}

	return pbValue, nil
}

func (p *ProjectPipelineService) startOrEndCron(identityInfo apistructs.IdentityInfo, pipelineDefinitionID string, projectID uint64, enable bool, apiError *errorresp.APIError) (*common.Cron, error) {
	if pipelineDefinitionID == "" {
		return nil, apiError.InvalidParameter(fmt.Errorf("pipelineDefinitionID：%s", pipelineDefinitionID))
	}

	definition, source, err := p.getPipelineDefinitionAndSource(pipelineDefinitionID)
	if err != nil {
		return nil, apiError.InternalError(err)
	}
	err = p.checkDataPermissionByProjectID(projectID, source)
	if err != nil {
		return nil, apiError.AccessDenied()
	}

	extraValue, err := def.GetExtraValue(definition)
	if err != nil {
		return nil, apiError.InternalError(fmt.Errorf("failed unmarshal pipeline extra error %v", err))
	}

	if err := p.checkRolePermission(identityInfo, extraValue.CreateRequest, apiError); err != nil {
		return nil, err
	}

	cron, err := p.PipelineCron.CronPaging(context.Background(), &cronpb.CronPagingRequest{
		PageNo:   1,
		PageSize: 1,
		Sources:  []string{extraValue.CreateRequest.PipelineSource.String()},
		YmlNames: []string{extraValue.CreateRequest.PipelineYmlName},
	})
	if err != nil {
		return nil, apiError.InternalError(err)
	}
	if len(cron.Data) == 0 {
		return nil, apiError.InternalError(fmt.Errorf("not find cron"))
	}

	if cron.Data[0].PipelineDefinitionID == "" {
		_, err := p.PipelineCron.CronUpdate(context.Background(), &cronpb.CronUpdateRequest{
			CronID:                 cron.Data[0].ID,
			PipelineYml:            cron.Data[0].PipelineYml,
			PipelineDefinitionID:   pipelineDefinitionID,
			CronExpr:               cron.Data[0].CronExpr,
			ConfigManageNamespaces: cron.Data[0].ConfigManageNamespaces,
		})
		if err != nil {
			return nil, apiError.InternalError(err)
		}
	}

	var dto *common.Cron
	if enable {
		orgStr := extraValue.CreateRequest.Labels[apistructs.LabelOrgID]
		orgID, err := strconv.ParseUint(orgStr, 10, 64)
		if err != nil {
			return nil, apiError.InternalError(fmt.Errorf("not found orgID"))
		}
		// update CmsNsConfigs
		if err = p.UpdateCmsNsConfigs(identityInfo.UserID, orgID); err != nil {
			return nil, apiError.InternalError(err)
		}

		result, err := p.PipelineCron.CronStart(context.Background(), &cronpb.CronStartRequest{
			CronID: cron.Data[0].ID,
		})
		if err != nil {
			return nil, apiError.InternalError(err)
		}
		dto = result.Data
	} else {
		result, err := p.PipelineCron.CronStop(context.Background(), &cronpb.CronStopRequest{
			CronID: cron.Data[0].ID,
		})
		if err != nil {
			return nil, apiError.InternalError(err)
		}
		dto = result.Data
	}
	return dto, nil
}

func getRunningStatus() []string {
	var runningStatus []string
	for _, status := range apistructs.ReconcilerRunningStatuses() {
		runningStatus = append(runningStatus, status.String())
	}
	return runningStatus
}

func (p *ProjectPipelineService) getRunningPipeline(pipelineSource string, pipelineYmlName string) (pipelineID uint64, err error) {
	var pipelinePageListRequest = apistructs.PipelinePageListRequest{
		PageNum:  1,
		PageSize: 1,
		Sources: []apistructs.PipelineSource{
			apistructs.PipelineSource(pipelineSource),
		},
		YmlNames: []string{
			pipelineYmlName,
		},
		Statuses: getRunningStatus(),
		DescCols: []string{
			"id",
		},
	}
	data, err := p.bundle.PageListPipeline(pipelinePageListRequest)
	if err != nil {
		return 0, apierrors.ErrCancelProjectPipeline.InternalError(err)
	}
	if len(data.Pipelines) == 0 {
		return 0, nil
	}
	return data.Pipelines[0].ID, nil
}

func (p *ProjectPipelineService) getLatestPipeline(pipelineSource string, pipelineYmlName string) (pipeline *apistructs.PagePipeline, err error) {
	var pipelinePageListRequest = apistructs.PipelinePageListRequest{
		PageNum:  1,
		PageSize: 1,
		Sources: []apistructs.PipelineSource{
			apistructs.PipelineSource(pipelineSource),
		},
		YmlNames: []string{
			pipelineYmlName,
		},
		NotStatuses: getRunningStatus(),
		DescCols: []string{
			"id",
		},
	}
	data, err := p.bundle.PageListPipeline(pipelinePageListRequest)
	if err != nil {
		return nil, apierrors.ErrCancelProjectPipeline.InternalError(err)
	}
	if len(data.Pipelines) == 0 {
		return nil, nil
	}
	return &data.Pipelines[0], nil
}

func (p *ProjectPipelineService) getPipelineDefinitionAndSource(pipelineDefinitionID string) (PipelineDefinition *dpb.PipelineDefinition, pipelineSource *spb.PipelineSource, err error) {
	definition, err := p.getPipelineDefinition(pipelineDefinitionID)
	if err != nil {
		return nil, nil, err
	}
	source, err := p.getPipelineSource(definition.PipelineSourceID)
	if err != nil {
		return nil, nil, err
	}
	return definition, source, nil
}

func (p *ProjectPipelineService) getPipelineDefinition(pipelineDefinitionID string) (PipelineDefinition *dpb.PipelineDefinition, err error) {
	var getReq dpb.PipelineDefinitionGetRequest
	getReq.PipelineDefinitionID = pipelineDefinitionID
	resp, err := p.PipelineDefinition.Get(context.Background(), &getReq)
	if err != nil {
		return nil, err
	}
	if resp == nil || resp.PipelineDefinition == nil || resp.PipelineDefinition.Extra == nil {
		return nil, fmt.Errorf("not find pipeline definition")
	}
	return resp.PipelineDefinition, nil
}

func (p *ProjectPipelineService) getPipelineSource(sourceID string) (pipelineSource *spb.PipelineSource, err error) {
	var sourceGetReq = spb.PipelineSourceGetRequest{}
	sourceGetReq.PipelineSourceID = sourceID
	source, err := p.PipelineSource.Get(context.Background(), &sourceGetReq)
	if err != nil {
		return nil, err
	}
	if source == nil || source.PipelineSource == nil || source.PipelineSource.PipelineYml == "" {
		return nil, fmt.Errorf("source %v pipeline yml was empty", sourceID)
	}
	return source.PipelineSource, nil
}

func (p *ProjectPipelineService) batchGetPipelineDefinition(pipelineDefinitionIDArray []string, projectID uint64) (map[string]*dpb.PipelineDefinition, error) {
	location, err := p.makeLocationByProjectID(projectID)
	if err != nil {
		return nil, err
	}
	resp, err := p.PipelineDefinition.List(context.Background(), &dpb.PipelineDefinitionListRequest{
		PageNo:   1,
		PageSize: int64(len(pipelineDefinitionIDArray)),
		Location: location,
		IdList:   pipelineDefinitionIDArray,
	})
	if err != nil {
		return nil, err
	}
	var pipelineDefinitionMap = map[string]*dpb.PipelineDefinition{}
	for _, v := range resp.Data {
		pipelineDefinitionMap[v.ID] = v
	}
	return pipelineDefinitionMap, nil
}

func (p *ProjectPipelineService) batchGetPipelineSources(pipelineSourceIDArray []string) (map[string]*spb.PipelineSource, error) {
	var pipelineSourceListRequest spb.PipelineSourceListRequest
	pipelineSourceListRequest.IdList = pipelineSourceIDArray
	resp, err := p.PipelineSource.List(context.Background(), &pipelineSourceListRequest)
	if err != nil {
		return nil, err
	}
	var pipelineSourceMap = map[string]*spb.PipelineSource{}
	for _, v := range resp.Data {
		pipelineSourceMap[v.ID] = v
	}
	return pipelineSourceMap, nil
}

func (p *ProjectPipelineService) autoRunPipeline(identityInfo apistructs.IdentityInfo, params AutoRunParams) (*apistructs.PipelineDTO, error) {
	source := params.source
	definition := params.definition

	extraValue, err := def.GetExtraValue(params.definition)
	if err != nil {
		return nil, apierrors.ErrRunProjectPipeline.InternalError(fmt.Errorf("failed unmarshal pipeline extra error %v", err))
	}
	createV2 := extraValue.CreateRequest

	if err := p.checkRolePermission(identityInfo, extraValue.CreateRequest, apierrors.ErrRunProjectPipeline); err != nil {
		return nil, err
	}

	appIDString := createV2.Labels[apistructs.LabelAppID]
	appID, err := strconv.ParseUint(appIDString, 10, 64)
	if err != nil {
		return nil, err
	}
	orgStr := createV2.Labels[apistructs.LabelOrgID]
	orgID, err := strconv.ParseUint(orgStr, 10, 64)
	if err != nil {
		return nil, err
	}

	// If source type is erda，should sync pipelineYml file
	pipelineYml := params.source.PipelineYml
	if source.SourceType == deftype.ErdaProjectPipelineType.String() {
		pipelineYml, err = p.fetchRemotePipeline(source, orgStr, identityInfo.UserID)
		if err != nil {
			logrus.Errorf("failed to fetchRemotePipeline, err: %s", err.Error())
			return nil, err
		}
		if pipelineYml != source.PipelineYml {
			_, err = p.PipelineSource.Update(context.Background(), &spb.PipelineSourceUpdateRequest{
				PipelineYml:      pipelineYml,
				PipelineSourceID: source.ID,
			})
			if err != nil {
				logrus.Errorf("failed to update pipelien source, err: %s", err.Error())
				return nil, err
			}
		}
	}

	// update user gittar token
	var worker = limit_sync_group.NewWorker(3)
	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		// update CmsNsConfigs
		if err = p.UpdateCmsNsConfigs(identityInfo.UserID, orgID); err != nil {
			return apierrors.ErrRunProjectPipeline.InternalError(err)
		}
		return nil
	})

	// runParams
	var extra apistructs.PipelineDefinitionExtraValue
	err = json.Unmarshal([]byte(definition.Extra.Extra), &extra)
	if err != nil {
		return nil, err
	}
	if params.runParams == nil {
		// Get the value of the last run
		params.runParams = extra.RunParams
	} else {
		// upload run params in definition extra
		worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
			extra.RunParams = params.runParams
			_, err := p.PipelineDefinition.UpdateExtra(context.Background(), &dpb.PipelineDefinitionExtraUpdateRequest{
				Extra:                jsonparse.JsonOneLine(extra),
				PipelineDefinitionID: definition.ID,
			})
			return err
		})
	}

	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		createV2, err = p.pipelineSvc.ConvertPipelineToV2(&apistructs.PipelineCreateRequest{
			PipelineYmlName:    filepath.Join(source.Path, source.Name),
			AppID:              appID,
			Branch:             createV2.Labels[apistructs.LabelBranch],
			PipelineYmlContent: pipelineYml,
			UserID:             identityInfo.UserID,
		})
		if err != nil {
			return err
		}
		return nil
	})
	if worker.Do().Error() != nil {
		return nil, worker.Error()
	}

	createV2.AutoRunAtOnce = true
	createV2.DefinitionID = definition.ID
	createV2.UserID = identityInfo.UserID

	// run params
	var pipelineRunParams apistructs.PipelineRunParams
	for _, runParams := range params.runParams {
		pipelineRunParams = append(pipelineRunParams, apistructs.PipelineRunParam{
			Name:  runParams.Name,
			Value: runParams.Value.AsInterface(),
		})
	}
	createV2.RunParams = pipelineRunParams

	value, err := p.bundle.CreatePipeline(createV2)
	if err != nil {
		return nil, apierrors.ErrRunProjectPipeline.InternalError(err)
	}
	return value, nil
}

func (p *ProjectPipelineService) ListApp(ctx context.Context, params *pb.ListAppRequest) (*pb.ListAppResponse, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrListAppProjectPipeline.InvalidParameter(err)
	}

	project, err := p.bundle.GetProject(params.ProjectID)
	if err != nil {
		return nil, apierrors.ErrListAppProjectPipeline.InternalError(err)
	}

	org, err := p.bundle.GetOrg(project.OrgID)
	if err != nil {
		return nil, apierrors.ErrListAppProjectPipeline.InternalError(err)
	}

	appResp, err := p.bundle.GetMyAppsByProject(apis.GetUserID(ctx), project.OrgID, project.ID, params.Name)
	if err != nil {
		return nil, apierrors.ErrListAppProjectPipeline.InternalError(err)
	}

	appNames := make([]string, 0, len(appResp.List))
	for _, v := range appResp.List {
		appNames = append(appNames, v.Name)
	}

	statistics, err := p.PipelineDefinition.StatisticsGroupByRemote(ctx, &dpb.PipelineDefinitionStatisticsRequest{
		Location: apistructs.MakeLocation(&apistructs.ApplicationDTO{
			OrgName:     org.Name,
			ProjectName: project.Name,
		}, apistructs.PipelineTypeCICD),
	})
	if err != nil {
		return nil, apierrors.ErrListAppProjectPipeline.InternalError(err)
	}

	appNamePipelineNumMap := make(map[string]*pipelineNum)
	for _, v := range appNames {
		appNamePipelineNumMap[v] = &pipelineNum{
			RunningNum: 0,
			FailedNum:  0,
			TotalNum:   0,
		}
	}

	for _, v := range statistics.GetPipelineDefinitionStatistics() {
		remoteName := getNameByRemote(v.Group)
		if v2, ok := appNamePipelineNumMap[remoteName.AppName]; ok {
			v2.FailedNum = int(v.FailedNum)
			v2.RunningNum = int(v.RunningNum)
			v2.TotalNum = int(v.TotalNum)
		}
	}

	apps := make([]*pb.Application, 0, len(appResp.List))
	for _, v := range appResp.List {
		apps = append(apps, &pb.Application{
			ID:             v.ID,
			Name:           v.Name,
			DisplayName:    v.DisplayName,
			Mode:           v.Mode,
			Desc:           v.Desc,
			Logo:           v.Logo,
			IsPublic:       v.IsPublic,
			Creator:        v.Creator,
			GitRepo:        v.GitRepo,
			OrgID:          v.OrgID,
			OrgDisplayName: v.OrgDisplayName,
			ProjectID:      v.ProjectID,
			ProjectName:    v.ProjectName,
			IsExternalRepo: v.IsExternalRepo,
			CreatedAt:      timestamppb.New(v.CreatedAt),
			UpdatedAt:      timestamppb.New(v.UpdatedAt),
			RunningNum:     uint64(appNamePipelineNumMap[v.Name].RunningNum),
			FailedNum:      uint64(appNamePipelineNumMap[v.Name].FailedNum),
			TotalNum:       uint64(appNamePipelineNumMap[v.Name].TotalNum),
		})
	}
	sort.Slice(apps, func(i, j int) bool {
		return apps[i].TotalNum > apps[j].TotalNum
	})
	return &pb.ListAppResponse{
		Data: apps,
	}, nil
}

type pipelineNum struct {
	RunningNum int `json:"runningNum"`
	FailedNum  int `json:"failedNum"`
	TotalNum   int `json:"totalNum"`
}

func (p *ProjectPipelineService) checkRolePermission(identityInfo apistructs.IdentityInfo, createRequest *apistructs.PipelineCreateRequestV2, apiError *errorresp.APIError) error {
	appIDString := createRequest.Labels[apistructs.LabelAppID]
	appID, err := strconv.ParseInt(appIDString, 10, 64)
	if err != nil {
		return apiError.InternalError(fmt.Errorf("definition extras not find appID, err: %v", err.Error()))
	}
	if err := p.Permission.CheckRuntimeBranch(identityInfo, uint64(appID), createRequest.Labels[apistructs.LabelBranch], apistructs.OperateAction); err != nil {
		return apiError.AccessDenied()
	}
	return nil
}

func (p *ProjectPipelineService) checkDataPermission(project *apistructs.ProjectDTO, org *apistructs.OrgDTO, source *spb.PipelineSource) error {
	if !strings.HasPrefix(source.Remote, filepath.Join(org.Name, project.Name)) {
		return fmt.Errorf("no permission")
	}
	return nil
}

func (p *ProjectPipelineService) checkDataPermissionByProjectID(projectID uint64, source *spb.PipelineSource) error {
	project, err := p.bundle.GetProject(projectID)
	if err != nil {
		return err
	}

	org, err := p.bundle.GetOrg(project.OrgID)
	if err != nil {
		return err
	}

	return p.checkDataPermission(project, org, source)
}

// UpdateCmsNsConfigs update CmsNsConfigs
func (e *ProjectPipelineService) UpdateCmsNsConfigs(userID string, orgID uint64) error {
	res, err := e.tokenService.QueryTokens(context.Background(), &tokenpb.QueryTokensRequest{
		Scope:     string(apistructs.OrgScope),
		ScopeId:   strconv.FormatUint(orgID, 10),
		Type:      mysqltokenstore.PAT.String(),
		CreatorId: userID,
	})
	if err != nil {
		return err
	}

	if res.Total == 0 {
		return errors.New("the member is not exist")
	}

	_, err = e.PipelineCms.UpdateCmsNsConfigs(apis.WithInternalClientContext(context.Background(), "dop"),
		&cmspb.CmsNsConfigsUpdateRequest{
			Ns:             utils.MakeUserOrgPipelineCmsNs(userID, orgID),
			PipelineSource: apistructs.PipelineSourceDice.String(),
			KVs: map[string]*cmspb.PipelineCmsConfigValue{
				utils.MakeOrgGittarUsernamePipelineCmsNsConfig(): {Value: "git", EncryptInDB: true},
				utils.MakeOrgGittarTokenPipelineCmsNsConfig():    {Value: res.Data[0].AccessKey, EncryptInDB: true}},
		})

	return err
}

func (p *ProjectPipelineService) makeLocationByProjectID(projectID uint64) (string, error) {
	projectDto, err := p.bundle.GetProject(projectID)
	if err != nil {
		return "", err
	}
	orgDto, err := p.bundle.GetOrg(projectDto.OrgID)
	if err != nil {
		return "", err
	}

	return apistructs.MakeLocation(&apistructs.ApplicationDTO{
		OrgName:     orgDto.Name,
		ProjectName: projectDto.Name,
	}, apistructs.PipelineTypeCICD), nil
}

func (p *ProjectPipelineService) makeLocationByAppID(appID uint64) (string, error) {
	app, err := p.bundle.GetApp(appID)
	if err != nil {
		return "", err
	}

	return apistructs.MakeLocation(&apistructs.ApplicationDTO{
		OrgName:     app.OrgName,
		ProjectName: app.ProjectName,
	}, apistructs.PipelineTypeCICD), nil
}

type RemoteName struct {
	OrgName     string
	ProjectName string
	AppName     string
}

func getNameByRemote(remote string) RemoteName {
	splits := strings.Split(remote, string(filepath.Separator))
	if len(splits) != 3 {
		return RemoteName{}
	}
	return RemoteName{
		OrgName:     splits[0],
		ProjectName: splits[1],
		AppName:     splits[2],
	}
}

func (p *ProjectPipelineService) fetchRemotePipeline(source *spb.PipelineSource, orgID, userID string) (string, error) {
	remoteName := getNameByRemote(source.Remote)
	searchINode := remoteName.ProjectName + "/" + remoteName.AppName + "/blob/" + source.Ref + "/" + filepath.Join(source.Path, source.Name)
	return p.bundle.GetGittarBlobNode("/wb/"+searchINode, orgID, userID)
}

func (p *ProjectPipelineService) ListUsedRefs(ctx context.Context, params deftype.ProjectPipelineUsedRefList) ([]string, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrListProjectPipelineRef.InvalidParameter(err)
	}
	project, err := p.bundle.GetProject(params.ProjectID)
	if err != nil {
		return nil, apierrors.ErrListProjectPipelineRef.InternalError(err)
	}

	org, err := p.bundle.GetOrg(project.OrgID)
	if err != nil {
		return nil, apierrors.ErrListProjectPipelineRef.InternalError(err)
	}

	remotes, err := p.GetRemotesByAppID(params.AppID, org.Name, project.Name)
	if err != nil {
		return nil, apierrors.ErrListProjectPipelineRef.InternalError(err)
	}

	resp, err := p.PipelineDefinition.ListUsedRefs(ctx, &dpb.PipelineDefinitionUsedRefListRequest{Location: apistructs.MakeLocation(&apistructs.ApplicationDTO{
		OrgName:     org.Name,
		ProjectName: project.Name,
	}, apistructs.PipelineTypeCICD), Remotes: remotes})
	if err != nil {
		return nil, apierrors.ErrListProjectPipelineRef.InternalError(err)
	}
	return resp.Ref, nil
}

type PipelineStatisticsByCategory struct {
	Key      apistructs.PipelineCategory
	Category string
	Rules    []string
	PipelineStatisticsNums
}

type PipelineStatisticsNums struct {
	RunningNum uint64
	FailedNum  uint64
	TotalNum   uint64
}

func (p *ProjectPipelineService) ListPipelineStatisticsByCategory(ctx context.Context) []PipelineStatisticsByCategory {
	return []PipelineStatisticsByCategory{
		{
			Key:      apistructs.CategoryBuildDeploy,
			Category: p.trans.Text(apis.Language(ctx), apistructs.CategoryKeyI18NameMap[apistructs.CategoryBuildDeploy]),
			Rules:    apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildDeploy],
		},
		{
			Key:      apistructs.CategoryBuildArtifact,
			Category: p.trans.Text(apis.Language(ctx), apistructs.CategoryKeyI18NameMap[apistructs.CategoryBuildArtifact]),
			Rules:    apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildArtifact],
		},
		{
			Key:      apistructs.CategoryBuildCombineArtifact,
			Category: p.trans.Text(apis.Language(ctx), apistructs.CategoryKeyI18NameMap[apistructs.CategoryBuildCombineArtifact]),
			Rules:    apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildCombineArtifact],
		},
		{
			Key:      apistructs.CategoryBuildIntegration,
			Category: p.trans.Text(apis.Language(ctx), apistructs.CategoryKeyI18NameMap[apistructs.CategoryBuildIntegration]),
			Rules:    apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildIntegration],
		},
		{
			Key:      apistructs.CategoryOthers,
			Category: p.trans.Text(apis.Language(ctx), apistructs.CategoryKeyI18NameMap[apistructs.CategoryOthers]),
			Rules:    nil,
		},
	}
}

func (p *ProjectPipelineService) ListPipelineCategory(ctx context.Context, params *pb.ListPipelineCategoryRequest) (*pb.ListPipelineCategoryResponse, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrListProjectPipelineCategory.InvalidParameter(err)
	}

	// Check project permission
	req := apistructs.PermissionCheckRequest{
		UserID:   apis.GetUserID(ctx),
		Scope:    apistructs.ProjectScope,
		ScopeID:  params.ProjectID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	}
	if access, err := p.bundle.CheckPermission(&req); err != nil || !access.Access {
		return nil, apierrors.ErrListProjectPipelineCategory.AccessDenied()
	}

	project, err := p.bundle.GetProject(params.ProjectID)
	if err != nil {
		return nil, apierrors.ErrListProjectPipelineCategory.InternalError(err)
	}

	org, err := p.bundle.GetOrg(project.OrgID)
	if err != nil {
		return nil, apierrors.ErrListProjectPipelineCategory.InternalError(err)
	}

	remotes, err := p.GetRemotesByAppID(params.AppID, org.Name, project.Name)
	if err != nil {
		return nil, apierrors.ErrListProjectPipelineCategory.InternalError(err)
	}

	staticsResp, err := p.PipelineDefinition.StatisticsGroupByFilePath(ctx, &dpb.PipelineDefinitionStatisticsRequest{
		Location: apistructs.MakeLocation(&apistructs.ApplicationDTO{
			OrgName:     org.Name,
			ProjectName: project.Name,
		}, apistructs.PipelineTypeCICD),
		Remotes: remotes,
	})
	if err != nil {
		return nil, apierrors.ErrListProjectPipelineCategory.InternalError(err)
	}
	categories := p.ListPipelineStatisticsByCategory(ctx)

	for _, statics := range staticsResp.PipelineDefinitionStatistics {
		if key, ok := apistructs.GetRuleCategoryKeyMap()[statics.Group]; ok {
			for i := range categories {
				if key == categories[i].Key {
					categories[i].TotalNum += statics.TotalNum
					categories[i].FailedNum += statics.FailedNum
					categories[i].RunningNum += statics.RunningNum
					break
				}
			}
			continue
		}
		for i := range categories {
			if categories[i].Key == apistructs.CategoryOthers {
				categories[i].TotalNum += statics.TotalNum
				categories[i].FailedNum += statics.FailedNum
				categories[i].RunningNum += statics.RunningNum
				break
			}
		}
	}
	data := make([]*pb.PipelineCategory, 0, len(categories))
	for _, v := range categories {
		data = append(data, &pb.PipelineCategory{
			Key:        v.Key.String(),
			Category:   v.Category,
			Rules:      v.Rules,
			RunningNum: v.RunningNum,
			FailedNum:  v.FailedNum,
			TotalNum:   v.TotalNum,
		})
	}
	return &pb.ListPipelineCategoryResponse{Data: data}, nil
}

func (p *ProjectPipelineService) GetRemotesByAppID(appID uint64, orgName, projectName string) ([]string, error) {
	if appID == 0 {
		return nil, nil
	}
	appDto, err := p.bundle.GetApp(appID)
	if err != nil {
		return nil, err
	}
	return getRemotes([]string{appDto.Name}, orgName, projectName), nil
}

func getRemotes(appNames []string, orgName, projectName string) []string {
	remotes := make([]string, 0, len(appNames))
	for _, v := range appNames {
		remotes = append(remotes, makeRemote(&apistructs.ApplicationDTO{
			OrgName:     orgName,
			ProjectName: projectName,
			Name:        v,
		}))
	}
	return remotes
}

func (p *ProjectPipelineService) OneClickCreate(ctx context.Context, params *pb.OneClickCreateProjectPipelineRequest) (*pb.OneClickCreateProjectPipelineResponse, error) {
	if err := params.Validate(); err != nil {
		return nil, apierrors.ErrOneClickCreateProjectPipeline.InvalidParameter(err)
	}
	if len(params.PipelineYmls) == 0 {
		return nil, apierrors.ErrOneClickCreateProjectPipeline.InvalidParameter(fmt.Errorf("the pipelineYmls is empty"))
	}

	uncategorizedPipelineYmls := pipelineYmlsFilterIn(params.PipelineYmls, func(yml string) bool {
		for k := range apistructs.GetRuleCategoryKeyMap() {
			if k == yml {
				return false
			}
		}
		return true
	})
	if len(uncategorizedPipelineYmls) > 10 {
		return nil, apierrors.ErrOneClickCreateProjectPipeline.InvalidParameter(fmt.Errorf("the uncategorized pipelineYmls s greater than 10"))
	}

	// permission check
	err := p.checkRolePermission(apistructs.IdentityInfo{
		UserID: apis.GetUserID(ctx),
	}, &apistructs.PipelineCreateRequestV2{
		Labels: map[string]string{
			apistructs.LabelAppID:  strconv.FormatUint(params.AppID, 10),
			apistructs.LabelBranch: params.Ref,
		},
	}, apierrors.ErrOneClickCreateProjectPipeline)
	if err != nil {
		return nil, err
	}

	createReq := make([]*pb.CreateProjectPipelineRequest, 0, len(params.PipelineYmls))
	for _, v := range params.PipelineYmls {
		createReq = append(createReq, &pb.CreateProjectPipelineRequest{
			ProjectID:  params.ProjectID,
			Name:       p.generatePipelineName(ctx, v),
			AppID:      params.AppID,
			SourceType: params.SourceType,
			Ref:        params.Ref,
			Path:       getFilePath(v),
			FileName:   filepath.Base(v),
		})
	}

	pipelines := make([]*pb.ProjectPipeline, 0, len(params.PipelineYmls))
	var (
		message bytes.Buffer
		errMsgs []string
	)
	wait := limit_sync_group.NewSemaphore(10)
	for _, v := range createReq {
		wait.Add(1)
		go func(params *pb.CreateProjectPipelineRequest) {
			defer wait.Done()
			pipeline, err := p.CreateOne(ctx, params)
			if err != nil {
				errMsgs = append(errMsgs, fmt.Sprintf("failed to create %s, err: %s", filepath.Join(params.Path, params.FileName), err.Error()))
			} else {
				pipelines = append(pipelines, pipeline)
			}
		}(v)
	}
	wait.Wait()
	if len(errMsgs) > 0 {
		message.WriteString(fmt.Sprintf("Create %d pipelines, %d Success, %d Failed", len(params.PipelineYmls), len(pipelines), len(errMsgs)))
		for _, v := range errMsgs {
			message.WriteString("\n" + v)
		}
	}

	return &pb.OneClickCreateProjectPipelineResponse{
		ProjectPipelines: pipelines,
		ErrMsg:           message.String(),
	}, nil
}

func (p *ProjectPipelineService) generatePipelineName(ctx context.Context, pipelineYml string) string {
	if v, ok := apistructs.GetRuleCategoryKeyMap()[pipelineYml]; ok {
		return p.trans.Text(apis.Language(ctx), apistructs.CategoryKeyI18NameMap[v])
	}
	return filepath.Base(pipelineYml)
}

func getFilePath(path string) string {
	dir := filepath.Dir(path)
	if dir == "." {
		return ""
	}
	return dir
}

func pipelineYmlsFilterIn(ymls []string, fn func(yml string) bool) (newYmls []string) {
	for i := range ymls {
		if fn(ymls[i]) {
			newYmls = append(newYmls, ymls[i])
		}
	}
	return
}
