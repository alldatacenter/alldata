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

package cms

import (
	"net/http"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/pkg/transport/http/encoding"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cms/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/strutil"
)

type config struct {
	Base64EncodedRsaPublicKey  string `file:"base64_encoded_rsa_public_key" env:"CMS_BASE64_ENCODED_RSA_PUBLIC_KEY"`
	Base64EncodedRsaPrivateKey string `file:"base64_encoded_rsa_private_key" env:"CMS_BASE64_ENCODED_RSA_PRIVATE_KEY"`
}

// +provider
type provider struct {
	Cfg      *config
	Log      logs.Logger
	Register transport.Register  `autowired:"service-register"`
	MySQL    mysqlxorm.Interface `autowired:"mysql-xorm"`

	// implements
	cmsService *cmsService
}

func (p *provider) Init(ctx servicehub.Context) error {
	rsaCrypt := encryption.NewRSAScrypt(encryption.RSASecret{
		PublicKey:          p.Cfg.Base64EncodedRsaPublicKey,
		PublicKeyDataType:  encryption.Base64,
		PrivateKey:         p.Cfg.Base64EncodedRsaPrivateKey,
		PrivateKeyDataType: encryption.Base64,
		PrivateKeyType:     encryption.PKCS1,
	})

	p.cmsService = &cmsService{
		p:  p,
		cm: NewPipelineCms(&db.Client{Interface: p.MySQL}, rsaCrypt),
	}

	if p.Register != nil {
		pb.RegisterCmsServiceImp(p.Register, p.cmsService, apis.Options(),
			transport.WithHTTPOptions(
				transhttp.WithDecoder(func(r *http.Request, data interface{}) error {
					v1Req, ok := data.(*pb.CmsNsConfigsUpdateV1Request)
					if !ok {
						return encoding.DecodeRequest(r, data)
					}
					// transform v1Req to req
					req := &pb.CmsNsConfigsUpdateRequest{
						Ns:  v1Req.Ns,
						KVs: make(map[string]*pb.PipelineCmsConfigValue, len(v1Req.KVs)),
					}
					for k, v := range v1Req.KVs {
						req.KVs[k] = &pb.PipelineCmsConfigValue{
							Value:       v,
							EncryptInDB: true,
							Type:        ConfigTypeKV,
							Operations: &pb.PipelineCmsConfigOperations{
								CanDownload: false,
								CanEdit:     true,
								CanDelete:   true,
							},
						}
					}
					// 兼容 cdp
					nsPrefix := strutil.TrimPrefixes(req.Ns, "cdp-action-")
					switch nsPrefix {
					case "dev":
						req.PipelineSource = apistructs.PipelineSourceCDPDev.String()
					case "test":
						req.PipelineSource = apistructs.PipelineSourceCDPTest.String()
					case "staging":
						req.PipelineSource = apistructs.PipelineSourceCDPStaging.String()
					case "prod":
						req.PipelineSource = apistructs.PipelineSourceCDPProd.String()
					default:
						req.PipelineSource = apistructs.PipelineSourceDefault.String()
					}
					return nil
				}),
			),
		)
	}

	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.pipeline.cms.CmsService" || ctx.Type() == pb.CmsServiceServerType() || ctx.Type() == pb.CmsServiceHandlerType():
		return p.cmsService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.pipeline.cms", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		Dependencies:         []string{"mysql-xorm-client", "service-register"},
		OptionalDependencies: []string{},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
