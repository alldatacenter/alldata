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

package audit

import (
	"context"
	"reflect"
	"strconv"
	"time"

	"github.com/bluele/gcache"
	"github.com/recallsong/go-utils/conv"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

// ScopeType .
type ScopeType string

// ScopeType values
const (
	SysScope       ScopeType = "sys"
	OrgScope       ScopeType = "org"
	ProjectScope   ScopeType = "project"
	AppScope       ScopeType = "app"
	PublisherScope ScopeType = "publisher"
)

type (
	// Auditor .
	Auditor interface {
		Recorder
		Begin() Recorder
		ScopeQueryer
		Audit(auditors ...*MethodAuditor) transport.ServiceOption
	}
	// Recorder .
	Recorder interface {
		Record(ctx context.Context, scope ScopeType, scopeID interface{}, template string, options ...Option)
		RecordError(ctx context.Context, scope ScopeType, scopeID interface{}, template string, options ...Option)
	}
	// ValueFetcher .
	ValueFetcher func() interface{}
	// ValueFetcherWithContext .
	ValueFetcherWithContext func(ctx context.Context) (interface{}, error)
	// ScopeQueryer .
	ScopeQueryer interface {
		GetOrg(id interface{}) (*apistructs.OrgDTO, error)
		GetProject(id interface{}) (*apistructs.ProjectDTO, error)
		GetApp(idObject interface{}) (*apistructs.ApplicationDTO, error)
	}
)

// AuditServiceName .
const AuditServiceName = "audit"

// GetAuditor .
func GetAuditor(ctx servicehub.Context) Auditor {
	a := ctx.Service(AuditServiceName)
	if a != nil {
		auditor, ok := a.(Auditor)
		if !ok {
			ctx.Logger().Warnf("service %s is not implement audit.Auditor", AuditServiceName)
		} else {
			return auditor
		}
	}
	ctx.Logger().Debugf("not found audit.Auditor, use NopAuditor")
	return nopAuditor
}

type (
	config struct {
		CacheTTL  time.Duration `file:"cache_ttl" default:"10m"`
		CacheSize int           `file:"cache_size" default:"5000"`
		Skip      bool          `file:"skip"`
	}
	provider struct {
		Cfg   *config
		Log   logs.Logger
		bdl   *bundle.Bundle
		cache gcache.Cache
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.bdl = bundle.New(bundle.WithCoreServices())
	p.cache = gcache.New(p.Cfg.CacheSize).LRU().Expiration(p.Cfg.CacheTTL).LoaderFunc(func(key interface{}) (interface{}, error) {
		if k, ok := key.(cacheKey); ok {
			switch k.scope {
			case OrgScope:
				return p.bdl.GetOrg(k.id)
			case ProjectScope:
				return p.bdl.GetProject(getUint64(k.id))
			case AppScope:
				return p.bdl.GetApp(getUint64(k.id))
			}
		}
		return nil, nil
	}).Build()
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return &auditor{
		ScopeQueryer: p,
		p:            p,
	}
}

func getUint64(v interface{}) uint64 {
	if str, ok := v.(string); ok {
		v, _ := strconv.ParseUint(str, 10, 64)
		return v
	}
	return conv.ToUint64(v, 0)
}

type cacheKey struct {
	scope ScopeType
	id    interface{}
}

func (p *provider) GetOrg(id interface{}) (*apistructs.OrgDTO, error) {
	val, err := p.cache.Get(cacheKey{
		scope: OrgScope,
		id:    id,
	})
	if err != nil || val == nil {
		return nil, err
	}
	return val.(*apistructs.OrgDTO), nil
}

func (p *provider) GetProject(id interface{}) (*apistructs.ProjectDTO, error) {
	val, err := p.cache.Get(cacheKey{
		scope: ProjectScope,
		id:    id,
	})
	if err != nil || val == nil {
		return nil, err
	}
	return val.(*apistructs.ProjectDTO), nil
}

func (p *provider) GetApp(id interface{}) (*apistructs.ApplicationDTO, error) {
	val, err := p.cache.Get(cacheKey{
		scope: AppScope,
		id:    id,
	})
	if err != nil || val == nil {
		return nil, err
	}
	return val.(*apistructs.ApplicationDTO), nil
}

func init() {
	servicehub.Register("audit", &servicehub.Spec{
		Services: []string{AuditServiceName},
		Types: []reflect.Type{
			reflect.TypeOf((*Auditor)(nil)).Elem(),
			reflect.TypeOf((*Recorder)(nil)).Elem(),
		},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
