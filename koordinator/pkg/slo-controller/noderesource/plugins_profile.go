/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package noderesource

import (
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/noderesource/framework"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/noderesource/plugins/batchresource"
)

// NOTE: functions in this file can be overwritten for extension

func init() {
	// NOTE: plugins run in order of the registration.
	framework.RegisterNodePrepareExtender(nodePreparePlugins...)
	framework.RegisterNodeSyncExtender(nodeSyncPlugins...)
	framework.RegisterResourceCalculateExtender(resourceCalculatePlugins...)
}

var (
	// NodeSyncPlugin implements the check of resource updating.
	nodePreparePlugins = []framework.NodePreparePlugin{
		&batchresource.Plugin{},
	}
	// NodePreparePlugin implements node resource preparing for the calculated results.
	nodeSyncPlugins = []framework.NodeSyncPlugin{
		&batchresource.Plugin{},
	}
	// ResourceCalculatePlugin implements resource counting and overcommitment algorithms.
	resourceCalculatePlugins = []framework.ResourceCalculatePlugin{
		&batchresource.Plugin{},
	}
)
