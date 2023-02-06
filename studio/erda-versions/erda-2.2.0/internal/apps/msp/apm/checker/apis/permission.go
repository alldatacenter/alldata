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

package apis

import (
	"context"
	"fmt"
	"strconv"

	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage/db"
	"github.com/erda-project/erda/pkg/common/errors"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

func (s *checkerV1Service) GetProjectFromMetricID() func(ctx context.Context, req interface{}) (string, error) {
	return GetProjectFromMetricID(s.metricDB, s.projectDB, s.projectServer)
}

func GetProjectFromMetricID(metricDB *db.MetricDB, projectDB *db.ProjectDB, projectService projectpb.ProjectServiceServer) func(ctx context.Context, req interface{}) (string, error) {
	getter := perm.FieldValue("Id")
	return func(ctx context.Context, req interface{}) (string, error) {
		mid, err := getter(ctx, req)
		if err != nil {
			return "", err
		}
		m, err := GetMetric(mid, metricDB)
		if err != nil {
			return "", err
		}
		if m.Extra != "" {
			extra, err := strconv.ParseInt(m.Extra, 10, 64)
			if err != nil {
				return "", errors.NewInternalServerError(err)
			}
			if extra == m.ProjectID {
				proj, err := projectDB.GetByProjectID(m.ProjectID)
				if err != nil {
					return "", errors.NewDatabaseError(err)
				}
				if proj != nil {
					return strconv.FormatInt(proj.ProjectID, 10), nil
				}
			}
		}
		proj, err := projectDB.GetByID(m.ProjectID)
		if err != nil {
			return "", errors.NewDatabaseError(err)
		}
		if proj == nil {
			project, err := projectService.GetProject(ctx, &projectpb.GetProjectRequest{ProjectID: strconv.FormatInt(m.ProjectID, 10)})
			if err != nil {
				return "", errors.NewDatabaseError(err)
			}
			if project == nil {
				return "", fmt.Errorf("not found id for permission")
			}
			return project.Data.Id, nil
		}

		return strconv.FormatInt(proj.ProjectID, 10), nil
	}
}

func GetMetric(mid string, metricDB *db.MetricDB) (*db.Metric, error) {
	metricID, err := strconv.ParseInt(mid, 10, 64)
	if err != nil {
		return nil, fmt.Errorf("invalid metricID")
	}
	m, err := metricDB.GetByID(metricID)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if m == nil {
		return nil, fmt.Errorf("not found id for permission")
	}
	return m, nil
}
