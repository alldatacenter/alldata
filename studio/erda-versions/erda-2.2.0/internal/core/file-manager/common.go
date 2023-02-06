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

package file_manager

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"strings"

	v1 "k8s.io/api/core/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/remotecommand"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/common/errors"
	perm "github.com/erda-project/erda/pkg/common/permission"
	remotecommandexec "github.com/erda-project/erda/pkg/k8s/remotecommand"
)

const instanceKey = "instance"

func (s *fileManagerService) getInstanceInfo(ctx context.Context, containerID, hostIP string) (*instanceInfo, error) {
	var instance *apistructs.InstanceInfoData
	pdata, ok := perm.GetPermissionDataFromContext(ctx, instanceKey)
	if ok && pdata != nil {
		instance = pdata.(*apistructs.InstanceInfoData)
	} else {
		resp, err := s.p.bdl.GetInstanceInfo(apistructs.InstanceInfoRequest{
			ContainerID: containerID,
			HostIP:      hostIP,
			Limit:       1,
		})
		if err != nil {
			return nil, errors.NewServiceInvokingError("failed to GetInstanceInfo: %s", err)
		}
		if !resp.Success {
			return nil, errors.NewInternalServerError(fmt.Errorf("failed to GetInstanceInfo: code=%s, msg=%s", resp.Error.Code, resp.Error.Msg))
		}
		if len(resp.Data) <= 0 {
			return nil, errors.NewNotFoundError(fmt.Sprintf("instance %s/%s", hostIP, containerID))
		}
		instance = &resp.Data[0]
	}
	metadata := parseInstanceMetadata(instance.Meta)
	info := &instanceInfo{
		ClusterName:   instance.Cluster,
		HostIP:        instance.HostIP,
		PodName:       metadata["k8spodname"],
		Namespace:     metadata["k8snamespace"],
		ContainerID:   instance.ContainerID,
		ContainerName: metadata["k8scontainername"],
		Metadata:      metadata,
	}
	return info, nil
}

type instanceInfo struct {
	ClusterName   string
	HostIP        string
	Namespace     string
	PodName       string
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

func (s *fileManagerService) execInPod(ctx context.Context, clluster, namespace, podName, container string, command []string, stdin io.Reader, stdout io.Writer) error {
	if len(namespace) <= 0 || len(podName) <= 0 {
		return errors.NewNotFoundError(fmt.Sprintf("pods %s/%s", namespace, podName))
	}
	client, cfg, err := s.p.Clients.GetClient(clluster)
	if err != nil {
		return errors.NewInternalServerError(fmt.Errorf("failed to GetClient(%q)", clluster))
	}
	req := client.CoreV1().RESTClient().Post().
		Resource("pods").
		Name(podName).
		Namespace(namespace).
		SubResource("exec").
		VersionedParams(&v1.PodExecOptions{
			Container: container,
			Command:   command,
			Stdin:     true,
			Stdout:    true,
			Stderr:    true,
			TTY:       false,
		}, scheme.ParameterCodec)
	executor, err := remotecommandexec.NewSPDYExecutor(cfg, http.MethodPost, req.URL())
	if err != nil {
		return errors.NewInternalServerError(fmt.Errorf("failed to NewSPDYExecutor: %s", err))
	}
	stderr := &strings.Builder{}
	err = executor.Stream(remotecommand.StreamOptions{
		Stdin:  stdin,
		Stdout: stdout,
		Stderr: stderr,
	})
	if err != nil {
		errmsg := stderr.String()
		if strings.Contains(errmsg, "No such file or directory") {
			return errors.NewNotFoundError("file or directory")
		}
		return errors.NewInternalServerError(fmt.Errorf("failed to exec: %s, %s", err, errmsg))
	}
	return nil
}

// ReaderFunc .
type ReaderFunc func(p []byte) (n int, err error)

// Read .
func (fn ReaderFunc) Read(p []byte) (n int, err error) {
	return fn(p)
}

// WriterFunc .
type WriterFunc func(p []byte) (n int, err error)

// Write .
func (fn WriterFunc) Write(p []byte) (n int, err error) {
	return fn(p)
}

func noExpandPath(path string) string {
	path = strings.ReplaceAll(path, `\`, `\\`)
	path = strings.ReplaceAll(path, `$`, `\$`)
	return path
}
