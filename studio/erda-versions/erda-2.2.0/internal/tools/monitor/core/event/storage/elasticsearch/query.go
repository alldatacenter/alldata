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
	"encoding/json"
	"fmt"
	"strings"

	"github.com/recallsong/go-utils/encoding/jsonx"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda/internal/tools/monitor/core/event"
	"github.com/erda-project/erda/internal/tools/monitor/core/event/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

func (p *provider) QueryPaged(ctx context.Context, sel *storage.Selector, pageNo, pageSize int) ([]*event.Event, error) {
	indices := p.Loader.Indices(ctx, sel.Start, sel.End, loader.KeyPath{
		Recursive: true,
	})
	if len(indices) == 0 {
		return nil, fmt.Errorf("no index located")
	}
	searchSource := p.getSearchSource(sel)
	searchSource.From((pageNo - 1) * pageSize).Size(pageSize)

	if sel.Debug {
		source, _ := searchSource.Source()
		fmt.Printf("indices: %v\nsearchSource: %s\n", strings.Join(indices, ","), jsonx.MarshalAndIndent(source))
	}

	timeout, cancel := context.WithTimeout(ctx, p.Cfg.QueryTimeout)
	defer cancel()
	resp, err := p.client.Search(indices...).
		IgnoreUnavailable(true).
		AllowNoIndices(true).
		SearchSource(searchSource).
		Sort("timestamp", false).
		Do(timeout)

	if err != nil {
		return nil, err
	}
	if resp.Error != nil {
		return nil, fmt.Errorf(resp.Error.Reason)
	}
	if resp.Hits.TotalHits == 0 {
		return nil, nil
	}

	return p.parseHits(resp.Hits.Hits), nil
}

func (p *provider) getSearchSource(sel *storage.Selector) *elastic.SearchSource {
	searchSource := elastic.NewSearchSource()
	query := elastic.NewBoolQuery().Filter(elastic.NewRangeQuery("timestamp").Gte(sel.Start).Lt(sel.End))
	for _, filter := range sel.Filters {
		val, ok := filter.Value.(string)
		if !ok {
			continue
		}
		switch filter.Op {
		case storage.EQ:
			query = query.Filter(elastic.NewTermQuery(filter.Key, val))
		case storage.REGEXP:
			query = query.Filter(elastic.NewRegexpQuery(filter.Key, val))
		}
	}
	return searchSource.Query(query)
}

func (p *provider) parseHits(hits []*elastic.SearchHit) (list []*event.Event) {
	for _, hit := range hits {
		if hit.Source == nil {
			continue
		}
		data, err := p.parseData(*hit.Source)
		if err != nil {
			continue
		}
		list = append(list, data)
	}
	return list
}

func (p *provider) parseData(bytes []byte) (*event.Event, error) {
	var data event.Event
	err := json.Unmarshal(bytes, &data)
	if err != nil {
		return nil, err
	}
	return &data, nil
}
