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
	"testing"

	"github.com/coreos/etcd/clientv3"
)

func Test_parseCronIDFromWatchedKey(t *testing.T) {
	type args struct {
		key string
	}
	tests := []struct {
		name    string
		args    args
		want    uint64
		wantErr bool
	}{
		{
			name: "test with error",
			args: args{
				key: etcdCronPrefixAddKey + "",
			},
			want:    0,
			wantErr: true,
		},
		{
			name: "test with add",
			args: args{
				key: etcdCronPrefixAddKey + "101",
			},
			want:    uint64(101),
			wantErr: false,
		},
		{
			name: "test with delete",
			args: args{
				key: etcdCronPrefixDeleteKey + "101",
			},
			want:    uint64(101),
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := parseCronIDFromWatchedKey(tt.args.key)
			if (err != nil) != tt.wantErr {
				t.Errorf("parseCronIDFromWatchedKey() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("parseCronIDFromWatchedKey() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_provider_getCronExprFromEtcd(t *testing.T) {
	type fields struct {
		EtcdClient *clientv3.Client
	}
	type args struct {
		ctx context.Context
		key string
	}

	etcd := clientv3.Client{
		KV: CronMockKV{},
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "test with error",
			fields: fields{
				EtcdClient: &etcd,
			},
			args: args{
				ctx: context.Background(),
				key: "1000",
			},
			want:    "",
			wantErr: true,
		},
		{
			name: "test with error2",
			fields: fields{
				EtcdClient: &etcd,
			},
			args: args{
				ctx: context.Background(),
				key: "1001",
			},
			want:    "",
			wantErr: true,
		},
		{
			name: "test with correct",
			fields: fields{
				EtcdClient: &etcd,
			},
			args: args{
				ctx: context.Background(),
				key: "1002",
			},
			want:    "*/1 * * * *",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &provider{
				EtcdClient: tt.fields.EtcdClient,
			}
			got, err := s.getCronExprFromEtcd(tt.args.ctx, tt.args.key)
			if (err != nil) != tt.wantErr {
				t.Errorf("getCronExprFromEtcd() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getCronExprFromEtcd() got = %v, want %v", got, tt.want)
			}
		})
	}
}
