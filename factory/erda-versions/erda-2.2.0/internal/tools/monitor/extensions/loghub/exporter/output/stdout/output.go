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

package stdout

import (
	"fmt"

	"github.com/recallsong/go-utils/reflectx"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/exporter"
)

type provider struct {
	exp exporter.Interface
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.exp = ctx.Service("logs-exporter-base").(exporter.Interface)
	return nil
}

func (p *provider) Start() error {
	return p.exp.NewConsumer(p.newOutput)
}

func (p *provider) Close() error { return nil }

func (p *provider) newOutput(i int) (exporter.Output, error) {
	return p, nil
}

func (p *provider) Write(key string, data []byte) error {
	fmt.Println(key, reflectx.BytesToString(data))
	return nil
}

func init() {
	servicehub.Register("logs-exporter-stdout", &servicehub.Spec{
		Services:     []string{"logs-exporter-stdout"},
		Dependencies: []string{"logs-exporter-base"},
		Description:  "logs export to stdout",
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
