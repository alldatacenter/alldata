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

package storekit

import (
	"fmt"
	"math"

	"github.com/recallsong/go-utils/conv"
)

// EmptyIterator .
type EmptyIterator struct{}

// First .
func (it EmptyIterator) First() bool { return false }

// Last .
func (it EmptyIterator) Last() bool { return false }

// Next .
func (it EmptyIterator) Next() bool { return false }

// Prev .
func (it EmptyIterator) Prev() bool { return false }

// Value .
func (it EmptyIterator) Value() interface{} { return nil }

// Error .
func (it EmptyIterator) Error() error { return nil }

// Close .
func (it EmptyIterator) Close() error { return nil }

// Total .
func (it EmptyIterator) Total() (int64, error) { return 0, nil }

// Int64Comparer .
type Int64Comparer struct{}

// Compare .
func (c Int64Comparer) Compare(a, b Data) int {
	ai := conv.ToInt64(a, math.MinInt64)
	bi := conv.ToInt64(b, math.MinInt64)
	if ai > bi {
		return 1
	} else if ai < bi {
		return -1
	}
	return 0
}

// ListIterator .
type ListIterator struct {
	list []Data
	i    int
	data Data
}

// NewListIterator .
func NewListIterator(list ...Data) Iterator {
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

// Total .
func (it *ListIterator) Total() (int64, error) {
	return int64(len(it.list)), nil
}

// MergedHeadOverlappedIterator .
func MergedHeadOverlappedIterator(cmp Comparer, its ...Iterator) Iterator {
	if len(its) <= 0 {
		return EmptyIterator{}
	}
	items := make([]*iteratorItem, len(its), len(its))
	for i, it := range its {
		items[i] = &iteratorItem{
			Iterator: it,
		}
	}
	return &headOverlappedIterator{
		cmp: cmp,
		its: items,
	}
}

type (
	iteratorItem struct {
		Iterator
		hasValue bool
		eof      bool
	}
	headOverlappedIterator struct {
		cmp     Comparer
		its     []*iteratorItem
		cur     *iteratorItem
		idx     int
		data    Data
		needCmp bool

		err  error
		next bool
		prev bool
	}
)

func (it *headOverlappedIterator) First() bool {
	if it.err != nil {
		return false
	}
	// TODO: implement
	it.err = ErrOpNotSupported
	return false
}

func (it *headOverlappedIterator) Last() bool {
	if it.err != nil {
		return false
	}
	// TODO: implement
	it.err = ErrOpNotSupported
	return false
}

func (it *headOverlappedIterator) Next() bool {
	if it.err != nil {
		return false
	}
	if it.prev {
		it.err = ErrOpNotSupported
		return false
	}
	it.next = true

	if it.cur == nil {
		it.idx = len(it.its) - 1
		it.cur = it.its[it.idx]
	} else if it.idx < 0 {
		return false
	}
loop:
	for {
		if !it.cur.Next() {
			if it.cur.Error() != nil {
				it.err = it.cur.Error()
				break loop
			}
			it.idx--
			if it.idx >= 0 {
				it.cur = it.its[it.idx]
				it.needCmp = true
				continue
			}
			break loop
		}
		if it.data == nil || !it.needCmp {
			it.data = it.cur.Value()
		} else {
			val := it.cur.Value()
			if it.cmp.Compare(val, it.data) <= 0 {
				continue
			}
			it.data = val
		}
		return true
	}
	it.data = nil
	return false
}

func (it *headOverlappedIterator) Prev() bool {
	if it.next {
		it.err = ErrOpNotSupported
		return false
	}
	it.prev = true

	if it.cur == nil {
		it.idx = 0
		it.cur = it.its[it.idx]
	} else if it.idx >= len(it.its) {
		return false
	}
loop:
	for {
		if !it.cur.hasValue {
			if !it.cur.Prev() {
				it.cur.eof = true
				if it.cur.Error() != nil {
					it.err = it.cur.Error()
					break loop
				}
				it.idx++
				if it.idx < len(it.its) {
					it.cur = it.its[it.idx]
					continue loop
				}
				break loop
			}
			it.cur.hasValue = true
		}
		for i, n := it.idx+1, len(it.its); i < n; i++ {
			iter := it.its[i]
			if iter.eof {
				continue
			}
			if !iter.hasValue {
				if !iter.Prev() {
					iter.eof = true
					if iter.Error() != nil {
						it.err = iter.Error()
						break loop
					}
					continue
				}
				iter.hasValue = true
			}
			if it.cmp.Compare(it.cur.Value(), iter.Value()) <= 0 {
				it.data, iter.hasValue = iter.Value(), false
				it.cur, it.idx = iter, i
			}
		}
		it.data, it.cur.hasValue = it.cur.Value(), false
		return true
	}
	it.data = nil
	return false
}

func (it *headOverlappedIterator) Value() Data  { return it.data }
func (it *headOverlappedIterator) Error() error { return it.err }
func (it *headOverlappedIterator) Close() error {
	var err error
	for _, iter := range it.its {
		if e := iter.Close(); e != nil {
			err = e
		}
	}
	return err
}

func (it *headOverlappedIterator) Total() (int64, error) {
	var total int64
	for _, item := range it.its {
		counter, ok := item.Iterator.(Counter)
		if !ok {
			return total, fmt.Errorf("%T not implement %T", item, counter)
		}
		count, err := counter.Total()
		if err != nil {
			return total, err
		}
		total += count
	}
	return total, nil
}

type orderedIterator struct {
	its  []*iteratorItem
	cur  *iteratorItem
	idx  int
	data Data

	err  error
	next bool
	prev bool
}

// OrderedIterator .
func OrderedIterator(its ...Iterator) Iterator {
	if len(its) <= 0 {
		return EmptyIterator{}
	}
	items := make([]*iteratorItem, len(its), len(its))
	for i, it := range its {
		items[i] = &iteratorItem{
			Iterator: it,
		}
	}
	return &orderedIterator{
		its: items,
	}
}

func (it *orderedIterator) First() bool {
	if it.err != nil {
		return false
	}
	// TODO: implement
	it.err = ErrOpNotSupported
	return false
}

func (it *orderedIterator) Last() bool {
	if it.err != nil {
		return false
	}
	// TODO: implement
	it.err = ErrOpNotSupported
	return false
}

// Next Iterate from the first iterator to last iterator
func (it *orderedIterator) Next() bool {
	if it.err != nil {
		return false
	}
	if it.prev {
		it.err = ErrOpNotSupported
		return false
	}
	it.next = true

	if len(it.its) == 0 {
		return false
	} else if it.cur == nil {
		it.idx = 0
		it.cur = it.its[it.idx]
	} else if it.idx >= len(it.its) {
		return false
	}
	for {
		if !it.cur.Next() {
			if it.cur.Error() != nil {
				it.err = it.cur.Error()
				break
			}
			it.idx++
			if it.idx < len(it.its) {
				it.cur = it.its[it.idx]
				continue
			}
			break
		}
		it.data = it.cur.Value()
		return true
	}
	it.data = nil
	return false
}

// Prev Iterate from the last iterator to first iterator
func (it *orderedIterator) Prev() bool {
	if it.next {
		it.err = ErrOpNotSupported
		return false
	}
	it.prev = true

	if len(it.its) == 0 {
		return false
	} else if it.cur == nil {
		it.idx = len(it.its) - 1
		it.cur = it.its[it.idx]
	} else if it.idx < 0 {
		return false
	}
	for {
		if !it.cur.Prev() {
			if it.cur.Error() != nil {
				it.err = it.cur.Error()
				break
			}
			it.idx--
			if it.idx >= 0 {
				it.cur = it.its[it.idx]
				continue
			}
			break
		}
		it.data = it.cur.Value()
		return true
	}
	it.data = nil
	return false
}

func (it *orderedIterator) Value() Data {
	return it.data
}

func (it *orderedIterator) Error() error {
	return it.err
}

func (it *orderedIterator) Close() error {
	var err error
	for _, iter := range it.its {
		if e := iter.Close(); e != nil {
			err = e
		}
	}
	return err
}
