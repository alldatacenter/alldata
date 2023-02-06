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

package clickhouse

import (
	"context"
	"fmt"
	"io"
	"math"
	"strconv"
	"sync/atomic"
	"time"

	cksdk "github.com/ClickHouse/clickhouse-go/v2"
	ckdriver "github.com/ClickHouse/clickhouse-go/v2/lib/driver"
	"github.com/doug-martin/goqu/v9"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/providers/clickhouse"
	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

func (p *provider) Iterator(ctx context.Context, sel *storage.Selector) (storekit.Iterator, error) {
	expr, tableMeta, err := p.buildSqlFromTablePart(sel)
	if err != nil {
		return nil, err
	}

	expr, highlightItems, err := p.appendSqlWherePart(expr, tableMeta, sel)
	if err != nil {
		return nil, err
	}

	pageSize := p.Cfg.ReadPageSize

	var count float64
	if c, ok := sel.Options[storage.SelectorKeyCount]; ok {
		count = math.Abs(float64(c.(int64)))
	}

	if count > 0 || sel.Meta.PreferredBufferSize > 0 {
		pageSize = int(math.Max(count, float64(sel.Meta.PreferredBufferSize)))
	}

	var callback = func(logItem *pb.LogItem) {
		if highlightItems == nil {
			return
		}
		highlight := map[string]*structpb.ListValue{}
		for k, v := range highlightItems {
			var items []interface{}
			for _, token := range v {
				items = append(items, token)
			}
			list, err := structpb.NewList(items)
			if err != nil {
				continue
			}
			highlight[k] = list
		}
		logItem.Highlight = highlight
	}

	id := sel.Skip.AfterId.ShortId()

	return newClickhouseIterator(
		ctx,
		p.clickhouse,
		expr,
		pageSize,
		sel.Skip.FromOffset,
		id,
		callback,
		sel.Meta.PreferredReturnFields,
		sel.Debug,
		p.Cfg.QueryTimeout,
		p.Cfg.QueryMaxThreads,
		p.Cfg.QueryMaxMemory,
	)
}

func newClickhouseIterator(
	ctx context.Context,
	ck clickhouse.Interface,
	sqlClause *goqu.SelectDataset,
	pageSize int,
	fromOffset int,
	searchAfterID string,
	callback func(item *pb.LogItem),
	returnFieldMode storage.ReturnFieldMode,
	debug bool,
	queryTimeout time.Duration,
	queryMaxThreads int,
	queryMaxMemory int64,
) (storekit.Iterator, error) {
	return &clickhouseIterator{
		ctx:             ctx,
		ck:              ck,
		sqlClause:       sqlClause,
		pageSize:        pageSize,
		fromOffset:      fromOffset,
		searchAfterID:   searchAfterID,
		callback:        callback,
		returnFieldMode: returnFieldMode,
		debug:           debug,
		queryTimeout:    queryTimeout,
		queryMaxThreads: queryMaxThreads,
		queryMaxMemory:  queryMaxMemory,
		lastID:          searchAfterID,
	}, nil
}

type iteratorDir int8

const (
	iteratorInitial = iota
	iteratorForward
	iteratorBackward
)

type clickhouseIterator struct {
	ctx             context.Context
	ck              clickhouse.Interface
	sqlClause       *goqu.SelectDataset
	pageSize        int
	fromOffset      int
	searchAfterID   string
	callback        func(item *pb.LogItem)
	returnFieldMode storage.ReturnFieldMode
	debug           bool

	queryTimeout    time.Duration
	queryMaxThreads int
	queryMaxMemory  int64

	lastResp ckdriver.Rows
	buffer   []interface{}
	value    interface{}

	total       int64
	totalCached bool

	dir    iteratorDir
	err    error
	closed bool
	lastID string
}

func (it *clickhouseIterator) First() bool {
	if it.checkClosed() {
		return false
	}

	it.lastID = ""
	it.fetch(iteratorForward)
	return it.yield()
}

func (it *clickhouseIterator) Last() bool {
	if it.checkClosed() {
		return false
	}
	it.lastID = ""
	it.fetch(iteratorBackward)
	return it.yield()
}

func (it *clickhouseIterator) Next() bool {
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

func (it *clickhouseIterator) Prev() bool {
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

func (it *clickhouseIterator) Value() storekit.Data {
	return it.value
}

func (it *clickhouseIterator) Error() error {
	if it.err == io.EOF {
		return nil
	}
	return it.err
}

func (it *clickhouseIterator) Close() error {
	it.closed = true
	if it.lastResp != nil {
		return it.lastResp.Close()
	}
	return nil
}

func (it *clickhouseIterator) Total() (int64, error) {
	if it.totalCached {
		return it.total, nil
	}
	err := it.count()
	if err != nil {
		return 0, err

	}
	return it.total, nil
}

func (it *clickhouseIterator) checkClosed() bool {
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

func (it *clickhouseIterator) yield() bool {
	if len(it.buffer) > 0 {
		it.value = it.buffer[0]
		it.buffer = it.buffer[1:]
		return true
	}
	return false
}

func (it *clickhouseIterator) fetch(dir iteratorDir) {
	it.dir = dir
	var reverse bool
	if it.dir == iteratorBackward {
		reverse = true
	}
	it.buffer = nil
	for it.err == nil && len(it.buffer) == 0 {
		fetchingRemote := false
		if it.lastResp == nil {
			fetchingRemote = true
			expr := it.sqlClause

			if reverse {
				if len(it.lastID) > 0 {
					expr = expr.Where(goqu.C("_id").Lt(it.lastID))
				}
				expr = expr.Order(goqu.C("org_name").Desc(), goqu.C("tenant_id").Desc(), goqu.C("group_id").Desc(), goqu.C("timestamp").Desc())
			} else {
				if len(it.lastID) > 0 {
					expr = expr.Where(goqu.C("_id").Gt(it.lastID))
				}
				expr = expr.Order(goqu.C("org_name").Asc(), goqu.C("tenant_id").Asc(), goqu.C("group_id").Asc(), goqu.C("timestamp").Asc())
			}

			expr = expr.Offset(uint(it.fromOffset)).Limit(uint(it.pageSize))

			switch it.returnFieldMode {
			case storage.ExcludeTagsField:
				expr = expr.Select("_id", "timestamp", "id", "content", "stream", "source")
			case storage.OnlyIdContent:
				expr = expr.Select("_id", "timestamp", "id", "content")
			default:
			}

			sql, _, err := expr.ToSQL()
			if it.debug {
				fmt.Printf("clickhouse fetch sql: %s\n", sql)
			}
			if err != nil {
				it.err = err
				return
			}
			ctx := it.buildQueryContext(it.ctx)
			rows, err := it.ck.Client().Query(ctx, sql)
			if err != nil {
				it.err = err
				return
			}
			it.lastResp = rows
		}

		for it.lastResp.Next() {
			var item logItem
			err := it.lastResp.ScanStruct(&item)
			if err != nil {
				it.err = err
				return
			}

			it.lastID = item.UniqId
			it.fromOffset = 0

			it.buffer = append(it.buffer, it.decode(&item))
			return
		}

		if len(it.buffer) == 0 {
			if fetchingRemote {
				it.err = io.EOF
			}
			_ = it.lastResp.Close()
			it.lastResp = nil
			return
		}
	}
}

func (it *clickhouseIterator) buildQueryContext(ctx context.Context) context.Context {
	settings := map[string]interface{}{}
	if it.queryTimeout > 0 {
		settings["max_execution_time"] = int(it.queryTimeout.Seconds()) + 5
	}
	if it.queryMaxThreads > 0 {
		settings["max_threads"] = it.queryMaxThreads
	}
	if it.queryMaxMemory > 0 {
		settings["max_memory_usage"] = it.queryMaxMemory
	}
	if len(settings) == 0 {
		return ctx
	}
	ctx = cksdk.Context(ctx, cksdk.WithSettings(settings))
	return ctx
}

func (it *clickhouseIterator) decode(log *logItem) *pb.LogItem {
	item := &pb.LogItem{
		UniqId:    log.UniqId,
		Id:        log.ID,
		Source:    log.Source,
		Stream:    log.Stream,
		Timestamp: strconv.FormatInt(log.Timestamp.UnixNano(), 10),
		UnixNano:  log.Timestamp.UnixNano(),
		Offset:    log.Offset,
		Content:   log.Content,
		Level:     log.Tags["level"],
		RequestId: log.Tags["request_id"],
		Tags:      log.Tags,
	}

	if it.callback != nil {
		it.callback(item)
	}

	return item
}

func (it *clickhouseIterator) count() error {
	expr := it.sqlClause
	expr = expr.Select(goqu.L("count(*)").As("count"))
	sql, _, err := expr.ToSQL()
	if it.debug {
		fmt.Printf("clickhouse count sql: %s\n", sql)
	}
	if err != nil {
		return err
	}
	var counter []struct {
		Count uint64 `ch:"count"`
	}
	err = it.ck.Client().Select(context.Background(), &counter, sql)
	if err != nil {
		return err
	}

	atomic.SwapInt64(&it.total, int64(counter[0].Count))
	it.totalCached = true
	return nil
}

type logItem struct {
	UniqId    string            `ch:"_id"`
	OrgName   string            `ch:"org_name"`
	TenantId  string            `ch:"tenant_id"`
	GroupId   string            `ch:"group_id"`
	Source    string            `ch:"source"`
	ID        string            `ch:"id"`
	Stream    string            `ch:"stream"`
	Content   string            `ch:"content"`
	Offset    int64             `ch:"offset"`
	Timestamp time.Time         `ch:"timestamp"`
	Tags      map[string]string `ch:"tags"`
}
