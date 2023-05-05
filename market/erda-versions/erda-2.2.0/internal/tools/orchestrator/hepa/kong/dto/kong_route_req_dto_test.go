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

package dto_test

import (
	"testing"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
)

func TestNewKongRouteReqDto(t *testing.T) {
	req := dto.NewKongRouteReqDto()
	if req.StripPath == nil || !*req.StripPath {
		t.Fatal("err")
	}
	if req.PathHandling == nil || *req.PathHandling != "v1" {
		t.Fatal("err")
	}
}

type versioning struct {
	version string
}

func (v versioning) GetVersion() (string, error) {
	return v.version, nil
}

func TestKongRouteReqDto_Adjust(t *testing.T) {
	var (
		req = dto.NewKongRouteReqDto()
		v   versioning
	)
	req.Adjust(dto.Versioning(v))
	if req.PathHandling != nil {
		t.Fatal("req.PathHandling should be nil")
	}

	v.version = "2.2.0"
	req.Adjust(dto.Versioning(v))
	if req.PathHandling == nil || *req.PathHandling != "v1" {
		t.Fatal("req.PathHandling should be v1")
	}
}

func TestKongRouteReqDto_AddTag(t *testing.T) {
	var req = dto.NewKongRouteReqDto()
	req.AddTag("package_id", "0x333")
	req.AddTag("api_id", "0xtag")
	t.Log(req.Tags)
}
