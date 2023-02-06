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

//import (
//	"context"
//	"encoding/json"
//	"io/ioutil"
//	"testing"
//
//	"github.com/ghodss/yaml"
//	"github.com/sirupsen/logrus"
//	"github.com/stretchr/testify/assert"
//	appsv1 "k8s.io/api/apps/v1"
//	corev1 "k8s.io/api/core/v1"
//	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
//
//	"github.com/erda-project/erda/apistructs"
//	"github.com/erda-project/erda/pkg/clientgo/kubernetes"
//	"github.com/erda-project/erda/pkg/clientgo/restclient"
//)
//
//func getClient(inetAddr string) (*kubernetes.Clientset, error) {
//	restclient.SetInetAddr("netportal.default.svc.cluster.local:80")
//	client, err := kubernetes.NewKubernetesClientSet(inetAddr)
//	if err != nil {
//		logrus.Errorf(err.Error())
//		return nil, err
//	}
//	return client, nil
//}
//
//var (
//	NodeName = "node-010000007033"
//	InetAddr = "inet://ingress-nginx.kube-system.svc.cluster.local?direct=on&ssl=on/kubernetes.default.svc.cluster.local"
//)
//
//func TestNodeDrain(t *testing.T) {
//	addr := InetAddr
//	client, err := getClient(addr)
//	assert.NoError(t, err)
//	req := apistructs.DrainNodeRequest{
//		NodeName:            NodeName,
//		Force:               false,
//		IgnoreAllDaemonSets: false,
//		DeleteLocalData:     false,
//		Timeout:             20,
//	}
//	err = DrainNode(context.TODO(), client, req)
//	assert.Contains(t, err.Error(), "cannot delete DaemonSet-managed Pods (use --ignore-daemonsets to ignore)")
//}
//
//// Ignore daemon set pod, only have ds pod on node
//func TestNodeDrainDSIgnore(t *testing.T) {
//	addr := InetAddr
//	client, err := getClient(addr)
//	assert.NoError(t, err)
//	req := apistructs.DrainNodeRequest{
//		NodeName:            NodeName,
//		Force:               false,
//		IgnoreAllDaemonSets: true,
//		DeleteLocalData:     false,
//		Timeout:             20,
//	}
//	err = DrainNode(context.TODO(), client, req)
//	assert.NoError(t, err)
//}
//
//func TestDrainStatelessPod(t *testing.T) {
//
//	client, deploy, err := getClientAndDeployment("./example/stateless_deploy.yaml")
//	ctx := context.TODO()
//
//	// uncordon node
//	err = UncordonNode(ctx, client, NodeName)
//	assert.NoError(t, err)
//
//	// apply deployment
//	_, err = client.AppsV1().Deployments("default").Create(context.TODO(), deploy, metav1.CreateOptions{})
//	assert.NoError(t, err)
//
//	req := apistructs.DrainNodeRequest{
//		NodeName:            NodeName,
//		Force:               false,
//		IgnoreAllDaemonSets: true,
//		DeleteLocalData:     false,
//		Timeout:             20,
//	}
//
//	// drain node
//	err = DrainNode(context.TODO(), client, req)
//	assert.NoError(t, err)
//
//	// delete deployment
//	err = client.AppsV1().Deployments("default").Delete(ctx, deploy.Name, metav1.DeleteOptions{})
//	assert.NoError(t, err)
//}
//
//func TestDrainPodWithLocalData(t *testing.T) {
//	client, deploy, err := getClientAndDeployment("./example/localdir_deploy.yaml")
//	assert.NoError(t, err)
//	pod, err := getPodSpec("./example/orphan_pod.yaml")
//	assert.NoError(t, err)
//	ctx := context.TODO()
//
//	// uncordon node
//	err = UncordonNode(ctx, client, NodeName)
//	assert.NoError(t, err)
//
//	// apply deployment
//	_, err = client.AppsV1().Deployments("default").Create(ctx, deploy, metav1.CreateOptions{})
//	assert.NoError(t, err)
//	_, err = client.CoreV1().Pods("default").Create(ctx, pod, metav1.CreateOptions{})
//	assert.NoError(t, err)
//
//	req := apistructs.DrainNodeRequest{
//		NodeName:            NodeName,
//		Force:               true,
//		IgnoreAllDaemonSets: true,
//		DeleteLocalData:     true,
//		Timeout:             20,
//	}
//
//	// drain node
//	err = DrainNode(context.TODO(), client, req)
//	assert.NoError(t, err)
//
//	// delete deployment
//	err = client.AppsV1().Deployments("default").Delete(ctx, deploy.Name, metav1.DeleteOptions{})
//	assert.NoError(t, err)
//}
//
//func getClientAndDeployment(path string) (client *kubernetes.Clientset, deploy *appsv1.Deployment, err error) {
//	// get client
//	addr := InetAddr
//	client, err = getClient(addr)
//	if err != nil {
//		return
//	}
//
//	// get deployment yaml file
//	content, err := ioutil.ReadFile(path)
//	if err != nil {
//		return
//	}
//	jsonByte, err := yaml.YAMLToJSON(content)
//	if err != nil {
//		return
//	}
//	statelessDeploy := appsv1.Deployment{}
//	err = json.Unmarshal(jsonByte, &statelessDeploy)
//	statelessDeploy.Spec.Template.Spec.NodeSelector = map[string]string{"kubernetes.io/hostname": NodeName}
//	if err != nil {
//		return
//	}
//	deploy = &statelessDeploy
//	return
//}
//
//func getPodSpec(path string) (pod *corev1.Pod, err error) {
//	// get pod yaml file
//	content, err := ioutil.ReadFile(path)
//	if err != nil {
//		return
//	}
//	jsonByte, err := yaml.YAMLToJSON(content)
//	if err != nil {
//		return
//	}
//	pod = &corev1.Pod{}
//	err = json.Unmarshal(jsonByte, pod)
//	pod.Spec.NodeSelector = map[string]string{"kubernetes.io/hostname": NodeName}
//	if err != nil {
//		return
//	}
//	return
//}
