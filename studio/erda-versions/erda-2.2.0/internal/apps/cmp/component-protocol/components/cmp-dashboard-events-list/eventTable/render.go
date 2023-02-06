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

package eventTable

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math"
	"strconv"
	"strings"

	"github.com/go-openapi/strfmt"
	"github.com/pkg/errors"
	"github.com/rancher/apiserver/pkg/types"
	"github.com/recallsong/go-utils/container/slice"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-events-list", "eventTable", func() servicehub.Provider {
		return &ComponentEventTable{}
	})
}

var steveServer cmp.SteveServer

func (t *ComponentEventTable) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (t *ComponentEventTable) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, _ *cptype.GlobalStateData) error {
	t.InitComponent(ctx)
	if err := t.GenComponentState(component); err != nil {
		return err
	}

	// set page no. and page size in first render
	if event.Operation == cptype.InitializeOperation {
		t.State.PageNo = 1
		t.State.PageSize = 20
		t.State.Sorter.Field = "lastSeen"
		t.State.Sorter.Order = "ascend"
	}
	// set page no. if triggered by filter
	if event.Operation == cptype.RenderingOperation || event.Operation == "changeSort" ||
		event.Operation == "changePageSize" {
		t.State.PageNo = 1
	}
	if err := t.DecodeURLQuery(); err != nil {
		return fmt.Errorf("failed to decode url query for eventTable component, %v", err)
	}
	if err := t.RenderList(); err != nil {
		return err
	}
	if err := t.EncodeURLQuery(); err != nil {
		return fmt.Errorf("failed to encode url query for eventTable component, %v", err)
	}
	t.SetComponentValue(ctx)
	t.Transfer(component)
	return nil
}

func (t *ComponentEventTable) InitComponent(ctx context.Context) {
	sdk := cputil.SDK(ctx)
	t.sdk = sdk
	t.ctx = ctx
	t.server = steveServer
}

func (t *ComponentEventTable) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}

	jsonData, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("failed to marshal for eventTable state, %v", err)
		return err
	}
	var state State
	err = json.Unmarshal(jsonData, &state)
	if err != nil {
		logrus.Errorf("failed to unmarshal for eventTable state, %v", err)
		return err
	}
	t.State = state
	return nil
}

func (t *ComponentEventTable) DecodeURLQuery() error {
	query, ok := t.sdk.InParams["eventTable__urlQuery"].(string)
	if !ok {
		return nil
	}
	decoded, err := base64.StdEncoding.DecodeString(query)
	if err != nil {
		return err
	}
	urlQuery := make(map[string]interface{})
	if err := json.Unmarshal(decoded, &urlQuery); err != nil {
		return err
	}
	t.State.PageNo = uint64(urlQuery["pageNo"].(float64))
	t.State.PageSize = uint64(urlQuery["pageSize"].(float64))
	sorterData := urlQuery["sorterData"].(map[string]interface{})
	t.State.Sorter.Field = sorterData["field"].(string)
	t.State.Sorter.Order = sorterData["order"].(string)
	return nil
}

func (t *ComponentEventTable) EncodeURLQuery() error {
	urlQuery := make(map[string]interface{})
	urlQuery["pageNo"] = int(t.State.PageNo)
	urlQuery["pageSize"] = int(t.State.PageSize)
	urlQuery["sorterData"] = t.State.Sorter

	jsonData, err := json.Marshal(urlQuery)
	if err != nil {
		return err
	}

	decoded := base64.StdEncoding.EncodeToString(jsonData)
	t.State.EventTableUQLQuery = decoded
	return nil
}

