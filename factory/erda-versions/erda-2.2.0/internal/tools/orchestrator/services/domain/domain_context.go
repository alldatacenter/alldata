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
	context2 "context"
	"fmt"
	"sort"
	"strings"

	"github.com/pkg/errors"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	DefaultDomainType = "DEFAULT"
	CustomDomainType  = "CUSTOM"
)

type context struct {
	Runtime    *dbclient.Runtime
	Cluster    *clusterpb.ClusterInfo
	RootDomain string
	Domains    []dbclient.RuntimeDomain

	// db and clients and etc.
	db         *dbclient.DBClient
	bdl        *bundle.Bundle
	clusterSvc clusterpb.ClusterServiceServer
}

func newCtx(db *dbclient.DBClient, bdl *bundle.Bundle, clusterSvc clusterpb.ClusterServiceServer) *context {
	return &context{
		db:         db,
		bdl:        bdl,
		clusterSvc: clusterSvc,
	}
}

func (ctx *context) load(runtimeId uint64) error {
	runtime, err := ctx.db.GetRuntime(runtimeId)
	if err != nil {
		return err
	}
	getClusterCtx := transport.WithHeader(context2.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
	resp, err := ctx.clusterSvc.GetCluster(getClusterCtx, &clusterpb.GetClusterRequest{IdOrName: runtime.ClusterName})
	if err != nil {
		return err
	}
	cluster := resp.Data
	rootDomains := strings.Split(cluster.WildcardDomain, ",")
	if len(rootDomains) == 0 {
		return errors.Errorf("集群未配置域名")
	}
	domains, err := ctx.db.FindDomainsByRuntimeId(runtimeId)
	if err != nil {
		return err
	}
	ctx.Runtime = runtime
	ctx.Cluster = cluster
	ctx.RootDomain = "." + rootDomains[0]
	ctx.Domains = domains
	return nil
}

func (ctx *context) GroupDomains() *apistructs.DomainGroup {
	group := make(apistructs.DomainGroup)
	sort.Slice(ctx.Domains, func(i, j int) bool {
		if ctx.Domains[i].DomainType == ctx.Domains[j].DomainType {
			return ctx.Domains[i].Domain < ctx.Domains[j].Domain
		}
		return ctx.Domains[i].DomainType == DefaultDomainType // DEFAULT is less
	})
	for _, d := range ctx.Domains {
		group[d.EndpointName] = append(group[d.EndpointName], convertDomainDTO(&d, ctx.RootDomain))
	}
	return &group
}

func convertDomainDTO(d *dbclient.RuntimeDomain, rootDomain string) *apistructs.Domain {
	if d == nil {
		return nil
	}
	item := apistructs.Domain{
		DomainID:   d.ID,
		AppName:    d.EndpointName,
		DomainType: d.DomainType,
		Domain:     d.Domain,
	}
	if d.DomainType == DefaultDomainType {
		item.CustomDomain = strings.TrimSuffix(d.Domain, rootDomain)
		item.RootDomain = rootDomain
	}
	return &item
}

func (ctx *context) UpdateDomains(group *apistructs.DomainGroup) error {
	list := make([]*apistructs.Domain, 0)
	for serviceName, l := range *group {
		for _, item := range l {
			item.AppName = serviceName
			list = append(list, item)
		}
	}

	// check request format
	for _, item := range list {
		if item.DomainType == DefaultDomainType {
			// check default domain
			if item.CustomDomain == "" {
				return apierrors.ErrUpdateDomain.InvalidParameter(strutil.Concat("默认域名为空: ", item.AppName))
			}
			item.Domain = item.CustomDomain + ctx.RootDomain
			continue
		}
		// check custom domain
		item.DomainType = CustomDomainType
		if item.Domain == "" {
			return apierrors.ErrUpdateDomain.InvalidParameter(strutil.Concat("自定义域名为空: ", item.AppName))
		}
	}

	// check domain not occupied by other runtime
	domainValues := make([]string, 0)
	for _, item := range list {
		domainValues = append(domainValues, item.Domain)
	}
	exists, err := ctx.db.FindDomains(domainValues)
	if err != nil {
		return apierrors.ErrUpdateDomain.InternalError(err)
	}
	for _, e := range exists {
		if e.RuntimeId != ctx.Runtime.ID {
			return apierrors.ErrUpdateDomain.InvalidState(
				fmt.Sprintf("域名 %s 已被 Runtime %d:%s 使用", e.Domain, e.RuntimeId, e.EndpointName))
		}
	}

	// check domain not duplicated in group
	mp := make(map[string]struct{})
	for _, item := range list {
		if _, exists := mp[item.Domain]; exists {
			return apierrors.ErrUpdateDomain.InvalidParameter(fmt.Sprintf("域名 %s 重复使用", item.Domain))
		}
		mp[item.Domain] = struct{}{}
	}

	beforeMap := make(map[string]*dbclient.RuntimeDomain)
	for i := range ctx.Domains {
		beforeMap[ctx.Domains[i].Domain] = &ctx.Domains[i]
	}
	afterMap := make(map[string]struct{})
	for i := range list {
		afterMap[list[i].Domain] = struct{}{}
	}
	for _, item := range list {
		domain, exist := beforeMap[item.Domain]
		if !exist {
			domain = &dbclient.RuntimeDomain{
				Domain: item.Domain,
			}
		}
		if domain.EndpointName == item.AppName &&
			domain.DomainType == item.DomainType &&
			domain.RuntimeId == ctx.Runtime.ID {
			// no need to update
			continue
		}
		domain.EndpointName = item.AppName
		domain.DomainType = item.DomainType
		domain.RuntimeId = ctx.Runtime.ID

		if err := ctx.db.SaveDomain(domain); err != nil {
			return apierrors.ErrUpdateDomain.InternalError(err)
		}
	}
	// clear useless domains
	for _, domain := range ctx.Domains {
		if _, exist := afterMap[domain.Domain]; exist {
			continue
		}
		if err := ctx.db.DeleteDomain(domain.Domain); err != nil {
			return apierrors.ErrUpdateDomain.InternalError(err)
		}
	}
	return nil
}
