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

package kuberneteslogs

import (
	"context"
	"fmt"
	"io"
	"os"
	"reflect"
	"strings"
	"testing"
	"time"

	"github.com/recallsong/go-utils/encoding/jsonx"
	v1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
)

type logTestItem struct {
	Timestamp int64
	Content   string
}

var (
	commonLogTestItems = []logTestItem{
		{Timestamp: 11, Content: "1111"},
		{Timestamp: 12, Content: "2222"},
		{Timestamp: 13, Content: "3333"},
		{Timestamp: 14, Content: "4444.0"},
		{Timestamp: 14, Content: "4444.1"},
		{Timestamp: 15, Content: "5555"},
		{Timestamp: 16, Content: "6666"},
		{Timestamp: 17, Content: "7777"},
		{Timestamp: 17, Content: "7777"},
		{Timestamp: 16, Content: "6666.1"},
		{Timestamp: 19, Content: "9999"},
	}
	commonTestOptions = map[string]interface{}{
		storage.IsLive:        true,
		storage.ContainerName: "container_name1",
		storage.PodNamespace:  "namespace1",
		storage.PodName:       "name1",
		storage.ClusterName:   "cluster_name1",
	}
	commonTestFilters = []*storage.Filter{
		{
			Key:   "id",
			Op:    storage.EQ,
			Value: "test_id",
		},
	}
)

var stdout = os.Stdout

func printf(tmp string, args ...interface{}) {
	fmt.Fprintf(stdout, tmp, args...)
}

func queryFuncForTest(expectNamespace, expectPod, expectContainer string, items []logTestItem) func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error) {
	return func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error) {
		if expectNamespace != it.podNamespace || expectPod != it.podName || expectContainer != opts.Container {
			return nil, fmt.Errorf("want keys: [%q,%q,%q], got keys: [%q,%q,%q]",
				expectNamespace, expectPod, expectContainer, it.podNamespace, it.podName, opts.Container)
		}
		var lines []string
		if opts.SinceTime == nil {
			for _, item := range items {
				t := time.Unix(item.Timestamp/int64(time.Second), item.Timestamp%int64(time.Second))
				line := t.Format(time.RFC3339Nano) + " " + item.Content
				lines = append(lines, line)
			}
		} else {
			for i, item := range items {
				t := time.Unix(item.Timestamp/int64(time.Second), item.Timestamp%int64(time.Second))
				if t.After(opts.SinceTime.Time) || t.Equal(opts.SinceTime.Time) {
					for n := len(items); i < n; i++ {
						item := items[i]
						t := time.Unix(item.Timestamp/int64(time.Second), item.Timestamp%int64(time.Second))
						line := t.Format(time.RFC3339Nano) + " " + item.Content
						lines = append(lines, line)
					}
					break
				}
			}
		}
		// printf("request since time: %v \n", opts.SinceTime)
		// printf("response logs: \n%s\n", strings.Join(lines, "\n"))
		return io.NopCloser(strings.NewReader(strings.Join(lines, "\n"))), nil
	}
}

const (
	kubernetesLogScheme = "container"
)

