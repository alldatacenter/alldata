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
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	db2 "github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/index/query/db"
)

func TestNewESClient(t *testing.T) {
	c := ESClient{
		Cluster: "cluster-1",
		Entrys:  []*IndexEntry{},
	}

	if len(c.Cluster) == 0 {
		t.Log("hennnnnnnn...")
	}
}

func TestGetLogIndices_WithNoneEmptyOrgId_Should_Return_Indices_With_OrgAlias(t *testing.T) {
	result := getLogIndices("rlogs-", "1")
	if len(result) == 0 {
		t.Errorf("should not return empty slice")
	}

	if result[0] != "rlogs-org-1" {
		t.Errorf("should return org alias")
	}
}

//go:generate mockgen -destination=./clients_mock_logs_test.go -package query github.com/erda-project/erda-infra/base/logs Logger
func TestGetESClientsFromLogAnalyticsByCluster_Should_Success(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	p := &provider{
		L:     NewMockLogger(ctrl),
		C:     &config{},
		mysql: &gorm.DB{},
		db: &db2.DB{
			LogDeployment:        db2.LogDeploymentDB{},
			LogServiceInstanceDB: db2.LogServiceInstanceDB{},
			LogInstanceDB:        db2.LogInstanceDB{},
		},
		bdl: bundle.New(),
	}

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

	p.getESClientsFromLogAnalyticsByCluster(1, "addon-1", "cluster-1")
}

func TestGetAllESClients_WithErrorAccessDb_Should_Return_Nil(t *testing.T) {
	p := &provider{
		db: &db2.DB{
			LogDeployment: db2.LogDeploymentDB{},
		},
	}

	defer monkey.UnpatchInstanceMethod(reflect.TypeOf(&p.db.LogDeployment), "List")
	monkey.PatchInstanceMethod(reflect.TypeOf(&p.db.LogDeployment), "List", func(_ *db2.LogDeploymentDB) ([]*db2.LogDeployment, error) {
		return nil, fmt.Errorf("boooooo!")
	})

	clients := p.getAllESClients()
	if clients != nil {
		t.Errorf("should return nil when fail to access logDeployment")
	}
}

func TestGetAllESClients_On_ExistsLogDeployment_Should_Return_None_Empty_Clients(t *testing.T) {
	ctrl := gomock.NewController(t)
	defer ctrl.Finish()
	p := provider{
		L:     NewMockLogger(ctrl),
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

	clients := p.getAllESClients()
	if len(clients) == 0 {
		t.Errorf("should return non-empty ESClients list when exists logDeployment")
	}
}

/*
func TestGetESClientsFromLogAnalyticsByLogDeployment_On_Preload_Enabled_Should_Try_Fill_ESClient_Entrys(t *testing.T) {
	p := &provider{
		db: &db.DB{
			LogDeployment: db.LogDeploymentDB{},
		},
		C: &config{
			IndexPreload: true,
		},
		timeRanges: make(map[string]map[string]*timeRange),
		reload:     make(chan struct{}),
	}
	p.indices.Store(map[string]map[string][]*IndexEntry{
		"cluster_1": map[string][]*IndexEntry{
			"addon_1": []*IndexEntry{
				&IndexEntry{Index: "rlogs-addon_1-2020.34-000001",
					Name: "addon_1",
				},
			},
		},
	})

	logDeployments := []*db.LogDeployment{
		&db.LogDeployment{
			ClusterName:  "cluster_1",
			ClusterType:  0,
			ESURL:        "http://localhost:9200",
			ESConfig:     "{}",
			CollectorURL: "http://collector:7096",
		},
	}

	clients := p.getESClientsFromLogAnalyticsByLogDeployment("addon_1", logDeployments...)
	if len(clients) == 0 || len(clients[0].Entrys) == 0 {
		t.Errorf("ESClient.Entrys should not empty when preload matched")
	}
}
*/
