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

package initializer

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"strings"

	"github.com/olivere/elastic"
	cfgpkg "github.com/recallsong/go-utils/config"
)

func (p *provider) initTemplates(ctx context.Context, client *elastic.Client, templates []template) error {
	// check templates
	contents := make([]string, len(templates), len(templates))
	names := make(map[string]bool)
	for i, t := range templates {
		if len(t.Name) <= 0 {
			return fmt.Errorf("template(%d).Name is unspecified", i)
		}
		if names[t.Name] {
			return fmt.Errorf("template(%d).Name = %q is conflicting", i, t.Name)
		}
		names[t.Name] = true

		if len(t.Path) <= 0 {
			return fmt.Errorf("template(%d).Path is unspecified", i)
		}
		template, err := ioutil.ReadFile(t.Path)
		if err != nil {
			return fmt.Errorf("failed to load template{%d, %q, %q}: %s", i, t.Name, t.Path, err)
		}
		template = cfgpkg.EscapeEnv(template)
		var m map[string]interface{}
		err = json.Unmarshal(template, &m)
		if err != nil {
			return fmt.Errorf("templates{%d, %q, %q} is not json format", i, t.Name, t.Path)
		}
		contents[i] = string(template)
	}

	select {
	case <-ctx.Done():
		return nil
	default:
	}

	// do request
	for i, t := range templates {
		resp, err := client.IndexPutTemplate(t.Name).
			BodyString(contents[i]).Do(ctx)
		if err != nil {
			return fmt.Errorf("failed to setup index template{%d, %q}: %s", i, t.Name, err)
		}
		if !resp.Acknowledged {
			return fmt.Errorf("failed to setup index template{%d, %q}: Acknowledged=false", i, t.Name)
		}
		p.Log.Infof("setup index template{%d, %q} success", i, t.Name)
	}
	return nil
}

func (p *provider) createIndices(ctx context.Context, client *elastic.Client, indices []createIndex) error {
	for _, index := range indices {
		if len(index.Index) <= 0 {
			continue
		}
		resp, err := p.es.Client().CatIndices().Index(index.Index).Columns("index").Do(ctx)
		if err != nil {
			return fmt.Errorf("failed to check index %q for creating", index.Index)
		}
		if len(resp) > 0 {
			continue
		}
		cresp, err := client.CreateIndex(index.Index).Do(ctx)
		if err != nil {
			if err, ok := err.(*elastic.Error); ok {
				if err.Status == 400 && err.Details != nil && strings.Contains(err.Details.Reason, "already exists") {
					return nil
				}
			}
			return fmt.Errorf("failed to create index %q: %s", index.Index, err)
		}
		if !cresp.Acknowledged {
			return fmt.Errorf("failed to create index %q: Acknowledged=false", index.Index)
		}
	}
	return nil
}
