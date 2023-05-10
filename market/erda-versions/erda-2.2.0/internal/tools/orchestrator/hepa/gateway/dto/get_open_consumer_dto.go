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

package dto

import (
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/orm"
	db "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
)

type GetOpenConsumersDto struct {
	DiceArgsDto
}

func (impl GetOpenConsumersDto) GenSelectOptions() []orm.SelectOption {
	var result []orm.SelectOption
	needCluster := true
	if impl.OrgId != "" {
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "org_id",
			Value:  impl.OrgId,
		})
	} else {
		needCluster = false
	}
	if impl.ProjectId != "" {
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "project_id",
			Value:  impl.ProjectId,
		})
	} else {
		needCluster = false
	}
	if impl.Env != "" {
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "env",
			Value:  impl.Env,
		})
	} else {
		needCluster = false
	}
	if needCluster {
		azDb, err := db.NewGatewayAzInfoServiceImpl()
		if err != nil {
			log.Error("create az db failed")
			return nil
		}
		az, err := azDb.GetAz(&orm.GatewayAzInfo{
			Env:       impl.Env,
			OrgId:     impl.OrgId,
			ProjectId: impl.ProjectId,
		})
		if err != nil {
			log.Error("get az failed")
			return nil
		}
		result = append(result, orm.SelectOption{
			Type:   orm.ExactMatch,
			Column: "az",
			Value:  az,
		})
	}
	return result
}
