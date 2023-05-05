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

package cputil

import (
	"context"
	"encoding/json"
	"fmt"
	"math"
	"reflect"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/rancher/wrangler/pkg/data"
	"github.com/sirupsen/logrus"
	appsv1 "k8s.io/api/apps/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/internal/apps/cmp"
	"github.com/erda-project/erda/internal/apps/cmp/cache"
	"github.com/erda-project/erda/pkg/k8sclient"
	"github.com/erda-project/erda/pkg/k8sclient/scheme"
)

// ParseWorkloadStatus get status for workloads from .metadata.fields
func ParseWorkloadStatus(obj data.Object) (string, string, bool, error) {
	kind := obj.String("kind")
	fields := obj.StringSlice("metadata", "fields")

	switch kind {
	case "Deployment":
		replicas := obj.String("status", "replicas")
		if replicas == "0" || replicas == "" {
			return "Stopped", "default", false, nil
		}
		conditions := obj.Slice("status", "conditions")
		available, progressing, failure := false, false, false
		for _, cond := range conditions {
			statusTrue := cond.String("status") == string(v1.ConditionTrue)
			switch cond.String("type") {
			case string(appsv1.DeploymentAvailable):
				available = statusTrue
			case string(appsv1.DeploymentProgressing):
				progressing = statusTrue
			case string(appsv1.DeploymentReplicaFailure):
				failure = statusTrue
			}
		}
		if failure {
			return "Abnormal", "error", false, nil
		}
		if available && progressing {
			return "Active", "success", true, nil
		}
		if available || progressing {
			return "Updating", "processing", true, nil
		}
		return "Abnormal", "error", false, nil
	case "DaemonSet":
		if len(fields) != 11 {
			return "", "", false, fmt.Errorf("daemonset %s has invalid fields length", obj.String("metadata", "name"))
		}
		desired := fields[1]
		if desired == "0" || desired == "" {
			return "Stopped", "default", false, nil
		}
		readyReplicas := fields[3]
		updatedReplicas := fields[4]
		if desired == readyReplicas && desired == updatedReplicas {
			return "Active", "success", true, nil
		}
		if desired != updatedReplicas {
			return "Updating", "processing", true, nil
		}
		return "Abnormal", "error", false, nil
	case "StatefulSet":
		replicas := obj.String("status", "replicas")
		if replicas == "0" || replicas == "" {
			return "Stopped", "default", false, nil
		}
		readyReplicas := obj.String("status", "readyReplicas")
		updatedReplicas := obj.String("status", "updatedReplicas")
		if replicas == readyReplicas && replicas == updatedReplicas {
			return "Active", "success", true, nil
		}
		if replicas != updatedReplicas {
			return "Updating", "processing", true, nil
		}
		return "Abnormal", "error", false, nil
	case "Job":
		if len(fields) != 7 {
			return "", "", false, fmt.Errorf("job %s has invalid fields length", obj.String("metadata", "name"))
		}
		active := obj.String("status", "active")
		failed := obj.String("status", "failed")
		if failed != "" && failed != "0" {
			return "Failed", "error", false, nil
		} else if active != "" && active != "0" {
			return "Active", "success", true, nil
		} else {
			return "Succeeded", "success", false, nil
		}
	case "CronJob":
		return "Active", "success", true, nil
	default:
		return "", "", false, fmt.Errorf("valid workload kind: %v", kind)
	}
}

// ParseWorkloadID get workloadKind, namespace and name from id
func ParseWorkloadID(id string) (apistructs.K8SResType, string, string, error) {
	splits := strings.Split(id, "_")
	if len(splits) != 3 {
		return "", "", "", fmt.Errorf("invalid workload id: %s", id)
	}
	return apistructs.K8SResType(splits[0]), splits[1], splits[2], nil
}

