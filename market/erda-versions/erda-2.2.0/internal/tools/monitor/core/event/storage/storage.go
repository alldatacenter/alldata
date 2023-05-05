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

	"github.com/erda-project/erda/internal/tools/monitor/core/event"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit"
)

type (
	// Storage .
	Storage interface {
		NewWriter(ctx context.Context) (storekit.BatchWriter, error)
		QueryPaged(ctx context.Context, sel *Selector, pageNo, pageSize int) ([]*event.Event, error)
	}

	// Operator .
	Operator int32

	// Filter .
	Filter struct {
		Key   string
		Op    Operator
		Value interface{}
	}

	// Selector .
	Selector struct {
		Start   int64
		End     int64
		Filters []*Filter
		Debug   bool
		Options map[string]interface{}
	}
)

const (
	// EQ equal
	EQ Operator = iota
	REGEXP
)
