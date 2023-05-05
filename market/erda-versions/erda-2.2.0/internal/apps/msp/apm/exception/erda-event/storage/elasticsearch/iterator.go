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
	"io"
	"strconv"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/erda-event/storage"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/model"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
)

func (p *provider) getSearchSource(sel *storage.Selector) *elastic.SearchSource {
	searchSource := elastic.NewSearchSource()
	query := elastic.NewBoolQuery()
	if len(sel.ErrorId) > 0 {
		query = query.Filter(elastic.NewQueryStringQuery("error_id:" + sel.ErrorId))
	}
	if len(sel.EventId) > 0 {
		query = query.Filter(elastic.NewQueryStringQuery("event_id:" + sel.EventId))
	}
	if len(sel.TerminusKey) > 0 {
		query = query.Filter(elastic.NewQueryStringQuery("tags.terminus_key:" + sel.TerminusKey))
	}

	return searchSource.Query(query)
}

func (p *provider) Iterator(ctx context.Context, sel *storage.Selector) (storekit.Iterator, error) {
	// TODO check org
	indices := p.Loader.Indices(ctx, sel.StartTime, sel.EndTime, loader.KeyPath{
		Recursive: true,
	})
	return &scrollIterator{
		ctx:          ctx,
		sel:          sel,
		searchSource: p.getSearchSource(sel),
		client:       p.client,
		timeout:      p.Cfg.QueryTimeout,
		pageSize:     p.Cfg.ReadPageSize,
		indices:      indices,
	}, nil
}

type iteratorDir int8

const (
	iteratorInitial = iota
	iteratorForward
	iteratorBackward
)

type scrollIterator struct {
	log          logs.Logger
	ctx          context.Context
	sel          *storage.Selector
	searchSource *elastic.SearchSource
	client       *elastic.Client
	timeout      time.Duration
	pageSize     int
	indices      []string

	scrollIDs    map[string]struct{}
	lastScrollID string
	dir          iteratorDir
	buffer       []*model.Event
	value        *model.Event
	size         int64
	err          error
	closed       bool
}

func (it *scrollIterator) First() bool {
	if it.checkClosed() {
		return false
	}
	it.release()
	it.fetch(iteratorForward)
	return it.yield()
}

func (it *scrollIterator) Last() bool {
	if it.checkClosed() {
		return false
	}
	it.release()
	it.fetch(iteratorBackward)
	return it.yield()
}

func (it *scrollIterator) Next() bool {
	if it.checkClosed() {
		return false
	}
	if it.dir == iteratorBackward {
		it.err = storekit.ErrOpNotSupported
		return false
	}
	if it.yield() {
		return true
	}
	it.fetch(iteratorForward)
	return it.yield()
}

func (it *scrollIterator) Prev() bool {
	if it.checkClosed() {
		return false
	}
	if it.dir == iteratorForward {
		it.err = storekit.ErrOpNotSupported
		return false
	}
	if it.yield() {
		return true
	}
	it.fetch(iteratorBackward)
	return it.yield()
}

func (it *scrollIterator) Value() storekit.Data { return it.value }
func (it *scrollIterator) Error() error {
	if it.err == io.EOF {
		return nil
	}
	return it.err
}

func (it *scrollIterator) release() (err error) {
	var list []string
	for id := range it.scrollIDs {
		if len(id) > 0 {
			list = append(list, id)
		}
	}
	if len(list) > 0 {
		_, err = it.client.ClearScroll(list...).Do(context.TODO())
		if err != nil {
			it.log.Errorf("failed to clear scroll: %s", err)
		}
	}
	it.scrollIDs, it.lastScrollID = nil, ""
	it.buffer = nil
	it.value = nil
	return nil
}

func (it *scrollIterator) fetch(dir iteratorDir) error {

	if len(it.indices) <= 0 {
		it.err = io.EOF
		return it.err
	}
	minutes := int64(it.timeout.Minutes())
	if minutes < 1 {
		minutes = 1
	}
	keepalive := strconv.FormatInt(minutes, 10) + "m"

	it.dir = dir
	it.buffer = nil
	for it.err == nil && len(it.buffer) <= 0 {
		func() error {
			// do query
			ctx, cancel := context.WithTimeout(it.ctx, it.timeout)
			defer cancel()
			var resp *elastic.SearchResult
			if len(it.lastScrollID) <= 0 {
				var ascending bool
				if it.dir != iteratorBackward {
					ascending = true
				}

				resp, it.err = it.client.Scroll(it.indices...).KeepAlive(keepalive).
					IgnoreUnavailable(true).AllowNoIndices(true).
					SearchSource(it.searchSource).Size(it.pageSize).Sort("timestamp", ascending).Do(ctx)
				if it.err != nil {
					return it.err
				}
			} else {
				resp, it.err = it.client.Scroll(it.indices...).ScrollId(it.lastScrollID).KeepAlive(keepalive).
					IgnoreUnavailable(true).AllowNoIndices(true).
					Size(it.pageSize).Do(ctx)
				if it.err != nil {
					return it.err
				}
			}

			// save scrollID
			if it.scrollIDs == nil {
				it.scrollIDs = make(map[string]struct{})
			}
			if resp != nil {
				it.scrollIDs[resp.ScrollId] = struct{}{}
				it.lastScrollID = resp.ScrollId
			}
			if resp == nil || resp.Hits == nil || len(resp.Hits.Hits) <= 0 {
				it.err = io.EOF
				return it.err
			}

			// parse result
			it.buffer = parseHits(resp.Hits.Hits)
			it.size = resp.Hits.TotalHits
			return nil
		}()
	}
	return nil
}

func (it *scrollIterator) yield() bool {
	if len(it.buffer) > 0 {
		it.value = it.buffer[0]
		it.buffer = it.buffer[1:]
		return true
	}
	return false
}

func (it *scrollIterator) Close() error {
	it.closed = true
	it.release()
	return nil
}

func (it *scrollIterator) checkClosed() bool {
	if it.closed {
		if it.err == nil {
			it.err = storekit.ErrIteratorClosed
		}
		return true
	}
	return false
}

func parseHits(hits []*elastic.SearchHit) (list []*model.Event) {
	for _, hit := range hits {
		if hit.Source == nil {
			continue
		}
		data, err := parseData(*hit.Source)
		if err != nil {
			continue
		}
		list = append(list, data)
	}
	return list
}

func parseData(bytes []byte) (*model.Event, error) {
	var data model.Event
	err := json.Unmarshal(bytes, &data)
	if err != nil {
		return nil, err
	}
	return &data, nil
}
