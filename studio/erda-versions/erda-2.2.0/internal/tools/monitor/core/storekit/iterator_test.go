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
	"reflect"
	"testing"

	"github.com/recallsong/go-utils/conv"
)

func TestNewListIterator_Next(t *testing.T) {
	type args struct {
	}
	tests := []struct {
		name string
		list []Data
		want []Data
	}{
		{
			list: []Data{},
			want: nil,
		},
		{
			list: []Data{1},
			want: []Data{1},
		},
		{
			list: []Data{1, 2, 3, 4, 5},
			want: []Data{1, 2, 3, 4, 5},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			it := NewListIterator(tt.list...)
			var result []Data
			for it.Next() {
				result = append(result, it.Value())
			}
			if !reflect.DeepEqual(result, tt.want) {
				t.Errorf("NewListIterator().Next() got %v, want %v", result, tt.want)
			}
		})
	}
}

func TestNewListIterator_Prev(t *testing.T) {
	type args struct {
	}
	tests := []struct {
		name string
		list []Data
		want []Data
	}{
		{
			list: []Data{},
			want: nil,
		},
		{
			list: []Data{1},
			want: []Data{1},
		},
		{
			list: []Data{1, 2, 3, 4, 5},
			want: []Data{5, 4, 3, 2, 1},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			it := NewListIterator(tt.list...)
			var result []Data
			for it.Prev() {
				result = append(result, it.Value())
			}
			if !reflect.DeepEqual(result, tt.want) {
				t.Errorf("NewListIterator().Prev() got %v, want %v", result, tt.want)
			}
		})
	}
}

func TestNewListIterator_Total(t *testing.T) {
	type args struct {
	}
	tests := []struct {
		name string
		list []Data
		want int64
	}{
		{
			list: []Data{},
			want: 0,
		},
		{
			list: []Data{1},
			want: 1,
		},
		{
			list: []Data{1, 2, 3, 4, 5},
			want: 5,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			it := NewListIterator(tt.list...).(Counter)
			var result int64
			result, _ = it.Total()
			if !reflect.DeepEqual(result, tt.want) {
				t.Errorf("NewListIterator().Prev() got %v, want %v", result, tt.want)
			}
		})
	}
}

type testInt64Comparer struct{}

func (c testInt64Comparer) Compare(a, b Data) int {
	ai := conv.ToInt64(a, 0) >> 32
	bi := conv.ToInt64(b, 0) >> 32
	if ai > bi {
		return 1
	} else if ai < bi {
		return -1
	}
	return 0
}

func int64v(h, l int32) int64 {
	return (int64(h) << 32) | int64(l)
}

func TestMergedHeadOverlappedIterator_Next(t *testing.T) {
	tests := []struct {
		name    string
		cmp     Comparer
		its     []Iterator
		want    []Data
		wantErr bool
	}{
		{
			cmp:  Int64Comparer{},
			its:  []Iterator{},
			want: nil,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(), NewListIterator(),
			},
			want: nil,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(),
				NewListIterator(7, 8, 9),
				NewListIterator(),
				NewListIterator(1, 2, 3),
			},
			want: []Data{
				1, 2, 3,
				7, 8, 9,
			},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(7, 8, 9),
				NewListIterator(4, 5, 6),
				NewListIterator(1, 2, 3),
			},
			want: []Data{
				1, 2, 3,
				4, 5, 6,
				7, 8, 9,
			},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 5, 6),
				NewListIterator(1, 2, 3),
			},
			want: []Data{
				1, 2, 3,
				4, 5, 6,
				7, 8,
			},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 6, 6),
				NewListIterator(2, 3),
			},
			want: []Data{
				2, 3,
				4, 6,
				7, 8,
			},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(1, 2, 3),
			},
			want: []Data{1, 2, 3},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(2, 3, 4),
				NewListIterator(1, 2, 3),
			},
			want: []Data{
				1, 2, 3,
				4,
			},
		},
		{
			cmp: testInt64Comparer{},
			its: []Iterator{
				NewListIterator(int64v(6, 0), int64v(7, 0), int64v(8, 0), int64v(9, 0)),
				NewListIterator(int64v(4, 1), int64v(5, 1), int64v(6, 1), int64v(7, 1)),
				NewListIterator(int64v(1, 2), int64v(2, 2), int64v(3, 2), int64v(4, 2)),
			},
			want: []Data{
				int64v(1, 2), int64v(2, 2), int64v(3, 2), int64v(4, 2),
				int64v(5, 1), int64v(6, 1), int64v(7, 1),
				int64v(8, 0), int64v(9, 0),
			},
		},
		{
			cmp: testInt64Comparer{},
			its: []Iterator{
				NewListIterator(int64v(3, 0), int64v(4, 0), int64v(5, 0), int64v(6, 0)),
				NewListIterator(),
				NewListIterator(int64v(1, 2), int64v(2, 2), int64v(3, 2), int64v(4, 2)),
			},
			want: []Data{
				int64v(1, 2), int64v(2, 2), int64v(3, 2), int64v(4, 2),
				int64v(5, 0), int64v(6, 0),
			},
		},
		{
			cmp: testInt64Comparer{},
			its: []Iterator{
				NewListIterator(int64v(6, 0), int64v(7, 0), int64v(8, 0)),
				NewListIterator(),
				NewListIterator(int64v(1, 2), int64v(2, 2), int64v(3, 2), int64v(4, 2)),
			},
			want: []Data{
				int64v(1, 2), int64v(2, 2), int64v(3, 2), int64v(4, 2),
				int64v(6, 0), int64v(7, 0), int64v(8, 0),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			it := MergedHeadOverlappedIterator(tt.cmp, tt.its...)
			var result []Data
			for it.Next() {
				result = append(result, it.Value())
			}
			if tt.wantErr && it.Error() == nil {
				t.Errorf("MergedHeadOverlappedIterator().Next() want error, but it successful")
			} else if !tt.wantErr && it.Error() != nil {
				t.Errorf("MergedHeadOverlappedIterator().Next() got error: %v", it.Error())
			} else if !reflect.DeepEqual(result, tt.want) {
				t.Errorf("MergedHeadOverlappedIterator().Next() got %v, want %v", result, tt.want)
			}
		})
	}
}

