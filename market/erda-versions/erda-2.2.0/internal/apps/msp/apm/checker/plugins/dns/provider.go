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

package dns

import (
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/plugins"
)

type config struct{}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger
}

func (p *provider) Init(ctx servicehub.Context) error { return nil }

func (p *provider) Validate(c *pb.Checker) error {
	return nil
}

func (p *provider) New(c *pb.Checker) (plugins.Handler, error) {
	return nil, fmt.Errorf("TODO ...")
}

func init() {
	servicehub.Register("erda.msp.apm.checker.task.plugins.dns", &servicehub.Spec{
		Services:   []string{"erda.msp.apm.checker.task.plugins.dns"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
