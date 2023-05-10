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

package errorbox

import (
	"context"

	pb1 "github.com/erda-project/erda-proto-go/core/dop/taskerror/pb"
	"github.com/erda-project/erda-proto-go/core/services/errorbox/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
)

type ErrorBoxService struct {
	p  *provider
	db *dao.DBClient
}

func (s *ErrorBoxService) ListErrorLog(ctx context.Context, req *pb.TaskErrorListRequest) (*pb1.ErrorLogListResponseData, error) {
	resourceTypes := make([]apistructs.ErrorResourceType, 0)
	for _, t := range req.ResourceTypes {
		resourceTypes = append(resourceTypes, apistructs.ErrorResourceType(t))
	}
	parmas := &apistructs.TaskErrorListRequest{
		ResourceTypes: resourceTypes,
		ResourceIDS:   req.ResourceIds,
		StartTime:     req.StartTime,
	}
	errLogs, err := s.List(parmas)
	if err != nil {
		return nil, apierrors.ErrListErrorLog.InternalError(err)
	}
	logs := make([]*pb1.ErrorLog, 0)
	for _, errLog := range errLogs {
		logs = append(logs, &pb1.ErrorLog{
			Id:             errLog.ID,
			Level:          string(errLog.Level),
			ResourceType:   string(errLog.ResourceType),
			ResourceId:     errLog.ResourceID,
			OccurrenceTime: errLog.OccurrenceTime.Format("2006-01-02 15:04:05"),
			HumanLog:       errLog.HumanLog,
			PrimevalLog:    errLog.PrimevalLog,
		})
	}
	return &pb1.ErrorLogListResponseData{List: logs}, nil
}
