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

package index

import (
	"fmt"
	"strings"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
)

// FindElection .
func FindElection(ctx servicehub.Context, required bool) (election.Interface, error) {
	const service = "etcd-election"
	var obj interface{}
	var name string
	if len(ctx.Label()) > 0 {
		name = service + "@" + ctx.Label()
		obj = ctx.Service(name)
	}
	if obj == nil {
		name = service + "@index"
		obj = ctx.Service(name)
	}
	if obj == nil {
		name = service
		obj = ctx.Service(name)
	}
	if obj != nil {
		election, ok := obj.(election.Interface)
		if !ok {
			return nil, fmt.Errorf("%q is not election.Interface", name)
		}
		ctx.Logger().Debugf("use Election(%q) for index clean", name)
		return election, nil
	} else if required {
		return nil, fmt.Errorf("%q is required", service)
	}
	return nil, nil
}

// FindElasticSearch .
func FindElasticSearch(ctx servicehub.Context, required bool) (elasticsearch.Interface, error) {
	service, name := FindService(ctx, "elasticsearch")
	ctx.Logger().Debugf("use ElasticSearch(%q)", name)
	es, _ := service.(elasticsearch.Interface)
	if es == nil && required {
		return nil, fmt.Errorf("%q is required", "elasticsearch")
	}
	return es, nil
}

// FindService .
func FindService(ctx servicehub.Context, service string) (interface{}, string) {
	name := service
	if len(ctx.Label()) > 0 {
		name = name + "@" + ctx.Label()
	}
	obj := ctx.Service(name)
	if obj == nil {
		obj = ctx.Service(service)
	}
	return obj, name
}

var keyReplacer = strings.NewReplacer(
	"-", "_",
	".", "_",
)

// NormalizeKey .
func NormalizeKey(s string) string {
	return keyReplacer.Replace(strings.ToLower(s))
}
