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

package odata

import (
	"sync"

	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

// Buffer. Store ObservableData
type Buffer struct {
	sync.Mutex
	buf []ObservableData
	cap int
}

func NewBuffer(cap int) *Buffer {
	return &Buffer{
		buf: make([]ObservableData, 0, cap),
		cap: cap,
	}
}

// return isFull
func (b *Buffer) Push(od ObservableData) {
	b.Lock()
	defer b.Unlock()
	if len(b.buf) == b.cap {
		panic("must not full when push od")
	}
	b.buf = append(b.buf, od)
}

func (b *Buffer) Full() bool {
	b.Lock()
	defer b.Unlock()
	return len(b.buf) == b.cap
}

func (b *Buffer) Empty() bool {
	b.Lock()
	defer b.Unlock()
	return len(b.buf) == 0
}

// FlushAll empty buffer, then return a copy of internal buf
func (b *Buffer) FlushAllMetrics() []*metric.Metric {
	b.Lock()
	defer b.Unlock()
	out := make([]*metric.Metric, len(b.buf))
	for i := range b.buf {
		out[i] = b.buf[i].(*metric.Metric)
	}

	b.buf = b.buf[:0]
	return out
}

// FlushAll empty buffer, then return a copy of internal buf
func (b *Buffer) FlushAllLogs() []*log.Log {
	b.Lock()
	defer b.Unlock()
	out := make([]*log.Log, len(b.buf))
	for i := range b.buf {
		out[i] = b.buf[i].(*log.Log)
	}

	b.buf = b.buf[:0]
	return out
}

// FlushAll empty buffer, then return a copy of internal buf
func (b *Buffer) FlushAllSpans() []*trace.Span {
	b.Lock()
	defer b.Unlock()
	out := make([]*trace.Span, len(b.buf))
	for i := range b.buf {
		out[i] = b.buf[i].(*trace.Span)
	}

	b.buf = b.buf[:0]
	return out
}

// FlushAll empty buffer, then return a copy of internal buf
func (b *Buffer) FlushAllRaws() []*Raw {
	b.Lock()
	defer b.Unlock()
	out := make([]*Raw, len(b.buf))
	for i := range b.buf {
		out[i] = b.buf[i].(*Raw)
	}

	b.buf = b.buf[:0]
	return out
}
