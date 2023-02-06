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

// import (
// 	"fmt"
// 	"reflect"
// 	"regexp"
// 	"sort"
// 	"testing"

// 	"github.com/scylladb/gocqlx/qb"

// 	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
// )

// func Test_provider_getLogItems(t *testing.T) {
// 	type fields struct {
// 		p *provider
// 	}
// 	type args struct {
// 		r *RequestCtx
// 	}
// 	tests := []struct {
// 		name string
// 		fields
// 		args    args
// 		want    []*pb.LogItem
// 		wantErr bool
// 	}{
// 		{
// 			name: "query with RequestID",
// 			args: args{
// 				r: &RequestCtx{
// 					RequestID:     "requestID-1",
// 					ApplicationID: "app-1",
// 				},
// 			},
// 			want: []*pb.LogItem{
// 				{
// 					Id:         "aaa",
// 					Source:     "container",
// 					Stream:     "stdout",
// 					TimeBucket: "1604880000000000000",
// 					Timestamp:  "1604880001000000000",
// 					Offset:     "11",
// 					Content:    "hello world",
// 					Level:      "INFO",
// 					RequestId:  "requestID-1",
// 				},
// 			},
// 			wantErr: false,
// 		},
// 		{
// 			name: "query base log",
// 			args: args{
// 				r: &RequestCtx{
// 					RequestID:     "",
// 					LogID:         "",
// 					Source:        "container",
// 					ID:            "aaa",
// 					Stream:        "stdout",
// 					Start:         1604880000000000000,
// 					End:           1604880001000000000,
// 					Count:         -200,
// 					ApplicationID: "app-1",
// 					ClusterName:   "",
// 				},
// 			},
// 			want: []*pb.LogItem{
// 				{
// 					Id:         "aaa",
// 					Source:     "container",
// 					Stream:     "stdout",
// 					TimeBucket: "1604880000000000000",
// 					Timestamp:  "1604880001000000000",
// 					Offset:     "11",
// 					Content:    "hello world",
// 					Level:      "INFO",
// 					RequestId:  "",
// 				},
// 				{
// 					Id:         "aaa",
// 					Source:     "container",
// 					Stream:     "stdout",
// 					TimeBucket: "1604880000000000000",
// 					Timestamp:  "1604880002000000000",
// 					Offset:     "11",
// 					Content:    "goodbye world",
// 					Level:      "INFO",
// 					RequestId:  "",
// 				},
// 			},
// 			wantErr: false,
// 		},
// 		{
// 			name: "query base log with pattern",
// 			args: args{
// 				r: &RequestCtx{
// 					RequestID:     "",
// 					LogID:         "",
// 					Source:        "container",
// 					ID:            "aaa",
// 					Stream:        "stdout",
// 					Start:         1604880000000000000,
// 					End:           1604880001000000000,
// 					Count:         -200,
// 					ApplicationID: "app-1",
// 					ClusterName:   "",
// 					Pattern:       "hello",
// 					patternRegexp: regexp.MustCompile("hello"),
// 				},
// 			},
// 			want: []*pb.LogItem{
// 				{
// 					Id:         "aaa",
// 					Source:     "container",
// 					Stream:     "stdout",
// 					TimeBucket: "1604880000000000000",
// 					Timestamp:  "1604880001000000000",
// 					Offset:     "11",
// 					Content:    "hello world",
// 					Level:      "INFO",
// 					RequestId:  "",
// 					Pattern:    "hello",
// 				},
// 			},
// 			wantErr: false,
// 		},
// 		{
// 			name: "query base log when count=0",
// 			args: args{
// 				r: &RequestCtx{
// 					RequestID:     "",
// 					LogID:         "",
// 					Source:        "container",
// 					ID:            "aaa",
// 					Stream:        "stdout",
// 					Start:         1604880000000000000,
// 					End:           1604880001000000000,
// 					Count:         0,
// 					ApplicationID: "app-1",
// 					ClusterName:   "",
// 				},
// 			},
// 			want:    nil,
// 			wantErr: false,
// 		},
// 		{
// 			name: "query base log with query error",
// 			fields: fields{
// 				p: &provider{
// 					cqlQuery: &mockCqlQuery{errorTrigger: true},
// 				},
// 			},
// 			args: args{
// 				r: &RequestCtx{
// 					RequestID:     "",
// 					LogID:         "",
// 					Source:        "container",
// 					ID:            "aaa",
// 					Stream:        "stdout",
// 					Start:         1604880000000000000,
// 					End:           1604880001000000000,
// 					Count:         -200,
// 					ApplicationID: "app-1",
// 					ClusterName:   "",
// 				},
// 			},
// 			want:    nil,
// 			wantErr: true,
// 		},
// 		{
// 			name: "query with RequestID with query error",
// 			fields: fields{
// 				p: &provider{
// 					cqlQuery: &mockCqlQuery{errorTrigger: true},
// 				},
// 			},
// 			args: args{
// 				r: &RequestCtx{
// 					RequestID:     "requestID-1",
// 					ApplicationID: "app-1",
// 				},
// 			},
// 			want:    nil,
// 			wantErr: true,
// 		},
// 	}
// 	for _, tt := range tests {
// 		t.Run(tt.name, func(t *testing.T) {
// 			mp := mockProvider()
// 			if tt.fields.p != nil {
// 				mp = tt.fields.p
// 			}
// 			got, err := mp.getLogItems(tt.args.r)
// 			if (err != nil) != tt.wantErr {
// 				t.Errorf("getLogItems() error = %v, wantErr %v", err, tt.wantErr)
// 				return
// 			}
// 			if !reflect.DeepEqual(got, tt.want) {
// 				t.Errorf("getLogItems() got = %v, want %v", got, tt.want)
// 			}
// 		})
// 	}
// }

