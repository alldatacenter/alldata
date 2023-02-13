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

package diagnotor

import (
	"context"
	"sync"

	"github.com/erda-project/erda-proto-go/core/monitor/diagnotor/pb"
)

type diagnotorAgentService struct {
	p   *provider
	pid int

	lock       sync.RWMutex
	lastStatus *pb.HostProcessStatus

	lastIOStat map[int32]*ioCountersStatEntry
	procStats  map[int32]*procStat
}

func (s *diagnotorAgentService) ListTargetProcesses(ctx context.Context, req *pb.ListTargetProcessesRequest) (*pb.ListTargetProcessesResponse, error) {
	s.lock.RLock()
	status := s.lastStatus
	s.lock.RUnlock()

	return &pb.ListTargetProcessesResponse{
		Data: status,
	}, nil
}
