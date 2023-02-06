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

package k8s

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
	orgCache "github.com/erda-project/erda/internal/tools/orchestrator/scheduler/cache/org"
)

type QuotaError struct {
	err error
}

func (e QuotaError) Error() string {
	return e.err.Error()
}

func NewQuotaError(msg string) error {
	return QuotaError{errors.New(msg)}
}

func IsQuotaError(err error) bool {
	_, ok := err.(QuotaError)
	return ok
}

func (k *Kubernetes) GetWorkspaceLeftQuota(ctx context.Context, projectID, workspace string) (cpu, mem int64, err error) {
	cpuQuota, memQuota, err := k.bdl.GetWorkspaceQuota(&apistructs.GetWorkspaceQuotaRequest{
		ProjectID: projectID,
		Workspace: workspace,
	})
	if err != nil {
		return
	}
	logrus.Infof("get workspace %s of project %s quota: cpu: %d. mem: %d", workspace, projectID, cpuQuota, memQuota)

	namespaces, err := k.bdl.GetWorkspaceNamespaces(&apistructs.GetWorkspaceNamespaceRequest{
		ProjectID: projectID,
		Workspace: workspace,
	})
	if err != nil {
		return 0, 0, err
	}

	cpuQty := resource.NewQuantity(0, resource.DecimalSI)
	memQty := resource.NewQuantity(0, resource.BinarySI)
	for _, namespace := range namespaces {
		pods, err := k.k8sClient.ClientSet.CoreV1().Pods(namespace).List(ctx, v1.ListOptions{})
		if err != nil {
			return 0, 0, err
		}
		for _, pod := range pods.Items {
			if pod.Status.Phase == "Pending" || pod.Status.Phase == "Succeeded" || pod.Status.Phase == "Failed" {
				continue
			}
			for _, container := range pod.Spec.Containers {
				if container.Resources.Requests == nil {
					continue
				}
				cpuQty.Add(*container.Resources.Requests.Cpu())
				memQty.Add(*container.Resources.Requests.Memory())
			}
		}
	}

	logrus.Infof("Requested resource for workspace %s in project %s, CPU: %d, Mem: %d\n", workspace, projectID, cpuQty.MilliValue(), memQty.Value())

	leftCPU := cpuQuota - cpuQty.MilliValue()
	leftMem := memQuota - memQty.Value()
	return leftCPU, leftMem, nil
}

func max(a, b int64) int64 {
	if a > b {
		return a
	}
	return b
}

func (k *Kubernetes) CheckQuota(ctx context.Context, projectID, workspace, runtimeID string, requestsCPU, requestsMem int64, kind, serviceName string) (bool, string, error) {
	if projectID == "" || workspace == "" {
		return true, "", nil
	}
	if requestsCPU <= 0 && requestsMem <= 0 {
		return true, "", nil
	}
	leftCPU, leftMem, err := k.GetWorkspaceLeftQuota(ctx, projectID, workspace)
	if err != nil {
		return false, "", err
	}

	// get org locale
	var locale string
	if orgDTO, ok := orgCache.GetOrgByProjectID(projectID); ok {
		locale = orgDTO.Locale
	}

	if requestsCPU > leftCPU || requestsMem > leftMem {
		humanLog, primevalLog := getLogContent(locale, requestsCPU, requestsMem, leftCPU, leftMem, kind, serviceName)
		if runtimeID != "" {
			if err = k.bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
				ErrorLog: apistructs.ErrorLog{
					ResourceType:   apistructs.RuntimeError,
					ResourceID:     runtimeID,
					OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
					HumanLog:       humanLog,
					PrimevalLog:    primevalLog,
					DedupID:        fmt.Sprintf("%s-scheduler-error", runtimeID),
				},
			}); err != nil {
				logrus.Errorf("failed to create quota error log when check quota, %v", err)
			} else {
				logrus.Infof("Create/Update quota error log for runtime %s succeeded", runtimeID)
			}
		}
		return false, primevalLog, nil
	}
	return true, "", nil
}