// func Test_convertToLogList(t *testing.T) {
// 	type args struct {
// 		list []*SavedLog
// 	}
// 	tests := []struct {
// 		name    string
// 		args    args
// 		want    []*pb.LogItem
// 		wantErr bool
// 	}{
// 		{
// 			name: "impossible gzip data",
// 			args: args{
// 				list: []*SavedLog{
// 					{
// 						ID:         "aaa",
// 						Source:     "container",
// 						Stream:     "stdout",
// 						TimeBucket: 1604880000000000000,
// 						Timestamp:  1604880001000000000,
// 						Offset:     11,
// 						Content:    []byte("impossible gzip data"),
// 						Level:      "INFO",
// 						RequestID:  "requestID-1",
// 					},
// 				},
// 			},
// 			want:    nil,
// 			wantErr: true,
// 		},
// 	}
// 	for _, tt := range tests {
// 		t.Run(tt.name, func(t *testing.T) {
// 			got, err := convertToLogList(tt.args.list)
// 			if (err != nil) != tt.wantErr {
// 				t.Errorf("convertToLogList() error = %v, wantErr %v", err, tt.wantErr)
// 				return
// 			}
// 			if !reflect.DeepEqual(got, tt.want) {
// 				t.Errorf("convertToLogList() got = %v, want %v", got, tt.want)
// 			}
// 		})
// 	}
// }

// type mockCqlQuery struct {
// 	errorTrigger bool
// 	emptyResult  bool
// }