func (t *ComponentEventTable) RenderList() error {
	userID := t.sdk.Identity.UserID
	orgID := t.sdk.Identity.OrgID

	req := apistructs.SteveRequest{
		UserID:      userID,
		OrgID:       orgID,
		Type:        apistructs.K8SEvent,
		Namespace:   t.State.FilterValues.Namespace,
		ClusterName: t.State.ClusterName,
	}

	var (
		list []types.APIObject
		err  error
	)
	list, err = t.server.ListSteveResource(t.ctx, &req)
	if err != nil {
		return err
	}

	var items []Item
	for _, item := range list {
		obj := item.Data()
		if t.State.FilterValues.Type != nil && !contain(t.State.FilterValues.Type, obj.String("_type")) {
			continue
		}
		fields := obj.StringSlice("metadata", "fields")
		if len(fields) != 10 {
			logrus.Errorf("length of event fields is invalid: %d", len(fields))
			continue
		}
		if t.State.FilterValues.Search != "" && !strings.Contains(fields[2], t.State.FilterValues.Search) &&
			!strings.Contains(fields[3], t.State.FilterValues.Search) &&
			!strings.Contains(fields[5], t.State.FilterValues.Search) &&
			!strings.Contains(fields[6], t.State.FilterValues.Search) {
			continue
		}
		count, err := strconv.ParseInt(fields[8], 10, 64)
		if err != nil {
			logrus.Errorf("failed to parse count for event %s, %v", fields[9], err)
			continue
		}
		lastSeen := fmt.Sprintf("%s %s", fields[0], t.sdk.I18n("ago"))
		lastSeenTimestamp, err := strfmt.ParseDuration(fields[0])
		var ts int64 = 0
		if err != nil {
			ts = math.MaxInt64
			lastSeen = t.sdk.I18n("unknown")
		} else {
			ts = lastSeenTimestamp.Milliseconds()
		}

		items = append(items, Item{
			LastSeen:          lastSeen,
			LastSeenTimestamp: ts,
			Type:              t.sdk.I18n(fields[1]),
			Reason:            fields[2],
			Object:            fields[3],
			Source:            fields[5],
			Message:           fields[6],
			Count:             fields[8],
			CountNum:          count,
			Name:              fields[9],
			Namespace:         obj.String("metadata", "namespace"),
		})
	}
	if t.State.Sorter.Field != "" {
		cmpWrapper := func(field, order string) func(int, int) bool {
			ascend := order == "ascend"
			switch field {
			case "lastSeen":
				return func(i int, j int) bool {
					less := items[i].LastSeenTimestamp < items[j].LastSeenTimestamp
					if ascend {
						return less
					}
					return !less
				}
			case "type":
				return func(i int, j int) bool {
					less := items[i].Type < items[j].Type
					if ascend {
						return less
					}
					return !less
				}
			case "reason":
				return func(i int, j int) bool {
					less := items[i].Reason < items[j].Reason
					if ascend {
						return less
					}
					return !less
				}
			case "object":
				return func(i int, j int) bool {
					less := items[i].Object < items[j].Object
					if ascend {
						return less
					}
					return !less
				}
			case "source":
				return func(i int, j int) bool {
					less := items[i].Source < items[j].Source
					if ascend {
						return less
					}
					return !less
				}
			case "message":
				return func(i int, j int) bool {
					less := items[i].Message < items[j].Message
					if ascend {
						return less
					}
					return !less
				}
			case "count":
				return func(i int, j int) bool {
					less := items[i].CountNum < items[j].CountNum
					if ascend {
						return less
					}
					return !less
				}
			case "name":
				return func(i int, j int) bool {
					less := items[i].Name < items[j].Name
					if ascend {
						return less
					}
					return !less
				}
			case "namespace":
				return func(i int, j int) bool {
					less := items[i].Namespace < items[j].Namespace
					if ascend {
						return less
					}
					return !less
				}
			default:
				return func(i int, j int) bool {
					return false
				}
			}
		}
		slice.Sort(items, cmpWrapper(t.State.Sorter.Field, t.State.Sorter.Order))
	}

	t.Data.List = items
	t.State.Total = uint64(len(items))
	return nil
}

func (t *ComponentEventTable) SetComponentValue(ctx context.Context) {
	t.Props = Props{
		RequestIgnore:   []string{"data"},
		PageSizeOptions: []string{"10", "20", "50", "100"},
		Columns: []Column{
			{
				DataIndex: "lastSeen",
				Title:     cputil.I18n(ctx, "lastSeen"),
				Sorter:    true,
			},
			{
				DataIndex: "type",
				Title:     cputil.I18n(ctx, "eventType"),
				Sorter:    true,
			},
			{
				DataIndex: "reason",
				Title:     cputil.I18n(ctx, "reason"),
				Sorter:    true,
			},
			{
				DataIndex: "object",
				Title:     cputil.I18n(ctx, "object"),
				Sorter:    true,
			},
			{
				DataIndex: "source",
				Title:     cputil.I18n(ctx, "source"),
				Sorter:    true,
				Hidden:    true,
			},
			{
				DataIndex: "message",
				Title:     cputil.I18n(ctx, "message"),
				Sorter:    true,
			},
			{
				DataIndex: "count",
				Title:     cputil.I18n(ctx, "count"),
				Sorter:    true,
				Hidden:    true,
			},
			{
				DataIndex: "name",
				Title:     cputil.I18n(ctx, "name"),
				Sorter:    true,
				Hidden:    true,
			},
			{
				DataIndex: "namespace",
				Title:     cputil.I18n(ctx, "namespace"),
				Sorter:    true,
			},
		},
		SortDirections: []string{"descend", "ascend"},
	}
	t.Operations = make(map[string]interface{})
	t.Operations[apistructs.OnChangeSortOperation.String()] = Operation{
		Key:    apistructs.OnChangeSortOperation.String(),
		Reload: true,
	}
}

func (t *ComponentEventTable) Transfer(c *cptype.Component) {
	c.Props = cputil.MustConvertProps(t.Props)
	c.Data = map[string]interface{}{"list": t.Data.List}
	c.State = map[string]interface{}{
		"clusterName":          t.State.ClusterName,
		"filterValues":         t.State.FilterValues,
		"pageNo":               t.State.PageNo,
		"pageSize":             t.State.PageSize,
		"sorterData":           t.State.Sorter,
		"total":                t.State.Total,
		"eventTable__urlQuery": t.State.EventTableUQLQuery,
	}
	c.Operations = t.Operations
}

func contain(arr []string, target string) bool {
	for _, str := range arr {
		if target == str {
			return true
		}
	}
	return false
}

func getRange(length, pageNo, pageSize int) (int, int) {
	l := (pageNo - 1) * pageSize
	if l >= length || l < 0 {
		l = 0
	}
	r := l + pageSize
	if r > length || r < 0 {
		r = length
	}
	return l, r
}
