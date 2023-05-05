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

package cache

import (
	"reflect"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/cache"
)

var (
	name      = "erda.apps.gallery.easy-memory-cache"
	cacheType = reflect.TypeOf((*Cache)(nil))
	spec      = servicehub.Spec{
		Services:    []string{name, name + "-client"},
		Types:       []reflect.Type{cacheType},
		Description: "easy-memory-cache",
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	}
)

func init() {
	servicehub.Register(name, &spec)
}

// +provider
type provider struct{}

// Init .
func (p *provider) Init(ctx servicehub.Context) error {
	logrus.WithField("provider", name).Infoln("Init")
	if c == nil {
		c = new(Cache)
	}
	bdl := bundle.New(bundle.WithCoreServices())
	orgID2Org = cache.New("dop-org-id-for-org", time.Hour*24, func(i interface{}) (interface{}, bool) {
		orgDTO, err := bdl.GetOrg(i.(string))
		if err != nil {
			return nil, false
		}
		return orgDTO, true
	})
	projID2Org = cache.New("dop-project-id-for-org", time.Minute*30, func(i interface{}) (interface{}, bool) {
		projectID, err := strconv.ParseUint(i.(string), 10, 32)
		if err != nil {
			return nil, false
		}
		projectDTO, err := bdl.GetProject(projectID)
		if err != nil {
			return nil, false
		}
		orgDTO, ok := c.GetOrgByOrgID(strconv.FormatUint(projectDTO.OrgID, 10))
		if !ok {
			return nil, false
		}
		return orgDTO, true
	})
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	logrus.WithField("provider", name).Infoln("Provide")
	if ctx.Service() == name+"-client" || ctx.Type() == cacheType {
		if c == nil {
			c = new(Cache)
		}
		return c
	}
	return p
}
