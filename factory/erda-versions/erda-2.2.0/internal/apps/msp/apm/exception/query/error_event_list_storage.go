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

	"github.com/erda-project/erda/internal/apps/msp/apm/exception/erda-event/storage"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/model"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

// ErrorEvent .
type ErrorEvent = interface{}

// ErrorEventListIterator .
type ErrorEventListIterator struct {
	list []ErrorEvent
	i    int
	data ErrorEvent
}

// NewErrorEventErrorListIterator .
func NewErrorEventErrorListIterator(list ...ErrorEvent) storekit.Iterator {
	return &ErrorEventListIterator{list: list, i: -1}
}

// First .
func (it *ErrorEventListIterator) First() bool {
	if len(it.list) <= 0 {
		return false
	}
	it.i = 0
	it.data = it.list[it.i]
	return true
}

// Last .
func (it *ErrorEventListIterator) Last() bool {
	if len(it.list) <= 0 {
		return false
	}
	it.i = len(it.list) - 1
	it.data = it.list[it.i]
	return true

}

// Next .
func (it *ErrorEventListIterator) Next() bool {
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
func (it *ErrorEventListIterator) Prev() bool {
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
func (it *ErrorEventListIterator) Value() ErrorEvent { return it.data }

// Error .
func (it *ErrorEventListIterator) Error() error { return nil }

// Close .
func (it *ErrorEventListIterator) Close() error { return nil }

type errorEventListStorage struct {
	exceptionEvent *model.Event
}

func (s *errorEventListStorage) NewWriter(ctx context.Context) (storekit.BatchWriter, error) {
	return storekit.DefaultNopWriter, nil
}

func (s *errorEventListStorage) Count(ctx context.Context, sel *storage.Selector) int64 {
	return int64(1)
}

func (s *errorEventListStorage) Iterator(ctx context.Context, sel *storage.Selector) (storekit.Iterator, error) {
	return NewErrorEventErrorListIterator(s.exceptionEvent), nil
}
