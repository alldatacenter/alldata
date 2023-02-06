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

package Header

import (
	"context"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp"
)

var steveServer cmp.SteveServer

func (header *Header) Init(ctx servicehub.Context) error {
	server, ok := ctx.Service("cmp").(cmp.SteveServer)
	if !ok {
		return errors.New("failed to init component, cmp service in ctx is not a steveServer")
	}
	steveServer = server
	return nil
}

func (header *Header) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	var (
		err error
		req apistructs.SteveRequest
	)
	header.SDK = cputil.SDK(ctx)
	req.OrgID = header.SDK.Identity.OrgID
	req.UserID = header.SDK.Identity.UserID
	req.Type = apistructs.K8SNode

	clusterName, ok := header.SDK.InParams["clusterName"].(string)
	if !ok {
		return errors.New("invalid clusterName")
	}
	req.ClusterName = clusterName

	nodeId, ok := header.SDK.InParams["nodeId"].(string)
	if !ok {
		return errors.New("invalid nodeID")
	}
	req.Name = nodeId

	resp, err := steveServer.GetSteveResource(ctx, &req)
	if err != nil {
		return err
	}
	node := resp.Data()
	(*gs)["node"] = node
	return nil
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodeDetail", "header", func() servicehub.Provider {
		return &Header{Type: "RowContainer"}
	})
}
