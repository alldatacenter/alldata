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
	"net/http"

	"github.com/gorilla/schema"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/publish_item"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/release"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/release_rule"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/template"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// Endpoints 定义 endpoint 方法
type Endpoints struct {
	db                 *dbclient.DBClient
	bdl                *bundle.Bundle
	release            *release.Release
	publishItem        *publish_item.PublishItem
	pipelineTemplate   *template.PipelineTemplate
	releaseRule        *release_rule.ReleaseRule
	queryStringDecoder *schema.Decoder
}

type Option func(*Endpoints)

// New 创建 Endpoints 对象.
func New(options ...Option) *Endpoints {
	e := &Endpoints{}

	for _, op := range options {
		op(e)
	}

	return e
}

// WithDBClient 配置 db
func WithDBClient(db *dbclient.DBClient) Option {
	return func(e *Endpoints) {
		e.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(e *Endpoints) {
		e.bdl = bdl
	}
}

// WithRelease 配置 release service
func WithRelease(release *release.Release) Option {
	return func(e *Endpoints) {
		e.release = release
	}
}

// WithExtension 配置 extension service
func WithPublishItem(publishItem *publish_item.PublishItem) Option {
	return func(e *Endpoints) {
		e.publishItem = publishItem
	}
}

func WithPipelineTemplate(pipelineTemplate *template.PipelineTemplate) Option {
	return func(e *Endpoints) {
		e.pipelineTemplate = pipelineTemplate
	}
}

func WithReleaseRule(rule *release_rule.ReleaseRule) Option {
	return func(e *Endpoints) {
		e.releaseRule = rule
	}
}

// WithQueryStringDecoder 配置 queryStringDecoder
func WithQueryStringDecoder(decoder *schema.Decoder) Option {
	return func(e *Endpoints) {
		e.queryStringDecoder = decoder
	}
}

// Release 获取 release service
func (e *Endpoints) Release() *release.Release {
	return e.release
}

// Routes 返回 endpoints 的所有 endpoint 方法，也就是 route.
func (e *Endpoints) Routes() []httpserver.Endpoint {
	return []httpserver.Endpoint{
		{Path: "/healthz", Method: http.MethodGet, Handler: e.Info},

		// Release相关
		{Path: "/api/releases/{releaseId}/actions/download", Method: http.MethodGet, WriterHandler: e.DownloadRelease},

		//模板市场
		//{Path: "/api/pipeline-templates/{scopeType}/{scopeId}", Method: http.MethodPost, Handler: e.CreatePipelineTemplate},
		{Path: "/api/pipeline-templates/actions/apply", Method: http.MethodPost, Handler: e.ApplyPipelineTemplate},
		{Path: "/api/pipeline-templates", Method: http.MethodGet, Handler: e.QueryPipelineTemplates},
		{Path: "/api/pipeline-templates/{name}/actions/render", Method: http.MethodPost, Handler: e.RenderPipelineTemplate},
		{Path: "/api/pipeline-templates/local/actions/render-spec", Method: http.MethodPost, Handler: e.RenderPipelineTemplateBySpec},
		{Path: "/api/pipeline-templates/{name}/actions/query-version", Method: http.MethodGet, Handler: e.GetPipelineTemplateVersion},
		{Path: "/api/pipeline-templates/{name}/actions/query-versions", Method: http.MethodGet, Handler: e.QueryPipelineTemplateVersions},

		{Path: "/api/pipeline-snippets/actions/query-snippet-yml", Method: http.MethodGet, Handler: e.querySnippetYml},

		//发布管理
		{Path: "/api/publish-items", Method: http.MethodPost, Handler: e.CreatePublishItem},
		{Path: "/api/publish-items", Method: http.MethodGet, Handler: e.QueryPublishItem},
		{Path: "/api/my-publish-items", Method: http.MethodGet, Handler: e.QueryMyPublishItem},
		{Path: "/api/publish-items/{publishItemId}", Method: http.MethodGet, Handler: e.GetPublishItem},
		{Path: "/api/publish-items/{publishItemId}/distribution", Method: http.MethodGet, Handler: e.GetPublishItemDistribution},
		{Path: "/api/publish-items/{publishItemId}", Method: http.MethodPut, Handler: e.UpdatePublishItem},
		{Path: "/api/publish-items/{publishItemId}", Method: http.MethodDelete, Handler: e.DeletePublishItem},
		{Path: "/api/publish-items/{publishItemId}/versions", Method: http.MethodPost, Handler: e.CreatePublishItemVersion},
		{Path: "/api/publish-items/{publishItemId}/versions", Method: http.MethodGet, Handler: e.QueryPublishItemVersion},
		{Path: "/api/publish-items/{publishItemId}/versions/{publishItemVersionId}/actions/{action}", Method: http.MethodPost, Handler: e.SetPublishItemVersionStatus},
		{Path: "/api/publish-items/versions/actions/{action}", Method: http.MethodPost, Handler: e.UpdatePublishItemVersionState},
		{Path: "/api/publish-items/{publishItemId}/versions/actions/public-version", Method: http.MethodGet, Handler: e.GetPublicVersion},
		{Path: "/api/publish-items/actions/latest-versions", Method: http.MethodPost, Handler: e.CheckLaststVersion},
		{Path: "/api/publish-items/{publishItemId}/versions/actions/get-h5-packagename", Method: http.MethodGet, Handler: e.GetH5PackageName},
		{Path: "/api/publish-items/{publishItemId}/list-monitor-keys", Method: http.MethodGet, Handler: e.ListMonitorKeys},
		{Path: "/api/publish-items/{publishItemId}/versions/create-offline-version", Method: http.MethodPost, Handler: e.CreateOffLineVersion},

		//发布管理-->安全管理
		{Path: "/api/publish-items/{publishItemId}/blacklist", Method: http.MethodGet, Handler: e.GetPublishItemBlacklist},
		{Path: "/api/publish-items/{publishItemId}/blacklist", Method: http.MethodPost, Handler: e.AddBlacklist},
		{Path: "/api/publish-items/{publishItemId}/blacklist/{blacklistId}", Method: http.MethodDelete, Handler: e.RemoveBlacklist},
		{Path: "/api/publish-items/{publishItemId}/erase", Method: http.MethodGet, Handler: e.GetPublishItemEraselist},
		{Path: "/api/publish-items/{publishItemId}/erase", Method: http.MethodPost, Handler: e.AddErase},
		{Path: "/api/publish-items/erase/status", Method: http.MethodPut, Handler: e.UpdateErase},
		{Path: "/api/publish-items/security/status", Method: http.MethodGet, Handler: e.GetSecurityStatus},

		// 分支 release 规则
		{Path: "/api/release-rules", Method: http.MethodPost, Handler: httpserver.Wrap(e.CreateRule, e.ReleaseRuleMiddleware)},
		{Path: "/api/release-rules", Method: http.MethodGet, Handler: httpserver.Wrap(e.ListRules, e.ReleaseRuleMiddleware)},
		{Path: "/api/release-rules/{id}", Method: http.MethodPut, Handler: httpserver.Wrap(e.UpdateRule, e.ReleaseRuleMiddleware)},
		{Path: "/api/release-rules/{id}", Method: http.MethodDelete, Handler: httpserver.Wrap(e.DeleteRule, e.ReleaseRuleMiddleware)},
	}
}
