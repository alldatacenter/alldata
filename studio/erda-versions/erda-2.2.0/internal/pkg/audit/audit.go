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
	"strconv"
	"time"

	"github.com/recallsong/go-utils/conv"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/pkg/transport/interceptor"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/common/apis"
)

// NopAuditor .
type NopAuditor struct{}

var nopAuditor Auditor = &NopAuditor{}

// Record .
func (a *NopAuditor) Record(ctx context.Context, scope ScopeType, scopeID interface{}, template string, options ...Option) {
}

// RecordError .
func (a *NopAuditor) RecordError(ctx context.Context, scope ScopeType, scopeID interface{}, template string, options ...Option) {
}

// Begin .
func (a *NopAuditor) Begin() Recorder { return a }

// GetOrg .
func (a *NopAuditor) GetOrg(id interface{}) (*apistructs.OrgDTO, error) { return nil, nil }

// GetProject .
func (a *NopAuditor) GetProject(id interface{}) (*apistructs.ProjectDTO, error) { return nil, nil }

// GetApp .
func (a *NopAuditor) GetApp(idObject interface{}) (*apistructs.ApplicationDTO, error) {
	return nil, nil
}

// Audit .
func (a *NopAuditor) Audit(auditors ...*MethodAuditor) transport.ServiceOption {
	return transport.WithInterceptors(func(h interceptor.Handler) interceptor.Handler { return h })
}

type auditor struct {
	ScopeQueryer
	p         *provider
	beginTime time.Time
}

var _ Auditor = (*auditor)(nil)

func (a *auditor) Record(ctx context.Context, scope ScopeType, scopeID interface{}, template string, options ...Option) {
	a.record(ctx, scope, scopeID, template, options, "success")
}

func (a *auditor) RecordError(ctx context.Context, scope ScopeType, scopeID interface{}, template string, options ...Option) {
	a.record(ctx, scope, scopeID, template, options, "failure")
}

func (a *auditor) Begin() Recorder {
	return &auditor{
		ScopeQueryer: a.ScopeQueryer,
		p:            a.p,
		beginTime:    time.Now(),
	}
}

func (a *auditor) record(ctx context.Context, scope ScopeType, scopeID interface{}, template string, options []Option, result string) {
	defer func() {
		err := recover()
		if err != nil {
			a.p.Log.Error(err)
		}
	}()
	opts := newOptions()
	for _, op := range options {
		op(opts)
	}
	var data map[string]interface{}
	if opts.getEntries != nil {
		m, err := opts.getEntries(ctx)
		if err != nil {
			a.p.Log.Error(err)
			return
		}
		data = m
	} else {
		data = make(map[string]interface{})
	}
	for entry := opts.entries; entry != nil; {
		val, err := getValue(ctx, entry.val)
		if err != nil {
			a.p.Log.Error(err)
			return
		}
		if entry.key == "isSkip" && val == true {
			return
		}
		data[entry.key] = val
		entry = entry.prev
	}

	userID := opts.getUserID(ctx)
	orgID := opts.getOrgID(ctx)
	now := time.Now().Unix()
	beginTime := a.beginTime.Unix()
	if beginTime <= 0 {
		beginTime = now
	}
	audit := apistructs.Audit{
		UserID:       userID,
		OrgID:        uint64(orgID),
		ScopeType:    apistructs.ScopeType(scope),
		Context:      data,
		TemplateName: apistructs.TemplateName(template),
		Result:       apistructs.Result(result),
		StartTime:    strconv.FormatInt(beginTime, 10),
		EndTime:      strconv.FormatInt(now, 10),
		ClientIP:     apis.GetClientIP(ctx),
		UserAgent:    apis.GetHeader(ctx, "User-Agent"),
	}

	scopeid, err := getValue(ctx, scopeID)
	if err != nil {
		a.p.Log.Error(err)
		return
	}
	idstr, ok := scopeid.(string)
	if ok {
		scopeid, err = strconv.Atoi(idstr)
		if err != nil {
			a.p.Log.Errorf("scopeId failed to parse int info: %s", err)
		}
	}
	audit.ScopeID = conv.ToUint64(scopeid, 0)
	if err := a.setupScopeInfo(ctx, opts, &audit); err != nil {
		a.p.Log.Errorf("failed to get scope info: %s", err)
	}

	err = a.p.bdl.CreateAuditEvent(&apistructs.AuditCreateRequest{
		Audit: audit,
	})
	if err != nil {
		a.p.Log.Errorf("failed to CreateAuditEvent: %s", err)
	}
}

