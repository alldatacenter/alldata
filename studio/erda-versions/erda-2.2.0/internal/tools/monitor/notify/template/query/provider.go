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

package query

import (
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"time"

	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/tools/monitor/notify/template/db"
	"github.com/erda-project/erda/internal/tools/monitor/notify/template/model"
	"github.com/erda-project/erda/pkg/http/httpclient"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-infra/providers/mysql"
	"github.com/erda-project/erda/bundle"
	bundlecmdb "github.com/erda-project/erda/internal/pkg/bundle-ex/cmdb"
)

type define struct{}

func (d *define) Services() []string  { return []string{"notify-query"} }
func (d *define) Summary() string     { return "notify-query" }
func (d *define) Description() string { return d.Summary() }
func (d *define) Config() interface{} { return &config{} }
func (d *define) Creator() servicehub.Creator {
	return func() servicehub.Provider {
		return &provider{}
	}
}
func (d *define) Dependencies() []string {
	return []string{"http-server", "mysql", "i18n"}
}

type config struct {
	Files []string `json:"files"`
}

type provider struct {
	C    *config
	N    *db.NotifyDB
	L    logs.Logger
	t    i18n.Translator
	bdl  *bundle.Bundle
	cmdb *bundlecmdb.Cmdb
}

func (p *provider) getUserDefineTemplate(scopeID, scope, name, nType string) ([]*model.GetNotifyRes, error) {
	customizeList := make([]*model.GetNotifyRes, 0)
	//according to scope and scopeID obtain user define template
	allCustomize, err := p.N.GetAllUserDefineNotify(scope, scopeID)
	if err != nil {
		return nil, err
	}
	allCustomizeRecords := *allCustomize
	//range all user define templates，parse and filter
	for _, v := range allCustomizeRecords {
		m := &model.GetNotifyRes{}
		metadata := model.Metadata{}
		err = yaml.Unmarshal([]byte(v.Metadata), &metadata)
		if err != nil {
			return nil, err
		}
		if (name != "" && metadata.Name != name) || (nType != "" && metadata.Type != nType) {
			continue
		}
		m.ID = v.NotifyID
		m.Name = metadata.Name
		customizeList = append(customizeList, m)
	}
	return customizeList, nil
}

func (p *provider) checkNotify(params model.CreateNotifyReq) error {
	if params.ScopeID == "" {
		return fmt.Errorf("create notify scopeID must not be empty")
	}
	if params.Scope == "" {
		return fmt.Errorf("create notify scope must not be empty")
	}
	if len(params.TemplateID) == 0 {
		return fmt.Errorf("create notify templateID must not be empty")
	}
	if params.NotifyGroupID == 0 {
		return fmt.Errorf("create notify notifyGroupID must not be empty")
	}
	return nil
}

func (p *provider) Init(ctx servicehub.Context) error {
	log := ctx.Logger()
	templateMap = make(map[string]model.Model)
	for _, file := range p.C.Files {
		f, err := os.Stat(file)
		if err != nil {
			return fmt.Errorf("fail to load notify file: %s", err)
		}
		if f.IsDir() {
			err := filepath.Walk(file, func(p string, info os.FileInfo, err error) error {
				if err != nil {
					return nil
				}
				f, err := ioutil.ReadFile(p)
				var model model.Model
				err = yaml.Unmarshal(f, &model)
				if err != nil {
					return err
				}
				if model.ID != "" {
					templateMap[model.ID] = model
				}
				return nil
			})
			if err != nil {
				return err
			}
		}
	}
	log.Infof("load notify files: %v", p.C.Files)
	p.N = db.New(ctx.Service("mysql").(mysql.Interface).DB())
	p.t = ctx.Service("i18n").(i18n.I18n).Translator("notify")
	routes := ctx.Service("http-server", interceptors.Recover(p.L), interceptors.CORS()).(httpserver.Router)
	p.initBundle()
	return p.initRoutes(routes)
}

func (p *provider) initBundle() {
	hc := httpclient.New(httpclient.WithTimeout(time.Second, time.Second*60))
	p.bdl = bundle.New(
		bundle.WithHTTPClient(hc),
		bundle.WithCoreServices(),
	)
	p.cmdb = bundlecmdb.New(bundlecmdb.WithHTTPClient(hc))
}

func init() {
	servicehub.RegisterProvider("notify-query", &define{})
}
