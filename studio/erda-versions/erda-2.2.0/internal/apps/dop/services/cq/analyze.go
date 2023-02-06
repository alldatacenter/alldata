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

package cq

import (
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/pkg/strutil"
)

type Language string

var (
	LanguageGo   Language = "go"
	LanguageJava Language = "java"
	LanguageJs   Language = "javascript"
)

type MRCQRequest struct {
	MRInfo apistructs.MergeRequestInfo
}

type CQRequest struct {
	AppID    uint64
	Commit   string
	Language Language
}

// Analyze trigger a pipeline to analyze code quality, return pipelineID and error.
func (cq *CQ) Analyze(req CQRequest, userID string) (uint64, error) {
	if req.Language == "" {
		return 0, fmt.Errorf("no language specified")
	}
	switch Language(req.Language) {
	case LanguageGo:
		return cq.GenerateCQPipeline4Go(req, userID)
	case "":
		logrus.Warnf("no language specified, skip analyze")
	default:
		logrus.Warnf("unknown language: %s, skip analyze", req.Language)
	}
	return 0, nil
}

// GenerateCQPipeline4Go 构造用于 Go 项目代码质量分析的流水线
func (cq *CQ) GenerateCQPipeline4Go(req CQRequest, userID string) (uint64, error) {
	// get clusterName
	app, _, _, _, clusterName, err := cq.getWorkspaceClusterByAppBranch(req.AppID, req.Commit)
	if err != nil {
		return 0, err
	}

	labels := make(map[string]string)
	commitInfo, err := cq.bdl.GetGittarCommit(app.GitRepoAbbrev, req.Commit, userID)
	if err != nil {
		return 0, err
	}
	commitDetail := apistructs.CommitDetail{
		CommitID: commitInfo.ID,
		Repo:     app.GitRepo,
		RepoAbbr: app.GitRepoAbbrev,
		Author:   commitInfo.Committer.Name,
		Email:    commitInfo.Committer.Email,
		Time:     &commitInfo.Committer.When,
		Comment:  commitInfo.CommitMessage,
	}
	commitByte, err := json.Marshal(&commitDetail)
	if err != nil {
		return 0, err
	}
	labels[apistructs.LabelCommitDetail] = string(commitByte)

	// generate pipelineyml
	pipeline := apistructs.PipelineYml{
		Version: "1.1",
		Stages: [][]*apistructs.PipelineYmlAction{
			{
				generateGitCheckoutAction("repo", "((gittar.repo))", req.Commit, "", "", 1),
			},
			{
				generateGolangCILintAction("golangci-lint", "${repo}", "terminus.io/dice/dice"),
			},
		},
	}
	pipelineYmlByte, err := yaml.Marshal(pipeline)
	if err != nil {
		return 0, err
	}

	result, err := cq.bdl.CreatePipeline(&apistructs.PipelineCreateRequestV2{
		PipelineYml:     string(pipelineYmlByte),
		ClusterName:     clusterName,
		PipelineYmlName: generateCQPipelineName(req.AppID, req.Commit),
		PipelineSource:  apistructs.PipelineSourceQA,
		Labels:          labels,
		ForceRun:        false,
		AutoRunAtOnce:   true,
		IdentityInfo: apistructs.IdentityInfo{
			InternalClient: "QA-MR-CQ-Robot",
		},
	})
	if err != nil {
		return 0, err
	}
	return result.ID, nil
}

func (cq *CQ) getWorkspaceClusterByAppBranch(appID uint64, gitRef string) (
	app *apistructs.ApplicationDTO,
	project *apistructs.ProjectDTO,
	branchRule *apistructs.ValidBranch,
	workspace apistructs.DiceWorkspace,
	clusterName string,
	err error,
) {
	// app
	app, err = cq.bdl.GetApp(appID)
	if err != nil {
		return
	}
	// get project branch rule for workspace
	rules, err := cq.branchRuleSvc.Query(apistructs.ProjectScope, int64(app.ProjectID))
	if err != nil {
		return
	}

	branchRule = diceworkspace.GetValidBranchByGitReference(gitRef, rules)
	workspace = apistructs.DiceWorkspace(branchRule.Workspace)
	// get clusterName from project
	project, err = cq.bdl.GetProject(app.ProjectID)
	if err != nil {
		return
	}
	for _ws, _clusterName := range project.ClusterConfig {
		if strutil.Equal(_ws, workspace.String(), true) {
			clusterName = _clusterName
			break
		}
	}
	return
}

// generateCQPipelineName 每个代码仓库下一个 commit 只有一个在运行
func generateCQPipelineName(appID uint64, commit string) string {
	return fmt.Sprintf("qa-cq-appID-%d-commit-%s", appID, commit)
}

func generateGitCheckoutAction(alias string, repoURL, branch, user, pass string, depth int) *apistructs.PipelineYmlAction {
	g := apistructs.PipelineYmlAction{
		Alias: alias,
		Type:  "git-checkout",
		Params: map[string]interface{}{
			"uri":      repoURL,
			"branch":   branch,
			"username": user,
			"password": pass,
			"depth":    depth,
		},
	}
	return &g
}

func generateGolangCILintAction(alias string, codeDir string, goPkg string) *apistructs.PipelineYmlAction {
	l := apistructs.PipelineYmlAction{
		Alias: alias,
		Type:  "custom-script",
		Image: conf.GolangCILintImage(),
		Commands: []string{
			fmt.Sprintf(`d="${GOPATH}/src/%s"`, goPkg),
			`mkdir -p "${d}"`,
			fmt.Sprintf("cp -a %s ${d}", codeDir),
			`ln -sv "${repo}" "${d}/dice"`,
			`cd "${d}"`,
			`golangci-lint run -v --timeout=20m`,
		},
	}
	return &l
}
