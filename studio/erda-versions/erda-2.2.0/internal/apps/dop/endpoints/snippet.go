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

package endpoints

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func (e *Endpoints) querySnippetYml(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrGetSnippetYaml.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		return apierrors.ErrGetSnippetYaml.AccessDenied().ToResp(), nil
	}

	var req apistructs.SnippetConfig
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(err).ToResp(), nil
	}

	ymlPath := req.Labels[apistructs.LabelGittarYmlPath]
	if ymlPath == "" {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(fmt.Errorf("labels key %v value is empty", apistructs.LabelGittarYmlPath)).ToResp(), nil
	}

	// get apps by project label
	projectIDString := req.Labels[apistructs.LabelProjectID]
	projectID, err := strconv.Atoi(projectIDString)
	if err != nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(err).ToResp(), nil
	}
	project, err := e.bdl.GetProject(uint64(projectID))
	if err != nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(err).ToResp(), nil
	}
	if project == nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(fmt.Errorf("not find project: ID %v", projectIDString)).ToResp(), nil
	}

	appName := getAppNameFromYmlPath(ymlPath)
	apps, err := e.bdl.GetAppsByProjectAndAppName(project.ID, project.OrgID, project.Creator, appName)
	if err != nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(err).ToResp(), nil
	}
	var matchApp apistructs.ApplicationDTO
	for _, app := range apps.List {
		if app.Name == appName {
			matchApp = app
			break
		}
	}
	appID := matchApp.ID

	branch := getBranchFromYmlPath(ymlPath, req.Name)

	if appID <= 0 {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(errors.New("labels key appID value is empty")).ToResp(), nil
	}

	if branch == "" {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(errors.New("labels key branch value is empty")).ToResp(), nil
	}

	app, err := e.bdl.GetApp(appID)
	if err != nil {
		return nil, apierrors.ErrGetApp.InternalError(err)
	}

	commit, err := e.bdl.GetGittarCommit(app.GitRepoAbbrev, branch, identityInfo.UserID)
	if err != nil {
		return nil, apierrors.ErrGetGittarCommit.InternalError(err)
	}

	repoAbbr := app.GitRepo
	commitID := commit.ID

	if repoAbbr == "" {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(errors.New("repoAbbr value is empty")).ToResp(), nil
	}

	if commitID == "" {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(errors.New("commitID value is empty")).ToResp(), nil
	}

	if strings.HasPrefix(req.Name, "/") {
		req.Name = strings.TrimPrefix(req.Name, "/")
	}

	yml, err := e.bdl.GetGittarFile(repoAbbr, commitID, req.Name, "", "", identityInfo.UserID)
	if err != nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(err).ToResp(), nil
	}

	pipelineYml, err := pipelineyml.New([]byte(yml))
	if err != nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(err).ToResp(), nil
	}

	workspace, err := e.fileTree.GetWorkspaceByBranch(strconv.Itoa(int(app.ProjectID)), branch)
	if err != nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(err).ToResp(), nil
	}

	pipelineYml.Spec().LoopStagesActions(func(stage int, action *pipelineyml.Action) {
		// git-checkout 设置下 params
		if action.Type == "git-checkout" {
			if action.Params == nil {
				action.Params = map[string]interface{}{}
			}

			url := action.Params["uri"]
			paramsBranch := action.Params["branch"]

			if url == nil || url == "((gittar.repo))" {
				action.Params["uri"] = repoAbbr
			}
			if paramsBranch == nil || paramsBranch == "((gittar.branch))" {
				action.Params["branch"] = branch
			}
		}
		// 设置上额外的 env 给 pipeline 设置到 task 的 evn 上
		actionEnv := map[string]string{}
		actionEnv[apistructs.DiceApplicationId] = strconv.Itoa(int(appID))
		actionEnv[apistructs.DiceApplicationName] = app.Name
		actionEnv[apistructs.DiceWorkspaceEnv] = workspace
		if action.Type == "release" || action.Type == "dice" {
			actionEnv[apistructs.GittarBranchEnv] = branch
		}
		actionEnvByte, err := json.Marshal(actionEnv)
		if err != nil {
			return
		}

		// json格式化后端 env 设置到 snippetConfig 的一个标签上
		action.SnippetConfig = &pipelineyml.SnippetConfig{
			Labels: map[string]string{
				apistructs.LabelActionEnv: string(actionEnvByte),
			},
		}
	})
	spec, err := pipelineyml.GenerateYml(pipelineYml.Spec())
	if err != nil {
		return apierrors.ErrGetSnippetYaml.InvalidParameter(err).ToResp(), nil
	}

	return httpserver.OkResp(string(spec))
}

func getAppNameFromYmlPath(ymlPath string) string {
	ymlPath = strings.TrimPrefix(ymlPath, "/")
	return strings.SplitN(ymlPath, "/", 2)[0]
}

func getBranchFromYmlPath(ymlPath string, name string) string {
	ymlPath = strings.TrimPrefix(ymlPath, "/")
	ymlPath = strings.Replace(ymlPath, name, "", 1)
	splits := strings.Split(ymlPath, "/")
	var branch string
	for i := 2; i < len(splits); i++ {
		branch += splits[i] + "/"
	}
	return branch[:len(branch)-1]
}
