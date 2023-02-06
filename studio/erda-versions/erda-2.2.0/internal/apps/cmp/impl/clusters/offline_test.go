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
	"net/http"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
)

func TestOfflineEdgeCluster(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient

	req := apistructs.OfflineEdgeClusterRequest{
		ClusterName: "fake-cluster",
		Force:       true,
	}

	// monkey patch Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryClusterInfo", func(_ *bundle.Bundle, _ string) (apistructs.ClusterInfoData, error) {
		return apistructs.ClusterInfoData{apistructs.DICE_IS_EDGE: "true"}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DereferenceCluster", func(_ *bundle.Bundle, _ uint64, _, _ string, _ bool) (string, error) {
		return "", nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DeleteCluster", func(_ *bundle.Bundle, _ string, _ ...http.Header) error {
		return nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListOrgClusterRelation", func(_ *bundle.Bundle, _, _ string) ([]apistructs.OrgClusterRelationDTO, error) {
		return []apistructs.OrgClusterRelationDTO{}, nil
	})

	// monkey record delete func
	monkey.Patch(createRecord, func(_ *dbclient.DBClient, _ dbclient.Record) (uint64, error) {
		return 0, nil
	})

	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	// monkey patch Credential with core services
	monkey.PatchInstanceMethod(reflect.TypeOf(c), "DeleteAccessKey", func(*Clusters, string) error {
		return nil
	})

	_, _, err := c.OfflineEdgeCluster(context.Background(), req, "", "")
	assert.NoError(t, err)
}

func TestOfflineWithDeleteClusterFailed(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient

	req := apistructs.OfflineEdgeClusterRequest{
		ClusterName: "fake-cluster",
		Force:       true,
	}

	// monkey patch Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryClusterInfo", func(_ *bundle.Bundle, _ string) (apistructs.ClusterInfoData, error) {
		return apistructs.ClusterInfoData{apistructs.DICE_IS_EDGE: "true"}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DereferenceCluster", func(_ *bundle.Bundle, _ uint64, _, _ string, _ bool) (string, error) {
		return "", nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DeleteCluster", func(_ *bundle.Bundle, _ string, _ ...http.Header) error {
		return fmt.Errorf("fake error")
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListOrgClusterRelation", func(_ *bundle.Bundle, _, _ string) ([]apistructs.OrgClusterRelationDTO, error) {
		return []apistructs.OrgClusterRelationDTO{}, nil
	})

	// monkey record delete func
	monkey.Patch(createRecord, func(_ *dbclient.DBClient, _ dbclient.Record) (uint64, error) {
		return 0, nil
	})

	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	// monkey patch Credential with core services
	monkey.PatchInstanceMethod(reflect.TypeOf(c), "DeleteAccessKey", func(*Clusters, string) error {
		return nil
	})

	_, _, err := c.OfflineEdgeCluster(context.Background(), req, "", "")
	assert.NoError(t, err)
}

func TestOfflineWithDeleteAKFailed(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient

	req := apistructs.OfflineEdgeClusterRequest{
		ClusterName: "fake-cluster",
		Force:       true,
	}

	// monkey patch Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryClusterInfo", func(_ *bundle.Bundle, _ string) (apistructs.ClusterInfoData, error) {
		return apistructs.ClusterInfoData{apistructs.DICE_IS_EDGE: "true"}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DereferenceCluster", func(_ *bundle.Bundle, _ uint64, _, _ string, _ bool) (string, error) {
		return "", nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DeleteCluster", func(_ *bundle.Bundle, _ string, _ ...http.Header) error {
		return nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListOrgClusterRelation", func(_ *bundle.Bundle, _, _ string) ([]apistructs.OrgClusterRelationDTO, error) {
		return []apistructs.OrgClusterRelationDTO{}, nil
	})

	// monkey record delete func
	monkey.Patch(createRecord, func(_ *dbclient.DBClient, _ dbclient.Record) (uint64, error) {
		return 0, nil
	})

	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	// monkey patch Credential with core services
	monkey.PatchInstanceMethod(reflect.TypeOf(c), "DeleteAccessKey", func(*Clusters, string) error {
		return fmt.Errorf("fake error")
	})

	_, _, err := c.OfflineEdgeCluster(context.Background(), req, "", "")
	assert.Error(t, err)
}