// GetWorkloadAgeAndImage get age and image for workloads from .metadata.fields
func GetWorkloadAgeAndImage(obj data.Object) (string, string, error) {
	kind := obj.String("kind")
	fields := obj.StringSlice("metadata", "fields")

	switch kind {
	case "Deployment":
		if len(fields) != 8 {
			return "", "", fmt.Errorf("deployment %s has invalid fields length", obj.String("metadata", "name"))
		}
		return fields[4], fields[6], nil
	case "DaemonSet":
		if len(fields) != 11 {
			return "", "", fmt.Errorf("daemonset %s has invalid fields length", obj.String("metadata", "name"))
		}
		return fields[7], fields[9], nil
	case "StatefulSet":
		if len(fields) != 5 {
			return "", "", fmt.Errorf("statefulSet %s has invalid fields length", obj.String("metadata", "name"))
		}
		return fields[2], fields[4], nil
	case "Job":
		if len(fields) != 7 {
			return "", "", fmt.Errorf("job %s has invalid fields length", obj.String("metadata", "name"))
		}
		return fields[3], fields[5], nil
	case "CronJob":
		if len(fields) != 9 {
			return "", "", fmt.Errorf("cronJob %s has invalid fields length", obj.String("metadata", "name"))
		}
		return fields[5], fields[7], nil
	default:
		return "", "", fmt.Errorf("invalid workload kind: %s", kind)
	}
}

// ResourceToString return resource with unit
// Only support resource.DecimalSI and resource.BinarySI format
// Original unit is m (for DecimalSI) or B (for resource.BinarySI)
// Accurate to 1 decimal places. Zero in suffix will be removed
func ResourceToString(sdk *cptype.SDK, res float64, format resource.Format) string {
	switch format {
	case resource.DecimalSI:
		return fmt.Sprintf("%s %s", strconv.FormatFloat(setPrec(res/1000, 1), 'f', -1, 64), sdk.I18n("Core"))
	case resource.BinarySI:
		units := []string{"B", "KiB", "MiB", "GiB", "TiB"}
		i := 0
		for res >= 1<<10 && i < len(units)-1 {
			res /= 1 << 10
			i++
		}
		return fmt.Sprintf("%s %s", strconv.FormatFloat(setPrec(res, 1), 'f', -1, 64), units[i])
	default:
		return fmt.Sprintf("%d", int64(res))
	}
}

func setPrec(f float64, prec int) float64 {
	pow := math.Pow10(prec)
	f = float64(int64(f*pow)) / pow
	return f
}

// CalculateNodeRes calculate unallocated cpu, memory and left cpu, mem, pods for given node and its allocated cpu, memory
func CalculateNodeRes(node data.Object, allocatedCPU, allocatedMem, allocatedPods int64) (unallocatedCPU, unallocatedMem, leftCPU, leftMem, leftPods int64) {
	allocatableCPUQty, _ := resource.ParseQuantity(node.String("status", "allocatable", "cpu"))
	allocatableMemQty, _ := resource.ParseQuantity(node.String("status", "allocatable", "memory"))
	allocatablePodQty, _ := resource.ParseQuantity(node.String("status", "allocatable", "pods"))
	capacityCPUQty, _ := resource.ParseQuantity(node.String("status", "capacity", "cpu"))
	capacityMemQty, _ := resource.ParseQuantity(node.String("status", "capacity", "memory"))

	unallocatedCPU = capacityCPUQty.MilliValue() - allocatableCPUQty.MilliValue()
	unallocatedMem = capacityMemQty.Value() - allocatableMemQty.Value()
	leftCPU = allocatableCPUQty.MilliValue() - allocatedCPU
	leftMem = allocatableMemQty.Value() - allocatedMem
	leftPods = allocatablePodQty.Value() - allocatedPods
	return
}

