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

package jaeger

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	common "github.com/erda-project/erda-proto-go/common/pb"
	jaegerpb "github.com/erda-project/erda-proto-go/oap/collector/receiver/jaeger/pb"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/protoparser/jsonmarshal"
)

type jaegerServiceImpl struct {
	Log logs.Logger
	p   *provider
}

func (s *jaegerServiceImpl) SpansWithThrift(ctx context.Context, req *jaegerpb.PostSpansRequest) (*common.VoidResponse, error) {
	if req.Spans != nil {
		for i := range req.Spans {
			err := jsonmarshal.ParseInterface(req.Spans[i], func(buf []byte) error {
				s.p.consumer(odata.NewRaw(buf))
				return nil
			})
			if err != nil {
				return nil, fmt.Errorf("parse failed: %w", err)
			}
		}
	}
	return &common.VoidResponse{}, nil
}
