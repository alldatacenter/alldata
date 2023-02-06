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

// Package endpoints release的handle
package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/sirupsen/logrus"

	definitionpb "github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	sourcepb "github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/pipeline"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

// ReleaseCallback gittar hook的回调，目前只响应TAG，自动触发pipeline进行release
func (e *Endpoints) ReleaseCallback(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.GittarPushEvent

	if r.Body == nil {
		logrus.Errorf("nil body")
		return apierrors.ErrReleaseCallback.MissingParameter("body").ToResp(), nil
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		logrus.Errorf("failed to decode body, (%+v)", err)
		return apierrors.ErrReleaseCallback.InvalidParameter(err).ToResp(), nil
	}
	logrus.Debugf("GittarCallback: request body: %+v", req)
	params := map[string]string{
		"beforeID":     req.Content.Before,
		"afterID":      req.Content.After,
		"ref":          req.Content.Ref,
		"pusherName":   req.Content.Pusher.NickName,
		"commitsCount": strconv.FormatInt(int64(req.Content.TotalCommitsCount), 10),
	}
	e.TriggerGitNotify(req.OrgID, req.ApplicationID, apistructs.GitPushEvent, params)

	appID, _ := strconv.ParseUint(req.ApplicationID, 10, 64)
	refName := ""
	if req.Content.IsTag {
		refName = strings.TrimPrefix(req.Content.Ref, "refs/tags/")
	} else {
		refName = strings.TrimPrefix(req.Content.Ref, "refs/heads/")
	}

	listYmlReq := apistructs.CICDPipelineYmlListRequest{
		AppID:  int64(appID),
		Branch: refName,
	}
	result := pipeline.GetPipelineYmlList(listYmlReq, e.bdl, req.Content.Pusher.ID)

	app, err := e.bdl.GetApp(appID)
	if err != nil {
		return nil, apierrors.ErrGetApp.InternalError(err)
	}
	rules, err := e.branchRule.Query(apistructs.AppScope, int64(app.ID))
	if err != nil {
		return nil, apierrors.ErrFetchConfigNamespace.InternalError(err)
	}

	projectRules, err := e.branchRule.Query(apistructs.ProjectScope, int64(app.ProjectID))
	if err != nil {
		return nil, apierrors.ErrFetchConfigNamespace.InternalError(err)
	}

	for _, each := range result {
		strPipelineYml, err := e.pipeline.FetchPipelineYml(app.GitRepo, refName, each, req.Content.Pusher.ID)
		if err != nil {
			logrus.Errorf("failed to fetch %v from gittar, req: %+v, (%+v)", each, req, err)
			continue
		}

		pipelineYml, err := pipelineyml.New([]byte(strPipelineYml))
		if err != nil {
			logrus.Errorf("failed to parse %v yaml:%v \n err:%v", each, pipelineYml, err)
			continue
		}

		if pipelineYml.Spec().On != nil && pipelineYml.Spec().On.Push != nil {
			if !diceworkspace.IsRefPatternMatch(refName, pipelineYml.Spec().On.Push.Branches) {
				continue
			}
		} else {
			// app setting only support run pipeline.yml
			if each != apistructs.DefaultPipelineYmlName {
				continue
			}

			validBranch := diceworkspace.GetValidBranchByGitReference(refName, rules)
			if !validBranch.IsTriggerPipeline {
				continue
			}
		}

		path, fileName := getSourcePathAndName(each)
		definitionID, err := e.getDefinitionID(ctx, app, refName, path, fileName)
		if err != nil {
			logrus.Errorf("failed to bind definition %v", err)
		}

		// 创建pipeline流程
		reqPipeline := &apistructs.PipelineCreateRequest{
			AppID:              uint64(req.Content.Repository.ApplicationID),
			Branch:             refName,
			Source:             apistructs.PipelineSourceDice,
			PipelineYmlSource:  apistructs.PipelineYmlSourceGittar,
			PipelineYmlContent: strPipelineYml,
			AutoRun:            true,
			UserID:             req.Content.Pusher.ID,
		}
		v2, err := e.pipeline.ConvertPipelineToV2(reqPipeline)
		if err != nil {
			logrus.Errorf("error convert to pipelineV2 %s, (%+v)", strPipelineYml, err)
			continue
		}
		validBranch := diceworkspace.GetValidBranchByGitReference(reqPipeline.Branch, projectRules)
		workspace := validBranch.Workspace
		v2.ForceRun = true
		v2.DefinitionID = definitionID
		v2.PipelineYmlName = fmt.Sprintf("%d/%s/%s/%s", reqPipeline.AppID, workspace, refName, strings.TrimPrefix(each, "/"))

		_, err = e.pipeline.CreatePipelineV2(v2)
		if err != nil {
			logrus.Errorf("create pipeline failed, pipeline: %s, (%+v)", strPipelineYml, err)
			continue
		}
	}
	return httpserver.OkResp("success")
}

func getSourcePathAndName(name string) (path, fileName string) {
	if strings.HasPrefix(name, apistructs.ErdaPipelinePath) {
		return apistructs.ErdaPipelinePath, strings.Replace(name, apistructs.ErdaPipelinePath+"/", "", 1)
	}
	if strings.HasPrefix(name, apistructs.DicePipelinePath) {
		return apistructs.DicePipelinePath, strings.Replace(name, apistructs.DicePipelinePath+"/", "", 1)
	}
	return "", name
}

func (e *Endpoints) getDefinitionID(ctx context.Context, app *apistructs.ApplicationDTO, branch, path, name string) (definitionID string, err error) {
	if app == nil {
		return "", nil
	}
	sourceList, err := e.PipelineSource.List(ctx, &sourcepb.PipelineSourceListRequest{
		Remote:     fmt.Sprintf("%v/%v/%v", app.OrgName, app.ProjectName, app.Name),
		Ref:        branch,
		Name:       name,
		Path:       path,
		SourceType: apistructs.SourceTypeErda,
	})
	if err != nil {
		return "", err
	}

	var source *sourcepb.PipelineSource
	for _, v := range sourceList.Data {
		source = v
		break
	}
	if source == nil {
		return "", nil
	}

	definitionList, err := e.PipelineDefinition.List(ctx, &definitionpb.PipelineDefinitionListRequest{
		SourceIDList: []string{source.ID},
		Location:     apistructs.MakeLocation(app, apistructs.PipelineTypeCICD),
	})
	if err != nil {
		return "", nil
	}

	for _, definition := range definitionList.Data {
		return definition.ID, nil
	}
	return "", nil
}
