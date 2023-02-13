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

package file_manager

import (
	"embed"
	"net/http"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-proto-go/core/services/filemanager/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
	k8sclient "github.com/erda-project/erda/pkg/k8s-client-manager"
)

//go:embed upload.html
var webfs embed.FS

type config struct {
}

// +provider
type provider struct {
	Cfg      *config
	Log      logs.Logger
	Register transport.Register  `autowired:"service-register" optional:"true"`
	Router   httpserver.Router   `autowired:"http-router"`
	Clients  k8sclient.Interface `autowired:"k8s-client-manager"`
	Perm     perm.Interface      `autowired:"permission"`

	bdl                *bundle.Bundle
	fileManagerService *fileManagerService
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())

	p.fileManagerService = &fileManagerService{p}
	if p.Register != nil {
		type FileManagerService = pb.FileManagerServiceServer
		pb.RegisterFileManagerServiceImp(p.Register, p.fileManagerService, apis.Options(), p.Perm.Check(
			perm.Method(FileManagerService.ListFiles, p.getScopeByRequest, "terminal", perm.ActionOperate, p.checkScopeID),
			perm.Method(FileManagerService.ReadFile, p.getScopeByRequest, "terminal", perm.ActionOperate, p.checkScopeID),
			perm.Method(FileManagerService.WriteFile, p.getScopeByRequest, "terminal", perm.ActionOperate, p.checkScopeID),
			perm.Method(FileManagerService.MakeDirectory, p.getScopeByRequest, "terminal", perm.ActionOperate, p.checkScopeID),
			perm.Method(FileManagerService.MoveFile, p.getScopeByRequest, "terminal", perm.ActionOperate, p.checkScopeID),
			perm.Method(FileManagerService.DeleteFile, p.getScopeByRequest, "terminal", perm.ActionOperate, p.checkScopeID),
		))
	}
	p.Router.Static("/api/containers/files/upload", "/", httpserver.WithFileSystem(http.FS(webfs)))
	p.Router.POST("/api/container/:containerID/files/upload", p.fileManagerService.UploadFile, permission.Intercepter(
		p.getScopeByHTTPRequest, p.checkScopeIDByHTTPRequest,
		"terminal", permission.ActionOperate,
	))
	p.Router.GET("/api/container/:containerID/files/download", p.fileManagerService.DownloadFile, permission.Intercepter(
		p.getScopeByHTTPRequest, p.checkScopeIDByHTTPRequest,
		"terminal", permission.ActionOperate,
	))
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.services.filemanager.FileManagerService" || ctx.Type() == pb.FileManagerServiceServerType() || ctx.Type() == pb.FileManagerServiceHandlerType():
		return p.fileManagerService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.services.filemanager", &servicehub.Spec{
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
