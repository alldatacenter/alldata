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

package storage

import (
	"context"

	"github.com/erda-project/erda-proto-go/oap/entity/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

type (
	// ListOptions .
	ListOptions struct {
		Type                  string
		Labels                map[string]string
		Limit                 int
		UpdateTimeUnixNanoMin int64
		UpdateTimeUnixNanoMax int64
		Debug                 bool
	}
	// Storage .
	Storage interface {
		NewWriter(ctx context.Context) (storekit.BatchWriter, error)
		SetEntity(ctx context.Context, data *pb.Entity) error
		RemoveEntity(ctx context.Context, typ, key string) (bool, error)
		GetEntity(ctx context.Context, typ, key string) (*pb.Entity, error)
		ListEntities(ctx context.Context, opts *ListOptions) ([]*pb.Entity, int64, error)
	}
)
