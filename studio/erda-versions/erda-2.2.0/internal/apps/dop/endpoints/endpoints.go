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

// Package endpoints 定义所有的 route handle.
package endpoints

import (
	"context"
	"fmt"
	"net/http"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
	"github.com/gorilla/schema"
	"github.com/sirupsen/logrus"

	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	dpb "github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	sourcepb "github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	dwfpb "github.com/erda-project/erda-proto-go/dop/devflowrule/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/event"
	"github.com/erda-project/erda/internal/apps/dop/services/apidocsvc"
	"github.com/erda-project/erda/internal/apps/dop/services/appcertificate"
	"github.com/erda-project/erda/internal/apps/dop/services/application"
	"github.com/erda-project/erda/internal/apps/dop/services/assetsvc"
	"github.com/erda-project/erda/internal/apps/dop/services/autotest"
	atv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
	"github.com/erda-project/erda/internal/apps/dop/services/cdp"
	"github.com/erda-project/erda/internal/apps/dop/services/certificate"
	"github.com/erda-project/erda/internal/apps/dop/services/code_coverage"
	"github.com/erda-project/erda/internal/apps/dop/services/comment"
	"github.com/erda-project/erda/internal/apps/dop/services/cq"
	"github.com/erda-project/erda/internal/apps/dop/services/environment"
	"github.com/erda-project/erda/internal/apps/dop/services/filetree"
	"github.com/erda-project/erda/internal/apps/dop/services/issue"
	"github.com/erda-project/erda/internal/apps/dop/services/issuestate"
	"github.com/erda-project/erda/internal/apps/dop/services/iteration"
	"github.com/erda-project/erda/internal/apps/dop/services/libreference"
	"github.com/erda-project/erda/internal/apps/dop/services/migrate"
	"github.com/erda-project/erda/internal/apps/dop/services/namespace"
	"github.com/erda-project/erda/internal/apps/dop/services/org"
	"github.com/erda-project/erda/internal/apps/dop/services/permission"
	"github.com/erda-project/erda/internal/apps/dop/services/pipeline"
	"github.com/erda-project/erda/internal/apps/dop/services/project"
	"github.com/erda-project/erda/internal/apps/dop/services/projectpipelinefiletree"
	"github.com/erda-project/erda/internal/apps/dop/services/publish_item"
	"github.com/erda-project/erda/internal/apps/dop/services/publisher"
	"github.com/erda-project/erda/internal/apps/dop/services/sceneset"
	"github.com/erda-project/erda/internal/apps/dop/services/sonar_metric_rule"
	"github.com/erda-project/erda/internal/apps/dop/services/test_report"
	"github.com/erda-project/erda/internal/apps/dop/services/testcase"
	mttestplan "github.com/erda-project/erda/internal/apps/dop/services/testplan"
	"github.com/erda-project/erda/internal/apps/dop/services/testset"
	"github.com/erda-project/erda/internal/apps/dop/services/ticket"
	"github.com/erda-project/erda/internal/apps/dop/services/workbench"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

const (
	// ReleaseCallbackPath ReleaseCallback的路径
	ReleaseCallbackPath     = "/api/actions/release-callback"
	CDPCallbackPath         = "/api/actions/cdp-callback"
	GitCreateMrCallback     = "/api/actions/git-mr-create-callback"
	GitMergeMrCallback      = "/api/actions/git-mr-merge-callback"
	GitCloseMrCallback      = "/api/actions/git-mr-close-callback"
	GitCommentMrCallback    = "/api/actions/git-mr-comment-callback"
	GitDeleteBranchCallback = "/api/actions/git-branch-delete-callback"
	GitDeleteTagCallback    = "/api/actions/git-tag-delete-callback"
	IssueCallback           = "/api/actions/issue-callback"
	MrCheckRunCallback      = "/api/actions/check-run-callback"
)

type EventCallback struct {
	Name   string
	Path   string
	Events []string
}

var eventCallbacks = []EventCallback{
	{Name: "git_push_release", Path: ReleaseCallbackPath, Events: []string{"git_push"}},
	{Name: "cdp_pipeline", Path: CDPCallbackPath, Events: []string{"pipeline"}},
	{Name: "git_create_mr", Path: GitCreateMrCallback, Events: []string{"git_create_mr"}},
	{Name: "git_merge_mr", Path: GitMergeMrCallback, Events: []string{"git_merge_mr"}},
	{Name: "git_close_mr", Path: GitCloseMrCallback, Events: []string{"git_close_mr"}},
	{Name: "git_comment_mr", Path: GitCommentMrCallback, Events: []string{"git_comment_mr"}},
	{Name: "git_delete_branch", Path: GitDeleteBranchCallback, Events: []string{"git_delete_branch"}},
	{Name: "git_delete_tag", Path: GitDeleteTagCallback, Events: []string{"git_delete_tag"}},
	{Name: "issue", Path: IssueCallback, Events: []string{"issue"}},
	{Name: "check-run", Path: MrCheckRunCallback, Events: []string{"check-run"}},
	{Name: "qa_git_mr_create", Path: "/api/callbacks/git-mr-create", Events: []string{"git_create_mr"}},
}

// Routes 返回 endpoints 的所有 endpoint 方法，也就是 route.
func (e *Endpoints) Routes() []httpserver.Endpoint {
	return []httpserver.Endpoint{
		{Path: "/_api/health", Method: http.MethodGet, Handler: e.Health},

		{Path: "/api/api-assets", Method: http.MethodPost, Handler: httpserver.Wrap(e.CreateAPIAsset, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets", Method: http.MethodGet, Handler: httpserver.Wrap(e.PagingAPIAssets, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}", Method: http.MethodGet, Handler: httpserver.Wrap(e.GetAPIAsset, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}", Method: http.MethodPut, Handler: httpserver.Wrap(e.UpdateAPIAsset, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}", Method: http.MethodDelete, Handler: httpserver.Wrap(e.DeleteAPIAsset, httpserver.WithI18nCodes)},

		{Path: "/api/api-assets/{assetID}/api-gateways", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListAPIGateways, httpserver.WithI18nCodes)},
		{Path: "/api/api-gateways/{projectID}", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListProjectAPIGateways, httpserver.WithI18nCodes)},

		{Path: "/api/api-assets/{assetID}/versions", Method: http.MethodGet, Handler: httpserver.Wrap(e.PagingAPIAssetVersions, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/versions", Method: http.MethodPost, Handler: httpserver.Wrap(e.CreateAPIVersion, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/versions/{versionID}", Method: http.MethodGet, Handler: httpserver.Wrap(e.GetAPIAssetVersion, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/versions/{versionID}", Method: http.MethodPut, Handler: httpserver.Wrap(e.UpdateAssetVersion, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/versions/{versionID}", Method: http.MethodDelete,
			Handler: httpserver.Wrap(e.DeleteAPIAssetVersion, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/versions/{versionID}/export", Method: http.MethodGet, WriterHandler: e.DownloadSpecText},

		{Path: "/api/api-assets/{assetID}/swagger-versions", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListSwaggerVersions, httpserver.WithI18nCodes)},

		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/clients", Method: http.MethodGet,
			Handler: httpserver.Wrap(e.ListSwaggerClient, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/attempt-test", Method: http.MethodPost,
			Handler: httpserver.Wrap(e.ExecuteAttemptTest, httpserver.WithI18nCodes)},

		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/minors/{minor}/instantiations", Method: http.MethodPost,
			Handler: httpserver.Wrap(e.CreateInstantiation, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/minors/{minor}/instantiations", Method: http.MethodGet,
			Handler: httpserver.Wrap(e.GetInstantiations, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/minors/{minor}/instantiations/{instantiationID}", Method: http.MethodPut,
			Handler: httpserver.Wrap(e.UpdateInstantiation, httpserver.WithI18nCodes)},

		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/slas", Method: http.MethodGet,
			Handler: httpserver.Wrap(e.ListSLAs, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/slas", Method: http.MethodPost,
			Handler: httpserver.Wrap(e.CreateSLA, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/slas/{slaID}", Method: http.MethodGet,
			Handler: httpserver.Wrap(e.GetSLA, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/slas/{slaID}", Method: http.MethodDelete,
			Handler: httpserver.Wrap(e.DeleteSLA, httpserver.WithI18nCodes)},
		{Path: "/api/api-assets/{assetID}/swagger-versions/{swaggerVersion}/slas/{slaID}", Method: http.MethodPut,
			Handler: httpserver.Wrap(e.UpdateSLA, httpserver.WithI18nCodes)},

		{Path: "/api/api-clients", Method: http.MethodPost, Handler: httpserver.Wrap(e.CreateClient, httpserver.WithI18nCodes)},
		{Path: "/api/api-clients", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListMyClients, httpserver.WithI18nCodes)},
		{Path: "/api/api-clients/{clientID}", Method: http.MethodGet, Handler: httpserver.Wrap(e.GetClient, httpserver.WithI18nCodes)},
		{Path: "/api/api-clients/{clientID}", Method: http.MethodPut, Handler: httpserver.Wrap(e.UpdateClient, httpserver.WithI18nCodes)},
		{Path: "/api/api-clients/{clientID}", Method: http.MethodDelete, Handler: httpserver.Wrap(e.DeleteClient, httpserver.WithI18nCodes)},

		{Path: "/api/api-clients/{clientID}/contracts", Method: http.MethodPost, Handler: httpserver.Wrap(e.CreateContract, httpserver.WithI18nCodes)},
		{Path: "/api/api-clients/{clientID}/contracts", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListContract, httpserver.WithI18nCodes)},
		{Path: "/api/api-clients/{clientID}/contracts/{contractID}", Method: http.MethodGet, Handler: httpserver.Wrap(e.GetContract, httpserver.WithI18nCodes)},
		{Path: "/api/api-clients/{clientID}/contracts/{contractID}", Method: http.MethodPut, Handler: httpserver.Wrap(e.UpdateContract, httpserver.WithI18nCodes)},
		{Path: "/api/api-clients/{clientID}/contracts/{contractID}", Method: http.MethodDelete, Handler: httpserver.Wrap(e.DeleteContract, httpserver.WithI18nCodes)},

		{Path: "/api/api-clients/{clientID}/contracts/{contractID}/operation-records", Method: http.MethodGet,
			Handler: httpserver.Wrap(e.ListContractRecords, httpserver.WithI18nCodes)},

		{Path: "/api/api-access", Method: http.MethodPost, Handler: httpserver.Wrap(e.CreateAccess, httpserver.WithI18nCodes)},
		{Path: "/api/api-access", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListAccess, httpserver.WithI18nCodes)},
		{Path: "/api/api-access/{accessID}", Method: http.MethodGet, Handler: httpserver.Wrap(e.GetAccess, httpserver.WithI18nCodes)},
		{Path: "/api/api-access/{accessID}", Method: http.MethodPut, Handler: httpserver.Wrap(e.UpdateAccess, httpserver.WithI18nCodes)},
		{Path: "/api/api-access/{accessID}", Method: http.MethodDelete, Handler: httpserver.Wrap(e.DeleteAccess, httpserver.WithI18nCodes)},

		{Path: "/api/api-app-services/{appID}", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListRuntimeServices, httpserver.WithI18nCodes)},

		{Path: "/api/apim-ws/api-docs/filetree/{inode}", Method: http.MethodGet, WriterHandler: e.APIDocWebsocket},
		{Path: "/api/apim/{treeName}/filetree", Method: http.MethodPost, Handler: httpserver.Wrap(e.CreateNode, httpserver.WithI18nCodes)},
		{Path: "/api/apim/{treeName}/filetree", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListChildrenNodes, httpserver.WithI18nCodes)},
		{Path: "/api/apim/{treeName}/filetree/{inode}", Method: http.MethodDelete, Handler: httpserver.Wrap(e.DeleteNode, httpserver.WithI18nCodes)},
		{Path: "/api/apim/{treeName}/filetree/{inode}", Method: http.MethodPut, Handler: httpserver.Wrap(e.UpdateNode, httpserver.WithI18nCodes)},
		{Path: "/api/apim/{treeName}/filetree/{inode}", Method: http.MethodGet, Handler: httpserver.Wrap(e.GetNodeDetail, httpserver.WithI18nCodes)},
		{Path: "/api/apim/{treeName}/filetree/{inode}/actions/{action}", Method: http.MethodPost, Handler: httpserver.Wrap(e.MvCpNode, httpserver.WithI18nCodes)},

		{Path: "/api/apim/operations", Method: http.MethodGet, Handler: httpserver.Wrap(e.SearchOperations, httpserver.WithI18nCodes)},
		{Path: "/api/apim/operations/{id}", Method: http.MethodGet, Handler: httpserver.Wrap(e.GetOperation, httpserver.WithI18nCodes)},

		{Path: "/api/apim/validate-swagger", Method: http.MethodPost, Handler: httpserver.Wrap(e.ValidateSwagger, httpserver.WithI18nCodes)},

		// gittar 事件回调
		{Path: ReleaseCallbackPath, Method: http.MethodPost, Handler: e.ReleaseCallback},
		{Path: MrCheckRunCallback, Method: http.MethodPost, Handler: e.checkrunCreate},

		// cdp 事件回调
		{Path: CDPCallbackPath, Method: http.MethodPost, Handler: e.CDPCallback},
		{Path: GitCreateMrCallback, Method: http.MethodPost, Handler: e.RepoMrEventCallback},
		{Path: GitMergeMrCallback, Method: http.MethodPost, Handler: e.RepoMrEventCallback},
		{Path: GitCloseMrCallback, Method: http.MethodPost, Handler: e.RepoMrEventCallback},
		{Path: GitCommentMrCallback, Method: http.MethodPost, Handler: e.RepoMrEventCallback},
		{Path: GitDeleteBranchCallback, Method: http.MethodPost, Handler: e.RepoBranchEventCallback},
		{Path: GitDeleteTagCallback, Method: http.MethodPost, Handler: e.RepoTagEventCallback},

		{Path: IssueCallback, Method: http.MethodPost, Handler: e.IssueCallback},

		// cicd
		{Path: "/api/cicd/{pipelineID}/tasks/{taskID}/logs", Method: http.MethodGet, Handler: e.CICDTaskLog},
		{Path: "/api/cicd/{pipelineID}/tasks/{taskID}/logs/actions/download", Method: http.MethodGet, ReverseHandler: e.ProxyCICDTaskLogDownload},

		// pipeline
		{Path: "/api/cicds", Method: http.MethodPost, Handler: e.pipelineCreate},
		{Path: "/api/cicds", Method: http.MethodGet, Handler: e.pipelineList},
		{Path: "/api/cicds/actions/pipeline-detail", Method: http.MethodGet, Handler: e.pipelineDetail},
		{Path: "/api/cicds/actions/pipelineYmls", Method: http.MethodGet, Handler: e.pipelineYmlList},
		{Path: "/api/cicds/actions/app-invoked-combos", Method: http.MethodGet, Handler: e.pipelineAppInvokedCombos},
		{Path: "/api/cicds/actions/fetch-pipeline-id", Method: http.MethodGet, Handler: e.fetchPipelineByAppInfo},
		{Path: "/api/cicds/actions/app-all-valid-branch-workspaces", Method: http.MethodGet, Handler: e.branchWorkspaceMap},
		{Path: "/api/cicds/{pipelineID}/actions/run", Method: http.MethodPost, Handler: e.pipelineRun},
		{Path: "/api/cicds/{pipelineID}/actions/cancel", Method: http.MethodPost, Handler: e.pipelineCancel},
		{Path: "/api/cicds/{pipelineID}/actions/rerun", Method: http.MethodPost, Handler: e.pipelineRerun},
		{Path: "/api/cicds/{pipelineID}/actions/rerun-failed", Method: http.MethodPost, Handler: e.pipelineRerunFailed},
		{Path: "/api/cicds/{pipelineID}", Method: http.MethodPut, Handler: e.pipelineOperate},

		{Path: "/api/cicds/{pipelineID}/actions/get-branch-rule", Method: http.MethodGet, Handler: e.pipelineGetBranchRule},

		// pipeline cron
		{Path: "/api/cicd-crons", Method: http.MethodGet, Handler: e.pipelineCronPaging},
		{Path: "/api/cicd-crons/{cronID}/actions/start", Method: http.MethodPut, Handler: e.pipelineCronStart},
		{Path: "/api/cicd-crons/{cronID}/actions/stop", Method: http.MethodPut, Handler: e.pipelineCronStop},
		{Path: "/api/cicd-crons", Method: http.MethodPost, Handler: e.pipelineCronCreate},
		// eventBox call back only support post method
		{Path: "/api/cicd-crons/actions/hook-for-update", Method: http.MethodPost, Handler: e.pipelineCronUpdate},
		{Path: "/api/cicd-crons/{cronID}", Method: http.MethodDelete, Handler: e.pipelineCronDelete},

		// pipeline_definitions
		{Path: "/api/cicd-pipelines/actions/hook-for-definition-update", Method: http.MethodPost, Handler: e.gittarPipelineDefinitionUpdate},

		// project pipeline
		{Path: "/api/cicds-project", Method: http.MethodPost, Handler: e.projectPipelineCreate},
		{Path: "/api/cicds-project/actions/pipeline-detail", Method: http.MethodGet, Handler: e.projectPipelineDetail},

		// cms
		{Path: "/api/cicds/multinamespace/configs", Method: http.MethodPost, Handler: e.getCmsNsConfigs},
		{Path: "/api/cicds/actions/fetch-config-namespaces", Method: http.MethodGet, Handler: e.getConfigNamespaces},
		{Path: "/api/cicds/actions/list-workspaces", Method: http.MethodGet, Handler: e.listConfigWorkspaces},

		{Path: "/api/cicd-pipeline/filetree/{inode}/actions/find-ancestors", Method: http.MethodGet, Handler: e.FindGittarFileTreeNodeAncestors},
		{Path: "/api/cicd-pipeline/filetree/actions/get-inode-by-pipeline", Method: http.MethodGet, Handler: e.GetGittarFileByPipelineId},
		{Path: "/api/cicd-pipeline/filetree", Method: http.MethodPost, Handler: e.CreateGittarFileTreeNode},
		{Path: "/api/cicd-pipeline/filetree/{inode}", Method: http.MethodDelete, Handler: e.DeleteGittarFileTreeNode},
		{Path: "/api/cicd-pipeline/filetree", Method: http.MethodGet, Handler: e.ListGittarFileTreeNodes},
		{Path: "/api/cicd-pipeline/filetree/{inode}", Method: http.MethodGet, Handler: e.GetGittarFileTreeNode},
		{Path: "/api/cicd-pipeline/filetree/actions/fuzzy-search", Method: http.MethodGet, Handler: e.FuzzySearchGittarFileTreeNodes},

		// gittar
		{Path: "/callback/gittar", Method: http.MethodPost, Handler: e.GittarWebHookCallback},
		{Path: "/api/callbacks/git-mr-create", Method: http.MethodPost, Handler: e.GittarMRCreateCallback},

		// sonar issues.
		{Path: "/api/qa/actions/sonar-results-store", Method: http.MethodPost, Handler: e.SonarIssuesStore},
		{Path: "/api/qa", Method: http.MethodGet, Handler: e.SonarIssues},

		// sonar metric key
		{Path: "/api/sonar-metric-rules", Method: http.MethodGet, Handler: e.PagingSonarMetricRules},
		{Path: "/api/sonar-metric-rules/{id}", Method: http.MethodGet, Handler: e.GetSonarMetricRules},
		{Path: "/api/sonar-metric-rules/{id}", Method: http.MethodPut, Handler: e.UpdateSonarMetricRules},
		{Path: "/api/sonar-metric-rules/actions/batch-insert", Method: http.MethodPost, Handler: e.BatchInsertSonarMetricRules},
		{Path: "/api/sonar-metric-rules/actions/batch-delete", Method: http.MethodDelete, Handler: e.BatchDeleteSonarMetricRules},
		{Path: "/api/sonar-metric-rules/{id}", Method: http.MethodDelete, Handler: e.DeleteSonarMetricRules},
		{Path: "/api/sonar-metric-rules/actions/query-metric-definition", Method: http.MethodGet, Handler: e.QuerySonarMetricRulesDefinition},
		{Path: "/api/sonar-metric-rules/actions/query-list", Method: http.MethodGet, Handler: e.QuerySonarMetricRules},

		// test platform
		{Path: "/api/qa/actions/get-sonar-credential", Method: http.MethodGet, Handler: e.GetSonarCredential},

		// pmp api test
		{Path: "/api/apitests", Method: http.MethodPost, Handler: e.CreateAPITest},
		{Path: "/api/apitests/{id}", Method: http.MethodPut, Handler: e.UpdateApiTest},
		{Path: "/api/apitests/{id}", Method: http.MethodGet, Handler: e.GetApiTests},
		{Path: "/api/apitests/{id}", Method: http.MethodDelete, Handler: e.DeleteApiTestsByApiID},
		{Path: "/api/apitests/actions/list-apis", Method: http.MethodGet, Handler: e.ListApiTests},

		// api test env
		{Path: "/api/testenv", Method: http.MethodPost, Handler: e.CreateAPITestEnv},
		{Path: "/api/testenv/{id}", Method: http.MethodPut, Handler: e.UpdateAPITestEnv},
		{Path: "/api/testenv/{id}", Method: http.MethodGet, Handler: e.GetAPITestEnv},
		{Path: "/api/testenv/{id}", Method: http.MethodDelete, Handler: e.DeleteAPITestEnvByEnvID},
		{Path: "/api/testenv/actions/list-envs", Method: http.MethodGet, Handler: e.ListAPITestEnvs},

		{Path: "/api/apitests/actions/execute-tests", Method: http.MethodPost, Handler: e.ExecuteApiTests},
		{Path: "/api/apitests/actions/cancel-testplan", Method: http.MethodPost, Handler: e.CancelApiTests},
		{Path: "/api/apitests/actions/attempt-test", Method: http.MethodPost, Handler: e.ExecuteManualTestAPI},
		{Path: "/api/apitests/actions/statistic-results", Method: http.MethodPost, Handler: e.StatisticResults},
		{Path: "/api/apitests/pipeline/{pipelineID}", Method: http.MethodGet, Handler: e.GetPipelineDetail},
		{Path: "/api/apitests/pipeline/{pipelineID}/task/{taskID}/logs", Method: http.MethodGet, Handler: e.GetPipelineTaskLogs},

		// 测试用例
		{Path: "/api/testcases", Method: http.MethodPost, Handler: e.CreateTestCase},
		{Path: "/api/testcases/{testCaseID}", Method: http.MethodGet, Handler: e.GetTestCase},
		{Path: "/api/testcases/actions/batch-create", Method: http.MethodPost, Handler: e.BatchCreateTestCases},
		{Path: "/api/testcases", Method: http.MethodGet, Handler: e.PagingTestCases},
		{Path: "/api/testcases/{testCaseID}", Method: http.MethodPut, Handler: e.UpdateTestCase},
		{Path: "/api/testcases/actions/batch-update", Method: http.MethodPost, Handler: e.BatchUpdateTestCases},
		{Path: "/api/testcases/actions/batch-copy", Method: http.MethodPost, Handler: e.BatchCopyTestCases},
		{Path: "/api/testcases/actions/batch-clean-from-recycle-bin", Method: http.MethodDelete, Handler: e.BatchCleanTestCasesFromRecycleBin},
		{Path: "/api/testcases/actions/export", Method: http.MethodGet, Handler: e.ExportTestCases},
		{Path: "/api/testcases/actions/import", Method: http.MethodPost, Handler: e.ImportTestCases},

		// 测试集 管理
		{Path: "/api/testsets", Method: http.MethodPost, Handler: e.CreateTestSet},
		{Path: "/api/testsets", Method: http.MethodGet, Handler: e.ListTestSets},
		{Path: "/api/testsets/{testSetID}", Method: http.MethodGet, Handler: e.GetTestSet},
		{Path: "/api/testsets/{testSetID}", Method: http.MethodPut, Handler: e.UpdateTestSet},
		{Path: "/api/testsets/{testSetID}/actions/copy", Method: http.MethodPost, Handler: e.CopyTestSet},
		{Path: "/api/testsets/{testSetID}/actions/recycle", Method: http.MethodPost, Handler: e.RecycleTestSet},
		{Path: "/api/testsets/{testSetID}/actions/clean-from-recycle-bin", Method: http.MethodDelete, Handler: e.CleanTestSetFromRecycleBin},
		{Path: "/api/testsets/{testSetID}/actions/recover-from-recycle-bin", Method: http.MethodPost, Handler: e.RecoverTestSetFromRecycleBin},

		// 测试计划
		{Path: "/api/testplans", Method: http.MethodPost, Handler: e.CreateTestPlan},
		{Path: "/api/testplans", Method: http.MethodGet, Handler: e.PagingTestPlans},
		{Path: "/api/testplans/{testPlanID}", Method: http.MethodGet, Handler: e.GetTestPlan},
		{Path: "/api/testplans/{testPlanID}", Method: http.MethodPut, Handler: e.UpdateTestPlan},
		{Path: "/api/testplans/{testPlanID}", Method: http.MethodDelete, Handler: e.DeleteTestPlan},
		{Path: "/api/testplans/{testPlanID}/testcase-relations", Method: http.MethodPost, Handler: e.CreateTestPlanCaseRelations},
		{Path: "/api/testplans/{testPlanID}/testcase-relations", Method: http.MethodGet, Handler: e.PagingTestPlanCaseRelations},
		{Path: "/api/testplans/testcase-relations/actions/internal-list", Method: http.MethodGet, Handler: e.InternalListTestPlanCaseRels},
		{Path: "/api/testplans/{testPlanID}/testcase-relations/{relationID}", Method: http.MethodGet, Handler: e.GetTestPlanCaseRel},
		{Path: "/api/testplans/{testPlanID}/testcase-relations/{relationID}/actions/add-issue-relations", Method: http.MethodPost, Handler: e.AddTestPlanCaseRelIssueRelations},
		{Path: "/api/testplans/{testPlanID}/testcase-relations/{relationID}/actions/remove-issue-relations", Method: http.MethodPost, Handler: e.RemoveTestPlanCaseRelIssueRelations},
		{Path: "/api/testplans/testcase-relations/actions/internal-remove-issue-relations", Method: http.MethodDelete, Handler: e.InternalRemoveTestPlanCaseRelIssueRelations},
		{Path: "/api/testplans/{testPlanID}/testcase-relations/actions/batch-update", Method: http.MethodPost, Handler: e.BatchUpdateTestPlanCaseRelations},
		{Path: "/api/testplans/{testPlanID}/actions/execute-apitest", Method: http.MethodPost, Handler: e.ExecuteTestPlanAPITest},
		{Path: "/api/testplans/{testPlanID}/actions/cancel-apitest/{pipelineID}", Method: http.MethodPost, Handler: e.CancelApiTestPipeline},
		{Path: "/api/testplans/{testPlanID}/actions/export", Method: http.MethodGet, WriterHandler: e.ExportTestPlanCaseRels},
		{Path: "/api/testplans/{testPlanID}/testsets", Method: http.MethodGet, Handler: e.ListTestPlanTestSets},
		{Path: "/api/testplans/{testPlanID}/actions/generate-report", Method: http.MethodGet, Handler: e.GenerateTestPlanReport},

		// 自动化测试 - 测试集
		{Path: "/api/autotests/filetree", Method: http.MethodPost, Handler: e.CreateAutoTestFileTreeNode},
		{Path: "/api/autotests/filetree", Method: http.MethodGet, Handler: e.ListAutoTestFileTreeNodes},
		{Path: "/api/autotests/filetree/actions/fuzzy-search", Method: http.MethodGet, Handler: e.FuzzySearchAutoTestFileTreeNodes},
		{Path: "/api/autotests/filetree/{inode}", Method: http.MethodDelete, Handler: e.DeleteAutoTestFileTreeNode},
		{Path: "/api/autotests/filetree/{inode}", Method: http.MethodPut, Handler: e.UpdateAutoTestFileTreeNodeBasicInfo},
		{Path: "/api/autotests/filetree/{inode}", Method: http.MethodGet, Handler: e.GetAutoTestFileTreeNode},
		{Path: "/api/autotests/filetree/{inode}/actions/move", Method: http.MethodPost, Handler: e.MoveAutoTestFileTreeNode},
		{Path: "/api/autotests/filetree/{inode}/actions/copy", Method: http.MethodPost, Handler: e.CopyAutoTestFileTreeNode},
		{Path: "/api/autotests/filetree/{inode}/actions/find-ancestors", Method: http.MethodGet, Handler: e.FindAutoTestFileTreeNodeAncestors},
		{Path: "/api/autotests/filetree/{inode}/actions/save-pipeline", Method: http.MethodPost, Handler: e.SaveAutoTestFileTreeNodePipeline},
		{Path: "/api/autotests/filetree/{inode}/actions/get-histories", Method: http.MethodGet, Handler: e.ListAutoTestFileTreeNodeHistory},

		// autotest global-configs
		{Path: "/api/autotests/global-configs", Method: http.MethodPost, Handler: e.CreateAutoTestGlobalConfig},
		{Path: "/api/autotests/global-configs/{ns}", Method: http.MethodPut, Handler: e.UpdateAutoTestGlobalConfig},
		{Path: "/api/autotests/global-configs/{ns}", Method: http.MethodDelete, Handler: e.DeleteAutoTestGlobalConfig},
		{Path: "/api/autotests/global-configs", Method: http.MethodGet, Handler: e.ListAutoTestGlobalConfigs},

		// autotest snippet
		{Path: "/api/autotests/pipeline-snippets/actions/batch-query-snippet-yml", Method: http.MethodPost, Handler: e.BatchQueryPipelineSnippetYaml},
		{Path: "/api/autotests/pipeline-snippets/actions/query-snippet-yml", Method: http.MethodPost, Handler: e.QueryPipelineSnippetYaml},
		// gittar snippet
		{Path: "/api/pipeline-snippets/actions/query-snippet-yml", Method: http.MethodPost, Handler: e.querySnippetYml},

		// 自动化测试 - 测试空间
		{Path: "/api/autotests/spaces", Method: http.MethodPost, Handler: e.CreateAutoTestSpace},
		{Path: "/api/autotests/spaces", Method: http.MethodPut, Handler: e.UpdateAutoTestSpace},
		{Path: "/api/autotests/spaces", Method: http.MethodGet, Handler: e.GetAutoTestSpaceList},
		{Path: "/api/autotests/spaces/{id}", Method: http.MethodGet, Handler: e.GetAutoTestSpace},
		{Path: "/api/autotests/spaces/{id}", Method: http.MethodDelete, Handler: e.DeleteAutoTestSpace},
		{Path: "/api/autotests/spaces/actions/copy", Method: http.MethodPost, Handler: e.CopyAutoTestSpaceV2},
		{Path: "/api/autotests/spaces/actions/export", Method: http.MethodPost, Handler: e.ExportAutoTestSpace},
		{Path: "/api/autotests/spaces/actions/import", Method: http.MethodPost, Handler: e.ImportAutotestSpace},
		{Path: "/api/autotests/spaces/actions/stats", Method: http.MethodGet, Handler: e.AutotestSpaceStats},

		// 自动化测试 - 场景
		{Path: "/api/autotests/scenes", Method: http.MethodPost, Handler: e.CreateAutoTestScene},
		{Path: "/api/autotests/scenes/{sceneID}", Method: http.MethodPut, Handler: e.UpdateAutoTestScene},
		{Path: "/api/autotests/scenes/actions/move-scene", Method: http.MethodPut, Handler: e.MoveAutoTestScene},
		{Path: "/api/autotests/scenes/actions/move-scene-v2", Method: http.MethodPut, Handler: e.MoveAutoTestSceneV2},
		{Path: "/api/autotests/scenes", Method: http.MethodGet, Handler: e.ListAutoTestScene},
		{Path: "/api/autotests/scenes/modal", Method: http.MethodGet, Handler: e.ListAutoTestScenes},
		{Path: "/api/autotests/scenes/{sceneID}", Method: http.MethodGet, Handler: e.GetAutoTestScene},
		{Path: "/api/autotests/scenes/{sceneID}", Method: http.MethodDelete, Handler: e.DeleteAutoTestScene},
		{Path: "/api/autotests/scenes/actions/copy", Method: http.MethodPost, Handler: e.CopyAutoTestScene},

		// 自动化测试 - 入参
		{Path: "/api/autotests/scenes/{sceneID}/actions/add-input", Method: http.MethodPost, Handler: e.CreateAutoTestSceneInput},
		{Path: "/api/autotests/scenes/{sceneID}/actions/delete-input", Method: http.MethodDelete, Handler: e.DeleteAutoTestSceneInput},
		{Path: "/api/autotests/scenes/{sceneID}/actions/update-input", Method: http.MethodPut, Handler: e.UpdateAutoTestSceneInput},
		{Path: "/api/autotests/scenes/{sceneID}/actions/list-input", Method: http.MethodGet, Handler: e.ListAutoTestSceneInput},

		// 自动化测试 - 出参
		{Path: "/api/autotests/scenes/{sceneID}/actions/add-output", Method: http.MethodPost, Handler: e.CreateAutoTestSceneOutput},
		{Path: "/api/autotests/scenes/{sceneID}/actions/delete-output", Method: http.MethodDelete, Handler: e.DeleteAutoTestSceneOutput},
		{Path: "/api/autotests/scenes/{sceneID}/actions/update-output", Method: http.MethodPut, Handler: e.UpdateAutoTestSceneOutput},
		{Path: "/api/autotests/scenes/{sceneID}/actions/list-output", Method: http.MethodGet, Handler: e.ListAutoTestSceneOutput},

		// 自动化测试 - 步骤
		{Path: "/api/autotests/scenes/{sceneID}/actions/add-step", Method: http.MethodPost, Handler: e.CreateAutoTestSceneStep},
		{Path: "/api/autotests/scenes-step/{stepID}", Method: http.MethodDelete, Handler: e.DeleteAutoTestSceneStep},
		{Path: "/api/autotests/scenes-step/{stepID}", Method: http.MethodPut, Handler: e.UpdateAutoTestSceneStep},
		{Path: "/api/autotests/scenes-step/actions/move", Method: http.MethodPut, Handler: e.MoveAutoTestSceneStep},
		{Path: "/api/autotests/scenes/{sceneID}/actions/get-step", Method: http.MethodGet, Handler: e.ListAutoTestSceneStep},
		{Path: "/api/autotests/scenes-step-output", Method: http.MethodGet, Handler: e.ListAutoTestSceneStepOutPut},
		{Path: "/api/autotests/scenes-step/{stepID}", Method: http.MethodGet, Handler: e.GetAutoTestSceneStep},

		// 自动化测试 - 测试计划
		{Path: "/api/autotests/testplans", Method: http.MethodPost, Handler: e.CreateTestPlanV2},
		{Path: "/api/autotests/testplans/{testPlanID}", Method: http.MethodDelete, Handler: e.DeleteTestPlanV2},
		{Path: "/api/autotests/testplans/{testPlanID}", Method: http.MethodPut, Handler: e.UpdateTestPlanV2},
		{Path: "/api/autotests/testplans", Method: http.MethodGet, Handler: e.PagingTestPlansV2},
		{Path: "/api/autotests/testplans/{testPlanID}", Method: http.MethodGet, Handler: e.GetTestPlanV2},
		{Path: "/api/autotests/testplans/{testPlanID}/actions/add-step", Method: http.MethodPost, Handler: e.AddTestPlanV2Step},
		{Path: "/api/autotests/testplans/{testPlanID}/actions/delete-step", Method: http.MethodDelete, Handler: e.DeleteTestPlanV2Step},
		{Path: "/api/autotests/testplans/{testPlanID}/actions/move-step", Method: http.MethodPut, Handler: e.MoveTestPlanV2Step},
		{Path: "/api/autotests/testplans-step/{stepID}", Method: http.MethodGet, Handler: e.GetTestPlanV2Step},
		{Path: "/api/autotests/testplans-step/{stepID}", Method: http.MethodPut, Handler: e.UpdateTestPlanV2Step},
		{Path: "/api/autotests/testplans/{testPlanID}/steps/actions/list-by-group-id", Method: http.MethodGet, Handler: e.ListTestPlanV2Step},
		{Path: "/api/autotests/testplans/history/actions/by-pipeline-id", Method: http.MethodGet, Handler: e.GetAutoTestExecHistory},

		{Path: "/api/reportsets/{pipelineID}", Method: http.MethodGet, Handler: e.queryReportSets},

		// 场景 执行取消
		{Path: "/api/autotests/scenes-step/{stepID}/actions/execute", Method: http.MethodPost, Handler: e.ExecuteDiceAutotestSceneStep},
		{Path: "/api/autotests/scenes/{sceneID}/actions/execute", Method: http.MethodPost, Handler: e.ExecuteDiceAutotestScene},
		{Path: "/api/autotests/scenes/{sceneID}/actions/cancel", Method: http.MethodPost, Handler: e.CancelDiceAutotestScene},

		// 计划 执行取消
		{Path: "/api/autotests/testplans/{testPlanID}/actions/execute", Method: http.MethodPost, Handler: e.ExecuteDiceAutotestTestPlans},
		{Path: "/api/autotests/testplans/{testPlanID}/actions/cancel", Method: http.MethodPost, Handler: e.CancelDiceAutotestTestPlans},

		//场景集
		{Path: "/api/autotests/scenesets/{setID}", Method: http.MethodGet, Handler: e.GetSceneSet},
		{Path: "/api/autotests/scenesets", Method: http.MethodGet, Handler: e.GetSceneSets},
		{Path: "/api/autotests/scenesets", Method: http.MethodPost, Handler: e.CreateSceneSet},
		{Path: "/api/autotests/scenesets/{setID}", Method: http.MethodPut, Handler: e.UpdateSceneSet},
		{Path: "/api/autotests/scenesets/{setID}", Method: http.MethodDelete, Handler: e.DeleteSceneSet},
		{Path: "/api/autotests/scenesets/actions/drag", Method: http.MethodPut, Handler: e.DragSceneSet},
		{Path: "/api/autotests/scenesets/actions/copy", Method: http.MethodPost, Handler: e.CopySceneSet},
		{Path: "/api/autotests/scenesets/actions/export", Method: http.MethodPost, Handler: e.ExportAutotestSceneSet},
		{Path: "/api/autotests/scenesets/actions/import", Method: http.MethodPost, Handler: e.ImportAutotestSceneSet},

		// migrate
		{Path: "/api/autotests/actions/migrate-from-autotestv1", Method: http.MethodGet, Handler: e.MigrateFromAutoTestV1},

		// 工单相关
		{Path: "/api/tickets", Method: http.MethodPost, Handler: e.CreateTicket},
		{Path: "/api/tickets/{ticketID}", Method: http.MethodPut, Handler: e.UpdateTicket},
		{Path: "/api/tickets/{ticketID}/actions/close", Method: http.MethodPut, Handler: e.CloseTicket},
		{Path: "/api/tickets/actions/close-by-key", Method: http.MethodPut, Handler: e.CloseTicketByKey},
		{Path: "/api/tickets/{ticketID}/actions/reopen", Method: http.MethodPut, Handler: e.ReopenTicket},
		{Path: "/api/tickets/{ticketID}", Method: http.MethodGet, Handler: e.GetTicket},
		{Path: "/api/tickets", Method: http.MethodGet, Handler: e.ListTicket},
		{Path: "/api/tickets/actions/batch-delete", Method: http.MethodDelete, Handler: e.DeleteTicket},

		// 工单评论相关
		{Path: "/api/comments", Method: http.MethodPost, Handler: e.CreateComment},
		{Path: "/api/comments/{commentID}", Method: http.MethodPut, Handler: e.UpdateComment},
		{Path: "/api/comments", Method: http.MethodGet, Handler: e.ListComments},
		{Path: "/api/comments/{commentID}", Method: http.MethodDelete, Handler: e.DeleteComment},

		// 分支规则
		{Path: "/api/branch-rules", Method: http.MethodPost, Handler: e.CreateBranchRule},
		{Path: "/api/branch-rules", Method: http.MethodGet, Handler: e.QueryBranchRules},
		{Path: "/api/branch-rules/{id}", Method: http.MethodPut, Handler: e.UpdateBranchRule},
		{Path: "/api/branch-rules/{id}", Method: http.MethodDelete, Handler: e.DeleteBranchRule},
		{Path: "/api/branch-rules/actions/app-all-valid-branch-workspaces", Method: http.MethodGet, Handler: e.GetAllValidBranchWorkspaces},

		// 配置管理相关
		{Path: "/api/config/namespace", Method: http.MethodPost, Handler: e.CreateNamespace},
		{Path: "/api/config/namespace", Method: http.MethodDelete, Handler: e.DeleteNamespace},
		{Path: "/api/config/namespace/relation", Method: http.MethodPost, Handler: e.CreateNamespaceRelation},
		{Path: "/api/config/namespace/relation", Method: http.MethodDelete, Handler: e.DeleteNamespaceRelation},
		{Path: "/api/config", Method: http.MethodPost, Handler: e.AddConfigs},
		{Path: "/api/config", Method: http.MethodGet, Handler: e.GetConfigs},
		{Path: "/api/config", Method: http.MethodPut, Handler: e.UpdateConfigs},
		{Path: "/api/config", Method: http.MethodDelete, Handler: e.DeleteConfig},
		{Path: "/api/config/actions/export", Method: http.MethodGet, Handler: e.ExportConfigs},
		{Path: "/api/config/actions/import", Method: http.MethodPost, Handler: e.ImportConfigs},
		{Path: "/api/config/deployment", Method: http.MethodGet, Handler: e.GetDeployConfigs},
		//{"/api/configmanage/configs/publish",Method:http.MethodPost,Handler: e.PublishConfig},
		//{"/api/configmanage/configs/publish/all",Method:http.MethodPost,Handler: e.PublishConfigs},
		{Path: "/api/config/actions/list-multinamespace-configs", Method: http.MethodPost, Handler: e.GetMultiNamespaceConfigs},
		// 以前的dice_config_namespace表数据不全，里面很多name没有了，导致check ns exist时报错，用这个接口修复
		{Path: "/api/config/namespace/fix-namespace-data-err", Method: http.MethodGet, Handler: e.FixDataErr},

		{Path: "/api/iterations", Method: http.MethodPost, Handler: e.CreateIteration},
		{Path: "/api/iterations/{id}", Method: http.MethodPut, Handler: e.UpdateIteration},
		{Path: "/api/iterations/{id}", Method: http.MethodDelete, Handler: e.DeleteIteration},
		{Path: "/api/iterations/{id}", Method: http.MethodGet, Handler: e.GetIteration},
		{Path: "/api/iterations", Method: http.MethodGet, Handler: e.PagingIterations},

		{Path: "/api/publishers", Method: http.MethodPost, Handler: e.CreatePublisher},
		{Path: "/api/publishers", Method: http.MethodPut, Handler: e.UpdatePublisher},
		{Path: "/api/publishers/{publisherID}", Method: http.MethodGet, Handler: e.GetPublisher},
		{Path: "/api/publishers/{publisherID}", Method: http.MethodDelete, Handler: e.DeletePublisher},
		{Path: "/api/publishers", Method: http.MethodGet, Handler: e.ListPublishers},
		{Path: "/api/publishers/actions/list-my-publishers", Method: http.MethodGet, Handler: e.ListMyPublishers},
		{Path: "/api/my-publish-items", Method: http.MethodGet, Handler: e.QueryMyPublishItem},

		// Certificate
		{Path: "/api/certificates", Method: http.MethodPost, Handler: e.CreateCertificate},
		{Path: "/api/certificates/{certificateID}", Method: http.MethodPut, Handler: e.UpdateCertificate},
		{Path: "/api/certificates/{certificateID}", Method: http.MethodGet, Handler: e.GetCertificate},
		{Path: "/api/certificates/{certificateID}", Method: http.MethodDelete, Handler: e.DeleteCertificate},
		{Path: "/api/certificates/actions/list-certificates", Method: http.MethodGet, Handler: e.ListCertificates},
		// Application Certificate
		{Path: "/api/certificates/actions/application-quote", Method: http.MethodPost, Handler: e.QuoteCertificate},
		{Path: "/api/certificates/actions/application-cancel-quote", Method: http.MethodDelete, Handler: e.CancelQuoteCertificate},
		{Path: "/api/certificates/actions/list-application-quotes", Method: http.MethodGet, Handler: e.ListQuoteCertificates},
		// push certificate config
		{Path: "/api/certificates/actions/push-configs", Method: http.MethodPost, Handler: e.PushCertificateConfig},

		// user-workbench
		{Path: "/api/workbench/actions/list", Method: http.MethodGet, Handler: e.GetWorkbenchData},
		{Path: "/api/workbench/issues/actions/list", Method: http.MethodGet, Handler: e.GetIssuesForWorkbench},

		{Path: "/api/lib-references", Method: http.MethodPost, Handler: e.CreateLibReference},
		{Path: "/api/lib-references/{id}", Method: http.MethodDelete, Handler: e.DeleteLibReference},
		{Path: "/api/lib-references", Method: http.MethodGet, Handler: e.ListLibReference},
		{Path: "/api/lib-references/actions/fetch-versions", Method: http.MethodGet, Handler: e.ListLibReferenceVersion},

		// 流水线filetree查询
		{Path: "/api/project-pipeline/filetree/{inode}/actions/find-ancestors", Method: http.MethodGet, Handler: e.FindFileTreeNodeAncestors},
		{Path: "/api/project-pipeline/filetree", Method: http.MethodPost, Handler: e.CreateFileTreeNode},
		{Path: "/api/project-pipeline/filetree/{inode}", Method: http.MethodDelete, Handler: e.DeleteFileTreeNode},
		{Path: "/api/project-pipeline/filetree", Method: http.MethodGet, Handler: e.ListFileTreeNodes},
		{Path: "/api/project-pipeline/filetree/{inode}", Method: http.MethodGet, Handler: e.GetFileTreeNode},
		{Path: "/api/project-pipeline/filetree/actions/fuzzy-search", Method: http.MethodGet, Handler: e.FuzzySearchFileTreeNodes},

		{Path: "/api/orgs/{orgID}/nexus", Method: http.MethodGet, Handler: e.GetOrgNexus},
		{Path: "/api/orgs/{orgID}/actions/show-nexus-password", Method: http.MethodGet, Handler: e.ShowOrgNexusPassword},
		{Path: "/api/orgs/{orgID}/actions/get-nexus-docker-credential-by-image", Method: http.MethodGet, Handler: e.GetNexusOrgDockerCredentialByImage},
		{Path: "/api/orgs/{orgID}/actions/create-publisher", Method: http.MethodPost, Handler: e.CreateOrgPublisher},
		{Path: "/api/orgs/{orgID}/actions/create-publisher", Method: http.MethodGet, Handler: e.CreateOrgPublisher},

		// code coverage
		{Path: "/api/code-coverage/actions/start", Method: http.MethodPost, Handler: e.StartCodeCoverage},
		{Path: "/api/code-coverage/actions/end", Method: http.MethodPost, Handler: e.EndCodeCoverage},
		{Path: "/api/code-coverage/actions/cancel", Method: http.MethodPost, Handler: e.CancelCodeCoverage},
		{Path: "/api/code-coverage/actions/ready-callBack", Method: http.MethodPost, Handler: e.ReadyCallBack},
		{Path: "/api/code-coverage/actions/end-callBack", Method: http.MethodPost, Handler: e.EndCallBack},
		{Path: "/api/code-coverage/actions/report-callBack", Method: http.MethodPost, Handler: e.ReportCallBack},
		{Path: "/api/code-coverage/records/actions/list", Method: http.MethodGet, Handler: e.ListCodeCoverageRecord},
		{Path: "/api/code-coverage/record/{id}", Method: http.MethodGet, Handler: e.GetCodeCoverageRecord},
		{Path: "/api/code-coverage/actions/status", Method: http.MethodGet, Handler: e.GetCodeCoverageRecordStatus},

		// test report
		{Path: "/api/projects/{projectID}/test-reports", Method: http.MethodPost, Handler: e.CreateTestReportRecord},
		{Path: "/api/projects/{projectID}/test-reports/actions/list", Method: http.MethodGet, Handler: e.ListTestReportRecord},
		{Path: "/api/projects/{projectID}/test-reports/{id}", Method: http.MethodGet, Handler: e.GetTestReportRecord},

		// project template
		{Path: "/api/orgs/{orgID}/projects/{projectID}/template/actions/export", Method: http.MethodGet, Handler: e.ExportProjectTemplate},
		{Path: "/api/orgs/{orgID}/projects/{projectID}/template/actions/import", Method: http.MethodPost, Handler: e.ImportProjectTemplate},
		{Path: "/api/projects/template/actions/parse", Method: http.MethodPost, Handler: e.ParseProjectTemplate},

		// project package
		{Path: "/api/orgs/{orgID}/projects/{projectID}/package/actions/export", Method: http.MethodPost, Handler: e.ExportProjectPackage},
		{Path: "/api/orgs/{orgID}/projects/{projectID}/package/actions/import", Method: http.MethodPost, Handler: e.ImportProjectPackage},
		{Path: "/api/projects/package/actions/parse", Method: http.MethodPost, Handler: e.ParseProjectPackage},

		// core-services org
		{Path: "/api/orgs", Method: http.MethodPost, Handler: e.CreateOrg},
		{Path: "/api/orgs/{orgID}", Method: http.MethodPut, Handler: e.UpdateOrg},
		{Path: "/api/orgs/{idOrName}", Method: http.MethodGet, Handler: e.GetOrg},
		{Path: "/api/orgs/{idOrName}", Method: http.MethodDelete, Handler: e.DeleteOrg},
		{Path: "/api/orgs", Method: http.MethodGet, Handler: e.ListOrg},
		{Path: "/api/orgs/actions/list-public", Method: http.MethodGet, Handler: e.ListPublicOrg},
		{Path: "/api/orgs/actions/get-by-domain", Method: http.MethodGet, Handler: e.GetOrgByDomain},
		{Path: "/api/orgs/actions/fetch-resources", Method: http.MethodGet, Handler: e.FetchOrgResources},

		// core-services project
		{Path: "/api/projects", Method: http.MethodPost, Handler: e.CreateProject},
		{Path: "/api/projects/{projectID}", Method: http.MethodDelete, Handler: e.DeleteProject},
		{Path: "/api/projects/{projectID}", Method: http.MethodGet, Handler: httpserver.WithI18nCodes(e.GetProject)},
		{Path: "/api/projects", Method: http.MethodGet, Handler: e.ListProject},

		// resources
		{Path: "/api/projects/{projectID}/applications-resources", Method: http.MethodGet, Handler: e.ApplicationsResources},

		// core-services application
		{Path: "/api/applications", Method: http.MethodPost, Handler: e.CreateApplication},
		{Path: "/api/applications/{applicationID}", Method: http.MethodDelete, Handler: e.DeleteApplication},
		{Path: "/api/applications/{applicationID}/actions/init", Method: http.MethodPut, Handler: e.InitApplication},
		{Path: "/api/applications/{applicationID}", Method: http.MethodPut, Handler: e.UpdateApplication},

		{Path: "/api/applications/actions/remove-publish-item-relations", Method: http.MethodPost, Handler: e.RemoveApplicationPublishItemRelations},
		{Path: "/api/applications/{applicationID}/actions/get-publish-item-relations", Method: http.MethodGet, Handler: e.GetApplicationPublishItemRelationsGroupByENV},
		{Path: "/api/applications/actions/query-publish-item-relations", Method: http.MethodGet, Handler: e.QueryApplicationPublishItemRelations},
		{Path: "/api/applications/{applicationID}/actions/update-publish-item-relations", Method: http.MethodPost, Handler: e.UpdateApplicationPublishItemRelations},

		// core-services member
		{Path: "/api/members/actions/list-roles", Method: http.MethodGet, Handler: e.ListMemberRoles},
		// approve
		{Path: "/api/approvals/actions/watch-status", Method: http.MethodPost, Handler: e.WatchApprovalStatusChanged},

		// test file records
		{Path: "/api/test-file-records/{id}", Method: http.MethodGet, Handler: e.GetFileRecord},
		{Path: "/api/test-file-records", Method: http.MethodGet, Handler: e.GetFileRecords},

		// 从dicehub迁移
		//发布管理-->安全管理
		{Path: "/api/publish-items/{publishItemId}/certification", Method: http.MethodGet, Handler: e.GetPublishItemCertificationlist},

		// 从dicehub迁移
		//统一大盘以及错误报告
		{Path: "/api/publish-items/{publishItemId}/statistics/trend", Method: http.MethodGet, Handler: e.GetStatisticsTrend},
		{Path: "/api/publish-items/{publishItemId}/statistics/versions", Method: http.MethodGet, Handler: e.GetStatisticsVersionInfo},
		{Path: "/api/publish-items/{publishItemId}/statistics/users", Method: http.MethodGet, Handler: e.CumulativeUsers},
		{Path: "/api/publish-items/{publishItemId}/statistics/channels", Method: http.MethodGet, Handler: e.GetStatisticsChannelInfo},
		{Path: "/api/publish-items/{publishItemId}/err/trend", Method: http.MethodGet, Handler: e.GetErrTrend},
		{Path: "/api/publish-items/{publishItemId}/err/list", Method: http.MethodGet, Handler: e.GetErrList},
		{Path: "/api/publish-items/{publishItemId}/metrics/{metricName}/histogram", Method: http.MethodGet, Handler: e.MetricsRouting},
		{Path: "/api/publish-items/{publishItemId}/metrics/{metricName}", Method: http.MethodGet, Handler: e.MetricsRouting},
		{Path: "/api/publish-items/{publishItemId}/err/effacts", Method: http.MethodGet, Handler: e.GetErrAffectUserRate},
		{Path: "/api/publish-items/{publishItemId}/err/rate", Method: http.MethodGet, Handler: e.GetCrashRate},
	}
}

func NotImplemented(ctx context.Context, request *http.Request, m map[string]string) (httpserver.Responser, error) {
	return httpserver.ErrResp(http.StatusNotImplemented, "", "not implemented")
}

// Endpoints 定义 endpoint 方法
type Endpoints struct {
	queryStringDecoder *schema.Decoder
	assetSvc           *assetsvc.Service
	fileTreeSvc        *apidocsvc.Service
	bdl                *bundle.Bundle
	pipeline           *pipeline.Pipeline
	cdp                *cdp.CDP
	event              *event.Event
	permission         *permission.Permission
	fileTree           *filetree.GittarFileTree
	pFileTree          *projectpipelinefiletree.FileTree
	pipelineCms        cmspb.CmsServiceServer

	db              *dao.DBClient
	testcase        *testcase.Service
	testset         *testset.Service
	mttestPlan      *mttestplan.TestPlan
	autotest        *autotest.Service
	autotestV2      *atv2.Service
	sonarMetricRule *sonar_metric_rule.Service
	cq              *cq.CQ
	sceneset        *sceneset.Service
	migrate         *migrate.Service

	store           jsonstore.JsonStore
	ossClient       *oss.Client
	etcdStore       *etcd.Store
	ticket          *ticket.Ticket
	comment         *comment.Comment
	branchRule      *branchrule.BranchRule
	namespace       *namespace.Namespace
	envConfig       *environment.EnvConfig
	issue           *issue.Issue
	issueState      *issuestate.IssueState
	workBench       *workbench.Workbench
	uc              *ucauth.UCClient
	iteration       *iteration.Iteration
	publisher       *publisher.Publisher
	certificate     *certificate.Certificate
	appCertificate  *appcertificate.AppCertificate
	libReference    *libreference.LibReference
	org             *org.Org
	project         *project.Project
	app             *application.Application
	codeCoverageSvc *code_coverage.CodeCoverage
	testReportSvc   *test_report.TestReport

	publishItem *publish_item.PublishItem

	PipelineCron       cronpb.CronServiceServer
	PipelineSource     sourcepb.SourceServiceServer
	PipelineDefinition dpb.DefinitionServiceServer
	DevFlowRule        dwfpb.DevFlowRuleServiceServer

	ImportChannel chan uint64
	ExportChannel chan uint64
	CopyChannel   chan uint64

	tokenService tokenpb.TokenServiceServer
}

type Option func(*Endpoints)

func New(options ...Option) *Endpoints {
	e := &Endpoints{}

	for _, op := range options {
		op(e)
	}

	return e
}

func WithQueryStringDecoder(decoder *schema.Decoder) Option {
	return func(e *Endpoints) {
		e.queryStringDecoder = decoder
	}
}

func WithAssetSvc(svc *assetsvc.Service) Option {
	return func(e *Endpoints) {
		e.assetSvc = svc
	}
}

func WithFileTreeSvc(svc *apidocsvc.Service) Option {
	return func(e *Endpoints) {
		e.fileTreeSvc = svc
	}
}

func WithProjectPipelineFileTree(fileTree *projectpipelinefiletree.FileTree) Option {
	return func(e *Endpoints) {
		e.pFileTree = fileTree
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(e *Endpoints) {
		e.bdl = bdl
	}
}

// WithPipeline 配置 pipeline
func WithPipeline(p *pipeline.Pipeline) Option {
	return func(e *Endpoints) {
		e.pipeline = p
	}
}

func WithPipelineCms(cms cmspb.CmsServiceServer) Option {
	return func(e *Endpoints) {
		e.pipelineCms = cms
	}
}

// WithCDP 配置 cdp
func WithCDP(c *cdp.CDP) Option {
	return func(e *Endpoints) {
		e.cdp = c
	}
}

// WithEvent 配置 event
func WithEvent(ev *event.Event) Option {
	return func(e *Endpoints) {
		e.event = ev
	}
}

// WithPermission 配置 permission
func WithPermission(perm *permission.Permission) Option {
	return func(e *Endpoints) {
		e.permission = perm
	}
}

func WithGittarFileTree(fileTree *filetree.GittarFileTree) Option {
	return func(e *Endpoints) {
		e.fileTree = fileTree
	}
}

func WithProject(p *project.Project) Option {
	return func(e *Endpoints) {
		e.project = p
	}
}

func WithApplication(app *application.Application) Option {
	return func(e *Endpoints) {
		e.app = app
	}
}

func WithPublishItem(publishItem *publish_item.PublishItem) Option {
	return func(e *Endpoints) {
		e.publishItem = publishItem
	}
}

// DBClient 获取db client
func (e *Endpoints) DBClient() *dao.DBClient {
	return e.db
}

func (e *Endpoints) UCClient() *ucauth.UCClient {
	return e.uc
}

// GetLocale 获取本地化资源
func (e *Endpoints) GetLocale(request *http.Request) *i18n.LocaleResource {
	return e.bdl.GetLocaleByRequest(request)
}

func (e *Endpoints) RegisterEvents() error {
	fmt.Println(discover.DOP())
	for _, callback := range eventCallbacks {
		ev := apistructs.CreateHookRequest{
			Name:   callback.Name,
			Events: callback.Events,
			URL:    strutil.Concat("http://", discover.DOP(), callback.Path),
			Active: true,
			HookLocation: apistructs.HookLocation{
				Org:         "-1",
				Project:     "-1",
				Application: "-1",
			},
		}
		if err := e.bdl.CreateWebhook(ev); err != nil {
			logrus.Errorf("failed to register %s event to eventbox, (%v)", callback.Name, err)
			return err
		}
		logrus.Infof("register release event to eventbox, event:%+v", ev)
	}
	return nil
}

func WithDB(db *dao.DBClient) Option {
	return func(e *Endpoints) {
		e.db = db
	}
}

func WithTestcase(svc *testcase.Service) Option {
	return func(e *Endpoints) {
		e.testcase = svc
	}
}

func WithTestSet(svc *testset.Service) Option {
	return func(e *Endpoints) {
		e.testset = svc
	}
}

func WithSonarMetricRule(sonarMetricRule *sonar_metric_rule.Service) Option {
	return func(e *Endpoints) {
		e.sonarMetricRule = sonarMetricRule
	}
}

// WithTestplan 设置 testplan endpoint
func WithTestplan(testPlan *mttestplan.TestPlan) Option {
	return func(e *Endpoints) {
		e.mttestPlan = testPlan
	}
}

func WithWorkbench(w *workbench.Workbench) Option {
	return func(e *Endpoints) {
		e.workBench = w
	}
}

func WithCQ(cq *cq.CQ) Option {
	return func(e *Endpoints) {
		e.cq = cq
	}
}

func WithAutoTest(svc *autotest.Service) Option {
	return func(e *Endpoints) {
		e.autotest = svc
	}
}

func WithAutoTestV2(svc *atv2.Service) Option {
	return func(e *Endpoints) {
		e.autotestV2 = svc
	}
}

func WithSceneSet(svc *sceneset.Service) Option {
	return func(e *Endpoints) {
		e.sceneset = svc
	}
}

func WithMigrate(svc *migrate.Service) Option {
	return func(e *Endpoints) {
		e.migrate = svc
	}
}

// WithTicket 配置 ticket service
func WithTicket(ticket *ticket.Ticket) Option {
	return func(e *Endpoints) {
		e.ticket = ticket
	}
}

// WithComment 配置 comment service
func WithComment(comment *comment.Comment) Option {
	return func(e *Endpoints) {
		e.comment = comment
	}
}

// WithBranchRule 配置 branchRule
func WithBranchRule(branchRule *branchrule.BranchRule) Option {
	return func(e *Endpoints) {
		e.branchRule = branchRule
	}
}

// WithNamespace 配置 namespace service
func WithNamespace(namespace *namespace.Namespace) Option {
	return func(e *Endpoints) {
		e.namespace = namespace
	}
}

// WithEnvConfig 配置 env config
func WithEnvConfig(envConfig *environment.EnvConfig) Option {
	return func(e *Endpoints) {
		e.envConfig = envConfig
	}
}

// WithIssue 配置 issue
func WithIssue(issue *issue.Issue) Option {
	return func(e *Endpoints) {
		e.issue = issue
	}
}

func WithIssueState(state *issuestate.IssueState) Option {
	return func(e *Endpoints) {
		e.issueState = state
	}
}

// WithUCClient 配置 UC Client
func WithUCClient(uc *ucauth.UCClient) Option {
	return func(e *Endpoints) {
		e.uc = uc
	}
}

// WithIteration 配置 iteration
func WithIteration(itr *iteration.Iteration) Option {
	return func(e *Endpoints) {
		e.iteration = itr
	}
}

// WithPublisher 配置 publisher service
func WithPublisher(pub *publisher.Publisher) Option {
	return func(e *Endpoints) {
		e.publisher = pub
	}
}

// WithCertificate 配置证书 service
func WithCertificate(cer *certificate.Certificate) Option {
	return func(e *Endpoints) {
		e.certificate = cer
	}
}

// WithAppCertificate 配置证书 service
func WithAppCertificate(cer *appcertificate.AppCertificate) Option {
	return func(e *Endpoints) {
		e.appCertificate = cer
	}
}

// WithOSSClient 配置OSS Client
func WithOSSClient(client *oss.Client) Option {
	return func(e *Endpoints) {
		e.ossClient = client
	}
}

// WithEtcdStore 配置 etcdStore
func WithEtcdStore(etcdStore *etcd.Store) Option {
	return func(e *Endpoints) {
		e.etcdStore = etcdStore
	}
}

// WithJSONStore 配置 jsonstore
func WithJSONStore(store jsonstore.JsonStore) Option {
	return func(e *Endpoints) {
		e.store = store
	}
}

// WithLibReference 设置 libReference service
func WithLibReference(libReference *libreference.LibReference) Option {
	return func(e *Endpoints) {
		e.libReference = libReference
	}
}

// WithOrg 配置 org service
func WithOrg(org *org.Org) Option {
	return func(e *Endpoints) {
		e.org = org
	}
}

func WithCodeCoverageExecRecord(svc *code_coverage.CodeCoverage) Option {
	return func(e *Endpoints) {
		e.codeCoverageSvc = svc
	}
}

func WithTestReportRecord(svc *test_report.TestReport) Option {
	return func(e *Endpoints) {
		e.testReportSvc = svc
	}
}

func WithPipelineCron(svc cronpb.CronServiceServer) Option {
	return func(e *Endpoints) {
		e.PipelineCron = svc
	}
}

func WithPipelineSource(svc sourcepb.SourceServiceServer) Option {
	return func(e *Endpoints) {
		e.PipelineSource = svc
	}
}

func WithPipelineDefinition(svc dpb.DefinitionServiceServer) Option {
	return func(e *Endpoints) {
		e.PipelineDefinition = svc
	}
}

func WithDevFlowRule(svc dwfpb.DevFlowRuleServiceServer) Option {
	return func(e *Endpoints) {
		e.DevFlowRule = svc
	}
}

var queryStringDecoder *schema.Decoder

func init() {
	queryStringDecoder = schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)
}

func (e *Endpoints) TestCaseService() *testcase.Service {
	return e.testcase
}

func (e *Endpoints) AutotestV2Service() *atv2.Service {
	return e.autotestV2
}

func (e *Endpoints) TestSetService() *testset.Service {
	return e.testset
}

func (e *Endpoints) IssueStateService() *issuestate.IssueState {
	return e.issueState
}

func (e *Endpoints) IssueService() *issue.Issue {
	return e.issue
}

func (e *Endpoints) CodeCoverageService() *code_coverage.CodeCoverage {
	return e.codeCoverageSvc
}

func (e *Endpoints) IterationService() *iteration.Iteration {
	return e.iteration
}

func (e *Endpoints) ManualTestCaseService() *testcase.Service {
	return e.testcase
}

func (e *Endpoints) ManualTestPlanService() *mttestplan.TestPlan {
	return e.mttestPlan
}

func (e *Endpoints) AutoTestPlanService() *atv2.Service {
	return e.autotestV2
}

func (e *Endpoints) ProjectService() *project.Project {
	return e.project
}

func (e *Endpoints) PermissionService() *permission.Permission {
	return e.permission
}

func WithTokenSvc(tokenService tokenpb.TokenServiceServer) Option {
	return func(e *Endpoints) {
		e.tokenService = tokenService
	}
}