func TestBatchOfflineEdgeCluster(t *testing.T) {
	var bdl *bundle.Bundle
	var db *dbclient.DBClient

	req := apistructs.BatchOfflineEdgeClusterRequest{
		Clusters: []string{"fake-cluster"},
		Force:    true,
	}

	// monkey patch Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryClusterInfo", func(_ *bundle.Bundle, _ string) (apistructs.ClusterInfoData, error) {
		return apistructs.ClusterInfoData{apistructs.DICE_IS_EDGE: "true"}, nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DereferenceCluster", func(_ *bundle.Bundle, _ uint64, _, _ string, _ bool) (string, error) {
		return "", nil
	})
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "DeleteCluster", func(_ *bundle.Bundle, _ string, _ ...http.Header) error {
		return nil
	})

	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListOrgClusterRelation", func(_ *bundle.Bundle, _, _ string) ([]apistructs.OrgClusterRelationDTO, error) {
		return []apistructs.OrgClusterRelationDTO{}, fmt.Errorf("fake error")
	})

	// monkey record delete func
	monkey.Patch(createRecord, func(_ *dbclient.DBClient, _ dbclient.Record) (uint64, error) {
		return 0, nil
	})

	c := New(db, bdl, nil, &fakeClusterServiceServer{})

	// monkey patch Credential with core services
	monkey.PatchInstanceMethod(reflect.TypeOf(c), "DeleteAccessKey", func(*Clusters, string) error {
		return nil
	})

	err := c.BatchOfflineEdgeCluster(context.Background(), req, "")
	assert.Error(t, err)
}

func TestOfflineEdgeClusters(t *testing.T) {
	type args struct {
		preCheck                 bool
		forceOffline             bool
		projectClusterReferError bool
		projectClusterReferred   bool
		runtimeClusterReferError bool
		runtimeClusterReferred   bool
	}

	tests := []struct {
		name             string
		args             args
		wantErr          bool
		wantPreCheckHint bool
	}{
		{
			name:    "test1_project_cluster_refer_error",
			wantErr: true,
			args:    args{projectClusterReferError: true},
		},
		{
			name:    "test2_runtime_cluster_refer_error",
			wantErr: true,
			args:    args{runtimeClusterReferError: true},
		},
		{
			name:             "test3_project_cluster_referred_pre_check",
			args:             args{preCheck: true, projectClusterReferred: true},
			wantPreCheckHint: true,
		},
		{
			name:             "test4_runtime_cluster_referred_pre_check",
			args:             args{preCheck: true, runtimeClusterReferred: true},
			wantPreCheckHint: true,
		},
	}

	var bdl *bundle.Bundle
	var db *dbclient.DBClient

	req := apistructs.OfflineEdgeClusterRequest{
		ClusterName: "fake-cluster",
	}

	ctx := context.Background()

	// monkey patch Bundle

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			req.PreCheck = tt.args.preCheck

			monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "QueryClusterInfo", func(_ *bundle.Bundle, _ string) (apistructs.ClusterInfoData, error) {
				if tt.args.forceOffline {
					req.Force = true
				}
				return apistructs.ClusterInfoData{apistructs.DICE_CLUSTER_NAME: "fake-cluster", apistructs.DICE_IS_EDGE: "true"}, nil
			})

			monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ProjectClusterReferred", func(_ *bundle.Bundle, userID, orgID, clusterName string) (referred bool, err error) {
				if tt.args.projectClusterReferError {
					return false, fmt.Errorf("fake error")
				}
				return tt.args.projectClusterReferred, nil
			})
			defer monkey.UnpatchAll()

			monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "FindClusterResource", func(_ *bundle.Bundle, clusterName, orgID string) (*apistructs.ResourceReferenceData, error) {
				if tt.args.runtimeClusterReferError {
					return nil, fmt.Errorf("fake error")
				}
				if tt.args.runtimeClusterReferred {
					return &apistructs.ResourceReferenceData{AddonReference: 1}, nil
				}
				return &apistructs.ResourceReferenceData{}, nil
			})

			// monkey record delete func
			monkey.Patch(createRecord, func(_ *dbclient.DBClient, _ dbclient.Record) (uint64, error) {
				return 0, nil
			})

			c := New(db, bdl, nil, &fakeClusterServiceServer{})

			_, hint, err := c.OfflineEdgeCluster(ctx, req, "", "")

			if (err != nil) != tt.wantErr {
				t.Errorf("OfflineEdgeCluster error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			if (hint != "") != tt.args.preCheck {
				t.Errorf("OfflineEdgeCluster hint = %v, wantPreCheckHint %v", hint, tt.wantPreCheckHint)
				return
			}
		})
	}
}
