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

package elasticsearch

import (
	"context"
	"fmt"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

func (p *provider) initDummyIndex(ctx context.Context, client *elastic.Client) error {
	if len(p.Cfg.DummyIndex) <= 0 {
		return nil
	}
	exists, err := client.IndexExists(p.Cfg.DummyIndex).Do(ctx)
	if err != nil {
		return err
	}
	if !exists {
		resp, err := client.CreateIndex(p.Cfg.DummyIndex).Do(ctx)
		if err != nil {
			return err
		}
		if !resp.Acknowledged {
			return fmt.Errorf("failed to create dummy index %q, acknowledged=false", p.Cfg.DummyIndex)
		}
		p.Log.Infof("create dummy index index %q successfully", p.Cfg.DummyIndex)

		// put dummy data
		_, err = client.Index().Index(p.Cfg.DummyIndex).Type(p.Cfg.IndexType).BodyJson(&metric.Metric{
			Name:      "monitor",
			Timestamp: time.Now().UnixNano(),
			Tags:      map[string]string{"from": "monitor"},
			Fields:    map[string]interface{}{"value": 1},
		}).Do(ctx)
		if err != nil {
			return fmt.Errorf("failed to put data to dummy index: %s", err)
		}
		p.Log.Infof("put dummy data into index %q successfully", p.Cfg.DummyIndex)
	}
	return nil
}
