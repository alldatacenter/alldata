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

package core

import (
	"context"
	"io"
	"net/http"
	"strconv"
	"strings"

	"github.com/gorilla/schema"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/http/encoding"
	"github.com/erda-project/erda-infra/pkg/transport/interceptor"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	stream "github.com/erda-project/erda/internal/apps/dop/providers/issue/stream/core"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
	perm "github.com/erda-project/erda/pkg/common/permission"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

type config struct {
}

type provider struct {
	Cfg      *config
	Log      logs.Logger
	Register transport.Register `autowired:"service-register" required:"true"`
	DB       *gorm.DB           `autowired:"mysql-client"`
	Perm     perm.Interface     `autowired:"permission"`

	issueService *IssueService
	Stream       stream.Interface
	Query        query.Interface
}

func (p *provider) Init(ctx servicehub.Context) error {
	queryStringDecoder := schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)
	p.issueService = &IssueService{
		db: &dao.DBClient{
			DBEngine: &dbengine.DBEngine{
				DB: p.DB,
			},
		},
		logger: p.Log,
		bdl:    bundle.New(bundle.WithCoreServices()),
		stream: p.Stream,
		query:  p.Query,
	}

	if p.Register != nil {
		type IssueService = pb.IssueCoreServiceServer
		pb.RegisterIssueCoreServiceImp(p.Register, p.issueService, apis.Options(), p.Perm.Check(
			perm.Method(IssueService.CreateIssue, perm.ScopeProject, IssueResource, perm.ActionCreate, ScopeID, perm.WithSkipPermInternalClient(true)),
			perm.Method(IssueService.PagingIssue, perm.ScopeProject, "project", perm.ActionGet, ScopeID, perm.WithSkipPermInternalClient(true)),
			perm.NoPermMethod(IssueService.GetIssue),
			perm.NoPermMethod(IssueService.UpdateIssue),
			perm.NoPermMethod(IssueService.DeleteIssue),
			perm.Method(IssueService.BatchUpdateIssue, perm.ScopeProject, IssueResource, perm.ActionUpdate, ScopeID, perm.WithSkipPermInternalClient(true)),
			perm.Method(IssueService.UpdateIssueType, perm.ScopeProject, "issue-type", perm.ActionUpdate, ScopeID, perm.WithSkipPermInternalClient(true)),
			perm.NoPermMethod(IssueService.SubscribeIssue),
			perm.NoPermMethod(IssueService.UnsubscribeIssue),
			perm.NoPermMethod(IssueService.BatchUpdateIssueSubscriber),
			perm.NoPermMethod(IssueService.CreateIssueProperty),
			perm.NoPermMethod(IssueService.DeleteIssueProperty),
			perm.NoPermMethod(IssueService.UpdateIssueProperty),
			perm.NoPermMethod(IssueService.GetIssueProperty),
			perm.NoPermMethod(IssueService.UpdateIssuePropertiesIndex),
			perm.NoPermMethod(IssueService.GetIssuePropertyUpdateTime),
			perm.NoPermMethod(IssueService.CreateIssuePropertyInstance),
			perm.NoPermMethod(IssueService.GetIssuePropertyInstance),
			perm.NoPermMethod(IssueService.GetIssueStage),
			perm.NoPermMethod(IssueService.UpdateIssueStage),
			perm.NoPermMethod(IssueService.AddIssueRelation),
			perm.NoPermMethod(IssueService.DeleteIssueRelation),
			perm.NoPermMethod(IssueService.GetIssueRelations),
			perm.Method(IssueService.CreateIssueState, perm.ScopeProject, "issue-state", perm.ActionCreate, ScopeID, perm.WithSkipPermInternalClient(true)),
			perm.Method(IssueService.DeleteIssueState, perm.ScopeProject, "issue-state", perm.ActionDelete, ScopeID, perm.WithSkipPermInternalClient(true)),
			perm.Method(IssueService.UpdateIssueStateRelation, perm.ScopeProject, "issue-state", perm.ActionUpdate, ScopeID, perm.WithSkipPermInternalClient(true)),
			perm.NoPermMethod(IssueService.GetIssueStates),
			perm.NoPermMethod(IssueService.GetIssueStateRelation),
			perm.Method(IssueService.ExportExcelIssue, perm.ScopeProject, "project", perm.ActionGet, ScopeID, perm.WithSkipPermInternalClient(true)),
			perm.Method(IssueService.ImportExcelIssue, perm.ScopeProject, "issue-import", perm.ActionCreate, ScopeID, perm.WithSkipPermInternalClient(true)),
		), transport.WithInterceptors(p.keepalive),
			transport.WithHTTPOptions(
				transhttp.WithEncoder(func(rw http.ResponseWriter, r *http.Request, data interface{}) error {
					if strutil.HasPrefixes(r.URL.Path, "/api/issues/actions/export-excel") {
						var req pb.ExportExcelIssueRequest
						if err := queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
							return apierrors.ErrExportExcelIssue.InvalidParameter(err)
						}
						if !req.IsDownload {
							return encoding.EncodeResponse(rw, r, data)
						}
						req.PageNo = 1
						req.PageSize = 1
						issues, _, err := p.Query.Paging(getIssuePagingRequest(&req))
						if err != nil {
							return err
						}
						pro, err := p.Query.GetBatchProperties(req.OrgID, req.Type)
						if err != nil {
							return err
						}
						reader, tableName, err := p.Query.ExportExcel(issues, pro, req.ProjectID, true, req.OrgID, req.Locale)
						if err != nil {
							return err
						}
						rw.Header().Add("Content-Disposition", "attachment;fileName="+tableName+".xlsx")
						rw.Header().Add("Content-Type", "application/vnd.ms-excel")

						if _, err := io.Copy(rw, reader); err != nil {
							return apierrors.ErrExportExcelIssue.InternalError(err)
						}
					}
					return encoding.EncodeResponse(rw, r, data)
				}),
			))
	}
	return nil
}

func (p *provider) keepalive(h interceptor.Handler) interceptor.Handler {
	return func(ctx context.Context, req interface{}) (interface{}, error) {

		return h(ctx, req)
	}
}

type IssuePermType interface {
	GetType() pb.IssueTypeEnum_Type
}

type IssuePermRequestWithUint64ProjectID interface {
	GetProjectID() uint64
}

type IssuePermRequestWithInt64ProjectID interface {
	GetProjectID() int64
}

func ScopeID(ctx context.Context, req interface{}) (string, error) {
	r, ok := req.(IssuePermRequestWithUint64ProjectID)
	if !ok {
		v, ok := req.(IssuePermRequestWithInt64ProjectID)
		if !ok {
			return "", errors.NewMissingParameterError("projectID")
		}
		id := v.GetProjectID()
		return strconv.FormatInt(id, 10), nil
	}
	id := r.GetProjectID()
	return strconv.FormatUint(id, 10), nil
}

func IssueResource(ctx context.Context, req interface{}) (string, error) {
	r, ok := req.(IssuePermType)
	if !ok {
		return "", errors.NewMissingParameterError("type")
	}
	t := strings.ToLower(r.GetType().String())
	return "issue-" + t, nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.dop.issue.core.IssueCoreService" || ctx.Type() == pb.IssueCoreServiceServerType() || ctx.Type() == pb.IssueCoreServiceHandlerType():
		return p.issueService
	}
	return p
}

func init() {
	servicehub.Register("erda.dop.issue.core", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
