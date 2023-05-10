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
	"encoding/json"
	"fmt"
	"math"
	"sort"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
)

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
	event cptype.ComponentEvent, globalStateData *cptype.GlobalStateData) error {
	if err := t.InitComponent(ctx); err != nil {
		return fmt.Errorf("failed to set eventTable component ctx bundle, %v", err)
	}
	if err := t.GenComponentState(component); err != nil {
		return err
	}
	if err := t.RenderList(); err != nil {
		return err
	}
	t.SetComponentValue(ctx)
	t.Transfer(component)
	return nil
}

func (t *ComponentEventTable) InitComponent(ctx context.Context) error {
	t.ctx = ctx
	t.server = steveServer
	t.SDK = cputil.SDK(ctx)
	return nil
}

func (t *ComponentEventTable) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}

	data, err := json.Marshal(component.State)
	if err != nil {
		logrus.Errorf("failed to marshal for eventTable state, %v", err)
		return err
	}
	var state State
	err = json.Unmarshal(data, &state)
	if err != nil {
		logrus.Errorf("failed to unmarshal for eventTable state, %v", err)
		return err
	}
	t.State = state
	return nil
}

func (t *ComponentEventTable) RenderList() error {
	splits := strings.Split(t.State.PodID, "_")
	if len(splits) != 2 {
		return fmt.Errorf("invalid pod id: %s", t.State.PodID)
	}
	namespace, name := splits[0], splits[1]
	userID := t.SDK.Identity.UserID
	orgID := t.SDK.Identity.OrgID

	req := apistructs.SteveRequest{
		UserID:      userID,
		OrgID:       orgID,
		Type:        apistructs.K8SEvent,
		ClusterName: t.State.ClusterName,
		Namespace:   namespace,
	}

	list, err := t.server.ListSteveResource(t.ctx, &req)
	if err != nil {
		return err
	}

	var items []Item
	for _, item := range list {
		obj := item.Data()
		fields := obj.StringSlice("metadata", "fields")
		if len(fields) != 10 {
			logrus.Errorf("length of event fields is invalid: %d", len(fields))
			continue
		}
		ref := fields[3]
		splits := strings.Split(ref, "/")
		if len(splits) != 2 {
			continue
		}
		res, refName := splits[0], splits[1]
		if res != "pod" || refName != name {
			continue
		}
		var ts int64 = math.MaxInt64
		lastSeen := "unknown"
		lastSeenTimestamp, err := time.ParseDuration(fields[0])
		if err == nil {
			lastSeen = fields[0]
			ts = lastSeenTimestamp.Milliseconds()
		}
		items = append(items, Item{
			ID:                obj.String("metadata", "name"),
			LastSeen:          lastSeen,
			LastSeenTimestamp: ts,
			Type:              t.SDK.I18n(fields[1]),
			Reason:            fields[2],
			Message:           fields[6],
		})
	}
	sort.Slice(items, func(i, j int) bool {
		return items[i].LastSeenTimestamp < items[j].LastSeenTimestamp
	})
	t.Data.List = items
	return nil
}

func (t *ComponentEventTable) SetComponentValue(ctx context.Context) {
	t.Props = Props{
		RequestIgnore: []string{"data"},
		RowKey:        "id",
		Pagination:    false,
		Columns: []Column{
			{
				DataIndex: "lastSeen",
				Title:     cputil.I18n(ctx, "lastSeen"),
				Width:     160,
			},
			{
				DataIndex: "type",
				Title:     cputil.I18n(ctx, "eventType"),
				Width:     100,
			},
			{
				DataIndex: "reason",
				Title:     cputil.I18n(ctx, "reason"),
				Width:     100,
			},
			{
				DataIndex: "message",
				Title:     cputil.I18n(ctx, "message"),
				Width:     120,
			},
		},
		SortDirections: []string{"descend", "ascend"},
	}
	t.Operations = make(map[string]interface{})
	t.Operations[apistructs.OnChangePageNoOperation.String()] = Operation{
		Key:    apistructs.OnChangePageNoOperation.String(),
		Reload: true,
	}
	t.Operations[apistructs.OnChangePageSizeOperation.String()] = Operation{
		Key:    apistructs.OnChangePageSizeOperation.String(),
		Reload: true,
	}
	t.Operations[apistructs.OnChangeSortOperation.String()] = Operation{
		Key:    apistructs.OnChangeSortOperation.String(),
		Reload: true,
	}
}

func (t *ComponentEventTable) Transfer(c *cptype.Component) {
	c.Props = cputil.MustConvertProps(t.Props)
	c.Data = map[string]interface{}{
		"list": t.Data.List,
	}
	c.State = map[string]interface{}{
		"clusterName": t.State.ClusterName,
		"podId":       t.State.PodID,
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
	if l >= length {
		l = 0
	}
	r := l + pageSize
	if r > length {
		r = length
	}
	return l, r
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-podDetail", "eventTable", func() servicehub.Provider {
		return &ComponentEventTable{}
	})
}
