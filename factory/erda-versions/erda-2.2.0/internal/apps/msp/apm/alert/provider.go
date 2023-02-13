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

package alert

import (
	"context"
	"strings"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/mysql"
	monitor "github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	alert "github.com/erda-project/erda-proto-go/msp/apm/alert/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	mperm "github.com/erda-project/erda/internal/apps/msp/instance/permission"
	db2 "github.com/erda-project/erda/internal/apps/msp/tenant/db"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/internal/tools/monitor/common/db"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type provider struct {
	C            *config
	DB           *gorm.DB
	Register     transport.Register `autowired:"service-register" optional:"true"`
	Perm         perm.Interface     `autowired:"permission"`
	MPerm        mperm.Interface    `autowired:"msp.permission"`
	alertService *alertService
	Monitor      monitor.AlertServiceServer `autowired:"erda.core.monitor.alert.AlertService" optional:"true"`
	authDb       *db.DB
	mspDb        *db2.DB
	bdl          *bundle.Bundle
	audit        audit.Auditor
	Tenant       tenantpb.TenantServiceServer `autowired:"erda.msp.tenant.TenantService"`

	microServiceFilterTags map[string]bool
}

type config struct {
	MicroServiceFilterTags string `file:"micro_service_filter_tags"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.audit = audit.GetAuditor(ctx)
	p.microServiceFilterTags = make(map[string]bool)
	for _, k := range strings.Split(p.C.MicroServiceFilterTags, ",") {
		k = strings.TrimSpace(k)
		if len(k) > 0 {
			p.microServiceFilterTags[k] = true
		}
	}
	p.authDb = db.New(ctx.Service("mysql").(mysql.Interface).DB())
	p.mspDb = db2.New(ctx.Service("mysql").(mysql.Interface).DB())
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())
	p.alertService = &alertService{
		p: p,
	}
	if p.Register != nil {
		type AlertService = alert.AlertServiceServer
		alert.RegisterAlertServiceImp(p.Register, p.alertService, apis.Options(), p.Perm.Check(
			perm.Method(AlertService.QueryAlertRule, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionList, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.QueryAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionList, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.GetAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionGet, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.CreateAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionCreate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.UpdateAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionUpdate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.UpdateAlertEnable, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionUpdate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.DeleteAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionDelete, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),

			perm.Method(AlertService.QueryCustomizeMetric, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionGet, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.QueryCustomizeNotifyTarget, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionGet, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.QueryCustomizeAlerts, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionList, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.GetCustomizeAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionGet, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.CreateCustomizeAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionCreate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.UpdateCustomizeAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionUpdate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.UpdateCustomizeAlertEnable, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionUpdate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.DeleteCustomizeAlert, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionDelete, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),

			perm.Method(AlertService.GetAlertRecordAttrs, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionList, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.GetAlertRecords, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionList, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.GetAlertRecord, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionGet, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.GetAlertHistories, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionList, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.CreateAlertRecordIssue, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionCreate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.UpdateAlertRecordIssue, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionUpdate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.Method(AlertService.DashboardPreview, perm.ScopeProject, perm.MonitorProjectAlert, perm.ActionUpdate, p.MPerm.TenantToProjectID("TenantGroup", "TenantID")),
			perm.NoPermMethod(AlertService.GetAlertConditions),
			perm.NoPermMethod(AlertService.GetAlertConditionsValue),
		),
			p.audit.Audit(
				//audit.Method(AlertService.UpdateCustomizeAlert, audit.ProjectScope, string(apistructs.UpdateMicroserviceCustomAlert),
				//	func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
				//		r := resp.(*alert.UpdateCustomizeAlertResponse)
				//		return r.Data, map[string]interface{}{}, nil
				//	},
				//),
				//audit.Method(AlertService.DeleteCustomizeAlert, audit.ProjectScope, string(apistructs.DeleteMicroserviceCustomAlert),
				//	func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
				//		r := resp.(*alert.DeleteCustomizeAlertResponse)
				//		return r.Data.ProjectId, map[string]interface{}{}, nil
				//	},
				//),
				audit.Method(AlertService.CreateCustomizeAlert, audit.ProjectScope, string(apistructs.CreateMicroserviceCustomAlert),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*alert.CreateCustomizeAlertResponse)
						return r.Data.ProjectId, map[string]interface{}{}, nil
					},
				),
				audit.Method(AlertService.CreateAlert, audit.ProjectScope, string(apistructs.CreateMicroserviceAlert),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*alert.CreateAlertResponse)
						return r.Data.ProjectId, map[string]interface{}{}, nil
					},
				),
				audit.Method(AlertService.UpdateAlert, audit.ProjectScope, string(apistructs.UpdateMicroserviceAlert),
					p.alertService.auditOperateMicroserviceAlert(apistructs.UpdateMicroserviceAlert, "update"),
				),
				audit.Method(AlertService.DeleteAlert, audit.ProjectScope, string(apistructs.DeleteMicroserviceAlert),
					p.alertService.auditOperateMicroserviceAlert(apistructs.DeleteMicroserviceAlert, "delete"),
				),
				audit.Method(AlertService.UpdateAlertEnable, audit.ProjectScope, string(apistructs.SwitchMicroserviceAlert),
					p.alertService.auditOperateMicroserviceAlert(apistructs.SwitchMicroserviceAlert, ""),
				),

				audit.Method(AlertService.UpdateCustomizeAlert, audit.ProjectScope, string(apistructs.UpdateMicroserviceCustomAlert),
					p.alertService.auditOperateMicroserviceCustomAlert(apistructs.UpdateMicroserviceCustomAlert, "update"),
				),

				audit.Method(AlertService.DeleteCustomizeAlert, audit.ProjectScope, string(apistructs.DeleteMicroserviceCustomAlert),
					p.alertService.auditOperateMicroserviceCustomAlert(apistructs.DeleteMicroserviceCustomAlert, "delete"),
				),

				audit.Method(AlertService.UpdateCustomizeAlertEnable, audit.ProjectScope, string(apistructs.SwitchMicroserviceCustomAlert),
					p.alertService.auditOperateMicroserviceCustomAlert(apistructs.SwitchMicroserviceCustomAlert, ""),
				),
			),
		)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.alert.AlertService" || ctx.Type() == alert.AlertServiceServerType() || ctx.Type() == alert.AlertServiceHandlerType():
		return p.alertService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.apm.alert", &servicehub.Spec{
		Services:             alert.ServiceNames(),
		Types:                alert.Types(),
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
