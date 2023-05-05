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

package domain

import (
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

// Domain 域名封装
type Domain struct {
	db         *dbclient.DBClient
	evMgr      *events.EventManager
	bdl        *bundle.Bundle
	clusterSvc clusterpb.ClusterServiceServer
}

// Option 域名对象配置选项
type Option func(*Domain)

// New 新建域名对象实例
func New(options ...Option) *Domain {
	d := &Domain{}
	for _, op := range options {
		op(d)
	}
	return d
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(d *Domain) {
		d.db = db
	}
}

// WithEventManager 配置 EventManager
func WithEventManager(evMgr *events.EventManager) Option {
	return func(d *Domain) {
		d.evMgr = evMgr
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(d *Domain) {
		d.bdl = bdl
	}
}

func WithClusterSvc(clusterSvc clusterpb.ClusterServiceServer) Option {
	return func(d *Domain) {
		d.clusterSvc = clusterSvc
	}
}

// List 查询域名列表
func (d *Domain) List(userID user.ID, orgID uint64, runtimeID uint64) (*apistructs.DomainGroup, error) {
	runtime, err := d.db.GetRuntime(runtimeID)
	if err != nil {
		return nil, apierrors.ErrListDomain.InternalError(err)
	}
	perm, err := d.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  runtime.ApplicationID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, apierrors.ErrListDomain.InternalError(err)
	}
	if !perm.Access {
		return nil, apierrors.ErrListDomain.AccessDenied()
	}
	dc := newCtx(d.db, d.bdl, d.clusterSvc)
	if err := dc.load(runtimeID); err != nil {
		return nil, err
	}
	return dc.GroupDomains(), nil
}

// Update 更新域名
func (d *Domain) Update(userID user.ID, orgID uint64, runtimeID uint64, group *apistructs.DomainGroup) error {
	runtime, err := d.db.GetRuntime(runtimeID)
	if err != nil {
		return apierrors.ErrUpdateDomain.InternalError(err)
	}
	perm, err := d.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  runtime.ApplicationID,
		Resource: "runtime-" + strutil.ToLower(runtime.Workspace),
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return apierrors.ErrUpdateDomain.InternalError(err)
	}
	if !perm.Access {
		return apierrors.ErrUpdateDomain.AccessDenied()
	}
	dc := newCtx(d.db, d.bdl, d.clusterSvc)
	if err := dc.load(runtimeID); err != nil {
		return err
	}
	return dc.UpdateDomains(group)
}