// func (m *mockCqlQuery) Query(builder *qb.SelectBuilder, binding qb.M, dest interface{}) error {
// 	stmt, _ := builder.ToCql()
// 	switch stmt {
// 	case "SELECT * FROM spot_prod.base_log_meta WHERE tags['dice_application_id']=? LIMIT 1 ",
// 		"SELECT * FROM spot_prod.base_log_meta WHERE source=? AND id=? LIMIT 1 ",
// 		"SELECT * FROM spot_prod.base_log_meta WHERE id=? AND source=? LIMIT 1 ": // get meta info
// 		tmp := []*LogMeta{
// 			{
// 				Source: "container",
// 				ID:     "aaa",
// 				Tags: map[string]string{
// 					"dice_application_id": "app-1",
// 					"dice_org_name":       "org-1",
// 					"dice_cluster_name":   "cluster-1",
// 				},
// 			},
// 		}
// 		reflect.ValueOf(dest).Elem().Set(reflect.ValueOf(tmp))
// 	case "SELECT * FROM spot_org_1.base_log WHERE request_id=? ":
// 		tmp := []*SavedLog{
// 			{
// 				ID:         "aaa",
// 				Source:     "container",
// 				Stream:     "stdout",
// 				TimeBucket: 1604880000000000000,
// 				Timestamp:  1604880001000000000,
// 				Offset:     11,
// 				Content:    gzipString("hello world"),
// 				Level:      "INFO",
// 				RequestID:  "requestID-1",
// 			},
// 		}
// 		reflect.ValueOf(dest).Elem().Set(reflect.ValueOf(tmp))
// 	case "SELECT * FROM spot_org_1.base_log WHERE source=? AND id=? AND stream=? AND time_bucket=? AND timestamp>=? AND timestamp<? ORDER BY timestamp DESC,offset DESC LIMIT 200 ":
// 		tmp := []*SavedLog{
// 			{
// 				ID:         "aaa",
// 				Source:     "container",
// 				Stream:     "stdout",
// 				TimeBucket: 1604880000000000000,
// 				Timestamp:  1604880002000000000,
// 				Offset:     11,
// 				Content:    gzipString("goodbye world"),
// 				Level:      "INFO",
// 				RequestID:  "",
// 			},
// 			{
// 				ID:         "aaa",
// 				Source:     "container",
// 				Stream:     "stdout",
// 				TimeBucket: 1604880000000000000,
// 				Timestamp:  1604880001000000000,
// 				Offset:     11,
// 				Content:    gzipString("hello world"),
// 				Level:      "INFO",
// 				RequestID:  "",
// 			},
// 		}
// 		reflect.ValueOf(dest).Elem().Set(reflect.ValueOf(tmp))
// 	case "SELECT * FROM spot_org_1.base_log WHERE source=? AND id=? AND stream=? AND time_bucket=? AND timestamp>=? AND timestamp<? ORDER BY timestamp ASC,offset ASC ":
// 		tmp := []*SavedLog{
// 			{
// 				ID:         "aaa",
// 				Source:     "container",
// 				Stream:     "stdout",
// 				TimeBucket: 1604880000000000000,
// 				Timestamp:  1604880001000000000,
// 				Offset:     11,
// 				Content:    gzipString("hello world"),
// 				Level:      "INFO",
// 				RequestID:  "",
// 			},
// 			{
// 				ID:         "aaa",
// 				Source:     "container",
// 				Stream:     "stdout",
// 				TimeBucket: 1604880000000000000,
// 				Timestamp:  1604880002000000000,
// 				Offset:     11,
// 				Content:    gzipString("goodbye world"),
// 				Level:      "INFO",
// 				RequestID:  "",
// 			},
// 		}
// 		reflect.ValueOf(dest).Elem().Set(reflect.ValueOf(tmp))
// 	case "SELECT * FROM spot_prod.base_log WHERE request_id=? ",
// 		"SELECT * FROM spot_prod.base_log WHERE source=? AND id=? AND stream=? AND time_bucket=? AND timestamp>=? AND timestamp<? ORDER BY timestamp DESC,offset DESC LIMIT 200 ":
// 	default:
// 	}
// 	if m.errorTrigger {
// 		return fmt.Errorf("error triggered")
// 	} else if m.emptyResult {
// 		reflect.ValueOf(dest).Elem().Set(reflect.Zero(reflect.TypeOf(dest).Elem()))
// 		return nil
// 	}
// 	return nil
// }

// func Test_filterWithRegexp(t *testing.T) {
// 	type args struct {
// 		logs []*pb.LogItem
// 		r    *RequestCtx
// 	}
// 	tests := []struct {
// 		name string
// 		args args
// 		want []*pb.LogItem
// 	}{
// 		{
// 			name: "normal",
// 			args: args{
// 				logs: []*pb.LogItem{
// 					{
// 						Timestamp: "1",
// 						Content:   "aaabbbccc",
// 					},
// 					{
// 						Timestamp: "2",
// 						Content:   "xxx",
// 					},
// 					{
// 						Timestamp: "3",
// 						Content:   "bbbddd",
// 					},
// 					{
// 						Timestamp: "4",
// 						Content:   "yyy",
// 					},
// 					{
// 						Timestamp: "5",
// 						Content:   "eeebbbxxx",
// 					},
// 				},
// 				r: &RequestCtx{
// 					patternRegexp: regexp.MustCompile("bbb"),
// 				},
// 			},
// 			want: []*pb.LogItem{
// 				{
// 					Timestamp: "1",
// 					Content:   "aaabbbccc",
// 				},
// 				{
// 					Timestamp: "3",
// 					Content:   "bbbddd",
// 				},
// 				{
// 					Timestamp: "5",
// 					Content:   "eeebbbxxx",
// 				},
// 			},
// 		},
// 		{
// 			name: "edge",
// 			args: args{
// 				logs: []*pb.LogItem{
// 					{
// 						Content: "aaabbbccc",
// 					},
// 				},
// 				r: &RequestCtx{
// 					patternRegexp: regexp.MustCompile("bbb"),
// 				},
// 			},
// 			want: []*pb.LogItem{
// 				{
// 					Content: "aaabbbccc",
// 				},
// 			},
// 		},
// 	}
// 	for _, tt := range tests {
// 		t.Run(tt.name, func(t *testing.T) {
// 			got := filterWithRegexp(tt.args.logs, tt.args.r)
// 			sort.Sort(Logs(got))
// 			if !reflect.DeepEqual(got, tt.want) {
// 				t.Errorf("filterWithRegexp() = %v, want %v", got, tt.want)
// 			}
// 		})
// 	}
// }
