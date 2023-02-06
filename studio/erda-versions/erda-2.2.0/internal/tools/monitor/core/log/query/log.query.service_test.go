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
	"math"
	"reflect"
	"testing"
	"time"

	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

func Test_toQuerySelector(t *testing.T) {
	tests := []struct {
		name    string
		req     Request
		want    *storage.Selector
		wantErr bool
	}{
		{
			req: &LogRequest{
				ID:    "testid",
				Start: 1,
				End:   math.MaxInt64,
			},
			wantErr: true,
		},
		{
			req: &LogRequest{
				ID:    "testid",
				Start: 10,
				End:   1,
			},
			wantErr: true,
		},
		{
			req: &LogRequest{
				ID:    "testid",
				Start: 0,
				End:   10,
			},
			want: &storage.Selector{
				Start:  0,
				End:    10,
				Scheme: "",
				Filters: []*storage.Filter{
					{
						Key:   "id",
						Op:    storage.EQ,
						Value: "testid",
					},
				},
				Options: map[string]interface{}{
					storage.SelectorKeyCount: int64(0),
					storage.IsLive:           false,
				},
			},
		},
		{
			req: &LogRequest{
				ID:     "testid",
				Start:  1,
				End:    100,
				Count:  -200,
				Source: "container",
			},
			want: &storage.Selector{
				Start:  1,
				End:    100,
				Scheme: "container",
				Filters: []*storage.Filter{
					{
						Key:   "id",
						Op:    storage.EQ,
						Value: "testid",
					},
					{
						Key:   "source",
						Op:    storage.EQ,
						Value: "container",
					},
				},
				Options: map[string]interface{}{
					storage.SelectorKeyCount: int64(-200),
					storage.IsLive:           false,
				},
			},
		},
		{
			req: &pb.GetLogByExpressionRequest{
				Start: 1,
				End:   100,
				Count: 100,
				ExtraFilter: &pb.ExtraFilter{
					After:          &pb.LogUniqueID{Id: "id-1", UnixNano: 123, Offset: 10},
					PositionOffset: 12,
				},
				QueryExpression: "tags.dice_org_id:1",
				QueryMeta: &pb.QueryMeta{
					OrgName:               "erda",
					PreferredBufferSize:   10,
					PreferredIterateStyle: pb.IterateStyle_Scroll,
				},
			},
			want: &storage.Selector{
				Start:  2,
				End:    100,
				Scheme: "advanced",
				Filters: []*storage.Filter{
					{
						Key:   "_",
						Op:    storage.EXPRESSION,
						Value: "tags.dice_org_id:1",
					},
				},
				Meta: storage.QueryMeta{
					OrgNames:              []string{"erda"},
					PreferredBufferSize:   10,
					PreferredIterateStyle: storage.Scroll,
				},
				Skip: storage.ResultSkip{
					AfterId:    &storage.UniqueId{Id: "id-1", Offset: 10, Timestamp: 123},
					FromOffset: 12,
				},
				Options: map[string]interface{}{
					storage.SelectorKeyCount: int64(100),
					storage.IsLive:           false,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := toQuerySelector(tt.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("toQuerySelector() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("toQuerySelector() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getIterator(t *testing.T) {
	tests := []struct {
		name    string
		sel     *storage.Selector
		service *logQueryService
		want    storekit.Iterator
		wantErr bool
	}{
		{
			sel: &storage.Selector{
				Start:  2,
				End:    100,
				Scheme: "advanced",
				Filters: []*storage.Filter{
					{
						Key:   "_",
						Op:    storage.EXPRESSION,
						Value: "tags.dice_org_id:1",
					},
				},
				Meta: storage.QueryMeta{
					OrgNames:              []string{"erda"},
					PreferredBufferSize:   10,
					PreferredIterateStyle: storage.Scroll,
				},
				Skip: storage.ResultSkip{
					AfterId:    &storage.UniqueId{Id: "id-1", Offset: 10, Timestamp: 123},
					FromOffset: 12,
				},
			},
			service: &logQueryService{
				storageReader: &mockStorage{},
			},
			want:    storekit.NewListIterator(1, 2, 3),
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := tt.service.getIterator(context.Background(), tt.sel)
			if (err != nil) != tt.wantErr {
				t.Errorf("getIterator() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getIterator() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_splitSelectors(t *testing.T) {
	start := int64(0)
	end := time.Date(2022, 1, 1, 1, 1, 1, 1, time.Local).UnixNano()
	sel := &storage.Selector{
		Start: start,
		End:   end,
	}

	tests := []struct {
		name        string
		sel         *storage.Selector
		interval    time.Duration
		deltaFactor float64
		maxSlices   int
		want        []*storage.Selector
		wantErr     bool
	}{
		{
			sel:         sel,
			interval:    time.Hour,
			deltaFactor: 1,
			maxSlices:   0,
			want: []*storage.Selector{
				{
					Start: 0,
					End:   end,
				},
			},
		},
		{
			sel:         sel,
			interval:    time.Hour,
			deltaFactor: 1,
			maxSlices:   1,
			want: []*storage.Selector{
				{
					Start: 0,
					End:   end,
				},
			},
		},
		{
			sel:         sel,
			interval:    time.Hour,
			deltaFactor: 1,
			maxSlices:   2,
			want: []*storage.Selector{
				{
					Start: 0,
					End:   end - int64(time.Hour),
				},
				{
					Start: end - int64(time.Hour),
					End:   end,
				},
			},
		},
		{
			sel: &storage.Selector{
				Start: end - int64(24*time.Hour),
				End:   end,
			},
			interval:    time.Hour * 24 * 365,
			deltaFactor: 1,
			maxSlices:   10,
			want: []*storage.Selector{
				{
					Start: end - int64(24*time.Hour),
					End:   end,
				},
			},
		},
		{
			sel: &storage.Selector{
				Start: end - int64(24*time.Hour),
				End:   end,
			},
			interval:    time.Hour * 12,
			deltaFactor: 1,
			maxSlices:   25,
			want: []*storage.Selector{
				{
					Start: end - int64(24*time.Hour),
					End:   end - int64(12*time.Hour),
				},
				{
					Start: end - int64(12*time.Hour),
					End:   end,
				},
			},
		},
		{
			sel: &storage.Selector{
				Start: end - int64(24*time.Hour),
				End:   end,
			},
			interval:    time.Hour,
			deltaFactor: 2,
			maxSlices:   4,
			want: []*storage.Selector{
				{
					Start: end - int64(24*time.Hour),
					End:   end - int64(7*time.Hour),
				},
				{
					Start: end - int64(7*time.Hour),
					End:   end - int64(3*time.Hour),
				},
				{
					Start: end - int64(3*time.Hour),
					End:   end - int64(time.Hour),
				},
				{
					Start: end - int64(time.Hour),
					End:   end,
				},
			},
		},
	}

	s := &logQueryService{}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := s.splitSelectors(tt.sel, tt.interval, tt.deltaFactor, tt.maxSlices)
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("splitSelectors() = %+v, want %+v", got, tt.want)
			}
		})
	}
}

func Test_realTimeNoIterator(t *testing.T) {
	tests := []struct {
		name    string
		req     *pb.GetLogByRuntimeRequest
		service *logQueryService
		want    bool
		wantErr bool
	}{
		{
			req: &pb.GetLogByRuntimeRequest{
				ContainerName: "no_container",
				Live:          true,
				Id:            "123",
			},
			service: &logQueryService{
				storageReader: &mockStorage{},
			},
			want:    false,
			wantErr: false,
		},
		{
			req: &pb.GetLogByRuntimeRequest{
				ContainerName: "container",
				Live:          false,
				Id:            "123",
			},
			service: &logQueryService{
				storageReader: &mockStorage{},
			},
			want:    false,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := tt.service.GetLogByRealtime(context.Background(), tt.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("Test_realTimeNoIterator error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got.IsFallback, tt.want) {
				t.Errorf("Test_realTimeNoIterator,IsFallBack = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_isRequestUseFallBack(t *testing.T) {
	now := time.Now()
	timeNow = func() time.Time {
		return now
	}
	tests := []struct {
		name string
		req  *pb.GetLogByRuntimeRequest
		want bool
	}{
		{
			name: "is_first_query",
			req: &pb.GetLogByRuntimeRequest{
				Start:        int64(0),
				End:          int64(0),
				IsFirstQuery: true,
			},
			want: true,
		},
		{
			name: "now-4",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(timeNow().UnixNano()),
				End:   int64(timeNow().Add(time.Second * 4).UnixNano()),
			},
			want: true,
		},
		{
			name: "-2-now",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(timeNow().Add(time.Second * -2).UnixNano()),
				End:   int64(timeNow().UnixNano()),
			},
			want: true,
		},
		{
			name: "-3-now",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(timeNow().Add(time.Second * -3).UnixNano()),
				End:   int64(timeNow().UnixNano()),
			},
			want: true,
		},
		{
			name: "-4-now",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(timeNow().Add(time.Second * -4).UnixNano()),
				End:   int64(timeNow().UnixNano()),
			},
			want: false,
		},
		{
			name: "-5-now",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(timeNow().Add(time.Second * -5).UnixNano()),
				End:   int64(timeNow().UnixNano()),
			},
			want: false,
		},
		{
			name: "0-now",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(0),
				End:   int64(timeNow().UnixNano()),
			},
			want: true,
		},
		{
			name: "0-(4)",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(0),
				End:   int64(timeNow().Add(time.Second * 4).UnixNano()),
			},
			want: false,
		},
		{
			name: "0-(3)",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(0),
				End:   int64(timeNow().Add(time.Second * 3).UnixNano()),
			},
			want: true,
		},
		{
			name: "0-(-2)",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(0),
				End:   int64(timeNow().Add(time.Second * -2).UnixNano()),
			},
			want: true,
		},
		{
			name: "0-(-3)",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(0),
				End:   int64(timeNow().Add(time.Second * -3).UnixNano()),
			},
			want: true,
		},
		{
			name: "0-(-4)",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(0),
				End:   int64(timeNow().Add(time.Second * -4).UnixNano()),
			},
			want: false,
		},
		{
			name: "0-(-5)",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(0),
				End:   int64(timeNow().Add(time.Second * -5).UnixNano()),
			},
			want: false,
		},
		{
			name: "-5-5",
			req: &pb.GetLogByRuntimeRequest{
				Start: int64(timeNow().Add(time.Second * -5).UnixNano()),
				End:   int64(timeNow().Add(time.Second * 5).UnixNano()),
			},
			want: false,
		},
	}
	service := &logQueryService{
		storageReader: &mockStorage{},
		p: &provider{
			Cfg: &config{
				DelayBackoffStartTime: time.Second * -3,
				DelayBackoffEndTime:   time.Second * 4,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := service.isRequestUseFallBack(tt.req)

			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Test_realTimeNoIterator,IsFallBack = %v, want %v", got, tt.want)
			}
		})
	}
}

type mockStorage struct {
}

func (m mockStorage) NewWriter(ctx context.Context) (storekit.BatchWriter, error) {
	panic("implement me")
}

func (m mockStorage) Iterator(ctx context.Context, sel *storage.Selector) (storekit.Iterator, error) {
	it := storekit.NewListIterator(1, 2, 3)
	return it, nil
}
