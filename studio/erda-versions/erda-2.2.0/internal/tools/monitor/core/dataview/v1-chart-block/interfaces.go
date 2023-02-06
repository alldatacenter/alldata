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

package block

import (
	"fmt"

	"github.com/erda-project/erda/internal/pkg/mysql"
	"github.com/erda-project/erda/internal/tools/monitor/utils"
	"github.com/erda-project/erda/pkg/crypto/uuid"
)

type DashboardAPI interface {
	CreateDashboard(block *UserBlock) (dash *DashboardBlockDTO, err error)
}

func (p *provider) CreateDashboard(body *UserBlock) (dash *DashboardBlockDTO, err error) {
	if len(body.ID) == 0 {
		body.ID = uuid.UUID()
	}
	if body.DataConfig == nil {
		body.DataConfig = &dataConfigDTO{}
	}
	if err := p.db.userBlock.Save(body); err != nil {
		if mysql.IsUniqueConstraintError(err) {
			return nil, fmt.Errorf("aleady existed, err: %s", err)
		}
		return nil, err
	}
	if body.ViewConfig != nil && body.DataConfig != nil {
		for _, v := range *body.ViewConfig {
			v.View.StaticData = struct{}{}
			for _, d := range *body.DataConfig {
				if v.I == d.I {
					v.View.StaticData = d.StaticData
				}
			}
		}
	}
	return &DashboardBlockDTO{
		ID:         body.ID,
		Name:       body.Name,
		Desc:       body.Desc,
		Scope:      body.Scope,
		ScopeID:    body.ScopeID,
		ViewConfig: body.ViewConfig,
		CreatedAt:  utils.ConvertTimeToMS(body.CreatedAt),
		UpdatedAt:  utils.ConvertTimeToMS(body.UpdatedAt),
		Version:    body.Version,
	}, nil
}
