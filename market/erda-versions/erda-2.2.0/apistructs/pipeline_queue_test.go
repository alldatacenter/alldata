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

package apistructs

import (
	"testing"

	"github.com/bmizerany/assert"
)

func TestPipelineQueueCreateRequest_Validate(t *testing.T) {
	var (
		validName              = "queue1"
		validSource            = PipelineSourceDefault
		validClusterName       = "test-cluster"
		validStrategy          = ScheduleStrategyInsidePipelineQueueOfFIFO
		validPriority    int64 = 100
		validCPU               = 123.456
		validMemoryMB          = 1234.56
	)
	type fields struct {
		Name             string
		PipelineSource   PipelineSource
		ClusterName      string
		ScheduleStrategy ScheduleStrategyInsidePipelineQueue
		Priority         int64
		MaxCPU           float64
		MaxMemoryMB      float64
		Labels           map[string]string
		IdentityInfo     IdentityInfo
	}
	tests := []struct {
		name    string
		fields  fields
		wantErr bool
	}{
		// name
		{
			name: "invalid empty name",
			fields: fields{
				Name: "",
			},
			wantErr: true,
		},
		{
			name: "invalid too long name",
			fields: fields{
				Name: func() string {
					var runes []rune
					for i := 0; i < 192; i++ {
						runes = append(runes, 'a')
					}
					return string(runes)
				}(),
			},
			wantErr: true,
		},
		{
			name: "valid name",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           validCPU,
				MaxMemoryMB:      validMemoryMB,
			},
			wantErr: false,
		},
		// pipeline source
		{
			name: "invalid empty source",
			fields: fields{
				Name:           validName,
				PipelineSource: "",
			},
			wantErr: true,
		},
		{
			name: "invalid unknown source",
			fields: fields{
				Name:           validName,
				PipelineSource: "xxx",
			},
			wantErr: true,
		},
		{
			name: "valid source",
			fields: fields{
				Name:             validName,
				PipelineSource:   PipelineSourceDefault,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           validCPU,
				MaxMemoryMB:      validMemoryMB,
			},
			wantErr: false,
		},
		// cluster name
		{
			name: "invalid empty cluster name",
			fields: fields{
				Name:           validName,
				PipelineSource: validSource,
				ClusterName:    "",
			},
			wantErr: true,
		},
		{
			name: "valid non-null cluster name",
			fields: fields{
				Name:           validName,
				PipelineSource: validSource,
				ClusterName:    validClusterName,
			},
			wantErr: false,
		},
		// schedule strategy
		{
			name: "valid empty strategy, use default",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: "",
				Priority:         validPriority,
			},
			wantErr: false,
		},
		{
			name: "valid FIFO strategy",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				Priority:         validPriority,
				ScheduleStrategy: ScheduleStrategyInsidePipelineQueueOfFIFO,
			},
			wantErr: false,
		},
		{
			name: "invalid unknown strategy",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: "xxx",
			},
			wantErr: true,
		},
		// priority
		{
			name: "invalid priority < 0",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         -1,
			},
			wantErr: true,
		},
		{
			name: "valid empty priority",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         0,
				MaxCPU:           validCPU,
				MaxMemoryMB:      validMemoryMB,
			},
			wantErr: false,
		},
		{
			name: "valid priority > 0",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           validCPU,
				MaxMemoryMB:      validMemoryMB,
			},
			wantErr: false,
		},
		// max cpu
		{
			name: "valid empty max cpu, means no cpu can use",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           0,
			},
			wantErr: false,
		},
		{
			name: "invalid max cpu < 0",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           -11.22,
			},
			wantErr: true,
		},
		{
			name: "valid max cpu",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           validCPU,
			},
			wantErr: false,
		},
		// max memoryMB
		{
			name: "valid empty memoryMB, means no memory can use",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           validCPU,
				MaxMemoryMB:      0,
			},
			wantErr: false,
		},
		{
			name: "invalid memoryMB < 0",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           validCPU,
				MaxMemoryMB:      -12.34,
			},
			wantErr: true,
		},
		{
			name: "valid memoryMB",
			fields: fields{
				Name:             validName,
				PipelineSource:   validSource,
				ClusterName:      validClusterName,
				ScheduleStrategy: validStrategy,
				Priority:         validPriority,
				MaxCPU:           validCPU,
				MaxMemoryMB:      validMemoryMB,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := PipelineQueueCreateRequest{
				Name:             tt.fields.Name,
				PipelineSource:   tt.fields.PipelineSource,
				ClusterName:      tt.fields.ClusterName,
				Priority:         tt.fields.Priority,
				ScheduleStrategy: tt.fields.ScheduleStrategy,
				MaxCPU:           tt.fields.MaxCPU,
				MaxMemoryMB:      tt.fields.MaxMemoryMB,
				Labels:           tt.fields.Labels,
				IdentityInfo:     tt.fields.IdentityInfo,
			}
			if err := req.Validate(); (err != nil) != tt.wantErr {
				t.Errorf("Validate() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestPipelineQueueUpdateRequest_Validate(t *testing.T) {
	type fields struct {
		ID                         uint64
		PipelineQueueCreateRequest PipelineQueueCreateRequest
	}
	tests := []struct {
		name    string
		fields  fields
		wantErr bool
	}{
		{
			name:    "invalid empty id",
			fields:  fields{},
			wantErr: true,
		},
		{
			name:    "cannot change queue's source",
			fields:  fields{ID: 1, PipelineQueueCreateRequest: PipelineQueueCreateRequest{PipelineSource: "test-source"}},
			wantErr: true,
		},
		{
			name:    "valid id and no source",
			fields:  fields{ID: 1, PipelineQueueCreateRequest: PipelineQueueCreateRequest{Name: "new-name"}},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			req := PipelineQueueUpdateRequest{
				ID:                         tt.fields.ID,
				PipelineQueueCreateRequest: tt.fields.PipelineQueueCreateRequest,
			}
			if err := req.Validate(); (err != nil) != tt.wantErr {
				t.Errorf("Validate() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func TestScheduleStrategyInsidePipelineQueue_IsValid(t *testing.T) {
	tests := []struct {
		name     string
		strategy ScheduleStrategyInsidePipelineQueue
		want     bool
	}{
		{
			name:     "invalid empty strategy",
			strategy: "",
			want:     false,
		},
		{
			name:     "invalid unknown strategy",
			strategy: "xxx",
			want:     false,
		},
		{
			name:     "valid fifo strategy",
			strategy: ScheduleStrategyInsidePipelineQueueOfFIFO,
			want:     true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.strategy.IsValid(); got != tt.want {
				t.Errorf("IsValid() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestScheduleStrategyInsidePipelineQueue_String(t *testing.T) {
	tests := []struct {
		name     string
		strategy ScheduleStrategyInsidePipelineQueue
		want     string
	}{
		{
			name:     "fifo",
			strategy: ScheduleStrategyInsidePipelineQueueOfFIFO,
			want:     "FIFO",
		},
		{
			name:     "unknown",
			strategy: "xxx",
			want:     "xxx",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := tt.strategy.String(); got != tt.want {
				t.Errorf("String() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestIsEndStatus(t *testing.T) {
	result := PipelineQueueValidateResult{
		IsEnd: true,
	}
	assert.Equal(t, true, result.IsEndStatus())
	result.IsEnd = false
	assert.Equal(t, false, result.IsEndStatus())
}
