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
	"sort"
	"testing"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/gateway/dto"
)

func TestSortByTypeList_Less(t *testing.T) {
	var list = dto.SortByTypeList{
		dto.RuntimeDomain{DomainType: dto.EDT_CUSTOM, Domain: "app-custom-1.terminus.io"},
		dto.RuntimeDomain{DomainType: dto.EDT_PACKAGE, Domain: "app-package-1.terminus.io"},
		dto.RuntimeDomain{DomainType: dto.EDT_CUSTOM, Domain: "app-custom3-.terminus.io"},
		dto.RuntimeDomain{DomainType: dto.EDT_DEFAULT, Domain: "app-default.terminus.io"},
		dto.RuntimeDomain{DomainType: dto.EDT_PACKAGE, Domain: "app-package-2.terminus.io"},
		dto.RuntimeDomain{DomainType: dto.EDT_CUSTOM, Domain: "app-custom-2.terminus.io"},
	}
	sort.Sort(list)
	for i := range list {
		t.Logf("[%d] DomainType: %s, Domain: %s", i, list[i].DomainType, list[i].Domain)
	}
}
