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

package cms

import (
	"context"

	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
)

type ConfigManager interface {
	// IdempotentCreateNs create ns idempontently
	IdempotentCreateNs(ctx context.Context, ns string) error
	// IdempotentDeleteNs delete ns and its configs idempotently
	IdempotentDeleteNs(ctx context.Context, ns string) error
	// PrefixListNs list ns by prefix
	PrefixListNs(ctx context.Context, nsPrefix string) ([]*pb.PipelineCmsNs, error)
	// UpdateConfigs create if not exists; update if already exist
	UpdateConfigs(ctx context.Context, ns string, kvs map[string]*pb.PipelineCmsConfigValue) error
	// DeleteConfigs delete configs by keys; if ns not exists, return nil
	DeleteConfigs(ctx context.Context, ns string, keys ...string) error
	// GetConfigs get configs: if ns is empty, return nil; if keys is not empty, return specified configs
	GetConfigs(ctx context.Context, ns string, globalDecrypt bool, keys ...*pb.PipelineCmsConfigKey) (map[string]*pb.PipelineCmsConfigValue, error)
	// BatchGetConfigs: get all config information under Namespaces
	BatchGetConfigs(ctx context.Context, req *pb.CmsNsConfigsBatchGetRequest) ([]*pb.PipelineCmsConfig, error)
}

func transformKeysToStrSlice(keys ...*pb.PipelineCmsConfigKey) []string {
	result := make([]string, 0, len(keys))
	for _, key := range keys {
		result = append(result, key.Key)
	}
	return result
}
