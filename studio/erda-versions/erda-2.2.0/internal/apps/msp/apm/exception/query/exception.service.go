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

	"github.com/erda-project/erda-proto-go/msp/apm/exception/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/query/source"
)

type exceptionService struct {
	p      *provider
	source source.ExceptionSource
}

func (s *exceptionService) GetExceptions(ctx context.Context, req *pb.GetExceptionsRequest) (*pb.GetExceptionsResponse, error) {
	exceptions, err := s.source.GetExceptions(ctx, req)
	if err != nil {
		return &pb.GetExceptionsResponse{}, err
	}
	return &pb.GetExceptionsResponse{Data: exceptions}, nil
}

func (s *exceptionService) GetExceptionEventIds(ctx context.Context, req *pb.GetExceptionEventIdsRequest) (*pb.GetExceptionEventIdsResponse, error) {
	ids, err := s.source.GetExceptionEventIds(ctx, req)
	if err != nil {
		return &pb.GetExceptionEventIdsResponse{}, err
	}
	return &pb.GetExceptionEventIdsResponse{Data: ids}, nil
}

func (s *exceptionService) GetExceptionEvent(ctx context.Context, req *pb.GetExceptionEventRequest) (*pb.GetExceptionEventResponse, error) {
	event, err := s.source.GetExceptionEvent(ctx, req)
	if err != nil {
		return &pb.GetExceptionEventResponse{}, err
	}
	return &pb.GetExceptionEventResponse{Data: event}, nil
}