func TestMergedHeadOverlappedIterator_Prev(t *testing.T) {
	tests := []struct {
		name    string
		cmp     Comparer
		its     []Iterator
		want    []Data
		wantErr bool
	}{
		{
			cmp:  Int64Comparer{},
			its:  []Iterator{},
			want: nil,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(), NewListIterator(),
			},
			want: nil,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 5, 6),
				NewListIterator(1, 2, 3),
			},
			want: []Data{
				8, 7,
				6, 5, 4,
				3, 2, 1,
			},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(),
				NewListIterator(6, 7, 8),
				NewListIterator(),
				NewListIterator(4, 5, 6),
				NewListIterator(1, 2, 3),
			},
			want: []Data{
				8, 7,
				6, 5, 4,
				3, 2, 1,
			},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 6, 6),
				NewListIterator(2, 3),
			},
			want: []Data{
				8, 7,
				6, 6, 4,
				3, 2,
			},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(1, 2, 3),
			},
			want: []Data{3, 2, 1},
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 6, 6),
			},
			want: []Data{
				8, 7,
				6, 6, 4,
			},
		},
		{
			cmp: testInt64Comparer{},
			its: []Iterator{
				NewListIterator(int64v(6, 0), int64v(7, 0), int64v(8, 0), int64v(9, 0)),
				NewListIterator(int64v(4, 1), int64v(5, 1), int64v(6, 1), int64v(7, 1)),
				NewListIterator(int64v(1, 2), int64v(2, 2), int64v(3, 2), int64v(4, 2)),
			},
			want: []Data{
				int64v(9, 0), int64v(8, 0),
				int64v(7, 1), int64v(6, 1), int64v(5, 1),
				int64v(4, 2), int64v(3, 2), int64v(2, 2), int64v(1, 2),
			},
		},
		{
			cmp: testInt64Comparer{},
			its: []Iterator{
				NewListIterator(int64v(6, 0), int64v(7, 0), int64v(8, 0), int64v(9, 0)),
				NewListIterator(),
				NewListIterator(int64v(1, 2), int64v(2, 2), int64v(3, 2), int64v(4, 2)),
			},
			want: []Data{
				int64v(9, 0), int64v(8, 0), int64v(7, 0), int64v(6, 0),
				int64v(4, 2), int64v(3, 2), int64v(2, 2), int64v(1, 2),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			it := MergedHeadOverlappedIterator(tt.cmp, tt.its...)
			var result []Data
			for it.Prev() {
				result = append(result, it.Value())
			}
			if tt.wantErr && it.Error() == nil {
				t.Errorf("MergedHeadOverlappedIterator().Prev() want error, but it successful")
			} else if !tt.wantErr && it.Error() != nil {
				t.Errorf("MergedHeadOverlappedIterator().Prev() got error: %v", it.Error())
			} else if !reflect.DeepEqual(result, tt.want) {
				t.Errorf("MergedHeadOverlappedIterator().Prev() got %v, want %v", result, tt.want)
			}
		})
	}
}