func getValue(ctx context.Context, val interface{}) (interface{}, error) {
	switch v := val.(type) {
	case ValueFetcher:
		return v(), nil
	case func() interface{}:
		return v(), nil
	case ValueFetcherWithContext:
		return v(ctx)
	case func(ctx context.Context) (interface{}, error):
		return v(ctx)
	}
	return val, nil
}

func (a *auditor) setupScopeInfo(ctx context.Context, opts *options, audit *apistructs.Audit) error {
	switch audit.ScopeType {
	case apistructs.OrgScope:
		if audit.OrgID == 0 {
			audit.OrgID = audit.ScopeID
		}
		if _, ok := audit.Context[OrgIDKey]; !ok {
			audit.Context[OrgIDKey] = audit.OrgID
		}
		if _, ok := audit.Context[OrgNameKey]; !ok {
			org, err := a.ScopeQueryer.GetOrg(audit.ScopeID)
			if err != nil {
				return err
			}
			audit.Context[OrgNameKey] = org.Name
		}
	case apistructs.ProjectScope:
		if audit.ProjectID == 0 {
			audit.ProjectID = audit.ScopeID
		}
		if _, ok := audit.Context[ProjectIDKey]; !ok {
			audit.Context[ProjectIDKey] = audit.ProjectID
		}
		if _, ok := audit.Context[ProjectNameKey]; !ok {
			project, err := a.ScopeQueryer.GetProject(audit.ScopeID)
			if err != nil {
				return err
			}
			audit.Context[ProjectNameKey] = project.Name
		}
	case apistructs.AppScope:
		if audit.AppID == 0 || audit.ProjectID == 0 {
			app, err := a.ScopeQueryer.GetApp(audit.ScopeID)
			if err != nil {
				return err
			}
			audit.ProjectID = app.ProjectID
			audit.AppID = audit.ScopeID
			project, err := a.ScopeQueryer.GetProject(app.ProjectID)
			if err != nil {
				return err
			}
			if _, ok := audit.Context[ProjectIDKey]; !ok {
				audit.Context[ProjectIDKey] = audit.ProjectID
			}
			if _, ok := audit.Context[AppIDKey]; !ok {
				audit.Context[AppIDKey] = audit.AppID
			}
			if _, ok := audit.Context[ProjectNameKey]; !ok {
				audit.Context[ProjectNameKey] = project.Name
			}
			if _, ok := audit.Context[AppNameKey]; !ok {
				audit.Context[AppNameKey] = app.Name
			}
		} else {
			if _, ok := audit.Context[ProjectIDKey]; !ok {
				audit.Context[ProjectIDKey] = audit.ProjectID
			}
			if _, ok := audit.Context[AppIDKey]; !ok {
				audit.Context[AppIDKey] = audit.AppID
			}
			if _, ok := audit.Context[ProjectNameKey]; !ok {
				project, err := a.ScopeQueryer.GetProject(audit.ProjectID)
				if err != nil {
					return err
				}
				audit.Context[ProjectNameKey] = project.Name
			}
			if _, ok := audit.Context[AppNameKey]; !ok {
				app, err := a.ScopeQueryer.GetApp(audit.AppID)
				if err != nil {
					return err
				}
				audit.Context[AppNameKey] = app.Name
			}
		}
	}
	return nil
}