func Test_cStorage_Iterator_Next(t *testing.T) {
	tests := []struct {
		name        string
		source      []logTestItem
		bufferLines int64
		sel         *storage.Selector
		want        []interface{}
	}{
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   11,
				End:     13,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "11",
					UnixNano:  11,
					Offset:    initialOffset,
					Content:   "1111",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "12",
					UnixNano:  12,
					Offset:    initialOffset,
					Content:   "2222",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   12,
				End:     13,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "12",
					UnixNano:  12,
					Offset:    initialOffset,
					Content:   "2222",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   13,
				End:     16,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "13",
					UnixNano:  13,
					Offset:    initialOffset,
					Content:   "3333",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "14",
					UnixNano:  14,
					Offset:    initialOffset,
					Content:   "4444.0",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "14",
					UnixNano:  14,
					Offset:    initialOffset + 1,
					Content:   "4444.1",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "15",
					UnixNano:  15,
					Offset:    initialOffset,
					Content:   "5555",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   100,
				End:     1000,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: nil,
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   10,
				End:     11,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: nil,
		},
		{
			source:      commonLogTestItems,
			bufferLines: defaultBufferLines,
			sel: &storage.Selector{
				Start:   11,
				End:     13,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "11",
					UnixNano:  11,
					Offset:    initialOffset,
					Content:   "1111",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "12",
					UnixNano:  12,
					Offset:    initialOffset,
					Content:   "2222",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: defaultBufferLines,
			sel: &storage.Selector{
				Start:   17,
				End:     18,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "17",
					UnixNano:  17,
					Offset:    initialOffset,
					Content:   "7777",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "17",
					UnixNano:  17,
					Offset:    initialOffset + 1,
					Content:   "7777",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: defaultBufferLines,
			sel: &storage.Selector{
				Start:   16,
				End:     20,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "16",
					UnixNano:  16,
					Offset:    initialOffset,
					Content:   "6666",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "17",
					UnixNano:  17,
					Offset:    initialOffset,
					Content:   "7777",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "17",
					UnixNano:  17,
					Offset:    initialOffset + 1,
					Content:   "7777",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "16",
					UnixNano:  16,
					Offset:    initialOffset,
					Content:   "6666.1",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "19",
					UnixNano:  19,
					Offset:    initialOffset,
					Content:   "9999",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			name:        "Option_No_Live",
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   11,
				End:     13,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: map[string]interface{}{
					storage.IsLive:        false,
					storage.ContainerName: "container_name1",
					storage.PodNamespace:  "namespace1",
					storage.PodName:       "name1",
					storage.ClusterName:   "cluster_name1",
				},
			},
			want: nil,
		},
		{
			name:        "No_Pod_Meta",
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   11,
				End:     13,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: map[string]interface{}{
					storage.IsLive:        true,
					storage.ContainerName: "",
					storage.PodNamespace:  "",
					storage.PodName:       "",
					storage.ClusterName:   "",
				},
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			tt = tt
			namespace := tt.sel.Options[storage.PodNamespace].(string)
			name := tt.sel.Options[storage.PodName].(string)
			container := tt.sel.Options[storage.ContainerName].(string)

			s := &cStorage{
				log: logrusx.New(),
				getQueryFunc: func(clusterName string) (func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error), error) {
					return queryFuncForTest(namespace, name, container, tt.source), nil
				},
				bufferLines: tt.bufferLines,
			}
			it, err := s.Iterator(context.TODO(), tt.sel)
			if err != nil {
				t.Errorf("cStorage.Iterator() got error: %s", it.Error())
				return
			}
			var got []interface{}
			for it.Next() {
				got = append(got, it.Value())
			}
			if it.Error() != nil {
				t.Errorf("cStorage.Iterator().Next() got error: %s", it.Error())
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("cStorage.Iterator().Next() \ngot %v, \nwant %v", jsonx.MarshalAndIndent(got), jsonx.MarshalAndIndent(tt.want))
			}
		})
	}
}

func Test_cStorage_Iterator_Prev(t *testing.T) {
	tests := []struct {
		name        string
		source      []logTestItem
		bufferLines int64
		sel         *storage.Selector
		want        []interface{}
	}{
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   11,
				End:     13,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "12",
					UnixNano:  12,
					Offset:    initialOffset,
					Content:   "2222",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "11",
					UnixNano:  11,
					Offset:    initialOffset,
					Content:   "1111",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   12,
				End:     13,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "12",
					UnixNano:  12,
					Offset:    initialOffset,
					Content:   "2222",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   13,
				End:     16,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "15",
					UnixNano:  15,
					Offset:    initialOffset,
					Content:   "5555",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "14",
					UnixNano:  14,
					Offset:    initialOffset + 1,
					Content:   "4444.1",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "14",
					UnixNano:  14,
					Offset:    initialOffset,
					Content:   "4444.0",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "13",
					UnixNano:  13,
					Offset:    initialOffset,
					Content:   "3333",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   100,
				End:     1000,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: nil,
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   0,
				End:     11,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: nil,
		},
		{
			source:      commonLogTestItems,
			bufferLines: defaultBufferLines,
			sel: &storage.Selector{
				Start:   10,
				End:     13,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "12",
					UnixNano:  12,
					Offset:    initialOffset,
					Content:   "2222",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "11",
					UnixNano:  11,
					Offset:    initialOffset,
					Content:   "1111",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: defaultBufferLines,
			sel: &storage.Selector{
				Start:   16,
				End:     18,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "16",
					UnixNano:  16,
					Offset:    initialOffset,
					Content:   "6666.1",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "17",
					UnixNano:  17,
					Offset:    initialOffset + 1,
					Content:   "7777",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "17",
					UnixNano:  17,
					Offset:    initialOffset,
					Content:   "7777",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "16",
					UnixNano:  16,
					Offset:    initialOffset,
					Content:   "6666",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: defaultBufferLines,
			sel: &storage.Selector{
				Start:   16,
				End:     20,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "19",
					UnixNano:  19,
					Offset:    initialOffset,
					Content:   "9999",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "16",
					UnixNano:  16,
					Offset:    initialOffset,
					Content:   "6666.1",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "17",
					UnixNano:  17,
					Offset:    initialOffset + 1,
					Content:   "7777",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "17",
					UnixNano:  17,
					Offset:    initialOffset,
					Content:   "7777",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "16",
					UnixNano:  16,
					Offset:    initialOffset,
					Content:   "6666",
					Level:     "",
					RequestId: "",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			namespace := tt.sel.Options[storage.PodNamespace].(string)
			name := tt.sel.Options[storage.PodName].(string)
			container := tt.sel.Options[storage.ContainerName].(string)
			s := &cStorage{
				log: logrusx.New(),
				getQueryFunc: func(clusterName string) (func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error), error) {
					return queryFuncForTest(namespace, name, container, tt.source), nil
				},
				bufferLines: tt.bufferLines,
			}
			it, err := s.Iterator(context.TODO(), tt.sel)
			if err != nil {
				t.Errorf("cStorage.Iterator() got error: %s", it.Error())
				return
			}
			var got []interface{}
			for it.Prev() {
				got = append(got, it.Value())
			}
			if it.Error() != nil {
				t.Errorf("cStorage.Iterator().Prev() got error: %s", it.Error())
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("cStorage.Iterator().Prev() \ngot %v, \nwant %v", jsonx.MarshalAndIndent(got), jsonx.MarshalAndIndent(tt.want))
			}
		})
	}
}

