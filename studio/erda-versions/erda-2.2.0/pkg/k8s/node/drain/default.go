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

package drain

import (
	"context"
	"fmt"
	"os"
	"time"

	"github.com/pkg/errors"
	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	utilerrors "k8s.io/apimachinery/pkg/util/errors"
	"k8s.io/client-go/kubernetes"

	"github.com/erda-project/erda/apistructs"
)

// You should first cordon the node, e.g. using RunCordonOrUncordon
func RunNodeDrain(ctx context.Context, drainer *Helper, nodeName string) error {
	list, errs := drainer.GetPodsForDeletion(ctx, nodeName)
	if errs != nil {
		return utilerrors.NewAggregate(errs)
	}
	if warnings := list.Warnings(); warnings != "" {
		fmt.Fprintf(drainer.ErrOut, "WARNING: %s\n", warnings)
	}

	if err := drainer.DeleteOrEvictPods(ctx, list.Pods()); err != nil {
		// Maybe warn about non-deleted pods here
		return err
	}
	return nil
}

// RunCordonOrUncordon demonstrates the canonical way to cordon or uncordon a Node
func RunCordonOrUncordon(ctx context.Context, client kubernetes.Interface, node *corev1.Node, desired bool) error {
	c := NewCordonHelper(node)

	if updateRequired := c.UpdateIfRequired(desired); !updateRequired {
		// Already done
		return nil
	}

	err, patchErr := c.PatchOrReplace(ctx, client)
	if patchErr != nil {
		return patchErr
	}
	if err != nil {
		return err
	}

	return nil
}

func DrainNode(ctx context.Context, kubeClient kubernetes.Interface, req apistructs.DrainNodeRequest) error {
	node, err := kubeClient.CoreV1().Nodes().Get(ctx, req.NodeName, metav1.GetOptions{})
	if err != nil {
		if apierrors.IsNotFound(err) {
			// If an admin deletes the node directly, we'll end up here.
			fmt.Printf("Could not find node from noderef, it may have already been deleted")
			return nil
		}
		return errors.Errorf("unable to get node %q: %v", req.NodeName, err)
	}

	drainer := &Helper{
		Client:              kubeClient,
		Force:               req.Force,
		IgnoreAllDaemonSets: req.IgnoreAllDaemonSets,
		DeleteLocalData:     req.DeleteLocalData,
		GracePeriodSeconds:  -1,
		// If a pod is not evicted in 20 seconds, retry the eviction next time the
		// machine gets reconciled again (to allow other machines to be reconciled).
		Timeout: req.Timeout * time.Second,
		OnPodDeletedOrEvicted: func(pod *corev1.Pod, usingEviction bool) {
			verbStr := "Deleted"
			if usingEviction {
				verbStr = "Evicted"
			}
			fmt.Printf(fmt.Sprintf("%s pod from Node", verbStr),
				"pod", fmt.Sprintf("%s/%s", pod.Name, pod.Namespace))
		},
		Out:    os.Stdout,
		ErrOut: os.Stderr,
		DryRun: false,
	}

	if IsNodeUnreachable(node) {
		return errors.Errorf("not not ready, node: %v", node.Name)
	}

	if err := RunCordonOrUncordon(ctx, kubeClient, node, true); err != nil {
		return errors.Errorf("cordon node %s failed, error: %v", node.Name, err)
	}

	if err := RunNodeDrain(ctx, drainer, node.Name); err != nil {
		// Machine will be re-reconciled after a drain failure.
		return errors.Errorf("drain node %s failed, error: %v", node.Name, err)
	}

	fmt.Printf("Drain successful")
	return nil
}

// IsNodeUnreachable returns true if a node is unreachable.
// Node is considered unreachable when its ready status is "Unknown".
func IsNodeUnreachable(node *corev1.Node) bool {
	if node == nil {
		return false
	}
	for _, c := range node.Status.Conditions {
		if c.Type == corev1.NodeReady {
			return c.Status == corev1.ConditionUnknown
		}
	}
	return false
}

func UncordonNode(ctx context.Context, client kubernetes.Interface, nodeName string) error {
	node, err := client.CoreV1().Nodes().Get(ctx, nodeName, metav1.GetOptions{})
	if err != nil {
		return err
	}
	err = RunCordonOrUncordon(ctx, client, node, false)
	if err != nil {
		return err
	}
	return nil
}
