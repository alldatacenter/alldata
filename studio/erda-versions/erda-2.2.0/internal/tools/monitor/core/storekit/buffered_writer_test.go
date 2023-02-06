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
)

type testCountWriter struct {
	capacity int
	data     []interface{}
	closed   bool
}

func (w *testCountWriter) WriteN(data ...interface{}) (int, error) {
	if w.closed {
		return 0, ErrWriterClosed
	}
	if len(w.data) >= w.capacity {
		return 0, fmt.Errorf("full buffer")
	} else if len(w.data)+len(data) > w.capacity {
		data = data[:w.capacity-len(w.data)]
	}
	w.data = append(w.data, data...)
	return len(data), nil
}

func (w *testCountWriter) Close() error {
	w.closed = true
	return nil
}

func TestNewBufferedWriter(t *testing.T) {
	type step struct {
		flush bool
		data  []interface{}
	}
	type args struct {
		bufferSize int
		capacity   int
		steps      []step
	}
	tests := []struct {
		name       string
		args       args
		wantBuffer []interface{}
		wantData   []interface{}
	}{
		{
			args: args{
				bufferSize: 5,
				capacity:   10,
				steps: []step{
					{
						data: []interface{}{1, 2, 3},
					},
					{
						data: []interface{}{4, 5, 6},
					},
				},
			},
			wantBuffer: []interface{}{6},
			wantData:   []interface{}{1, 2, 3, 4, 5},
		},
		{
			args: args{
				bufferSize: 5,
				capacity:   10,
				steps: []step{
					{
						data: []interface{}{1, 2, 3},
					},
					{
						data: []interface{}{4, 5, 6},
					},
					{
						flush: true,
					},
				},
			},
			wantBuffer: []interface{}{},
			wantData:   []interface{}{1, 2, 3, 4, 5, 6},
		},
		{
			args: args{
				bufferSize: 1,
				capacity:   5,
				steps: []step{
					{
						data: []interface{}{1, 2, 3},
					},
					{
						data: []interface{}{4, 5, 6},
					},
				},
			},
			wantBuffer: []interface{}{6},
			wantData:   []interface{}{1, 2, 3, 4, 5},
		},
		{
			args: args{
				bufferSize: 1,
				capacity:   5,
				steps: []step{
					{
						data: []interface{}{1, 2, 3},
					},
					{
						data: []interface{}{4, 5, 6},
					},
					{
						data: []interface{}{7, 8, 9},
					},
				},
			},
			wantBuffer: []interface{}{6},
			wantData:   []interface{}{1, 2, 3, 4, 5},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			w := &testCountWriter{capacity: tt.args.capacity}
			bw := NewBufferedWriter(w, tt.args.bufferSize)
			for _, step := range tt.args.steps {
				bw.WriteN(step.data...)
				if step.flush {
					bw.Flush()
				}
			}
			if !reflect.DeepEqual(bw.buf, tt.wantBuffer) {
				t.Errorf("got buffer %v, want buffer %v", bw.buf, tt.wantBuffer)
			}
			if !reflect.DeepEqual(w.data, tt.wantData) {
				t.Errorf("got data %v, want data %v", w.data, tt.wantData)
			}
		})
	}
}
