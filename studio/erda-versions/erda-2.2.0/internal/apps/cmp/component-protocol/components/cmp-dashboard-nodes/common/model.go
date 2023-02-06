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

package common

import (
	"errors"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

const (
	Default = "Default"
)

var (

	// cmp bashboard table
	CMPDashboardChangePageNoOperationKey   cptype.OperationKey = "changePageNo"
	CMPDashboardChangePageSizeOperationKey cptype.OperationKey = "changePageSize"
	CMPDashboardSortByColumnOperationKey   cptype.OperationKey = "changeSort"

	// cmp bashboard clusterFilter
	CMPDashboardFilterOperationKey cptype.OperationKey = "filter"

	// Freeze Button
	CMPDashboardUncordonNode cptype.OperationKey = "uncordon"
	CMPDashboardCordonNode   cptype.OperationKey = "cordon"
	CMPDashboardDrainNode    cptype.OperationKey = "drain"
	CMPDashboardOfflineNode  cptype.OperationKey = "offline"
	CMPDashboardOnlineNode   cptype.OperationKey = "online"
	CMPDashboardBatchSubmit  cptype.OperationKey = "batchSubmit"

	CMPDashboardTableTabs cptype.OperationKey = "changeViewType"

	CMPDashboardSubmit cptype.OperationKey = "submit"

	CMPDashboardRemoveLabel cptype.OperationKey = "deleteLabel"
)
var (
	NodeNotFoundErr    = errors.New("node not found")
	ClusterNotFoundErr = errors.New("cluster not found")
	NothingToBeDoneErr = errors.New("nothing to be done")

	TypeNotAvailableErr = errors.New("type not available")
	ResourceNotFoundErr = errors.New("resource type not available")

	//util error
	PtrRequiredErr = errors.New("ptr is required")
)
