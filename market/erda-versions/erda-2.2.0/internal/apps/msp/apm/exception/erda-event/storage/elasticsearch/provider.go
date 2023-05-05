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

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/erda-event/storage"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/model"
	"github.com/erda-project/erda/internal/tools/monitor/core/settings/retention-strategy"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/creator"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

type (
	config struct {
		QueryTimeout time.Duration `file:"query_timeout" default:"1m"`
		WriteTimeout time.Duration `file:"write_timeout" default:"1m"`
		ReadPageSize int           `file:"read_page_size" default:"1024"`
		IndexType    string        `file:"index_type" default:"error-events"`
	}
	provider struct {
		Cfg          *config
		Log          logs.Logger
		ES1          elasticsearch.Interface `autowired:"elasticsearch@error_event" optional:"true"`
		ES2          elasticsearch.Interface `autowired:"elasticsearch" optional:"true"`
		Loader       loader.Interface        `autowired:"elasticsearch.index.loader@error_event"`
		Creator      creator.Interface       `autowired:"elasticsearch.index.creator@error_event" optional:"true"`
		Retention    retention.Interface     `autowired:"storage-retention-strategy@error_event" optional:"true"`
		client       *elastic.Client
		es           elasticsearch.Interface
		queryTimeout string
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	if p.ES1 != nil {
		p.es = p.ES1
	} else if p.ES2 != nil {
		p.es = p.ES2
	} else {
		return fmt.Errorf("elasticsearch is required")
	}
	p.client = p.es.Client()
	if p.Retention != nil {
		ctx.AddTask(func(c context.Context) error {
			p.Retention.Loading(ctx)
			return nil
		})
	}
	return nil
}

var _ storage.Storage = (*provider)(nil)

func (p *provider) NewWriter(ctx context.Context) (storekit.BatchWriter, error) {
	if p.Creator == nil || p.Retention == nil {
		return nil, fmt.Errorf("elasticsearch.index.creator@error_event and storage-retention-strategy@error_event is required for Writer")
	}
	w := p.es.NewWriter(&elasticsearch.WriteOptions{
		Timeout: p.Cfg.WriteTimeout,
		Enc: func(val interface{}) (index, id, typ string, body interface{}, err error) {
			data := val.(*model.Event)
			var wait <-chan error
			wait, index = p.Creator.Ensure(data.Tags["org_name"])
			if wait != nil {
				select {
				case <-wait:
				case <-ctx.Done():
					return "", "", "", nil, storekit.ErrExitConsume
				}
			}
			return index, data.EventId, p.Cfg.IndexType, data, nil
		},
	})
	return w, nil
}

func init() {
	servicehub.Register("error-event-storage-elasticsearch", &servicehub.Spec{
		Services:   []string{"error-event-storage-elasticsearch-reader", "error-event-storage-writer"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
