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
	"io"
	"strconv"
	"sync/atomic"
	"time"

	"github.com/olivere/elastic"

	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

// SortItem .
type SortItem struct {
	Key       string
	Ascending bool
}

// NewSearchIterator .
func NewSearchIterator(
	ctx context.Context,
	client *elastic.Client,
	timeout time.Duration,
	fromOffset int,
	pageSize int,
	indices []string,
	sorts []*SortItem,
	searchAfter []interface{},
	search func() (*elastic.SearchSource, error),
	decode func(data *elastic.SearchHit) (interface{}, error),
) (storekit.Iterator, error) {
	if pageSize <= 1 {
		// avoid query result only contains the last duplicated data,
		// then lastSortValues keep previous values, but it not allowed
		return nil, fmt.Errorf("pageSize must greater than 1")
	}
	ms := int64(timeout.Milliseconds())
	if ms < 1 {
		ms = 1
	}
	timeoutMS := strconv.FormatInt(ms, 10) + "ms"
	return &searchIterator{
		ctx:            ctx,
		client:         client,
		timeout:        timeout,
		timeoutMS:      timeoutMS,
		pageSize:       pageSize,
		indices:        indices,
		search:         search,
		sorts:          sorts,
		decode:         decode,
		fromOffset:     fromOffset,
		searchAfter:    searchAfter,
		lastSortValues: searchAfter,
	}, nil
}

// NewScrollIterator .
func NewScrollIterator(
	ctx context.Context,
	client *elastic.Client,
	timeout time.Duration,
	pageSize int,
	indices []string,
	sorts []*SortItem,
	search func() (*elastic.SearchSource, error),
	decode func(data *elastic.SearchHit) (interface{}, error),
) (storekit.Iterator, error) {
	if pageSize <= 0 {
		return nil, fmt.Errorf("pageSize must greater than 0")
	}
	searchSource, err := search()
	if err != nil {
		return nil, err
	}
	minutes := int64(timeout.Minutes())
	if minutes < 1 {
		minutes = 1
	}
	keepalive := strconv.FormatInt(minutes, 10) + "m"
	return &scrollIterator{
		ctx:          ctx,
		client:       client,
		timeout:      timeout,
		keepalive:    keepalive,
		pageSize:     pageSize,
		indices:      indices,
		searchSource: searchSource,
		sorts:        sorts,
		decode:       decode,
	}, nil
}

type iteratorDir int8

const (
	iteratorInitial = iota
	iteratorForward
	iteratorBackward
)

type searchIterator struct {
	ctx         context.Context
	client      *elastic.Client
	timeout     time.Duration
	timeoutMS   string
	fromOffset  int
	pageSize    int
	indices     []string
	search      func() (*elastic.SearchSource, error)
	sorts       []*SortItem
	searchAfter []interface{}
	decode      func(data *elastic.SearchHit) (interface{}, error)
	limiter     storekit.RateLimiter

	lastID         string
	lastSortValues []interface{}
	dir            iteratorDir
	buffer         []interface{}
	value          interface{}
	err            error
	closed         bool
	total          int64
	totalCached    bool
}

func (it *searchIterator) First() bool {
	if it.checkClosed() {
		return false
	}
	it.lastSortValues, it.lastID = it.searchAfter, ""
	it.fetch(iteratorForward)
	return it.yield()
}

func (it *searchIterator) Last() bool {
	if it.checkClosed() {
		return false
	}
	it.lastSortValues, it.lastID = it.searchAfter, ""
	it.fetch(iteratorBackward)
	return it.yield()
}

