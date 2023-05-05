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

package middleware

import (
	"context"
	"fmt"
	"net/http"
	"net/url"

	"github.com/sirupsen/logrus"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apiserver/pkg/endpoints/request"
	corev1client "k8s.io/client-go/kubernetes/typed/core/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/steve/predefined"
	"github.com/erda-project/erda/pkg/k8sclient"
)

type ShellHandler struct {
	ctx context.Context
}

// NewShellHandler create a new ShellHandler
func NewShellHandler(ctx context.Context) *ShellHandler {
	return &ShellHandler{ctx: ctx}
}

// HandleShell forwards the request to cluster-agent pod
func (s *ShellHandler) HandleShell(next http.Handler) http.Handler {
	return http.HandlerFunc(func(resp http.ResponseWriter, req *http.Request) {
		vars, _ := req.Context().Value(varsKey).(map[string]string)
		if vars["kubectl-shell"] == "" {
			next.ServeHTTP(resp, req)
			return
		}
		user, ok := request.UserFrom(req.Context())
		if !ok || user.GetName() == "system:unauthenticated" {
			resp.WriteHeader(http.StatusForbidden)
			resp.Write(apistructs.NewSteveError(apistructs.PermissionDenied, "access denied").JSON())
			return
		}

		clusterName := vars["clusterName"]
		client, err := k8sclient.New(clusterName)
		if err != nil {
			logrus.Errorf("failed to get k8s client for cluster %s in steve handle shell, %v", clusterName, err)
			resp.WriteHeader(http.StatusInternalServerError)
			resp.Write(apistructs.NewSteveError(apistructs.ServerError, "server error").JSON())
			return
		}

		group := user.GetGroups()[0]
		userGroup, ok := predefined.UserGroups[group]
		if !ok {
			resp.WriteHeader(http.StatusForbidden)
			resp.Write(apistructs.NewSteveError(apistructs.PermissionDenied, "access denied").JSON())
			return
		}

		serviceAccount, err := client.ClientSet.CoreV1().ServiceAccounts(userGroup.ServiceAccountNamespace).
			Get(s.ctx, userGroup.ServiceAccountName, metav1.GetOptions{})
		if err != nil {
			logrus.Errorf("failed to get serviceAccount %s in steve handle shell, %v", userGroup.ServiceAccountName, err)
			resp.WriteHeader(http.StatusInternalServerError)
			resp.Write(apistructs.NewSteveError(apistructs.ServerError, "interval server error").JSON())
			return
		}
		if len(serviceAccount.Secrets) == 0 {
			logrus.Errorf("serviceAccount %s does not have a secret", userGroup.ServiceAccountName)
			resp.WriteHeader(http.StatusInternalServerError)
			resp.Write(apistructs.NewSteveError(apistructs.ServerError, "interval server error").JSON())
			return
		}
		secretName := serviceAccount.Secrets[0].Name
		secret, err := client.ClientSet.CoreV1().Secrets(userGroup.ServiceAccountNamespace).Get(s.ctx, secretName, metav1.GetOptions{})
		if err != nil {
			logrus.Errorf("failed to get secret %s in steve handle shell", secretName)
			resp.WriteHeader(http.StatusInternalServerError)
			resp.Write(apistructs.NewSteveError(apistructs.ServerError, "interval server error").JSON())
			return
		}
		token := string(secret.Data["token"])

		podClient := client.ClientSet.CoreV1().Pods("")

		for _, pod := range s.getAgentPods(podClient) {
			if !isPodReady(&pod) {
				continue
			}
			vars := url.Values{}
			vars.Add("container", "cluster-agent")
			vars.Add("stdout", "1")
			vars.Add("stdout", "1")
			vars.Add("stdin", "1")
			vars.Add("stderr", "1")
			vars.Add("tty", "1")
			vars.Add("command", "kubectl-shell.sh")
			vars.Add("command", token)

			path := fmt.Sprintf("/api/k8s/clusters/%s/api/v1/namespaces/%s/pods/%s/exec", clusterName, pod.Namespace, pod.Name)

			req.URL.Path = path
			req.URL.RawQuery = vars.Encode()
			next.ServeHTTP(resp, req)
			return
		}

		logrus.Errorf("failed to find a ready cluster-agent pod for cluster %s", clusterName)
		resp.WriteHeader(http.StatusInternalServerError)
		resp.Write(apistructs.NewSteveError(apistructs.ServerError,
			fmt.Sprintf("cluster %s does not have a ready agent pod", clusterName)).JSON())
	})
}

func isPodReady(pod *v1.Pod) bool {
	return pod.Status.Phase == v1.PodRunning
}

func (s *ShellHandler) getAgentPods(podClient corev1client.PodInterface) []v1.Pod {
	var res []v1.Pod
	pods, err := podClient.List(s.ctx, metav1.ListOptions{
		LabelSelector: "app=cluster-agent",
	})
	if err != nil {
		logrus.Errorf("failed to list cluster-agent pod in steve handle shell by app=cluster-agent label, %v", err)
		//resp.WriteHeader(http.StatusInternalServerError)
		//resp.Write(apistructs.NewSteveError(apistructs.ServerError, "interval server error").JSON())
		//return
	}

	res = append(res, pods.Items...)
	pods, err = podClient.List(s.ctx, metav1.ListOptions{
		LabelSelector: "dice/component=cluster-agent",
	})
	if err != nil {
		logrus.Errorf("failed to list cluster-agent pod in steve handle shell by dice/component=cluster-agent label, %v", err)
	}
	res = append(res, pods.Items...)
	return res
}