func TestMergedHeadOverlappedIterator_Total(t *testing.T) {
	tests := []struct {
		name    string
		cmp     Comparer
		its     []Iterator
		want    int64
		wantErr bool
	}{
		{
			cmp:  Int64Comparer{},
			its:  []Iterator{},
			want: 0,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(), NewListIterator(),
			},
			want: 0,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 5, 6),
				NewListIterator(1, 2, 3),
			},
			want: 9,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 5, 6),
				NewListIterator(1, 2, 3),
			},
			want: 9,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				MockIterator{returnTotal: 1},
				MockIterator{returnTotal: 2},
			},
			want: 3,
		},
		{
			cmp: Int64Comparer{},
			its: []Iterator{
				MockIterator{returnError: fmt.Errorf("mock error")},
			},
			want:    0,
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			it := MergedHeadOverlappedIterator(tt.cmp, tt.its...)
			result, err := it.(Counter).Total()
			if tt.wantErr && err == nil {
				t.Errorf("MergedHeadOverlappedIterator().Prev() want error, but it successful")
			} else if !tt.wantErr && err != nil {
				t.Errorf("MergedHeadOverlappedIterator().Prev() got error: %v", it.Error())
			} else if !reflect.DeepEqual(result, tt.want) {
				t.Errorf("MergedHeadOverlappedIterator().Prev() got %v, want %v", result, tt.want)
			}
		})
	}
}

func TestOrderedIterator_Next(t *testing.T) {
	tests := []struct {
		name    string
		its     []Iterator
		want    []Data
		wantErr bool
	}{
		{
			its:  []Iterator{},
			want: nil,
		},
		{
			its: []Iterator{
				NewListIterator(), NewListIterator(),
			},
			want: nil,
		},
		{
			its: []Iterator{
				NewListIterator(),
				NewListIterator(1, 2, 3),
				NewListIterator(),
				NewListIterator(7, 8, 9),
			},
			want: []Data{
				1, 2, 3,
				7, 8, 9,
			},
		},
		{
			its: []Iterator{
				NewListIterator(1, 2, 3),
				NewListIterator(4, 5, 6),
				NewListIterator(7, 8, 9),
			},
			want: []Data{
				1, 2, 3,
				4, 5, 6,
				7, 8, 9,
			},
		},
		{
			its: []Iterator{
				NewListIterator(1, 2, 3),
			},
			want: []Data{1, 2, 3},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			it := OrderedIterator(tt.its...)
			var result []Data
			for it.Next() {
				result = append(result, it.Value())
			}
			if tt.wantErr && it.Error() == nil {
				t.Errorf("OrderedIterator().Next() want error, but it successful")
			} else if !tt.wantErr && it.Error() != nil {
				t.Errorf("OrderedIterator().Next() got error: %v", it.Error())
			} else if !reflect.DeepEqual(result, tt.want) {
				t.Errorf("OrderedIterator().Next() got %v, want %v", result, tt.want)
			}
		})
	}
}

func TestOrderedIterator_Prev(t *testing.T) {
	tests := []struct {
		name    string
		its     []Iterator
		want    []Data
		wantErr bool
	}{
		{
			its:  []Iterator{},
			want: nil,
		},
		{
			its: []Iterator{
				NewListIterator(), NewListIterator(),
			},
			want: nil,
		},
		{
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 5, 6),
				NewListIterator(1, 2, 3),
			},
			want: []Data{
				3, 2, 1,
				6, 5, 4,
				8, 7, 6,
			},
		},
		{
			its: []Iterator{
				NewListIterator(),
				NewListIterator(6, 7, 8),
				NewListIterator(),
				NewListIterator(4, 5, 6),
				NewListIterator(1, 2, 3),
			},
			want: []Data{
				3, 2, 1,
				6, 5, 4,
				8, 7, 6,
			},
		},
		{
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 6, 6),
				NewListIterator(2, 3),
			},
			want: []Data{
				3, 2,
				6, 6, 4,
				8, 7, 6,
			},
		},
		{
			its: []Iterator{
				NewListIterator(1, 2, 3),
			},
			want: []Data{3, 2, 1},
		},
		{
			its: []Iterator{
				NewListIterator(6, 7, 8),
				NewListIterator(4, 6, 6),
			},
			want: []Data{
				6, 6, 4,
				8, 7, 6,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			it := OrderedIterator(tt.its...)
			var result []Data
			for it.Prev() {
				result = append(result, it.Value())
			}
			if tt.wantErr && it.Error() == nil {
				t.Errorf("OrderedIterator().Prev() want error, but it successful")
			} else if !tt.wantErr && it.Error() != nil {
				t.Errorf("OrderedIterator().Prev() got error: %v", it.Error())
			} else if !reflect.DeepEqual(result, tt.want) {
				t.Errorf("OrderedIterator().Prev() got %v, want %v", result, tt.want)
			}
		})
	}
}

// MockIterator .
type MockIterator struct {
	returnError error
	returnTotal int64
}

// First .
func (it MockIterator) First() bool { return false }

// Last .
func (it MockIterator) Last() bool { return false }

// Next .
func (it MockIterator) Next() bool { return false }

// Prev .
func (it MockIterator) Prev() bool { return false }

// Value .
func (it MockIterator) Value() interface{} { return nil }

// Error .
func (it MockIterator) Error() error { return nil }

// Close .
func (it MockIterator) Close() error { return nil }

// Total .
func (it MockIterator) Total() (int64, error) {
	if it.returnError != nil {
		return 0, it.returnError
	}
	return it.returnTotal, nil
}
