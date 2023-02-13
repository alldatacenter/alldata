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

package credential

import (
	"context"
	"net/http"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/http/encoding"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda-proto-go/msp/credential/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

type provider struct {
	Cfg                  *config
	Register             transport.Register `autowired:"service-register"`
	credentialKeyService *accessKeyService
	TokenService         tokenpb.TokenServiceServer `autowired:"erda.core.token.TokenService"`
	bdl                  *bundle.Bundle
	audit                audit.Auditor
	Tenant               tenantpb.TenantServiceServer `autowired:"erda.msp.tenant.TenantService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.audit = audit.GetAuditor(ctx)
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())
	p.credentialKeyService = &accessKeyService{
		p: p,
	}
	if p.Register != nil {
		type AccessKeyService = pb.AccessKeyServiceServer
		pb.RegisterAccessKeyServiceImp(p.Register, p.credentialKeyService, apis.Options(),
			transport.WithHTTPOptions(
				transhttp.WithEncoder(func(rw http.ResponseWriter, r *http.Request, data interface{}) error {
					if resp, ok := data.(*apis.Response); ok && resp != nil {
						if data, ok := resp.Data.(*pb.DownloadAccessKeyFileResponse); ok {
							rw.Header().Set("Cache-Control", "no-cache, no-store, must-revalidate")
							rw.Header().Set("Pragma", "no-cache")
							rw.Header().Set("Expires", "0")
							rw.Header().Set("charset", "utf-8")
							rw.Header().Set("Content-Disposition", "attachment;filename=accessKey.csv")
							rw.Header().Set("Content-Type", "application/octet-stream")
							fluster := rw.(http.Flusher)
							rw.Write(data.Content)
							fluster.Flush()
							return nil
						}
					}
					return encoding.EncodeResponse(rw, r, data)
				})),
			p.audit.Audit(
				audit.Method(AccessKeyService.CreateAccessKey, audit.ProjectScope, string(apistructs.CreateServiceToken),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*pb.CreateAccessKeyResponse)
						return r.Data.ProjectId, map[string]interface{}{}, nil
					}),
				audit.Method(AccessKeyService.DeleteAccessKey, audit.ProjectScope, string(apistructs.DeleteServiceToken),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*pb.DeleteAccessKeyResponse)
						return r.Data, map[string]interface{}{}, nil
					},
				),
			),
		)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.credential.AccessKeyService" || ctx.Type() == pb.AccessKeyServiceServerType() || ctx.Type() == pb.AccessKeyServiceHandlerType():
		return p.TokenService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.credential", &servicehub.Spec{
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
