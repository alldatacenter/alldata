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
	"fmt"
	"reflect"
	"runtime"
	"strings"
	"unicode"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/pkg/transport/interceptor"
)

type (
	// MethodAuditor .
	MethodAuditor struct {
		method      string
		scope       ScopeType
		template    string
		getter      GetScopeIDAndEntries
		options     []Option
		recordError bool
	}
	// GetScopeIDAndEntries .
	GetScopeIDAndEntries func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error)
)

func (a *auditor) Audit(auditors ...*MethodAuditor) transport.ServiceOption {
	methods := make(map[string]*MethodAuditor)
	for _, audit := range auditors {
		if _, ok := methods[audit.method]; ok {
			panic(fmt.Errorf("method %q already exists for audit", audit.method))
		}
		if len(audit.method) <= 0 {
			panic(fmt.Errorf("invalid method %q for audit", audit.method))
		}
		methods[audit.method] = audit
	}
	return transport.WithInterceptors(func(h interceptor.Handler) interceptor.Handler {
		if a.p.Cfg.Skip {
			return h
		}
		return func(ctx context.Context, req interface{}) (interface{}, error) {
			info := transport.ContextServiceInfo(ctx)
			ma := methods[info.Method()]
			if ma == nil {
				return h(ctx, req)
			}
			rec := a.Begin()
			ctxData := &optionContextData{}
			ctx = withOptionDataContext(ctx, ctxData)
			resp, err := h(ctx, req)
			if err == nil || ma.recordError {
				var (
					loaded    bool
					scopeID   interface{}
					entries   map[string]interface{}
					loadError error
				)
				loadScopeIDAndEntries := func() {
					if !loaded {
						loaded = true
						scopeID, entries, loadError = ma.getter(ctx, req, resp, err)
					}
				}
				ctxOpts := ctxData.opts
				opts := make([]Option, 1+len(ma.options)+len(ctxOpts))
				opts[0] = Entries(func(ctx context.Context) (map[string]interface{}, error) {
					loadScopeIDAndEntries()
					return entries, loadError
				})
				copy(opts[1:], ma.options)
				copy(opts[1+len(ma.options):], ctxOpts)
				if err != nil {
					rec.RecordError(ctx, ma.scope, func(ctx context.Context) (interface{}, error) {
						loadScopeIDAndEntries()
						return scopeID, loadError
					}, ma.template, opts...)
				} else {
					rec.Record(ctx, ma.scope, func(ctx context.Context) (interface{}, error) {
						loadScopeIDAndEntries()
						return scopeID, loadError
					}, ma.template, opts...)
				}
			}
			return resp, err
		}
	})
}

// Method .
func Method(method interface{}, scope ScopeType, template string, getter GetScopeIDAndEntries, options ...Option) *MethodAuditor {
	return &MethodAuditor{
		method:   getMethodName(method),
		scope:    scope,
		template: template,
		getter:   getter,
		options:  options,
	}
}

// MethodWithError .
func MethodWithError(method interface{}, scope ScopeType, template string, getter GetScopeIDAndEntries, options ...Option) *MethodAuditor {
	ma := Method(method, scope, template, getter, options...)
	ma.recordError = true
	return ma
}

func getMethodName(method interface{}) string {
	if method == nil {
		return ""
	}
	name, ok := method.(string)
	if ok {
		return name
	}
	name = getMethodFullName(method)
	parts := strings.Split(name, ".")
	if len(parts) < 2 {
		panic(fmt.Errorf("function %s is not method of type", name))
	}
	name = parts[len(parts)-1]
	idx := strings.IndexFunc(name, func(r rune) bool {
		return !unicode.IsLetter(r) && !unicode.IsDigit(r) && r != '_'
	})
	if idx >= 0 {
		return name[:idx]
	}
	return name
}

func getMethodFullName(method interface{}) string {
	if method == nil {
		return ""
	}
	name, ok := method.(string)
	if ok {
		return name
	}
	val := reflect.ValueOf(method)
	if val.Kind() != reflect.Func {
		panic(fmt.Errorf("method %V not function", method))
	}
	fn := runtime.FuncForPC(val.Pointer())
	return fn.Name()
}
