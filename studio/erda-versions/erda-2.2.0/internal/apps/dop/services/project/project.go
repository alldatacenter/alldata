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

package project

import (
	"time"

	"github.com/erda-project/erda-infra/providers/i18n"
	dashboardPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/namespace"
	"github.com/erda-project/erda/pkg/cache"
)

type Project struct {
	db         *dbclient.DBClient
	bdl        *bundle.Bundle
	trans      i18n.Translator
	cmp        dashboardPb.ClusterResourceServer
	namespace  *namespace.Namespace
	clusterSvc clusterpb.ClusterServiceServer

	appOwnerCache    *cache.Cache
	CreateFileRecord func(req apistructs.TestFileRecordRequest) (uint64, error)
	UpdateFileRecord func(req apistructs.TestFileRecordRequest) error
	tokenService     tokenpb.TokenServiceServer
}

func New(options ...Option) *Project {
	p := new(Project)
	for _, f := range options {
		f(p)
	}
	p.appOwnerCache = cache.New("ApplicationOwnerCache", time.Minute, p.updateMemberCache)
	return p
}