// IsJsonEqual return true if objA and objB is same after marshal by json.
// Used for unit testing.
func IsJsonEqual(objA, objB interface{}) (bool, error) {
	dataA, err := json.Marshal(objA)
	if err != nil {
		return false, err
	}

	dataB, err := json.Marshal(objB)
	if err != nil {
		return false, err
	}
	if string(dataA) == string(dataB) {
		return true, nil
	}

	fmt.Printf("objA:\n%s\n", string(dataA))
	fmt.Printf("objB:\n%s\n", string(dataB))
	return false, nil
}

// IsDeepEqual return true if objA and objB is deep equal.
// Used for unit testing.
func IsDeepEqual(objA, objB interface{}) (bool, error) {
	mA := cptype.ExtraMap{}
	mB := cptype.ExtraMap{}
	cputil.MustObjJSONTransfer(objA, &mA)
	cputil.MustObjJSONTransfer(objB, &mB)
	return reflect.DeepEqual(mA, mB), nil
}

// GetImpersonateClient authenticate user by steve server and return an impersonate k8s client
func GetImpersonateClient(steveServer cmp.SteveServer, userID, orgID, clusterName string) (*k8sclient.K8sClient, error) {
	user, err := steveServer.Auth(userID, orgID, clusterName)
	if err != nil {
		return nil, err
	}

	config, err := k8sclient.GetRestConfig(clusterName)
	if err != nil {
		return nil, errors.Errorf("failed to get rest config for cluster %s, %v", clusterName, err)
	}

	// impersonate user
	config.Impersonate.UserName = user.GetName()
	config.Impersonate.Groups = user.GetGroups()
	config.Impersonate.Extra = user.GetExtra()

	client, err := k8sclient.NewForRestConfig(config, k8sclient.WithSchemes(scheme.LocalSchemeBuilder...))
	if err != nil {
		return nil, errors.Errorf("failed to get k8s client, %v", err)
	}
	return client, nil
}

const (
	ProjectsDisplayNameCache = "projectDisplayName"
	NamespacesCache          = "allNamespaces"
)

func getAllProjectsDisplayName(bdl *bundle.Bundle, orgID string) (map[uint64]string, error) {
	scopeID, err := strconv.ParseUint(orgID, 10, 64)
	if err != nil {
		return nil, apierrors.ErrInvoke.InvalidParameter(fmt.Sprintf("invalid org id %s, %v", orgID, err))
	}
	projects, err := bdl.GetAllProjects()
	if err != nil {
		return nil, err
	}

	id2displayName := make(map[uint64]string)
	for _, project := range projects {
		if project.OrgID != scopeID {
			continue
		}
		id2displayName[project.ID] = project.DisplayName
	}
	return id2displayName, nil
}

// GetAllProjectsDisplayNameFromCache get all projects in org and return a project id to project display name map with cache
func GetAllProjectsDisplayNameFromCache(bdl *bundle.Bundle, orgID string) (map[uint64]string, error) {
	logrus.Infof("start get all projects display name")
	defer func() {
		logrus.Infof("end get all projects display name")
	}()
	cacheKey := cache.GenerateKey(orgID, ProjectsDisplayNameCache)
	values, expired, err := cache.GetFreeCache().Get(cacheKey)
	if err != nil {
		return nil, errors.Errorf("failed to get project displayName from cache, %v", err)
	}
	if values == nil {
		id2displayName, err := getAllProjectsDisplayName(bdl, orgID)
		if err != nil {
			return nil, err
		}
		values, err := cache.GetInterfaceValue(id2displayName)
		if err != nil {
			return nil, errors.Errorf("failed to marshal cache value for projects dispalyName, %v", err)
		}
		if err := cache.GetFreeCache().Set(cacheKey, values, time.Second.Nanoseconds()*30); err != nil {
			logrus.Errorf("failed to set cache for projects displayName, %v", err)
		}
		return id2displayName, nil
	}
	if expired {
		go func() {
			id2displayName, err := getAllProjectsDisplayName(bdl, orgID)
			if err != nil {
				logrus.Errorf("failed to get all projects displayName in goroutine, %v", err)
				return
			}
			values, err := cache.GetInterfaceValue(id2displayName)
			if err != nil {
				logrus.Errorf("failed to marshal cache value for projects displayName in goroutine, %v", err)
				return
			}
			if err := cache.GetFreeCache().Set(cacheKey, values, time.Second.Nanoseconds()*30); err != nil {
				logrus.Errorf("failed to set cache for projects displayName in goroutinue, %v", err)
				return
			}
		}()
	}
	id2displayName := values[0].Value().(map[uint64]string)
	return id2displayName, nil
}