func (it *searchIterator) Next() bool {
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

func (it *searchIterator) Prev() bool {
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

func (it *searchIterator) Value() storekit.Data { return it.value }
func (it *searchIterator) Error() error {
	if it.err == io.EOF {
		return nil
	}
	return it.err
}

func (it *searchIterator) fetch(dir iteratorDir) {
	it.dir = dir
	var reverse bool
	if it.dir == iteratorBackward {
		reverse = true
	}
	it.buffer = nil
	for it.err == nil && len(it.buffer) <= 0 {
		searchSource, err := it.search()
		if err != nil {
			it.err = err
			continue
		}
		ss := it.client.Search(it.indices...).IgnoreUnavailable(true).AllowNoIndices(true).Timeout(it.timeoutMS).
			SearchSource(searchSource).From(it.fromOffset).Size(it.pageSize).SearchAfter(it.lastSortValues...)
		if reverse {
			for _, item := range it.sorts {
				ss = ss.Sort(item.Key, !item.Ascending)
			}
		} else {
			for _, item := range it.sorts {
				ss = ss.Sort(item.Key, item.Ascending)
			}
		}
		resp, err := it.doRequest(ss)
		if err != nil {
			it.err = err
			continue
		}

		if resp == nil || resp.Hits == nil || len(resp.Hits.Hits) <= 0 {
			it.err = io.EOF
			continue
		}
		atomic.SwapInt64(&it.total, resp.TotalHits())
		it.totalCached = true
		it.buffer = it.parseHits(resp.Hits.Hits)
		if len(resp.Hits.Hits) < it.pageSize {
			it.err = io.EOF
			continue
		}
	}
}

func (it *searchIterator) doRequest(ss *elastic.SearchService) (*elastic.SearchResult, error) {
	ctx, cancel := context.WithTimeout(it.ctx, it.timeout)
	defer cancel()
	return ss.Do(ctx)
}

func (it *searchIterator) count() error {
	searchSource, err := it.search()
	if err != nil {
		it.err = err
		return it.err
	}
	ss := it.client.Search(it.indices...).IgnoreUnavailable(true).AllowNoIndices(true).Timeout(it.timeoutMS).
		SearchSource(searchSource).Size(0)
	var resp *elastic.SearchResult
	ctx, cancel := context.WithTimeout(it.ctx, it.timeout)
	defer cancel()
	resp, it.err = ss.Do(ctx)
	if it.err != nil {
		return it.err
	}
	if resp == nil || resp.Error != nil {
		it.err = fmt.Errorf(resp.Error.Reason)
		return it.err
	}
	atomic.SwapInt64(&it.total, resp.TotalHits())
	it.totalCached = true
	return nil
}

func (it *searchIterator) yield() bool {
	if len(it.buffer) > 0 {
		it.value = it.buffer[0]
		it.buffer = it.buffer[1:]
		return true
	}
	return false
}

func (it *searchIterator) Close() error {
	it.closed = true
	return nil
}

func (it *searchIterator) Total() (int64, error) {
	if it.totalCached {
		return it.total, nil
	}
	err := it.count()
	if err != nil {
		return 0, err
	}
	return it.total, nil
}

func (it *searchIterator) checkClosed() bool {
	if it.closed {
		if it.err == nil {
			it.err = storekit.ErrIteratorClosed
		}
		return true
	}
	select {
	case <-it.ctx.Done():
		if it.err == nil {
			it.err = storekit.ErrIteratorClosed
		}
		return true
	default:
	}
	return false
}

func (it *searchIterator) parseHits(hits []*elastic.SearchHit) (list []interface{}) {
	lastID := it.lastID
	checkID := len(lastID) > 0
	for _, hit := range hits {
		if hit.Source == nil || (checkID && hit.Id == lastID) {
			continue
		}
		it.fromOffset = 0
		it.lastSortValues = hit.Sort
		it.lastID = hit.Id
		data, err := it.decode(hit)
		if err != nil {
			continue
		}
		list = append(list, data)
	}
	return list
}

type scrollIterator struct {
	ctx          context.Context
	client       *elastic.Client
	timeout      time.Duration
	keepalive    string
	pageSize     int
	indices      []string
	searchSource *elastic.SearchSource
	sorts        []*SortItem
	decode       func(data *elastic.SearchHit) (interface{}, error)

	scrollIDs    map[string]struct{}
	lastScrollID string
	dir          iteratorDir
	buffer       []interface{}
	value        interface{}
	err          error
	closed       bool
	total        int64
	totalCached  bool
}

func (it *scrollIterator) First() bool {
	if it.checkClosed() {
		return false
	}
	if err := it.release(); err != nil {
		return false
	}
	it.fetch(iteratorForward)
	return it.yield()
}

func (it *scrollIterator) Last() bool {
	if it.checkClosed() {
		return false
	}
	if err := it.release(); err != nil {
		return false
	}
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
		it.err = err
	}
	it.scrollIDs, it.lastScrollID = nil, ""
	it.buffer = nil
	it.value = nil
	return err
}

func (it *scrollIterator) fetch(dir iteratorDir) error {
	it.dir = dir
	var reverse bool
	if it.dir == iteratorBackward {
		reverse = true
	}
	it.buffer = nil
	for it.err == nil && len(it.buffer) <= 0 {
		func() error {
			// do query
			ctx, cancel := context.WithTimeout(it.ctx, it.timeout)
			defer cancel()
			var resp *elastic.SearchResult
			if len(it.lastScrollID) <= 0 {
				ss := it.client.Scroll(it.indices...).KeepAlive(it.keepalive).
					IgnoreUnavailable(true).AllowNoIndices(true).
					SearchSource(it.searchSource).Size(it.pageSize)
				if reverse {
					for _, item := range it.sorts {
						ss = ss.Sort(item.Key, !item.Ascending)
					}
				} else {
					for _, item := range it.sorts {
						ss = ss.Sort(item.Key, item.Ascending)
					}
				}
				resp, it.err = ss.Do(ctx)
			} else {
				resp, it.err = it.client.Scroll(it.indices...).ScrollId(it.lastScrollID).KeepAlive(it.keepalive).
					IgnoreUnavailable(true).AllowNoIndices(true).
					Size(it.pageSize).Do(ctx)
			}
			if it.err != nil {
				return it.err
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

			atomic.SwapInt64(&it.total, resp.TotalHits())
			it.totalCached = true
			// parse result
			it.buffer = it.parseHits(resp.Hits.Hits)
			return nil
		}()
	}
	return nil
}

func (it *scrollIterator) count() error {
	ss := it.client.Search(it.indices...).IgnoreUnavailable(true).AllowNoIndices(true).
		SearchSource(it.searchSource).Size(0)
	var resp *elastic.SearchResult
	ctx, cancel := context.WithTimeout(it.ctx, it.timeout)
	defer cancel()
	resp, it.err = ss.Do(ctx)
	if it.err != nil {
		return it.err
	}
	if resp == nil || resp.Error != nil {
		it.err = fmt.Errorf(resp.Error.Reason)
		return it.err
	}
	atomic.SwapInt64(&it.total, resp.TotalHits())
	it.totalCached = true
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

func (it *scrollIterator) Total() (int64, error) {
	if it.totalCached {
		return it.total, nil
	}
	err := it.count()
	if err != nil {
		return 0, err

	}
	return it.total, nil
}

func (it *scrollIterator) checkClosed() bool {
	if it.closed {
		if it.err == nil {
			it.err = storekit.ErrIteratorClosed
		}
		return true
	}
	select {
	case <-it.ctx.Done():
		if it.err == nil {
			it.err = storekit.ErrIteratorClosed
		}
		return true
	default:
	}
	return false
}

func (it *scrollIterator) parseHits(hits []*elastic.SearchHit) (list []interface{}) {
	for _, hit := range hits {
		if hit.Source == nil {
			continue
		}
		data, err := it.decode(hit)
		if err != nil {
			continue
		}
		list = append(list, data)
	}
	return list
}
