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

package cassandra

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"regexp"
	"strings"
	"time"

	"github.com/scylladb/gocqlx/qb"

	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

var columns = map[string]string{
	"tags.request_id": "request_id",
	"source":          "source",
	"id":              "id",
	"stream":          "stream",
}

func (p *provider) Iterator(ctx context.Context, sel *storage.Selector) (_ storekit.Iterator, err error) {
	var cmps []qb.Cmp
	values := make(qb.M)
	matcher := func(data *pb.LogItem) bool { return true }
	var applicationID, clusterName string
	for _, filter := range sel.Filters {
		if filter.Value == nil {
			continue
		}
		switch filter.Key {
		case "content":
			switch filter.Op {
			case storage.REGEXP:
				exp, _ := filter.Value.(string)
				if len(exp) > 0 {
					regex, err := regexp.Compile(exp)
					if err != nil {
						return nil, fmt.Errorf("invalid regexp %q", exp)
					}
					matcher = func(data *pb.LogItem) bool {
						return regex.MatchString(data.Content)
					}
				}
			case storage.CONTAINS:
				exp, _ := filter.Value.(string)
				if len(exp) > 0 {
					matcher = func(data *pb.LogItem) bool {
						return strings.Contains(data.Content, exp)
					}
				}
			case storage.EQ:
				val, _ := filter.Value.(string)
				if len(val) > 0 {
					matcher = func(data *pb.LogItem) bool {
						return data.Content == val
					}
				}
			}
		default:
			if filter.Op != storage.EQ {
				return nil, fmt.Errorf("%s only support EQ filter", filter.Key)
			}
			switch filter.Key {
			case "tags.dice_application_id":
				applicationID, _ = filter.Value.(string)
				continue
			case "tags.dice_cluster_name":
				clusterName, _ = filter.Value.(string)
				continue
			}
			col, ok := columns[filter.Key]
			if !ok {
				return storekit.EmptyIterator{}, nil
			}
			cmps = append(cmps, qb.Eq(col))
			values[col] = filter.Value
		}
	}
	if _, ok := values["request_id"]; ok {
		meta, err := p.queryLogMetaWithFilters(qb.M{
			"tags['dice_application_id']": applicationID,
		})
		if err != nil {
			return nil, err
		}
		return p.queryAllLogs(p.getTableName(meta), cmps, values, matcher)
	}
	if _, ok := values["id"]; !ok {
		return nil, fmt.Errorf("id is required")
	}
	source, ok := values["source"]
	if !ok {
		return nil, fmt.Errorf("source is required")
	}
	_, hasStream := values["stream"]

	table := DefaultBaseLogTable
	if len(applicationID) > 0 {
		meta, err := p.queryLogMetaWithFilters(qb.M{
			"source": values["source"],
			"id":     values["id"],
		})
		if err != nil {
			return nil, err
		}
		if source == "container" {
			if meta == nil {
				return storekit.EmptyIterator{}, nil
			}
			if meta.Tags["dice_application_id"] != applicationID {
				return storekit.EmptyIterator{}, nil
			}
		}
		table = p.getTableName(meta)
	} else if len(clusterName) > 0 {
		meta, err := p.queryLogMetaWithFilters(qb.M{
			"source": values["source"],
			"id":     values["id"],
		})
		if err != nil {
			return nil, err
		}
		if meta == nil {
			return storekit.EmptyIterator{}, nil
		}
		if meta.Tags["dice_cluster_name"] != clusterName {
			return storekit.EmptyIterator{}, nil
		}
		table = p.getTableName(meta)
	} else {
		meta, _ := p.queryLogMetaWithFilters(qb.M{
			"source": values["source"],
			"id":     values["id"],
		})
		if meta != nil {
			table = p.getTableName(meta)
		}
	}
	return &logsIterator{
		ctx:       ctx,
		sel:       sel,
		queryFunc: p.queryFunc,
		table:     table,
		cmps:      cmps,
		values:    values,
		matcher:   matcher,
		pageSize:  uint(p.Cfg.ReadPageSize),
		allStream: !hasStream,
		start:     sel.Start,
		end:       sel.End,
		offset:    -1,
	}, nil
}

func (p *provider) queryAllLogs(table string, cmps []qb.Cmp, values qb.M, matcher func(data *pb.LogItem) bool) (storekit.Iterator, error) {
	var list []*SavedLog
	err := p.queryFunc(
		qb.Select(table).Where(cmps...),
		values, &list,
	)
	if err != nil {
		return nil, fmt.Errorf("retrive %s failed: %w", table, err)
	}
	logs, err := convertToLogItems(list, matcher)
	if err != nil {
		return nil, err
	}
	return storekit.NewListIterator(logs...), nil
}

type iteratorDir int8

const (
	iteratorInitial = iota
	iteratorForward
	iteratorBackward
)

type logsIterator struct {
	ctx       context.Context
	sel       *storage.Selector
	queryFunc func(builder *qb.SelectBuilder, binding qb.M, dest interface{}) error

	table    string
	cmps     []qb.Cmp
	values   qb.M
	matcher  func(data *pb.LogItem) bool
	pageSize uint

	allStream bool
	dir       iteratorDir
	start     int64
	end       int64
	offset    int64
	timestamp int64

	buffer []interface{}
	value  interface{}
	err    error
	closed bool
}

func (it *logsIterator) First() bool {
	if it.checkClosed() {
		return false
	}
	it.start = it.sel.Start
	it.end = it.sel.End
	it.offset = -1
	it.fetch(iteratorForward)
	return it.yield()
}