func getAllNamespaces(ctx context.Context, steveServer cmp.SteveServer, userID, orgID, clusterName string) ([]string, error) {
	client, err := GetImpersonateClient(steveServer, userID, orgID, clusterName)
	if err != nil {
		return nil, err
	}

	var namespaces []string
	list, err := client.ClientSet.CoreV1().Namespaces().List(ctx, v1.ListOptions{})
	if err != nil {
		return nil, err
	}

	for _, namespace := range list.Items {
		namespaces = append(namespaces, namespace.Name)
	}
	return namespaces, nil
}

// GetAllNamespacesFromCache get all namespaces name list by k8s client with cache
func GetAllNamespacesFromCache(ctx context.Context, steveServer cmp.SteveServer, userID, orgID, clusterName string) ([]string, error) {
	logrus.Infof("start get all namespaces")
	defer func() {
		logrus.Infof("end get all namespaces")
	}()
	cacheKey := cache.GenerateKey(clusterName, NamespacesCache)
	values, expired, err := cache.GetFreeCache().Get(cacheKey)
	if err != nil {
		return nil, errors.Errorf("failed to get namespaces from cache, %v", err)
	}
	if values == nil {
		namespaces, err := getAllNamespaces(ctx, steveServer, userID, orgID, clusterName)
		if err != nil {
			return nil, err
		}
		comb := strings.Join(namespaces, ",")
		value, err := cache.GetStringValue(comb)
		if err != nil {
			return nil, errors.Errorf("failed to get cache string value, %v", err)
		}
		if err := cache.GetFreeCache().Set(cacheKey, value, time.Second.Nanoseconds()*30); err != nil {
			logrus.Errorf("failed to set cache for all namespaces, %v", err)
		}
		return namespaces, nil
	}
	if expired {
		go func() {
			ctx, cancel := context.WithTimeout(context.Background(), time.Second*15)
			defer cancel()
			namespaces, err := getAllNamespaces(ctx, steveServer, userID, orgID, clusterName)
			if err != nil {
				logrus.Errorf("failed to get all namespaces from cahce in goroutine, %v", err)
				return
			}
			comb := strings.Join(namespaces, ",")
			value, err := cache.GetStringValue(comb)
			if err != nil {
				logrus.Errorf("failed to get cache string value in goroutine, %v", err)
				return
			}
			if err := cache.GetFreeCache().Set(cacheKey, value, time.Second.Nanoseconds()*30); err != nil {
				logrus.Errorf("failed to set cache for all namespaces, %v", err)
			}
		}()
	}
	comb := values[0].String()
	namespaces := strings.Split(comb, ",")
	return namespaces, nil
}

// PodStatus is a map of pod status to normal status
var PodStatus = map[string]string{
	"Completed":         "success",
	"ContainerCreating": "processing",
	"CrashLoopBackOff":  "error",
	"Error":             "error",
	"Evicted":           "default",
	"ImagePullBackOff":  "error",
	"ErrImagePull":      "error",
	"Pending":           "processing",
	"Running":           "success",
	"Terminating":       "processing",
	"OOMKilled":         "error",
}

func ParsePodStatus(state string) (string, bool) {
	breathing := false
	if state == "Running" || state == "ContainerCreating" {
		breathing = true
	}
	status := PodStatus[state]
	if status == "" {
		status = "default"
	}
	return status, breathing
}
