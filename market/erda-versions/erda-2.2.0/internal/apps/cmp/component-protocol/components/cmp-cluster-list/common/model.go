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
	"context"
	"fmt"
	"reflect"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/square/go-jose.v2/json"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/conf"
	"github.com/erda-project/erda/pkg/k8sclient"
)

const (
	CMPClusterList cptype.OperationKey = "list"

	StatusPending         = "pending"
	StatusOnline          = "online"
	StatusOffline         = "offline"
	StatusInitializing    = "initializing"
	StatusInitializeError = "initialize error"
	StatusUnknown         = "unknown"

	Success    = "success"
	Error      = "error"
	Default    = "default"
	Processing = "processing"
	Warning    = "warning"
)

var (
	PtrRequiredErr     = errors.New("pointer required")
	NothingToBeDoneErr = errors.New("nothing to be done")

	diceOperator = "/apis/dice.terminus.io/v1beta1/namespaces/%s/dices/dice"
	erdaOperator = "/apis/erda.terminus.io/v1beta1/namespaces/%s/erdas/erda"
	checkCRDs    = []string{erdaOperator, diceOperator}
)

// Transfer transfer a to b with json, kind of b must be pointer
func Transfer(a, b interface{}) error {
	if reflect.ValueOf(b).Kind() != reflect.Ptr {
		return PtrRequiredErr
	}
	if a == nil {
		return NothingToBeDoneErr
	}
	aBytes, err := json.Marshal(a)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(aBytes, b); err != nil {
		return err
	}
	return nil
}

func GetClusterStatus(kc *k8sclient.K8sClient, meta *clusterpb.ClusterInfo) (string, error) {
	if kc == nil || kc.ClientSet == nil {
		return "", fmt.Errorf("kubernetes client is nil")
	}

	// if manage config is nil, cluster import with inet or other
	if meta.ManageConfig == nil {
		return StatusOffline, nil
	}

	switch meta.ManageConfig.Type {
	case apistructs.ManageProxy:
		if meta.ManageConfig.Token == "" || meta.ManageConfig.Address == "" {
			return StatusPending, nil
		}
	case apistructs.ManageToken:
		if meta.ManageConfig.Token == "" || meta.ManageConfig.Address == "" {
			return StatusOffline, nil
		}
	case apistructs.ManageCert:
		if meta.ManageConfig.CertData == "" && meta.ManageConfig.KeyData == "" {
			return StatusOffline, nil
		}
	}

	ec := &apistructs.DiceCluster{}

	var (
		res           []byte
		err           error
		resourceExist bool
	)

	for _, selfLink := range checkCRDs {
		res, err = kc.ClientSet.Discovery().RESTClient().Get().
			AbsPath(fmt.Sprintf(selfLink, conf.ErdaNamespace())).
			DoRaw(context.Background())
		if err != nil {
			logrus.Error(err)
			continue
		}
		resourceExist = true
		break
	}

	if !resourceExist {
		return StatusUnknown, nil
	}

	if err = json.Unmarshal(res, &ec); err != nil {
		logrus.Errorf("unmarsharl data error, data: %v", string(res))
		return StatusUnknown, err
	}

	switch ec.Status.Phase {
	case apistructs.ClusterPhaseRunning:
		return StatusOnline, nil
	case apistructs.ClusterPhaseNone, apistructs.ClusterPhaseCreating, apistructs.ClusterPhaseInitJobs,
		apistructs.ClusterPhasePending, apistructs.ClusterPhaseUpdating:
		return StatusInitializing, nil
	case apistructs.ClusterPhaseFailed:
		return StatusInitializeError, nil
	default:
		return StatusUnknown, nil
	}
}

func ParseManageType(mc *clusterpb.ManageConfig) string {
	if mc == nil {
		return "create"
	}

	switch mc.Type {
	case apistructs.ManageProxy:
		return "agent"
	case apistructs.ManageToken, apistructs.ManageCert:
		return "import"
	default:
		return "create"
	}
}

func RescaleBinary(num float64) string {
	metrics := []string{"B", "KiB", "MiB", "GiB", "TiB"}
	i := 0
	for num > 1024 && i < len(metrics)-1 {
		num /= 1024
		i++
	}
	return fmt.Sprintf("%.1f %s", num, metrics[i])
}
