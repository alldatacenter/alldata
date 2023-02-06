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

package card

import (
	"context"
	"fmt"

	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda/internal/apps/msp/apm/service/view/common"
)

type RpsCard struct {
	*BaseCard
}

func (r *RpsCard) GetCard(ctx context.Context) (*ServiceCard, error) {
	interval := (r.BaseCard.EndTime - r.BaseCard.StartTime) / 1e3
	statement := fmt.Sprintf("SELECT sum(elapsed_count::field)/%v "+
		"FROM %s "+
		"WHERE (target_terminus_key::tag=$terminus_key OR source_terminus_key::tag=$terminus_key) "+
		"%s "+
		"%s ",
		interval,
		common.GetDataSourceNames(r.Layer),
		common.BuildServerSideServiceIdFilterSql("$service_id", r.Layer),
		common.BuildLayerPathFilterSql(r.LayerPath, "$layer_path", r.FuzzyPath, r.Layer))

	var layerPathParam *structpb.Value
	if r.FuzzyPath {
		layerPathParam = common.NewStructValue(map[string]interface{}{"regex": ".*" + r.LayerPath + ".*"})
	} else {
		layerPathParam = structpb.NewStringValue(r.LayerPath)
	}

	queryParams := map[string]*structpb.Value{
		"terminus_key": structpb.NewStringValue(r.TenantId),
		"service_id":   structpb.NewStringValue(r.ServiceId),
		"layer_path":   layerPathParam,
	}

	return r.QueryAsServiceCard(ctx, statement, queryParams, string(CardTypeRps), "reqs/s", common.FormatFloatWith2Digits)
}
