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

package etcd

import (
	"context"
	"fmt"
	"io"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/coreos/etcd/clientv3"
)

var (
	goodEndpoint = "http://localhost:2379"
	badEndpoint  = "http://localhost:23791"
)

// mockMaintenance used to mock etcdclient.Status, because Status is provided by embed interface clientv3.Maintenance,
// and default struct implement of clientv3.Maintenance is unexported.
type mockMaintenance struct{}

func (o mockMaintenance) Status(ctx context.Context, endpoint string) (*clientv3.StatusResponse, error) {
	if endpoint == goodEndpoint {
		return &clientv3.StatusResponse{}, nil
	}
	return nil, fmt.Errorf("fake bad endpoint error")
}
func (o mockMaintenance) AlarmList(ctx context.Context) (*clientv3.AlarmResponse, error) {
	panic("implement me")
}
func (o mockMaintenance) AlarmDisarm(ctx context.Context, m *clientv3.AlarmMember) (*clientv3.AlarmResponse, error) {
	panic("implement me")
}
func (o mockMaintenance) Defragment(ctx context.Context, endpoint string) (*clientv3.DefragmentResponse, error) {
	panic("implement me")
}
func (o mockMaintenance) HashKV(ctx context.Context, endpoint string, rev int64) (*clientv3.HashKVResponse, error) {
	panic("implement me")
}
func (o mockMaintenance) Snapshot(ctx context.Context) (io.ReadCloser, error) {
	panic("implement me")
}
func (o mockMaintenance) MoveLeader(ctx context.Context, transfereeID uint64) (*clientv3.MoveLeaderResponse, error) {
	panic("implement me")
}

func Test_checkEtcdStatus(t *testing.T) {
	etcdClient := &clientv3.Client{
		Maintenance: &mockMaintenance{},
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(etcdClient), "Status", func(client *clientv3.Client, ctx context.Context, endpoint string) (*clientv3.StatusResponse, error) {
		return nil, nil
	})
	type args struct {
		etcdClient *clientv3.Client
		endpoints  string
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "etcd ready",
			args: args{
				etcdClient: etcdClient,
				endpoints:  goodEndpoint,
			},
			wantErr: false,
		},
		{
			name: "etcd not ready",
			args: args{
				etcdClient: etcdClient,
				endpoints:  badEndpoint,
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if err := checkEtcdStatus(tt.args.etcdClient, tt.args.endpoints); (err != nil) != tt.wantErr {
				t.Errorf("checkEtcdStatus() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