func (it *logsIterator) Last() bool {
	if it.checkClosed() {
		return false
	}
	it.start = it.sel.Start
	it.end = it.sel.End
	it.offset = -1
	it.fetch(iteratorBackward)
	return it.yield()
}

func (it *logsIterator) Next() bool {
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

func (it *logsIterator) Prev() bool {
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

func (it *logsIterator) Value() storekit.Data { return it.value }
func (it *logsIterator) Error() error {
	if it.err == io.EOF {
		return nil
	}
	return it.err
}

func (it *logsIterator) yield() bool {
	if len(it.buffer) > 0 {
		it.value = it.buffer[0]
		it.buffer = it.buffer[1:]
		return true
	}
	return false
}

func (it *logsIterator) Close() error {
	it.closed = true
	return nil
}

func (it *logsIterator) checkClosed() bool {
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

func (it *logsIterator) fetch(dir iteratorDir) error {
	it.buffer = nil
	order := qb.ASC
	lessFunc := lessSavedLog
	it.dir = dir
	if it.dir == iteratorBackward {
		order = qb.DESC
		lessFunc = reverseLessSavedLog
	}
	var bucket int64
	for it.err == nil && len(it.buffer) <= 0 && it.start < it.end {
		var logs []*SavedLog
		if it.allStream {
			stdoutLogs, err := it.fetchWithStream(order, "stdout", it.offset, &bucket)
			if err != nil {
				it.err = err
			}
			stderrLogs, err := it.fetchWithStream(order, "stderr", it.offset, &bucket)
			if err != nil {
				it.err = err
			}
			logs = mergeSavedLog(stdoutLogs, stderrLogs, lessFunc)
		} else {
			logs, it.err = it.fetchWithStream(order, "", it.offset, &bucket)
		}
		lognum := len(logs)
		if it.timestamp > 0 && it.offset >= 0 {
			for lognum > 0 && logs[0].Timestamp == it.timestamp && logs[0].Offset == it.offset {
				logs = logs[1:]
				lognum--
			}
		}
		if order == qb.ASC {
			if lognum > 0 {
				last := logs[lognum-1]
				it.start = last.Timestamp
				it.timestamp, it.offset = last.Timestamp, last.Offset
			} else {
				it.start = bucket + dayDuration
			}
		} else {
			if lognum > 0 {
				last := logs[lognum-1]
				it.end = last.Timestamp
				it.timestamp, it.offset = last.Timestamp, last.Offset
			} else {
				it.end = bucket - 1
			}
		}
		if lognum <= 0 {
			continue
		}
		it.buffer, it.err = convertToLogItems(logs, it.matcher)
		if it.err != nil {
			return it.err
		}
	}
	return nil
}

func (it *logsIterator) fetchWithStream(order qb.Order, stream string, offset int64, bucket *int64) ([]*SavedLog, error) {
	cmps := make([]qb.Cmp, len(it.cmps), len(it.cmps)+4)
	copy(cmps, it.cmps)
	cmps = append(cmps,
		qb.Eq("time_bucket"),
		qb.GtOrEqNamed("timestamp", "start"),
	)
	if it.offset >= 0 && order == qb.DESC {
		cmps = append(cmps, qb.LtOrEqNamed("timestamp", "end"))
	} else {
		cmps = append(cmps, qb.LtNamed("timestamp", "end"))
	}

	if order == qb.ASC {
		*bucket = truncateDate(it.start)
	} else {
		*bucket = truncateDate(it.end)
	}
	if len(stream) > 0 {
		cmps = append(cmps, qb.Eq("stream"))
		it.values["stream"] = stream
	}
	it.values["time_bucket"] = *bucket
	it.values["start"] = it.start
	it.values["end"] = it.end

	builder := qb.Select(it.table).Where(cmps...).
		OrderBy("timestamp", order).OrderBy("offset", order).Limit(uint(it.pageSize))
	var logs []*SavedLog
	err := it.queryFunc(builder, it.values, &logs)
	if err != nil {
		return nil, err
	}
	return logs, nil
}

func mergeSavedLog(a, b []*SavedLog, less func(a, b *SavedLog) bool) []*SavedLog {
	an := len(a)
	if an <= 0 {
		return b
	}
	bn := len(b)
	if bn <= 0 {
		return a
	}
	list := make([]*SavedLog, an+bn, an+bn)
	var i, ai, bi int
	for ai < an && bi < bn {
		if less(a[ai], b[bi]) {
			list[i] = a[ai]
			ai++
		} else {
			list[i] = b[bi]
			bi++
		}
		i++
	}
	for ai < an {
		list[i] = a[ai]
		ai++
		i++
	}
	for bi < bn {
		list[i] = b[bi]
		bi++
		i++
	}
	return list
}

func lessSavedLog(a, b *SavedLog) bool {
	if a.Timestamp < b.Timestamp {
		return true
	} else if a.Timestamp > b.Timestamp {
		return false
	}
	if a.Offset < b.Offset {
		return true
	} else if a.Offset > b.Offset {
		return false
	}
	return bytes.Compare(a.Content, b.Content) < 0
}

func reverseLessSavedLog(a, b *SavedLog) (bb bool) {
	return lessSavedLog(b, a)
}

const dayDuration = int64(time.Hour) * 24

func truncateDate(unixNano int64) int64 {
	return unixNano - unixNano%dayDuration
}
