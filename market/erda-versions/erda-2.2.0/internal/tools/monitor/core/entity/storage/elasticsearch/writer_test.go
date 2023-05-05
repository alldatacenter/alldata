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
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda-proto-go/oap/entity/pb"
)

func Test_WriteN_Should_Success(t *testing.T) {
	monkey.Patch((*provider).SetEntities, func(p *provider, ctx context.Context, list []*pb.Entity) (int, error) {
		return len(list), nil
	})
	defer monkey.Unpatch((*provider).SetEntities)

	w := Writer{
		ctx: context.Background(),
		p:   &provider{},
	}

	var entities []interface{}
	entities = append(entities, &pb.Entity{Id: "1"})
	entities = append(entities, &pb.Entity{Id: "2"})
	result, err := w.WriteN(entities...)
	if err != nil {
		t.Errorf("error assert failed, expect: nil, but got: %s", err)
	}
	if result != 2 {
		t.Errorf("result assert failed, expect: %d, but got: %d", 2, result)
	}
}
