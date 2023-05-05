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
	"time"
)

type PipelineGCInfo struct {
	CreatedAt time.Time `json:"createdAt,omitempty"`
	GCAt      time.Time `json:"gcAt,omitempty"`
	TTL       uint64    `json:"ttl,omitempty"`
	LeaseID   string    `json:"leaseID,omitempty"`
	Data      []byte    `json:"data,omitempty"`
}

func MakePipelineGCInfo(ttl uint64, leaseID string, data []byte) PipelineGCInfo {
	now := time.Now()
	return PipelineGCInfo{
		CreatedAt: now,
		GCAt:      now.Add(time.Second * time.Duration(ttl)),
		TTL:       ttl,
		LeaseID:   leaseID,
		Data:      data,
	}
}

type PipelineGCDBOption struct {
	NeedArchive bool `json:"needArchive"`
}