func Test_cStorage_Iterator_FirstNext(t *testing.T) {
	tests := []struct {
		name        string
		source      []logTestItem
		bufferLines int64
		sel         *storage.Selector
		want        []interface{}
	}{
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   13,
				End:     15,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "13",
					UnixNano:  13,
					Offset:    initialOffset,
					Content:   "3333",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "14",
					UnixNano:  14,
					Offset:    initialOffset,
					Content:   "4444.0",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "14",
					UnixNano:  14,
					Offset:    initialOffset + 1,
					Content:   "4444.1",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   100,
				End:     1000,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: nil,
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   10,
				End:     11,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			namespace := tt.sel.Options[storage.PodNamespace].(string)
			name := tt.sel.Options[storage.PodName].(string)
			container := tt.sel.Options[storage.ContainerName].(string)
			s := &cStorage{
				log: logrusx.New(),
				getQueryFunc: func(clusterName string) (func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error), error) {
					return queryFuncForTest(namespace, name, container, tt.source), nil
				},
				bufferLines: tt.bufferLines,
			}
			it, err := s.Iterator(context.TODO(), tt.sel)
			if err != nil {
				t.Errorf("cStorage.Iterator() got error: %s", it.Error())
				return
			}
			var got []interface{}
			if it.First() {
				got = append(got, it.Value())
			}
			for it.Next() {
				got = append(got, it.Value())
			}
			if it.Error() != nil {
				t.Errorf("cStorage.Iterator() First() and Next() got error: %s", it.Error())
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("cStorage.Iterator() First() and Next() \ngot %v, \nwant %v", jsonx.MarshalAndIndent(got), jsonx.MarshalAndIndent(tt.want))
			}
		})
	}
}

func Test_cStorage_Iterator_LastPrev(t *testing.T) {
	tests := []struct {
		name        string
		source      []logTestItem
		bufferLines int64
		sel         *storage.Selector
		want        []interface{}
	}{
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   13,
				End:     15,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: []interface{}{
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "14",
					UnixNano:  14,
					Offset:    initialOffset + 1,
					Content:   "4444.1",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "14",
					UnixNano:  14,
					Offset:    initialOffset,
					Content:   "4444.0",
					Level:     "",
					RequestId: "",
				},
				&pb.LogItem{
					Id:        "test_id",
					Source:    "container",
					Stream:    "",
					Timestamp: "13",
					UnixNano:  13,
					Offset:    initialOffset,
					Content:   "3333",
					Level:     "",
					RequestId: "",
				},
			},
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   100,
				End:     1000,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: commonTestOptions,
			},
			want: nil,
		},
		{
			source:      commonLogTestItems,
			bufferLines: 1,
			sel: &storage.Selector{
				Start:   10,
				End:     11,
				Scheme:  kubernetesLogScheme,
				Filters: commonTestFilters,
				Options: map[string]interface{}{
					storage.IsLive:        false,
					storage.ContainerName: "container_name1",
					storage.PodNamespace:  "namespace1",
					storage.PodName:       "name1",
					storage.ClusterName:   "cluster_name1",
				},
			},
			want: nil,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			namespace := tt.sel.Options[storage.PodNamespace].(string)
			name := tt.sel.Options[storage.PodName].(string)
			container := tt.sel.Options[storage.ContainerName].(string)
			s := &cStorage{
				log: logrusx.New(),
				getQueryFunc: func(clusterName string) (func(it *logsIterator, opts *v1.PodLogOptions) (io.ReadCloser, error), error) {
					return queryFuncForTest(namespace, name, container, tt.source), nil
				},
				bufferLines: tt.bufferLines,
			}
			it, err := s.Iterator(context.TODO(), tt.sel)
			if err != nil {
				t.Errorf("cStorage.Iterator() got error: %s", it.Error())
				return
			}
			var got []interface{}
			if it.Last() {
				got = append(got, it.Value())
			}
			for it.Prev() {
				got = append(got, it.Value())
			}
			if it.Error() != nil {
				t.Errorf("cStorage.Iterator() Last() and Prev()  got error: %s", it.Error())
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("cStorage.Iterator() Last() and Prev() \ngot %v, \nwant %v", jsonx.MarshalAndIndent(got), jsonx.MarshalAndIndent(tt.want))
			}
		})
	}
}
