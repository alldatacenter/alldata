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

package apis

import (
	"context"
	"fmt"
	"io/ioutil"
	"strings"
	"time"

	"github.com/ghodss/yaml"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/cassandra"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-infra/providers/mysql"
	channelpb "github.com/erda-project/erda-proto-go/core/messenger/notifychannel/pb"
	"github.com/erda-project/erda-proto-go/core/monitor/alert/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/internal/pkg/bundle-ex/cmdb"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/adapt"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/cql"
	"github.com/erda-project/erda/internal/tools/monitor/core/alert/alert-apis/db"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
	"github.com/erda-project/erda/internal/tools/monitor/core/event/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type config struct {
	OrgFilterTags               string `file:"org_filter_tags"`
	MicroServiceFilterTags      string `file:"micro_service_filter_tags"`
	MicroServiceOtherFilterTags string `file:"micro_service_other_filter_tags"`
	SilencePolicy               string `file:"silence_policy"`
	AlertConditions             string `file:"alert_conditions"`
	Cassandra                   struct {
		Enabled                 bool `file:"enabled"`
		cassandra.SessionConfig `file:"session"`
		GCGraceSeconds          int `file:"gc_grace_seconds" default:"86400"`
	} `file:"cassandra"`
}

type provider struct {
	C                           *config
	L                           logs.Logger
	metricq                     metricq.Queryer `autowired:"metrics-query" optional:"true"`
	EventStorage                storage.Storage `autowired:"event-storage-elasticsearch-reader" optional:"true"`
	t                           i18n.Translator
	db                          *db.DB
	cql                         *cql.Cql
	a                           *adapt.Adapt
	bdl                         *bundle.Bundle
	cmdb                        *cmdb.Cmdb
	silencePolicies             map[string]bool
	orgFilterTags               map[string]bool
	microServiceFilterTags      map[string]bool
	microServiceOtherFilterTags map[string]bool
	alertConditions             []*AlertConditions
	audit                       audit.Auditor

	Register      transport.Register           `autowired:"service-register" optional:"true"`
	Metric        metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
	Perm          perm.Interface               `autowired:"permission"`
	alertService  *alertService
	NotifyChannel channelpb.NotifyChannelServiceServer `autowired:"erda.core.messenger.notifychannel.NotifyChannelService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.audit = audit.GetAuditor(ctx)
	p.silencePolicies = make(map[string]bool)
	p.orgFilterTags = make(map[string]bool)
	p.microServiceFilterTags = make(map[string]bool)
	p.microServiceOtherFilterTags = make(map[string]bool)
	for _, k := range strings.Split(p.C.OrgFilterTags, ",") {
		k = strings.TrimSpace(k)
		if len(k) > 0 {
			p.orgFilterTags[k] = true
		}
	}
	for _, k := range strings.Split(p.C.MicroServiceFilterTags, ",") {
		k = strings.TrimSpace(k)
		if len(k) > 0 {
			p.microServiceFilterTags[k] = true
		}
	}
	for _, k := range strings.Split(p.C.SilencePolicy, ",") {
		k = strings.TrimSpace(k)
		if len(k) > 0 {
			p.silencePolicies[k] = true
		}
	}
	for _, k := range strings.Split(p.C.MicroServiceOtherFilterTags, ",") {
		k = strings.TrimSpace(k)
		if len(k) > 0 {
			p.microServiceOtherFilterTags[k] = true
		}
	}
	p.alertConditions = make([]*AlertConditions, 0)
	f, err := ioutil.ReadFile(p.C.AlertConditions)
	if err != nil {
		return err
	}
	err = yaml.Unmarshal(f, &p.alertConditions)
	if err != nil {
		return err
	}

	if p.C.Cassandra.Enabled {
		cassandra, ok := ctx.Service("cassandra").(cassandra.Interface)
		if ok && cassandra != nil {
			session, err := cassandra.NewSession(&p.C.Cassandra.SessionConfig)
			if err != nil {
				return fmt.Errorf("fail to create cassandra session: %s", err)
			}
			p.cql = cql.New(session.Session())
			if err := p.cql.Init(p.L, p.C.Cassandra.GCGraceSeconds); err != nil {
				return fmt.Errorf("fail to init cassandra: %s", err)
			}
		}
	}

	p.t = ctx.Service("i18n").(i18n.I18n).Translator("alert")
	p.db = db.New(ctx.Service("mysql").(mysql.Interface).DB())
	p.metricq = ctx.Service("metrics-query").(metricq.Queryer)
	hc := httpclient.New(httpclient.WithTimeout(time.Second, time.Second*60))
	p.cmdb = cmdb.New(cmdb.WithHTTPClient(hc))
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())

	dashapi := ctx.Service("chart-block").(block.DashboardAPI)
	p.a = adapt.New(p.L, p.metricq, p.EventStorage, p.t, p.db, p.cql, p.bdl, p.cmdb, dashapi, p.orgFilterTags, p.microServiceFilterTags, p.microServiceOtherFilterTags, p.silencePolicies)

	p.alertService = &alertService{
		p: p,
	}

	if p.Register != nil {
		type MonitorService = pb.AlertServiceServer
		pb.RegisterAlertServiceImp(p.Register, p.alertService, apis.Options(), p.Perm.Check(
			perm.NoPermMethod(MonitorService.QueryCustomizeMetric),
			perm.NoPermMethod(MonitorService.QueryCustomizeNotifyTarget),
			perm.NoPermMethod(MonitorService.QueryCustomizeAlert),
			perm.NoPermMethod(MonitorService.GetCustomizeAlert),
			perm.NoPermMethod(MonitorService.GetCustomizeAlertDetail),
			perm.NoPermMethod(MonitorService.CreateCustomizeAlert),
			perm.NoPermMethod(MonitorService.UpdateCustomizeAlert),
			perm.NoPermMethod(MonitorService.UpdateCustomizeAlertEnable),
			perm.NoPermMethod(MonitorService.DeleteCustomizeAlert),
			perm.Method(MonitorService.QueryOrgCustomizeMetric, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.QueryOrgCustomizeNotifyTarget, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.QueryOrgCustomizeAlerts, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.GetOrgCustomizeAlertDetail, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.CreateOrgCustomizeAlert, perm.ScopeOrg, "monitor_org_alert", perm.ActionCreate, perm.OrgIDValue()),
			perm.Method(MonitorService.UpdateOrgCustomizeAlert, perm.ScopeOrg, "monitor_org_alert", perm.ActionUpdate, perm.OrgIDValue()),
			perm.Method(MonitorService.UpdateOrgCustomizeAlertEnable, perm.ScopeOrg, "monitor_org_alert", perm.ActionUpdate, perm.OrgIDValue()),
			perm.Method(MonitorService.DeleteOrgCustomizeAlert, perm.ScopeOrg, "monitor_org_alert", perm.ActionDelete, perm.OrgIDValue()),
			perm.NoPermMethod(MonitorService.QueryDashboardByAlert),
			perm.Method(MonitorService.QueryOrgDashboardByAlert, perm.ScopeOrg, "monitor_org_alert", perm.ActionCreate, perm.OrgIDValue()),
			perm.NoPermMethod(MonitorService.QueryAlertRule),
			perm.NoPermMethod(MonitorService.QueryAlert),
			perm.NoPermMethod(MonitorService.GetAlert),
			perm.NoPermMethod(MonitorService.GetAlertDetail),
			perm.NoPermMethod(MonitorService.CreateAlert),
			perm.NoPermMethod(MonitorService.UpdateAlert),
			perm.NoPermMethod(MonitorService.UpdateAlertEnable),
			perm.NoPermMethod(MonitorService.DeleteAlert),
			perm.NoPermMethod(MonitorService.GetRawAlertExpression),
			perm.Method(MonitorService.QueryOrgAlertRule, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.QueryOrgAlert, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.GetOrgAlertDetail, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.CreateOrgAlert, perm.ScopeOrg, "monitor_org_alert", perm.ActionCreate, perm.OrgIDValue()),
			perm.Method(MonitorService.UpdateOrgAlert, perm.ScopeOrg, "monitor_org_alert", perm.ActionUpdate, perm.OrgIDValue()),
			perm.Method(MonitorService.UpdateOrgAlertEnable, perm.ScopeOrg, "monitor_org_alert", perm.ActionUpdate, perm.OrgIDValue()),
			perm.Method(MonitorService.DeleteOrgAlert, perm.ScopeOrg, "monitor_org_alert", perm.ActionDelete, perm.OrgIDValue()),
			perm.NoPermMethod(MonitorService.GetAlertRecordAttr),
			perm.NoPermMethod(MonitorService.QueryAlertRecord),
			perm.NoPermMethod(MonitorService.GetAlertRecord),
			perm.NoPermMethod(MonitorService.QueryAlertHistory),
			perm.NoPermMethod(MonitorService.CreateAlertIssue),
			perm.NoPermMethod(MonitorService.UpdateAlertIssue),
			perm.Method(MonitorService.GetOrgAlertRecordAttr, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.QueryOrgAlertRecord, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.QueryOrgHostsAlertRecord, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.GetOrgAlertRecord, perm.ScopeOrg, "monitor_org_alert", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(MonitorService.QueryOrgAlertHistory, perm.ScopeOrg, "monitor_org_alert", perm.ActionList, perm.OrgIDValue()),
			perm.Method(MonitorService.CreateOrgAlertIssue, perm.ScopeOrg, "monitor_org_alert", perm.ActionCreate, perm.OrgIDValue()),
			perm.Method(MonitorService.UpdateOrgAlertIssue, perm.ScopeOrg, "monitor_org_alert", perm.ActionUpdate, perm.OrgIDValue()),
			perm.NoPermMethod(MonitorService.GetAlertConditions),
			perm.NoPermMethod(MonitorService.GetAlertConditionsValue),
			perm.NoPermMethod(MonitorService.GetAlertEvents),
			perm.NoPermMethod(MonitorService.SuppressAlertEvent),
			perm.NoPermMethod(MonitorService.CancelSuppressAlertEvent),
		),
			p.audit.Audit(
				audit.Method(MonitorService.UpdateOrgCustomizeAlert, audit.OrgScope, string(apistructs.UpdateOrgCustomAlert),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(MonitorService.CreateOrgCustomizeAlert, audit.OrgScope, string(apistructs.CreateOrgCustomAlert),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(MonitorService.DeleteOrgCustomizeAlert, audit.OrgScope, string(apistructs.DeleteOrgCustomAlert),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(MonitorService.CreateOrgAlert, audit.OrgScope, string(apistructs.CreateOrgAlert),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(MonitorService.UpdateOrgAlert, audit.OrgScope, string(apistructs.UpdateOrgAlert),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),
				audit.Method(MonitorService.DeleteOrgAlert, audit.OrgScope, string(apistructs.DeleteOrgAlert),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						return apis.GetOrgID(ctx), map[string]interface{}{}, nil
					},
				),

				audit.Method(MonitorService.UpdateOrgAlertEnable, audit.OrgScope, string(apistructs.SwitchOrgAlert), p.alertService.auditOperateOrgAlert("")),
				audit.Method(MonitorService.UpdateOrgCustomizeAlertEnable, audit.OrgScope, string(apistructs.SwitchOrgCustomAlert), p.alertService.auditOperateOrgCustomAlert(apistructs.SwitchOrgCustomAlert, "")),
			),
		)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.monitor.alert" || ctx.Type() == pb.AlertServiceServerType() || ctx.Type() == pb.AlertServiceHandlerType():
		return p.alertService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.monitor.alert", &servicehub.Spec{
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
