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
	"github.com/erda-project/erda/internal/tools/monitor/core/event"
	"github.com/erda-project/erda/internal/tools/monitor/core/event/storage"
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
		IndexType    string        `file:"index_type" default:"event"`
	}
	provider struct {
		Cfg       *config
		Log       logs.Logger
		ES1       elasticsearch.Interface `autowired:"elasticsearch@event" optional:"true"`
		ES2       elasticsearch.Interface `autowired:"elasticsearch" optional:"true"`
		Loader    loader.Interface        `autowired:"elasticsearch.index.loader@event"`
		Creator   creator.Interface       `autowired:"elasticsearch.index.creator@event" optional:"true"`
		Retention retention.Interface     `autowired:"storage-retention-strategy@event" optional:"true"`
		es        elasticsearch.Interface
		client    *elastic.Client
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
		return nil, fmt.Errorf("elasticsearch.index.creator@event and storage-retention-strategy@event is required for Writer")
	}
	w := p.es.NewWriter(&elasticsearch.WriteOptions{
		Timeout: p.Cfg.WriteTimeout,
		Enc: func(val interface{}) (index, id, typ string, body interface{}, err error) {
			data := val.(*event.Event)
			var wait <-chan error
			namespace, key := "full_cluster", p.Retention.GetConfigKey(data.Name, data.Tags)
			if len(key) > 0 {
				wait, index = p.Creator.Ensure(data.Name, namespace, key)
			} else {
				wait, index = p.Creator.Ensure(data.Name, namespace)
			}
			if wait != nil {
				select {
				case <-wait:
				case <-ctx.Done():
					return "", "", "", nil, storekit.ErrExitConsume
				}
			}
			return index, id, p.Cfg.IndexType, &Document{
				Event: data,
				Date:  getUnixMillisecond(data.Timestamp),
			}, nil
		},
	})
	return w, nil
}

// Document .
type Document struct {
	*event.Event
	Date int64 `json:"@timestamp"`
}

const maxUnixMillisecond int64 = 9999999999999

func getUnixMillisecond(ts int64) int64 {
	if ts > maxUnixMillisecond {
		return ts / int64(time.Millisecond)
	}
	return ts
}

func init() {
	servicehub.Register("event-storage-elasticsearch", &servicehub.Spec{
		Services:     []string{"event-storage-elasticsearch-reader", "event-storage-writer"},
		Dependencies: []string{"elasticsearch"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
