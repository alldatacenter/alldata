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

package cassandra_v2

import (
	"context"
	"io"

	"github.com/scylladb/gocqlx/qb"

	"github.com/erda-project/erda/internal/apps/msp/apm/trace/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

func (p *provider) Iterator(ctx context.Context, sel *storage.Selector) (storekit.Iterator, error) {
	var cmps []qb.Cmp
	values := make(qb.M)
	cmps = append(cmps, qb.Eq("trace_id"))
	values["trace_id"] = sel.TraceId
	table := DefaultSpanTable

	return &spanIterator{
		ctx:       ctx,
		sel:       sel,
		queryFunc: p.queryFunc,
		table:     table,
		cmps:      cmps,
		values:    values,
		pageSize:  uint(p.Cfg.ReadPageSize),
	}, nil

}

type iteratorDir int8

const (
	iteratorInitial = iota
	iteratorForward
	iteratorBackward
)

type spanIterator struct {
	ctx       context.Context
	sel       *storage.Selector
	queryFunc func(builder *qb.SelectBuilder, binding qb.M, dest interface{}) error

	table    string
	cmps     []qb.Cmp
	values   qb.M
	pageSize uint

	dir iteratorDir

	buffer []interface{}
	value  interface{}
	err    error
	closed bool
}

func (it *spanIterator) First() bool {
	if it.checkClosed() {
		return false
	}
	it.fetch(iteratorForward)
	return it.yield()
}

func (it *spanIterator) Last() bool {
	if it.checkClosed() {
		return false
	}
	it.fetch(iteratorBackward)
	return it.yield()
}

func (it *spanIterator) Next() bool {
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

func (it *spanIterator) Prev() bool {
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

func (it *spanIterator) Value() storekit.Data { return it.value }
func (it *spanIterator) Error() error {
	if it.err == io.EOF {
		return nil
	}
	return it.err
}

func (it *spanIterator) yield() bool {
	if len(it.buffer) > 0 {
		it.value = it.buffer[0]
		it.buffer = it.buffer[1:]
		return true
	}
	return false
}

func (it *spanIterator) Close() error {
	it.closed = true
	return nil
}

func (it *spanIterator) checkClosed() bool {
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

func (it *spanIterator) fetch(dir iteratorDir) error {
	it.buffer = nil
	order := qb.ASC
	it.dir = dir
	if it.dir == iteratorBackward {
		order = qb.DESC
	}
	for it.err == nil && len(it.buffer) <= 0 {
		var spans []*SavedSpan

		builder := qb.Select(it.table).Where(it.cmps...).
			OrderBy("start_time", order).Limit(uint(it.pageSize))
		err := it.queryFunc(builder, it.values, &spans)
		if err != nil {
			return err
		}

		it.buffer = convertToPbSpans(spans)
		if it.err != nil {
			return it.err
		}
	}
	return nil
}
