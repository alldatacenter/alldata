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

package clusters

import (
	"context"
	"fmt"
	"reflect"
	"strings"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
)

func patch(bdl *bundle.Bundle, c *Clusters) {
	// monkey patch get upgrade record
	// monkey record delete func
	monkey.Patch(getUpgradeRecords, func(c *dbclient.DBClient, cluster string) ([]dbclient.Record, error) {
		if cluster == "" {
			return nil, fmt.Errorf("empty cluster name")
		}
		if c == nil {
			return nil, fmt.Errorf("invalid db client")
		}
		return []dbclient.Record{}, nil
	})

	// monkey patch Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryClusterInfo", func(_ *bundle.Bundle, cluster string) (apistructs.ClusterInfoData, error) {
		if strings.Contains(cluster, "edge") {
			return apistructs.ClusterInfoData{apistructs.DICE_IS_EDGE: "true",
				apistructs.DICE_CLUSTER_TYPE: "k8s",
				apistructs.DICE_VERSION:      "1.3",
				apistructs.DICE_CLUSTER_NAME: "fake-edge-cluster",
			}, nil
		}
		return apistructs.ClusterInfoData{
			apistructs.DICE_CLUSTER_TYPE: "k8s",
			apistructs.DICE_VERSION:      "1.4",
		}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreatePipeline", func(_ *bundle.Bundle, req interface{}) (*apistructs.PipelineDTO, error) {
		return &apistructs.PipelineDTO{ID: 11}, nil
	})

	// monkey record delete func
	monkey.Patch(createRecord, func(c *dbclient.DBClient, r dbclient.Record) (uint64, error) {
		if r.UserID == "" {
			return 0, fmt.Errorf("invalid user id")
		}
		return 0, nil
	})

	// monkey patch Credential with core services
	monkey.PatchInstanceMethod(reflect.TypeOf(c), "GetOrCreateAccessKey", func(_ *Clusters, _ context.Context, cluster string) (*tokenpb.Token, error) {
		if strings.Contains(cluster, "without-access-key") {
			return nil, fmt.Errorf("get or create access key failed")
		}
		return &tokenpb.Token{AccessKey: "clusterAccessKey"}, nil
	})
}

func TestUpgradeEdgeCluster(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient
	db = &dbclient.DBClient{}
	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	patch(bdl, c)

	req := apistructs.UpgradeEdgeClusterRequest{
		ClusterName: "fake-edge-cluster",
		PreCheck:    false,
	}

	_, _, _, err := c.UpgradeEdgeCluster(context.Background(), req, "123", "")
	assert.NoError(t, err)
}

func TestEmptyCluster(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient
	db = &dbclient.DBClient{}
	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	patch(bdl, c)

	req := apistructs.UpgradeEdgeClusterRequest{
		ClusterName: "",
		PreCheck:    false,
	}
	_, _, _, err := c.UpgradeEdgeCluster(context.Background(), req, "123", "")
	assert.Contains(t, err.Error(), "empty cluster name")
}

func TestEmptyDBClient(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient
	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	patch(bdl, c)

	req := apistructs.UpgradeEdgeClusterRequest{
		ClusterName: "fake-edge-cluster",
		PreCheck:    false,
	}

	_, _, _, err := c.UpgradeEdgeCluster(context.Background(), req, "123", "")
	assert.Contains(t, err.Error(), "invalid db client")
}

func TestClusterWithoutAccessKey(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient
	db = &dbclient.DBClient{}
	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	patch(bdl, c)

	req := apistructs.UpgradeEdgeClusterRequest{
		ClusterName: "fake-edge-cluster-without-access-key",
		PreCheck:    false,
	}

	_, _, _, err := c.UpgradeEdgeCluster(context.Background(), req, "123", "")
	assert.Contains(t, err.Error(), "get or create access key failed")
}

func TestClusterWithoutUserID(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient
	db = &dbclient.DBClient{}
	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	patch(bdl, c)

	req := apistructs.UpgradeEdgeClusterRequest{
		ClusterName: "fake-edge-cluster",
		PreCheck:    false,
	}

	_, _, _, err := c.UpgradeEdgeCluster(context.Background(), req, "", "")
	assert.Contains(t, err.Error(), "invalid user id")
}