func getLogContent(locale string, deltaCPU, deltaMem, leftCPU, leftMem int64, kind, serviceName string) (string, string) {
	leftCPU = max(leftCPU, 0)
	leftMem = max(leftMem, 0)
	reqCPUStr := resourceToString(float64(deltaCPU), "cpu")
	leftCPUStr := resourceToString(float64(leftCPU), "cpu")
	reqMemStr := resourceToString(float64(deltaMem), "memory")
	leftMemStr := resourceToString(float64(leftMem), "memory")

	logrus.Infof("Checking workspace quota, requests cpu:%s cores, left %s cores; requests memory: %s, left %s",
		reqCPUStr, leftCPUStr, reqMemStr, leftMemStr)

	humanLogs := []string{i18n.Sprintf(locale, "NotEnoughQuotaOnCurrentWorkspace")}
	primevalLogs := []string{"Resource quota is not enough in current workspace"}
	switch kind {
	case "stateless":
		humanLogs = append(humanLogs, i18n.Sprintf(locale, "FailedToDeployService", serviceName))
		primevalLogs = append(primevalLogs, fmt.Sprintf("failed to deploy service %s", serviceName))
	case "stateful":
		humanLogs = append(humanLogs, i18n.Sprintf(locale, "FailedTodDeployAddon", serviceName))
		primevalLogs = append(primevalLogs, fmt.Sprintf("failed to deploy addon %s.", serviceName))
	case "update":
		humanLogs = append(humanLogs, i18n.Sprintf(locale, "FailedToUpdateService", serviceName))
		primevalLogs = append(primevalLogs, fmt.Sprintf("failed to update service %s", serviceName))
	case "scale":
		humanLogs = append(humanLogs, i18n.Sprintf(locale, "FailedToScaleService", serviceName))
		primevalLogs = append(primevalLogs, fmt.Sprintf("failed to scale service %s", serviceName))
	}

	if deltaCPU > leftCPU {
		humanLogs = append(humanLogs, i18n.Sprintf(locale, "CPURequiredMoreThanRemaining", reqCPUStr, leftCPUStr))
		primevalLogs = append(primevalLogs, fmt.Sprintf("Requests CPU added %s core(s), which is greater than the current remaining CPU %s core(s)", reqCPUStr, leftCPUStr))
	}
	if deltaMem > leftMem {
		humanLogs = append(humanLogs, i18n.Sprintf(locale, "MemRequiredMoreThanRemaining", reqMemStr, leftMemStr))
		primevalLogs = append(primevalLogs, fmt.Sprintf("Requests memory added %s, which is greater than the current remaining %s", reqMemStr, leftMemStr))
	}
	return strings.Join(humanLogs, "，"), strings.Join(primevalLogs, ". ")
}

func getRequestsResources(containers []corev1.Container) (cpu, mem int64) {
	cpuQuantity := resource.NewQuantity(0, resource.DecimalSI)
	memQuantity := resource.NewQuantity(0, resource.BinarySI)
	for _, c := range containers {
		if c.Resources.Requests == nil {
			continue
		}
		cpuQuantity.Add(*c.Resources.Requests.Cpu())
		memQuantity.Add(*c.Resources.Requests.Memory())
	}
	return cpuQuantity.MilliValue(), memQuantity.Value()
}

func resourceToString(res float64, tp string) string {
	switch tp {
	case "cpu":
		return fmt.Sprintf("%.3f", res/1000)
	case "memory":
		isNegative := 1.0
		if res < 0 {
			res = -res
			isNegative = -1
		}
		units := []string{"B", "KB", "MB", "GB", "TB"}
		i := 0
		for res >= 1<<10 && i < len(units)-1 {
			res /= 1 << 10
			i++
		}
		return fmt.Sprintf("%.3f%s", res*isNegative, units[i])
	default:
		return fmt.Sprintf("%.f", res)
	}
}
