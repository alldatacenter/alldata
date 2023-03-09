/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package metriccache

import (
	"reflect"
	"testing"
	"time"
)

func EqualPodResourceMetric(a, b *podResourceMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualNodeResourceMetric(a, b *nodeResourceMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualContainerResourceMetric(a, b *containerResourceMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualPodThrottledMetric(a, b *podThrottledMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualContainerThrottledMetric(a, b *containerThrottledMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualBECPUResourceMetric(a, b *beCPUResourceMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualContainerCPIMetric(a, b *containerCPIMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualContainerPSIMetric(a, b *containerPSIMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualPodCPIMetric(a, b *podPSIMetric) bool {
	if !a.Timestamp.Equal(b.Timestamp) {
		return false
	}
	a.Timestamp = b.Timestamp
	return reflect.DeepEqual(a, b)
}

func EqualRawRecord(a, b *rawRecord) bool {
	return reflect.DeepEqual(a, b)
}

func Test_storage_PodResourceMetric_CRUD(t *testing.T) {
	now := time.Now()
	uid := "test-uid"
	type args struct {
		samples []podResourceMetric
		uid     string
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []podResourceMetric
	}{
		{
			name: "pod-metric-crud",
			args: args{
				samples: []podResourceMetric{
					{
						ID:              uint64(now.UnixNano()),
						PodUID:          uid,
						CPUUsedCores:    1,
						MemoryUsedBytes: 2,
						Timestamp:       now,
					},
				},
				uid:   uid,
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []podResourceMetric{
				{
					ID:              uint64(now.UnixNano()),
					PodUID:          uid,
					CPUUsedCores:    1,
					MemoryUsedBytes: 2,
					Timestamp:       now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			for _, sample := range tt.args.samples {
				err := s.InsertPodResourceMetric(&sample)
				if err != nil {
					t.Errorf("insert pod metric error %v", err)
				}
			}

			got, err := s.GetPodResourceMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetPodResourceMetric got error %v", err)
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetPodResourceMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualPodResourceMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetPodResourceMetric() = %v, want %v", got[i], tt.want[i])
				}
			}

			gotNum, err := s.CountPodResourceMetric()
			if err != nil {
				t.Errorf("CountPodResourceMetric got error %v", err)
			}
			if gotNum != int64(len(tt.want)) {
				t.Errorf("CountPodResourceMetric() = %v, want %v", gotNum, len(tt.want))
			}

			err = s.DeletePodResourceMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeletePodResourceMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetPodResourceMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetPodResourceMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetPodResourceMetric after delete %v", gotAfterDelete)
			}
		})
	}
}

func Test_storage_NodeResourceMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		samples []nodeResourceMetric
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []nodeResourceMetric
	}{
		{
			name: "node-metric-crud",
			args: args{
				samples: []nodeResourceMetric{
					{
						ID:              uint64(now.UnixNano() + 1),
						CPUUsedCores:    1,
						MemoryUsedBytes: 2,
						Timestamp:       now,
					},
				},
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []nodeResourceMetric{
				{
					ID:              uint64(now.UnixNano() + 1),
					CPUUsedCores:    1,
					MemoryUsedBytes: 2,
					Timestamp:       now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			for _, sample := range tt.args.samples {
				err := s.InsertNodeResourceMetric(&sample)
				if err != nil {
					t.Errorf("insert node metric error %v", err)
				}
			}

			got, err := s.GetNodeResourceMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetNodeResourceMetric() get error %v", err)
				return
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetNodeResourceMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualNodeResourceMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetNodeResourceMetric() = %v, want %v", got, tt.want)
				}
			}

			gotNum, err := s.CountNodeResourceMetric()
			if err != nil {
				t.Errorf("CountNodeResourceMetric got error %v", err)
			}
			if gotNum != int64(len(tt.want)) {
				t.Errorf("CountNodeResourceMetric() = %v, want %v", gotNum, len(tt.want))
			}

			err = s.DeleteNodeResourceMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeleteNodeResourceMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetNodeResourceMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetNodeResourceMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetNodeResourceMetric after delete %v", gotAfterDelete)
			}
		})
	}
}

func Test_storage_ContainerResourceMetric_CRUD(t *testing.T) {
	now := time.Now()
	id := "test-container-id"
	type args struct {
		samples []containerResourceMetric
		id      string
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []containerResourceMetric
	}{
		{
			name: "container-resource-metric-crud",
			args: args{
				samples: []containerResourceMetric{
					{
						ID:              uint64(now.UnixNano()),
						ContainerID:     id,
						CPUUsedCores:    1,
						MemoryUsedBytes: 2,
						Timestamp:       now,
					},
				},
				id:    id,
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []containerResourceMetric{
				{
					ID:              uint64(now.UnixNano()),
					ContainerID:     id,
					CPUUsedCores:    1,
					MemoryUsedBytes: 2,
					Timestamp:       now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			for _, sample := range tt.args.samples {
				err := s.InsertContainerResourceMetric(&sample)
				if err != nil {
					t.Errorf("insert container metric error %v", err)
				}
			}

			got, err := s.GetContainerResourceMetric(&tt.args.id, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetContainerResourceMetric got error %v", err)
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetContainerResourceMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualContainerResourceMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetContainerResourceMetric() = %v, want %v", got[i], tt.want[i])
				}
			}

			gotNum, err := s.CountContainerResourceMetric()
			if err != nil {
				t.Errorf("CountContainerResourceMetric got error %v", err)
			}
			if gotNum != int64(len(tt.want)) {
				t.Errorf("CountContainerResourceMetric() = %v, want %v", gotNum, len(tt.want))
			}

			err = s.DeleteContainerResourceMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeleteContainerResourceMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetContainerResourceMetric(&tt.args.id, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetContainerResourceMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetContainerResourceMetric after delete %v", gotAfterDelete)
			}
		})
	}
}

func Test_storage_BECPUResourceMetric_CRUD(t *testing.T) {
	now := time.Now()
	type args struct {
		samples []beCPUResourceMetric
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []beCPUResourceMetric
	}{
		{
			name: "beCPUResourceMetric-crud",
			args: args{
				samples: []beCPUResourceMetric{
					{
						ID:              uint64(now.UnixNano() + 1),
						CPUUsedCores:    1,
						CPURequestCores: 2,
						CPULimitCores:   2,
						Timestamp:       now,
					},
				},
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []beCPUResourceMetric{
				{
					ID:              uint64(now.UnixNano() + 1),
					CPUUsedCores:    1,
					CPURequestCores: 2,
					CPULimitCores:   2,
					Timestamp:       now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			for _, sample := range tt.args.samples {
				err := s.InsertBECPUResourceMetric(&sample)
				if err != nil {
					t.Errorf("insert beCPUResourceMetric error %v", err)
				}
			}

			got, err := s.GetBECPUResourceMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetBECPUResourceMetric() get error %v", err)
				return
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetBECPUResourceMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualBECPUResourceMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetBECPUResourceMetric() = %v, want %v", got, tt.want)
				}
			}

			gotNum, err := s.CountBECPUResourceMetric()
			if err != nil {
				t.Errorf("CountBECPUResourceMetric got error %v", err)
			}
			if gotNum != int64(len(tt.want)) {
				t.Errorf("CountBECPUResourceMetric() = %v, want %v", gotNum, len(tt.want))
			}

			err = s.DeleteBECPUResourceMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeleteBECPUResourceMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetBECPUResourceMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetBECPUResourceMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetBECPUResourceMetric after delete %v", gotAfterDelete)
			}
		})
	}
}

func Test_storage_rawRecord_CRUD(t *testing.T) {
	type args struct {
		samples []*rawRecord
	}
	tests := []struct {
		name string
		args args
		want []*rawRecord
	}{
		{
			name: "raw-record-crud",
			args: args{
				samples: []*rawRecord{
					{
						RecordType: "test field",
						RecordStr:  "test record",
					},
					{
						RecordType: "test field 1",
						RecordStr:  "test record",
					},
					{
						RecordType: "test field",
						RecordStr:  "test record",
					},
					{
						RecordType: "test field 2",
						RecordStr:  "test record",
					},
				},
			},
			want: []*rawRecord{
				{
					RecordType: "test field",
					RecordStr:  "test record",
				},
				{
					RecordType: "test field 1",
					RecordStr:  "test record",
				},
				{
					RecordType: "test field 2",
					RecordStr:  "test record",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var err error
			s, _ := NewStorage()
			defer s.Close()

			for i := range tt.args.samples {
				err = s.InsertRawRecord(tt.args.samples[i])
				if err != nil {
					t.Errorf("InsertRawRecord got error %v", err)
				}
				t.Logf("InsertRawRecord insert record %v", tt.args.samples[i])
			}

			for _, record := range tt.want {
				got, err := s.GetRawRecord(record.RecordType)
				if err != nil {
					t.Errorf("GetRawRecord record %v got error %v", record.RecordType, err)
				}
				if !EqualRawRecord(got, record) {
					t.Errorf("GetRawRecord() = %v, want %v", got, record)
				}
			}
		})
	}
}

func Test_storage_ContainerThrottledMetric_CRUD(t *testing.T) {
	now := time.Now()
	id := "test-container-id"
	type args struct {
		samples []containerThrottledMetric
		id      string
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []containerThrottledMetric
	}{
		{
			name: "container-throttled-metric-crud",
			args: args{
				samples: []containerThrottledMetric{
					{
						ID:                uint64(now.UnixNano()),
						ContainerID:       id,
						CPUThrottledRatio: 0.6,
						Timestamp:         now,
					},
				},
				id:    id,
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []containerThrottledMetric{
				{
					ID:                uint64(now.UnixNano()),
					ContainerID:       id,
					CPUThrottledRatio: 0.6,
					Timestamp:         now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			for _, sample := range tt.args.samples {
				err := s.InsertContainerThrottledMetric(&sample)
				if err != nil {
					t.Errorf("insert container metric error %v", err)
				}
			}

			got, err := s.GetContainerThrottledMetric(&tt.args.id, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetContainerThrottledMetric got error %v", err)
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetContainerThrottledMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualContainerThrottledMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetContainerThrottledMetric() = %v, want %v", got[i], tt.want[i])
				}
			}

			gotNum, err := s.CountContainerThrottledMetric()
			if err != nil {
				t.Errorf("CountContainerThrottledMetric got error %v", err)
			}
			if gotNum != int64(len(tt.want)) {
				t.Errorf("CountContainerThrottledMetric() = %v, want %v", gotNum, len(tt.want))
			}

			err = s.DeleteContainerThrottledMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeleteContainerThrottledMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetContainerThrottledMetric(&tt.args.id, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetContainerThrottledMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetContainerThrottledMetric after delete %v", gotAfterDelete)
			}
		})
	}
}

func Test_storage_PodThrottledMetric_CRUD(t *testing.T) {
	now := time.Now()
	uid := "test-pod-uid"
	type args struct {
		samples []podThrottledMetric
		uid     string
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []podThrottledMetric
	}{
		{
			name: "pod-throttled-metric-crud",
			args: args{
				samples: []podThrottledMetric{
					{
						ID:                uint64(now.UnixNano()),
						PodUID:            uid,
						CPUThrottledRatio: 0.6,
						Timestamp:         now,
					},
				},
				uid:   uid,
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []podThrottledMetric{
				{
					ID:                uint64(now.UnixNano()),
					PodUID:            uid,
					CPUThrottledRatio: 0.6,
					Timestamp:         now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			defer s.Close()
			for _, sample := range tt.args.samples {
				err := s.InsertPodThrottledMetric(&sample)
				if err != nil {
					t.Errorf("insert pod metric error %v", err)
				}
			}

			got, err := s.GetPodThrottledMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetPodThrottledMetric got error %v", err)
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetPodThrottledMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualPodThrottledMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetPodThrottledMetric() = %v, want %v", got[i], tt.want[i])
				}
			}

			gotNum, err := s.CountPodThrottledMetric()
			if err != nil {
				t.Errorf("CountPodThrottledMetric got error %v", err)
			}
			if gotNum != int64(len(tt.want)) {
				t.Errorf("CountPodThrottledMetric() = %v, want %v", gotNum, len(tt.want))
			}

			err = s.DeletePodThrottledMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeletePodThrottledMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetPodThrottledMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetPodThrottledMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetPodThrottledMetric after delete %v", gotAfterDelete)
			}
		})
	}
}

func Test_storage_ContainerCPIMetric_CRUD(t *testing.T) {
	now := time.Now()
	uid := "test-container-uid"
	type args struct {
		samples []containerCPIMetric
		uid     string
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []containerCPIMetric
	}{
		{
			name: "container-cpi-metric-crud",
			args: args{
				samples: []containerCPIMetric{
					{
						ID:           uint64(now.UnixNano()),
						ContainerID:  uid,
						Cycles:       6,
						Instructions: 6,
						Timestamp:    now,
					},
				},
				uid:   uid,
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []containerCPIMetric{
				{
					ID:           uint64(now.UnixNano()),
					ContainerID:  uid,
					Cycles:       6,
					Instructions: 6,
					Timestamp:    now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			for _, sample := range tt.args.samples {
				err := s.InsertContainerCPIMetric(&sample)
				if err != nil {
					t.Errorf("insert container cpi metric error %v", err)
				}
			}

			got, err := s.GetContainerCPIMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetContainerCPIMetric got error %v", err)
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetContainerCPIMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualContainerCPIMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetContainerCPIMetric() = %v, want %v", got[i], tt.want[i])
				}
			}

			err = s.DeleteContainerCPIMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeleteContainerCPIMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetContainerCPIMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetContainerCPIMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetContainerCPIMetric after delete %v", gotAfterDelete)
			}
		})
	}
}

func Test_storage_ContainerPSIMetric_CRUD(t *testing.T) {
	now := time.Now()
	uid := "test-container-uid"
	type args struct {
		samples []containerPSIMetric
		uid     string
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []containerPSIMetric
	}{
		{
			name: "container-psi-metric-crud",
			args: args{
				samples: []containerPSIMetric{
					{
						ID:           uint64(now.UnixNano()),
						ContainerID:  uid,
						SomeCPUAvg10: 6,
						SomeMemAvg10: 6,
						SomeIOAvg10:  6,
						FullCPUAvg10: 6,
						FullMemAvg10: 6,
						FullIOAvg10:  6,
						Timestamp:    now,
					},
				},
				uid:   uid,
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []containerPSIMetric{
				{
					ID:           uint64(now.UnixNano()),
					ContainerID:  uid,
					SomeCPUAvg10: 6,
					SomeMemAvg10: 6,
					SomeIOAvg10:  6,
					FullCPUAvg10: 6,
					FullMemAvg10: 6,
					FullIOAvg10:  6,
					Timestamp:    now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			for _, sample := range tt.args.samples {
				err := s.InsertContainerPSIMetric(&sample)
				if err != nil {
					t.Errorf("insert container psi metric error %v", err)
				}
			}

			got, err := s.GetContainerPSIMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetContainerPSIMetric got error %v", err)
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetContainerPSIMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualContainerPSIMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetContainerPSIMetric() = %v, want %v", got[i], tt.want[i])
				}
			}

			err = s.DeleteContainerPSIMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeleteContainerPSIMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetContainerPSIMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetContainerPSIMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetContainerPSIMetric after delete %v", gotAfterDelete)
			}
		})
	}
}

func Test_storage_PodPSIMetric_CRUD(t *testing.T) {
	now := time.Now()
	uid := "test-pod-uid"
	type args struct {
		samples []podPSIMetric
		uid     string
		start   time.Time
		end     time.Time
	}
	tests := []struct {
		name string
		args args
		want []podPSIMetric
	}{
		{
			name: "pod-psi-metric-crud",
			args: args{
				samples: []podPSIMetric{
					{
						ID:           uint64(now.UnixNano()),
						PodUID:       uid,
						SomeCPUAvg10: 6,
						SomeMemAvg10: 6,
						SomeIOAvg10:  6,
						FullCPUAvg10: 6,
						FullMemAvg10: 6,
						FullIOAvg10:  6,
						Timestamp:    now,
					},
				},
				uid:   uid,
				start: now.Add(-5 * time.Second),
				end:   now.Add(5 * time.Second),
			},
			want: []podPSIMetric{
				{
					ID:           uint64(now.UnixNano()),
					PodUID:       uid,
					SomeCPUAvg10: 6,
					SomeMemAvg10: 6,
					SomeIOAvg10:  6,
					FullCPUAvg10: 6,
					FullMemAvg10: 6,
					FullIOAvg10:  6,
					Timestamp:    now,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s, _ := NewStorage()
			for _, sample := range tt.args.samples {
				err := s.InsertPodPSIMetric(&sample)
				if err != nil {
					t.Errorf("insert pod psi metric error %v", err)
				}
			}

			got, err := s.GetPodPSIMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetPodPSIMetric got error %v", err)
			}
			if len(got) != len(tt.want) {
				t.Errorf("GetPodPSIMetric() = %v, want %v", got, tt.want)
			}
			for i := range got {
				if !EqualPodCPIMetric(&got[i], &tt.want[i]) {
					t.Errorf("GetPodPSIMetric() = %v, want %v", got[i], tt.want[i])
				}
			}

			err = s.DeletePodPSIMetric(&tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("DeletePodPSIMetric got error %v", err)
			}

			gotAfterDelete, err := s.GetPodPSIMetric(&tt.args.uid, &tt.args.start, &tt.args.end)
			if err != nil {
				t.Errorf("GetPodPSIMetric got error %v", err)
			}
			if len(gotAfterDelete) != 0 {
				t.Errorf("GetPodPSIMetric after delete %v", gotAfterDelete)
			}
		})
	}
}
