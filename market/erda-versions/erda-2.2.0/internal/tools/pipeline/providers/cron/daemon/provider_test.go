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

package daemon

import (
	"context"
	"fmt"
	"testing"

	"github.com/coreos/etcd/clientv3"
	"github.com/coreos/etcd/mvcc/mvccpb"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
)

type CronMockEdgeRegister struct {
	edgepipeline_register.MockEdgeRegister
}

func (e *CronMockEdgeRegister) IsEdge() bool {
	return false
}

func (e *CronMockEdgeRegister) CanProxyToEdge(source apistructs.PipelineSource, clusterName string) bool {
	return false
}

type CronMockKV struct {
	edgepipeline_register.MockKV
}

func (o CronMockKV) Put(ctx context.Context, key, val string, opts ...clientv3.OpOption) (*clientv3.PutResponse, error) {
	return nil, nil
}
func (o CronMockKV) Get(ctx context.Context, key string, opts ...clientv3.OpOption) (*clientv3.GetResponse, error) {
	if key == "1000" {
		return nil, fmt.Errorf("not found")
	}
	if key == "1001" {
		return &clientv3.GetResponse{
			Kvs:   nil,
			Count: 0,
		}, nil
	}
	return &clientv3.GetResponse{
		Kvs: []*mvccpb.KeyValue{
			{
				Key:   nil,
				Value: []byte("*/1 * * * *"),
				Lease: 0,
			},
		},
		Count: 1,
	}, nil
}

func Test_provider_AddIntoPipelineCrond(t *testing.T) {
	type fields struct {
		EtcdClient           *clientv3.Client
		EdgePipelineRegister edgepipeline_register.Interface
	}
	type args struct {
		cron *db.PipelineCron
	}

	etcdClient := &clientv3.Client{
		KV: CronMockKV{},
	}
	edgePipelineRegister := CronMockEdgeRegister{}
	enable := false
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test with add",
			fields: fields{
				EtcdClient:           etcdClient,
				EdgePipelineRegister: &edgePipelineRegister,
			},
			args: args{
				cron: &db.PipelineCron{
					ID:       1,
					CronExpr: "*/1 * * * *",
					Enable:   &enable,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				EtcdClient:           tt.fields.EtcdClient,
				EdgePipelineRegister: tt.fields.EdgePipelineRegister,
			}
			if err := p.AddIntoPipelineCrond(tt.args.cron); (err != nil) != tt.wantErr {
				t.Errorf("AddIntoPipelineCrond() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_provider_DeleteFromPipelineCrond(t *testing.T) {
	type fields struct {
		EtcdClient           *clientv3.Client
		EdgePipelineRegister edgepipeline_register.Interface
	}
	type args struct {
		cron *db.PipelineCron
	}

	etcdClient := &clientv3.Client{KV: CronMockKV{}}
	edgePipelineRegister := CronMockEdgeRegister{}
	enable := false
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test with delete",
			fields: fields{
				EtcdClient:           etcdClient,
				EdgePipelineRegister: &edgePipelineRegister,
			},
			args: args{
				cron: &db.PipelineCron{
					ID:       1,
					CronExpr: "*/1 * * * *",
					Enable:   &enable,
				},
			},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				EtcdClient:           tt.fields.EtcdClient,
				EdgePipelineRegister: tt.fields.EdgePipelineRegister,
			}
			if err := p.DeleteFromPipelineCrond(tt.args.cron); (err != nil) != tt.wantErr {
				t.Errorf("DeleteFromPipelineCrond() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
