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

package registercenter

import (
	"context"

	"github.com/erda-project/erda-proto-go/msp/registercenter/pb"
	"github.com/erda-project/erda/pkg/common/errors"
)

// GetDubboInterfaceTime depracated
func (s *registerCenterService) GetDubboInterfaceTime(ctx context.Context, req *pb.GetDubboInterfaceTimeRequest) (*pb.GetDubboInterfaceTimeResponse, error) {
	return nil, errors.NewUnimplementedError("GetDubboInterfaceTime")
}

// GetDubboInterfaceQPS depracated
func (s *registerCenterService) GetDubboInterfaceQPS(ctx context.Context, req *pb.GetDubboInterfaceQPSRequest) (*pb.GetDubboInterfaceQPSResponse, error) {
	return nil, errors.NewUnimplementedError("GetDubboInterfaceQPS")
}

// GetDubboInterfaceFailed depracated
func (s *registerCenterService) GetDubboInterfaceFailed(ctx context.Context, req *pb.GetDubboInterfaceFailedRequest) (*pb.GetDubboInterfaceFailedResponse, error) {
	return nil, errors.NewUnimplementedError("GetDubboInterfaceFailed")
}

// GetDubboInterfaceAvgTime depracated
func (s *registerCenterService) GetDubboInterfaceAvgTime(ctx context.Context, req *pb.GetDubboInterfaceAvgTimeRequest) (*pb.GetDubboInterfaceAvgTimeResponse, error) {
	return nil, errors.NewUnimplementedError("GetDubboInterfaceAvgTime")
}
