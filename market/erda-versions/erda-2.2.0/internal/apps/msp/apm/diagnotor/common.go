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

package diagnotor

import (
	"context"
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/common/errors"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

func (p *provider) getProjectByTerminusKey(tk, clusterName string) (string, string, string, error) {
	info, err := p.monitor.GetByTerminusKey(tk)
	if err != nil {
		return "", "", "", err
	}
	if info == nil {
		return "", "", "", fmt.Errorf("not found terminusKey(%q)", tk)
	}
	if len(clusterName) > 0 {
		info.ClusterName = clusterName
	}
	return info.ProjectId, strings.ToLower(info.Workspace), info.ClusterName, nil
}

func (p *provider) getScopeID(ctx context.Context, req interface{}) (string, error) {
	type reuqestForPermission interface {
		GetTerminusKey() string
	}
	r, ok := req.(reuqestForPermission)
	if !ok {
		return "", fmt.Errorf("invalid reuqest")
	}
	projectID, workspace, clusterName, err := p.getProjectByTerminusKey(r.GetTerminusKey(), "")
	if err != nil {
		return "", err
	}
	perm.SetPermissionDataFromContext(ctx, scopeKey, &scopeInfo{
		TerminusKey: r.GetTerminusKey(),
		ClusterName: clusterName,
		ProjectID:   projectID,
		Workspace:   workspace,
	})

	return projectID, nil
}

type scopeInfo struct {
	TerminusKey string
	ClusterName string
	ProjectID   string
	Workspace   string
}

const scopeKey = "scope"

func (p *provider) checkScopeID(ctx context.Context, req interface{}) (string, error) {
	r, ok := req.(reuqestForPermission)
	if !ok {
		return "", fmt.Errorf("invalid reuqest")
	}

	if len(r.GetTerminusKey()) <= 0 {
		return "", fmt.Errorf("terminusKey is required")
	}
	if len(r.GetInstanceIP()) <= 0 {
		return "", fmt.Errorf("instanceIP is required")
	}
	projectID, workspace, clusterName, err := p.getProjectByTerminusKey(r.GetTerminusKey(), r.GetClusterName())
	if err != nil {
		return "", err
	}
	resp, err := p.bdl.GetInstanceInfo(apistructs.InstanceInfoRequest{
		Cluster:    clusterName,
		ProjectID:  projectID,
		Workspace:  workspace,
		InstanceIP: r.GetInstanceIP(),
		Limit:      1,
	})
	if err != nil {
		return "", err
	}
	if len(resp.Data) <= 0 {
		return "", fmt.Errorf("not found instance for {project=%s, workspace=%s}", projectID, workspace)
	}

	instance := resp.Data[0]
	metadata := parseInstanceMetadata(instance.Meta)
	perm.SetPermissionDataFromContext(ctx, instanceKey, &instanceInfo{
		ClusterName:   clusterName,
		HostIP:        instance.HostIP,
		PodName:       metadata["k8spodname"],
		PodIP:         r.GetInstanceIP(),
		Namespace:     metadata["k8snamespace"],
		ContainerID:   instance.ContainerID,
		ContainerName: metadata["k8scontainername"],
		Metadata:      metadata,
	})

	return projectID, nil
}

type reuqestForPermission interface {
	GetTerminusKey() string
	GetClusterName() string
	GetInstanceIP() string
}

type instanceInfo struct {
	ClusterName   string
	HostIP        string
	Namespace     string
	PodName       string
	PodIP         string
	ContainerID   string
	ContainerName string
	Metadata      map[string]string
}

func parseInstanceMetadata(text string) map[string]string {
	kvs := make(map[string]string)
	for _, line := range strings.SplitN(text, ",", -1) {
		kv := strings.SplitN(line, "=", 2)
		if len(kv) != 2 {
			continue
		}
		key := strings.TrimSpace(kv[0])
		val := strings.TrimSpace(kv[1])
		if len(key) > 0 && len(val) > 0 {
			kvs[key] = val
		}
	}
	return kvs
}

const instanceKey = "instance"

func unwrapRpcError(err error) error {
	return errors.FromGrpcError(err)
}
