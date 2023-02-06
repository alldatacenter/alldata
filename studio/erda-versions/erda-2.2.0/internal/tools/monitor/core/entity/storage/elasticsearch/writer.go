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

package elasticsearch

import (
	"context"

	"github.com/ahmetb/go-linq/v3"

	"github.com/erda-project/erda-proto-go/oap/entity/pb"
)

// Writer .
type Writer struct {
	p   *provider
	ctx context.Context
}

// WriteN .
func (w *Writer) WriteN(vals ...interface{}) (n int, err error) {
	var entities []*pb.Entity
	linq.From(vals).ToSlice(&entities)
	return w.p.SetEntities(w.ctx, entities)
}

// Write .
func (w *Writer) Write(val interface{}) error {
	if val == nil {
		return nil
	}
	return w.p.SetEntity(w.ctx, val.(*pb.Entity))
}

// Close .
func (w *Writer) Close() error { return nil }
