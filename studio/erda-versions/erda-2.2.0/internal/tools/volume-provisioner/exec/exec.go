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

package exec

import (
	"bytes"
	"context"
	"fmt"
	"os/exec"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	restclient "k8s.io/client-go/rest"
	"k8s.io/client-go/tools/remotecommand"
)

type CmdExecutor struct {
	config    *restclient.Config
	client    kubernetes.Interface
	namespace string
}

func NewCmdExecutor(config *restclient.Config, client kubernetes.Interface, namespace string) *CmdExecutor {
	return &CmdExecutor{config, client, namespace}
}

// OnLocal execute 'cmd' on the specified node
func (c *CmdExecutor) OnLocal(cmd string) error {
	cc := exec.Command("/bin/sh", "-c", cmd)
	if _, err := cc.Output(); err != nil {
		return err
	}

	return nil
}

// OnPods execute 'cmd' on the specified pods
func (c *CmdExecutor) OnPods(cmd string, listOption metav1.ListOptions) error {
	pods, err := c.client.CoreV1().Pods(c.namespace).List(context.Background(), listOption)
	if err != nil {
		return err
	}
	for i := range pods.Items {
		if err := c.OnPod(cmd, &pods.Items[i]); err != nil {
			return err
		}
	}
	return nil
}

// OnNodesPods execute 'cmd' on the specified pods of the specified nodes
func (c *CmdExecutor) OnNodesPods(cmd string, nodeListOption, podListOption metav1.ListOptions) error {
	nodes, err := c.client.CoreV1().Nodes().List(context.Background(), nodeListOption)
	if err != nil {
		return err
	}
	for _, node := range nodes.Items {
		podOpt := podListOption
		podOpt.FieldSelector = fmt.Sprintf("spec.nodeName=%s", node.Name)
		if err := c.OnPods(cmd, podOpt); err != nil {
			return err
		}
	}
	return err
}

// OnPod execute 'cmd' on 'pod'
func (c *CmdExecutor) OnPod(cmd string, pod *v1.Pod) error {
	logrus.Infof("namespace: %s, pod: %s, cmd: %v", pod.Namespace, pod.Name, cmd)
	req := c.client.CoreV1().RESTClient().Post().
		Resource("pods").
		Name(pod.Name).
		Namespace(pod.Namespace).
		SubResource("exec").
		Param("container", pod.Spec.Containers[0].Name).
		Param("stdout", "true").
		Param("stderr", "true")
	for _, c := range []string{"/bin/sh", "-c", cmd} {
		req.Param("command", c)
	}
	exec, err := remotecommand.NewSPDYExecutor(c.config, "POST", req.URL())
	if err != nil {
		logrus.Errorf("Failed to create NewSPDYExecutor: %v", err)
		return err
	}
	var b bytes.Buffer
	var bErr bytes.Buffer
	err = exec.Stream(remotecommand.StreamOptions{
		Stdout: &b,
		Stderr: &bErr,
		Tty:    false,
	})
	logrus.Infof("Result stdout: %s", b.String())
	logrus.Infof("Result stderr: %s", bErr.String())
	if err != nil {
		logrus.Errorf("Failed to create Stream: %v", err)
		return err
	}
	return nil
}
