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
	"reflect"
	"testing"

	"github.com/olivere/elastic"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	db2 "github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/index/query/db"
)

func TestReloadAllIndices_Should_Success(t *testing.T) {
	ctrl := gomock.NewController(t)
	//defer ctrl.Finish()

	logger := NewMockLogger(ctrl)

	logger.EXPECT().Infof(gomock.Any(), gomock.Any())
	logger.EXPECT().Debug(gomock.Any())

	p := provider{
		L:     logger,
		C:     &config{},
		mysql: &gorm.DB{},
		db: &db2.DB{
			LogDeployment:        db2.LogDeploymentDB{},
			LogServiceInstanceDB: db2.LogServiceInstanceDB{},
			LogInstanceDB:        db2.LogInstanceDB{},
		},
		bdl:        bundle.New(),
		timeRanges: make(map[string]map[string]*timeRange),
		reload:     make(chan struct{}),
	}

	defer monkey.UnpatchInstanceMethod(reflect.TypeOf(&p.db.LogDeployment), "List")
	monkey.PatchInstanceMethod(reflect.TypeOf(&p.db.LogDeployment), "List", func(_ *db2.LogDeploymentDB) ([]*db2.LogDeployment, error) {
		return []*db2.LogDeployment{
			&db2.LogDeployment{
				ClusterName:  "cluster-1",
				ClusterType:  0,
				ESURL:        "http://localhost:9200",
				ESConfig:     "{}",
				CollectorURL: "http://collector:7096",
			},
		}, nil
	})

	defer monkey.UnpatchInstanceMethod(reflect.TypeOf(&p.db.LogDeployment), "QueryByOrgIDAndClusters")
	monkey.PatchInstanceMethod(reflect.TypeOf(&p.db.LogDeployment), "QueryByOrgIDAndClusters", func(_ *db2.LogDeploymentDB, orgID int64, clusters ...string) ([]*db2.LogDeployment, error) {
		return []*db2.LogDeployment{
			{LogType: string(db.LogTypeLogService), ESURL: "http://localhost:9200"},
		}, nil
	})

	defer monkey.UnpatchInstanceMethod(reflect.TypeOf(&p.db.LogInstanceDB), "GetByLogKey")
	monkey.PatchInstanceMethod(reflect.TypeOf(&p.db.LogInstanceDB), "GetByLogKey", func(_ *db2.LogInstanceDB, logKey string) (*db2.LogInstance, error) {
		return &db2.LogInstance{LogType: string(db.LogTypeLogAnalytics), LogKey: "logKey-1", Config: `{"MSP_ENV_ID":"msp_env_id_1"}`}, nil
	})

	defer monkey.UnpatchInstanceMethod(reflect.TypeOf(&p.db.LogInstanceDB), "GetListByClusterAndProjectIdAndWorkspace")
	monkey.PatchInstanceMethod(reflect.TypeOf(&p.db.LogInstanceDB), "GetListByClusterAndProjectIdAndWorkspace", func(_ *db2.LogInstanceDB, clusterName, projectId, workspace string) ([]db2.LogInstance, error) {
		return []db2.LogInstance{
			{LogType: string(db.LogTypeLogService), LogKey: "logKey-3", Config: `{"MSP_ENV_ID":"msp_env_id_1"}`},
			{LogType: string(db.LogTypeLogService), LogKey: "logKey-2", Config: `{"MSP_ENV_ID":"msp_env_id_1"}`},
			{LogType: string(db.LogTypeLogAnalytics), LogKey: "logKey-1", Config: `{"MSP_ENV_ID":"msp_env_id_1"}`},
		}, nil
	})

	defer monkey.Unpatch((*elastic.CatIndicesService).Do)
	monkey.Patch((*elastic.CatIndicesService).Do, func(s *elastic.CatIndicesService, ctx context.Context) (elastic.CatIndicesResponse, error) {
		return elastic.CatIndicesResponse{
			elastic.CatIndicesResponseRow{
				Index:       "logKey-1-0001",
				StoreSize:   "1000",
				DocsCount:   1000,
				DocsDeleted: 0,
			},
		}, nil
	})

	defer monkey.Unpatch((*elastic.SearchService).Do)
	monkey.Patch((*elastic.SearchService).Do, func(s *elastic.SearchService, ctx context.Context) (*elastic.SearchResult, error) {
		return &elastic.SearchResult{
			Hits: &elastic.SearchHits{
				TotalHits: 10,
				Hits:      []*elastic.SearchHit{},
			},
			Aggregations: elastic.Aggregations{},
		}, nil
	})

	p.reloadAllIndices()
}
