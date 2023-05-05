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

package query

import (
	"context"

	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

// Data .
type Data = interface{}

// ListIterator .
type ListIterator struct {
	list []Data
	i    int
	data Data
}

// NewListIterator .
func NewListIterator(list ...Data) storekit.Iterator {
	return &ListIterator{list: list, i: -1}
}

// First .
func (it *ListIterator) First() bool {
	if len(it.list) <= 0 {
		return false
	}
	it.i = 0
	it.data = it.list[it.i]
	return true
}

// Last .
func (it *ListIterator) Last() bool {
	if len(it.list) <= 0 {
		return false
	}
	it.i = len(it.list) - 1
	it.data = it.list[it.i]
	return true

}

// Next .
func (it *ListIterator) Next() bool {
	if it.i < 0 {
		return it.First()
	}
	if it.i >= len(it.list)-1 {
		return false
	}
	it.i++
	it.data = it.list[it.i]
	return true
}

// Prev .
func (it *ListIterator) Prev() bool {
	if it.i < 0 {
		return it.Last()
	}
	if it.i <= 0 {
		return false
	}
	it.i--
	it.data = it.list[it.i]
	return true
}

// Value .
func (it *ListIterator) Value() Data { return it.data }

// Error .
func (it *ListIterator) Error() error { return nil }

// Close .
func (it *ListIterator) Close() error { return nil }

type listStorage struct {
	span *trace.Span
}

func (s *listStorage) NewWriter(ctx context.Context) (storekit.BatchWriter, error) {
	return storekit.DefaultNopWriter, nil
}

func (s *listStorage) Count(ctx context.Context, traceId string) int64 {
	return int64(1)
}

func (s *listStorage) Iterator(ctx context.Context, sel *storage.Selector) (storekit.Iterator, error) {
	return NewListIterator(s.span), nil
}
