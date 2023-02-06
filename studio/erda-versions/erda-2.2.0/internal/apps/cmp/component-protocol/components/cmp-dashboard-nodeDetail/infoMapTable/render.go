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

package infoMapTable

import (
	"context"
	"fmt"
	"sort"

	"github.com/rancher/wrangler/pkg/data"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/components/cmp-dashboard-nodeDetail/common"
)

func (infoMapTable *InfoMapTable) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	node := (*gs)["node"].(data.Object)
	infoMapTable.SDK = cputil.SDK(ctx)
	memSizeQty, err := resource.ParseQuantity(node.String("status", "capacity", "memory"))
	if err != nil {
		return err
	}
	memSize := memSizeQty.AsApproximateFloat64()
	pairs := make([]Pair, 0)
	mapp := node.Map("status", "nodeInfo")
	pairs = []Pair{{
		Id: fmt.Sprintf("%d", 0),
		Label: Label{
			Value:       infoMapTable.SDK.I18n("cpu-size"),
			RenderType:  "text",
			StyleConfig: StyleConfig{"bold"},
		},
		Value: node.String("status", "capacity", "cpu"),
	}, {
		Id: fmt.Sprintf("%d", 1),
		Label: Label{
			Value:       infoMapTable.SDK.I18n("memory-size"),
			RenderType:  "text",
			StyleConfig: StyleConfig{"bold"},
		},
		Value: infoMapTable.reScale(memSize),
	},
	}
	keys := make([]string, 0)
	for k := range mapp {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool {
		return keys[i] < keys[j]
	})
	i := 0
	for _, k := range keys {
		i++
		pairs = append(pairs, Pair{
			Id: fmt.Sprintf("%d", i),
			Label: Label{
				Value:       infoMapTable.SDK.I18n(k),
				RenderType:  "text",
				StyleConfig: StyleConfig{"bold"},
			},
			Value: mapp[k].(string),
		})
	}
	infoMapTable.Props = infoMapTable.getProps()
	c.Data = map[string]interface{}{"list": pairs}
	err = common.Transfer(infoMapTable.Props, &c.Props)
	if err != nil {
		return err
	}
	return nil
}

func (infoMapTable *InfoMapTable) reScale(v float64) string {
	strs := []string{"B", "KB", "MB", "GB", "TB"}
	i := 0
	for ; v >= 1024; i++ {
		v /= 1024
	}
	return fmt.Sprintf("%.1f", v) + strs[i]
}

func (infoMapTable *InfoMapTable) getProps() Props {
	return Props{
		RowKey:     "id",
		Bordered:   true,
		ShowHeader: false,
		Pagination: false,
		Columns: []Column{{
			DataIndex: "label",
			Title:     "",
			Width:     100,
		}, {
			DataIndex: "value",
			Title:     "",
			Width:     100,
		}},
	}
}
func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodeDetail", "infoMapTable", func() servicehub.Provider {
		return &InfoMapTable{Type: "Table"}
	})
}
