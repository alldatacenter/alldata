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

package permission

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/apps/msp/instance"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	httpperm "github.com/erda-project/erda/internal/tools/monitor/common/permission"
	"github.com/erda-project/erda/pkg/common/errors"
	"github.com/erda-project/erda/pkg/common/permission"
)

// Interface .
type Interface interface {
	TenantToProjectID(tgroup, tenantID string) permission.ValueGetter
	TerminusKeyToProjectID(key string) permission.ValueGetter
	TerminusKeyToProjectIDForHTTP(keys ...string) httpperm.ValueGetter
}

// TenantToProjectID .
func (p *provider) TenantToProjectID(tgroup, tenantID string) permission.ValueGetter {
	groupGetter := permission.FieldValue(tgroup)
	tenantGetter := permission.FieldValue(tenantID)
	return func(ctx context.Context, req interface{}) (string, error) {
		tg, _ := groupGetter(ctx, req)
		tID, _ := tenantGetter(ctx, req)
		idByTg, _ := p.getProjectIDByGroupIDOrTenantID(tg)
		if idByTg == "" {
			idByTID, err := p.getProjectIDByGroupIDOrTenantID(tID)
			if err != nil {
				return "", err
			}
			return idByTID, nil
		}
		return idByTg, nil
	}
}

func (p *provider) getProjectIDByGroupIDOrTenantID(id string) (string, error) {
	if id == "" {
		return "", errors.NewNotFoundError(id)
	}
	projectID, _ := p.getProjectIDByGroupID(id)
	if projectID == "" {
		return p.getProjectIDByTenantID(id)
	}
	return projectID, nil
}

func (p *provider) getProjectIDByGroupID(group string) (string, error) {
	id, err := p.getProjectIdByMSPTenantID(group)
	if err != nil {
		return "", errors.NewDatabaseError(err)
	}
	if id != "" {
		return id, nil
	}

	tenant, err := p.instanceTenantDB.GetInstanceByTenantGroup(group)
	if err != nil {
		return "", errors.NewDatabaseError(err)
	}
	tmc, err := p.tmcDB.GetByEngine(tenant.Engine)
	if err != nil {
		return "", errors.NewDatabaseError(err)
	}
	if tmc == nil {
		return "", errors.NewDatabaseError(err)
	}
	if strings.EqualFold(tmc.ServiceType, string(instance.ServiceTypeMicroService)) {
		id := p.getProjectIDByTenant(tenant)
		if len(id) > 0 {
			return id, nil
		}
	}

	monitor, err := p.monitorDB.GetByFields(map[string]interface{}{
		"MonitorId": tenant.ID,
	})
	if monitor != nil {
		return monitor.ProjectId, nil
	}
	if err != nil {
		return "", errors.NewDatabaseError(err)
	}

	return "", errors.NewNotFoundError(group)
}

func (p *provider) getProjectIDByTenantID(id string) (string, error) {
	pid, err := p.getProjectIdByMSPTenantID(id)
	if err != nil {
		return "", errors.NewDatabaseError(err)
	}
	if pid != "" {
		return id, nil
	}

	tenant, err := p.instanceTenantDB.GetByID(id)
	if err != nil {
		return "", errors.NewDatabaseError(err)
	}
	if tenant == nil {
		return "", fmt.Errorf("fail to find tenant by id %q", id)
	}
	projectId := p.getProjectIDByTenant(tenant)
	if len(projectId) <= 0 {
		return "", fmt.Errorf("fail to find project id by tenant %q", tenant.ID)
	}
	return projectId, nil
}

func (p *provider) getProjectIdByMSPTenantID(id string) (string, error) {
	mspTenant, err := p.MSPTenantDB.QueryTenant(id)
	if err != nil {
		return "", err
	}
	if mspTenant != nil {
		return mspTenant.RelatedProjectId, nil
	}
	return "", nil
}

func (p *provider) getProjectIDByTenant(tenant *instancedb.InstanceTenant) string {
	if len(tenant.Options) <= 0 {
		return ""
	}
	options := make(map[string]interface{})
	json.Unmarshal([]byte(tenant.Options), &options)
	pid := options["projectId"]
	if pid == nil {
		return ""
	}
	return fmt.Sprint(pid)
}

func (p *provider) TerminusKeyToProjectID(terminusKey string) permission.ValueGetter {
	tkGetter := permission.FieldValue(terminusKey)
	return func(ctx context.Context, req interface{}) (string, error) {
		tk, err := tkGetter(ctx, req)
		if err != nil {
			return "", err
		}
		m, err := p.monitorDB.GetByTerminusKey(tk)
		if err != nil {
			return "", errors.NewDatabaseError(err)
		}
		return m.ProjectId, nil
	}
}

func (p *provider) TerminusKeyToProjectIDForHTTP(keys ...string) httpperm.ValueGetter {
	return func(ctx httpserver.Context) (string, error) {
		req := ctx.Request()
		for _, key := range keys {
			key := req.URL.Query().Get(key)
			if len(key) <= 0 {
				continue
			}
			id, err := p.getProjectIdByMSPTenantID(key)
			if err != nil {
				return "", errors.NewDatabaseError(err)
			}
			if id != "" {
				return id, nil
			}

			m, err := p.monitorDB.GetByTerminusKey(key)
			if err != nil {
				return "", fmt.Errorf("fail to get monitor: %s", err)
			}
			if m == nil || m.IsDelete > 0 {
				return "", fmt.Errorf("monitor not found")
			}
			return m.ProjectId, nil
		}
		return "", fmt.Errorf("terminus key not found")
	}
}
