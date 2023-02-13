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

package runtime

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/spec"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

type dbServiceImpl struct {
	db *dbclient.DBClient
}

func (d *dbServiceImpl) UpdateRuntime(runtime *dbclient.Runtime) error {
	return d.db.UpdateRuntime(runtime)
}

func (d *dbServiceImpl) GetRuntime(id uint64) (*dbclient.Runtime, error) {
	return d.db.GetRuntime(id)
}

func (d *dbServiceImpl) FindDomainsByRuntimeId(id uint64) ([]dbclient.RuntimeDomain, error) {
	return d.db.FindDomainsByRuntimeId(id)
}

func (d *dbServiceImpl) FindLastDeployment(id uint64) (*dbclient.Deployment, error) {
	return d.db.FindLastDeployment(id)
}

func (d *dbServiceImpl) FindRuntime(id spec.RuntimeUniqueId) (*dbclient.Runtime, error) {
	return d.db.FindRuntime(id)
}

func (d *dbServiceImpl) GetRuntimeAllowNil(id uint64) (*dbclient.Runtime, error) {
	return d.db.GetRuntimeAllowNil(id)
}

func newDBService(db *dbclient.DBClient) DBService {
	return &dbServiceImpl{db: db}
}

func NewDBService(orm *gorm.DB) DBService {
	return newDBService(&dbclient.DBClient{
		DBEngine: &dbengine.DBEngine{
			DB: orm,
		},
	})
}
