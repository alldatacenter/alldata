/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package elasticquota

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"net"
	"net/http"
	"net/http/httptest"
	"net/url"
	"sort"
	"strings"
	"sync"
	"testing"
	"time"

	gocmp "github.com/google/go-cmp/cmp"
	nrtinformers "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/informers/externalversions"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	apiruntime "k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	v1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	"k8s.io/client-go/rest"
	"k8s.io/klog/v2"
	extenderv1 "k8s.io/kube-scheduler/extender/v1"
	_ "k8s.io/kubernetes/pkg/api/v1/resource"
	schedulerconfig "k8s.io/kubernetes/pkg/scheduler/apis/config"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultpreemption"
	plfeature "k8s.io/kubernetes/pkg/scheduler/framework/plugins/feature"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/noderesources"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	"k8s.io/kubernetes/pkg/scheduler/framework/runtime"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"
	imageutils "k8s.io/kubernetes/test/utils/image"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
	pgclientset "sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned"
	pgfake "sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned/fake"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/v1beta2"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/elasticquota/core"
)

type ElasticQuotaSetAndHandle struct {
	framework.Handle
	pgclientset.Interface
}

func ElasticQuotaPluginFactoryProxy(clientSet pgclientset.Interface, factoryFn runtime.PluginFactory) runtime.PluginFactory {
	return func(args apiruntime.Object, handle framework.Handle) (framework.Plugin, error) {
		return factoryFn(args, ElasticQuotaSetAndHandle{Handle: handle, Interface: clientSet})
	}
}

func ElasticQuotaPluginFactoryProxyWithPlugin(clientSet pgclientset.Interface, factoryFn runtime.PluginFactory, plugin *framework.Plugin) runtime.PluginFactory {
	return func(args apiruntime.Object, handle framework.Handle) (framework.Plugin, error) {
		var err error
		*plugin, err = factoryFn(args, ElasticQuotaSetAndHandle{Handle: handle, Interface: clientSet})
		return *plugin, err
	}
}

func mockPodsList(w http.ResponseWriter, r *http.Request) {
	bear := r.Header.Get("Authorization")
	if bear == "" {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	parts := strings.Split(bear, "Bearer")
	if len(parts) != 2 {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}

	http_token := strings.TrimSpace(parts[1])
	if len(http_token) < 1 {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	if http_token != token {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	podList := new(corev1.PodList)
	b, err := json.Marshal(podList)
	if err != nil {
		log.Printf("codec error %+v", err.Error())
		w.WriteHeader(http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusOK)
	w.Header().Set("Content-Type", "application/json")
	w.Write(b)
}

func parseHostAndPort(rawURL string) (string, string, error) {
	u, err := url.Parse(rawURL)
	if err != nil {
		return "", "0", err
	}
	return net.SplitHostPort(u.Host)
}

var (
	token string
)

func newPluginTestSuit(t *testing.T, nodes []*corev1.Node) *pluginTestSuit {
	setLoglevel("5")
	var v1beta2args v1beta2.ElasticQuotaArgs
	v1beta2.SetDefaults_ElasticQuotaArgs(&v1beta2args)
	var elasticQuotaArgs config.ElasticQuotaArgs
	err := v1beta2.Convert_v1beta2_ElasticQuotaArgs_To_config_ElasticQuotaArgs(&v1beta2args, &elasticQuotaArgs, nil)
	assert.NoError(t, err)

	elasticQuotaPluginConfig := schedulerconfig.PluginConfig{
		Name: Name,
		Args: &elasticQuotaArgs,
	}

	koordClientSet := fake.NewSimpleClientset()
	koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)

	pgClientSet := pgfake.NewSimpleClientset()
	proxyNew := ElasticQuotaPluginFactoryProxy(pgClientSet, New)

	registeredPlugins := []schedulertesting.RegisterPluginFunc{
		func(reg *runtime.Registry, profile *schedulerconfig.KubeSchedulerProfile) {
			profile.PluginConfig = []schedulerconfig.PluginConfig{
				elasticQuotaPluginConfig,
			}
		},
		schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
		schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
		schedulertesting.RegisterPreFilterPlugin(Name, proxyNew),
	}

	cs := kubefake.NewSimpleClientset()
	informerFactory := informers.NewSharedInformerFactory(cs, 0)
	snapshot := newTestSharedLister(nil, nodes)

	server := httptest.NewTLSServer(http.HandlerFunc(mockPodsList))
	defer server.Close()

	address, portStr, err := parseHostAndPort(server.URL)
	if err != nil {
		t.Fatal(err)
	}
	cfg := &rest.Config{
		Host:        net.JoinHostPort(address, portStr),
		BearerToken: token,
		TLSClientConfig: rest.TLSClientConfig{
			Insecure: true,
		},
	}
	if token == "" {
		flag.StringVar(&token, "token", "mockTest", "")
		flag.Parse()
	}
	fh, err := schedulertesting.NewFramework(
		registeredPlugins,
		"koord-scheduler",
		runtime.WithClientSet(cs),
		runtime.WithInformerFactory(informerFactory),
		runtime.WithSnapshotSharedLister(snapshot),
		runtime.WithKubeConfig(cfg),
	)
	assert.Nil(t, err)
	return &pluginTestSuit{
		Handle:                           fh,
		koordinatorSharedInformerFactory: koordSharedInformerFactory,
		proxyNew:                         proxyNew,
		elasticQuotaArgs:                 &elasticQuotaArgs,
		client:                           pgClientSet,
	}
}

func newPluginTestSuitWithPod(t *testing.T, nodes []*corev1.Node, pods []*corev1.Pod) *pluginTestSuit {
	setLoglevel("5")
	var v1beta2args v1beta2.ElasticQuotaArgs
	v1beta2.SetDefaults_ElasticQuotaArgs(&v1beta2args)
	var elasticQuotaArgs config.ElasticQuotaArgs
	err := v1beta2.Convert_v1beta2_ElasticQuotaArgs_To_config_ElasticQuotaArgs(&v1beta2args, &elasticQuotaArgs, nil)
	assert.NoError(t, err)

	elasticQuotaPluginConfig := schedulerconfig.PluginConfig{
		Name: Name,
		Args: &elasticQuotaArgs,
	}

	koordClientSet := fake.NewSimpleClientset()
	koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)

	pgClientSet := pgfake.NewSimpleClientset()
	var plugin framework.Plugin
	proxyNew := ElasticQuotaPluginFactoryProxyWithPlugin(pgClientSet, New, &plugin)

	registeredPlugins := []schedulertesting.RegisterPluginFunc{
		func(reg *runtime.Registry, profile *schedulerconfig.KubeSchedulerProfile) {
			profile.PluginConfig = []schedulerconfig.PluginConfig{
				elasticQuotaPluginConfig,
			}
		},
		schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
		schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
		schedulertesting.RegisterPluginAsExtensions(noderesources.FitName, func(plArgs apiruntime.Object, fh framework.Handle) (framework.Plugin, error) {
			return noderesources.NewFit(plArgs, fh, plfeature.Features{})
		}, "Filter", "PreFilter"),
		schedulertesting.RegisterPreFilterPlugin(Name, proxyNew),
	}

	cs := kubefake.NewSimpleClientset()
	informerFactory := informers.NewSharedInformerFactory(cs, 0)
	snapshot := newTestSharedLister(pods, nodes)

	server := httptest.NewTLSServer(http.HandlerFunc(mockPodsList))
	defer server.Close()

	address, portStr, err := parseHostAndPort(server.URL)
	if err != nil {
		t.Fatal(err)
	}
	cfg := &rest.Config{
		Host:        net.JoinHostPort(address, portStr),
		BearerToken: token,
		TLSClientConfig: rest.TLSClientConfig{
			Insecure: true,
		},
	}
	if token == "" {
		flag.StringVar(&token, "token", "mockTest", "")
		flag.Parse()
	}
	fh, err := schedulertesting.NewFramework(
		registeredPlugins,
		"koord-scheduler",
		runtime.WithClientSet(cs),
		runtime.WithInformerFactory(informerFactory),
		runtime.WithSnapshotSharedLister(snapshot),
		runtime.WithKubeConfig(cfg),
		runtime.WithPodNominator(NewPodNominator()),
	)
	assert.Nil(t, err)
	return &pluginTestSuit{
		Handle:                           fh,
		koordinatorSharedInformerFactory: koordSharedInformerFactory,
		proxyNew:                         proxyNew,
		elasticQuotaArgs:                 &elasticQuotaArgs,
		client:                           pgClientSet,
		Framework:                        fh,
		plugin:                           plugin,
	}
}

var _ framework.SharedLister = &testSharedLister{}

type testSharedLister struct {
	nodes       []*corev1.Node
	nodeInfos   []*framework.NodeInfo
	nodeInfoMap map[string]*framework.NodeInfo
}

func (f *testSharedLister) NodeInfos() framework.NodeInfoLister {
	return f
}

func (f *testSharedLister) List() ([]*framework.NodeInfo, error) {
	return f.nodeInfos, nil
}

func (f *testSharedLister) HavePodsWithAffinityList() ([]*framework.NodeInfo, error) {
	return nil, nil
}

func (f *testSharedLister) HavePodsWithRequiredAntiAffinityList() ([]*framework.NodeInfo, error) {
	return nil, nil
}

func (f *testSharedLister) Get(nodeName string) (*framework.NodeInfo, error) {
	return f.nodeInfoMap[nodeName], nil
}

func newTestSharedLister(pods []*corev1.Pod, nodes []*corev1.Node) *testSharedLister {
	nodeInfoMap := make(map[string]*framework.NodeInfo)
	nodeInfos := make([]*framework.NodeInfo, 0)
	for _, pod := range pods {
		nodeName := pod.Spec.NodeName
		if _, ok := nodeInfoMap[nodeName]; !ok {
			nodeInfoMap[nodeName] = framework.NewNodeInfo()
		}
		nodeInfoMap[nodeName].AddPod(pod)
	}
	for _, node := range nodes {
		if _, ok := nodeInfoMap[node.Name]; !ok {
			nodeInfoMap[node.Name] = framework.NewNodeInfo()
		}
		nodeInfoMap[node.Name].SetNode(node)
	}

	for _, v := range nodeInfoMap {
		nodeInfos = append(nodeInfos, v)
	}

	return &testSharedLister{
		nodes:       nodes,
		nodeInfos:   nodeInfos,
		nodeInfoMap: nodeInfoMap,
	}
}

type pluginTestSuit struct {
	framework.Handle
	framework.Framework
	koordinatorSharedInformerFactory koordinatorinformers.SharedInformerFactory
	nrtSharedInformerFactory         nrtinformers.SharedInformerFactory
	proxyNew                         runtime.PluginFactory
	elasticQuotaArgs                 *config.ElasticQuotaArgs
	client                           *pgfake.Clientset
	plugin                           framework.Plugin
}

func TestNew(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	assert.NotNil(t, p)
	assert.Nil(t, err)
	assert.Equal(t, Name, p.Name())
}

func TestPlugin_OnNodeAdd(t *testing.T) {
	tests := []struct {
		name     string
		nodes    []*corev1.Node
		totalRes corev1.ResourceList
	}{
		{
			name:     "add invalid node",
			nodes:    []*corev1.Node{},
			totalRes: corev1.ResourceList{},
		},
		{
			name: "add invalid node 2",
			nodes: []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						DeletionTimestamp: &metav1.Time{Time: time.Now()},
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(100, 1000),
					},
				},
			},
			totalRes: corev1.ResourceList{},
		},
		{
			name: "add normal node",
			nodes: []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "node1",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(100, 1000),
					},
				},
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "node2",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(100, 1000),
					},
				},
			},
			totalRes: createResourceList(200, 2000),
		},
		{
			name: "add same node twice",
			nodes: []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "node1",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(100, 1000),
					},
				},
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "node1",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(100, 1000),
					},
				},
			},
			totalRes: createResourceList(100, 1000),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, err := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
			assert.NotNil(t, p)
			assert.Nil(t, err)
			eQP := p.(*Plugin)
			for _, node := range tt.nodes {
				eQP.OnNodeAdd(node)
			}
			gqm := eQP.groupQuotaManager
			assert.NotNil(t, gqm)
			assert.Equal(t, tt.totalRes, gqm.GetClusterTotalResource())
		})
	}
}

func TestPlugin_OnNodeDelete(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	assert.NotNil(t, p)
	assert.Nil(t, err)
	eQP := p.(*Plugin)
	gqp := eQP.groupQuotaManager
	gqp.UpdateClusterTotalResource(createResourceList(400, 4000))
	assert.NotNil(t, gqp)
	nodes := []*corev1.Node{defaultCreateNode("1"), defaultCreateNode("2"), defaultCreateNode("3")}
	for _, node := range nodes {
		eQP.OnNodeAdd(node)
	}
	for i, node := range nodes {
		eQP.OnNodeDelete(node)
		assert.Equal(t, gqp.GetClusterTotalResource(), createResourceList(600-int64(i)*100, 6000-int64(i)*1000))
	}
}

func TestPlugin_OnNodeUpdate(t *testing.T) {
	nodes := []*corev1.Node{defaultCreateNodeWithResourceVersion("1"), defaultCreateNodeWithResourceVersion("2"),
		defaultCreateNodeWithResourceVersion("3")}
	tests := []struct {
		name     string
		nodes    []*corev1.Node
		totalRes corev1.ResourceList
	}{
		{
			name:     "update invalid node",
			nodes:    []*corev1.Node{},
			totalRes: createResourceList(300, 3000),
		},
		{
			name: "increase node resource",
			nodes: []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "1",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(200, 2000),
					},
				},
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "2",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(200, 2000),
					},
				},
			},
			totalRes: createResourceList(500, 5000),
		},
		{
			name: "decrease node resource",
			nodes: []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "1",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(50, 500),
					},
				},
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "2",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(50, 500),
					},
				},
			},
			totalRes: createResourceList(200, 2000),
		},
		{
			name: "node not exist",
			nodes: []*corev1.Node{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "4",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(50, 500),
					},
				},
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "5",
					},
					Status: corev1.NodeStatus{
						Allocatable: createResourceList(50, 500),
					},
				},
			},
			totalRes: createResourceList(300, 3000),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
			plugin := p.(*Plugin)
			for _, node := range nodes {
				plugin.OnNodeAdd(node)
			}
			for i, node := range tt.nodes {
				plugin.OnNodeUpdate(nodes[i], node)
			}
			assert.Equal(t, p.(*Plugin).groupQuotaManager.GetClusterTotalResource(), tt.totalRes)
		})
	}
}

func defaultCreateNodeWithResourceVersion(nodeName string) *corev1.Node {
	node := defaultCreateNode(nodeName)
	node.ResourceVersion = "3"
	return node
}

func defaultCreateNode(nodeName string) *corev1.Node {
	return &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: nodeName,
		},
		Status: corev1.NodeStatus{
			Allocatable: createResourceList(100, 1000),
		},
	}
}

func createResourceList(cpu, mem int64) corev1.ResourceList {
	return corev1.ResourceList{
		// use NewMilliQuantity to calculate the runtimeQuota correctly in cpu dimension
		// when the request is smaller than 1 core.
		corev1.ResourceCPU:    *resource.NewMilliQuantity(cpu*1000, resource.DecimalSI),
		corev1.ResourceMemory: *resource.NewQuantity(mem, resource.BinarySI),
	}
}

func TestPlugin_OnQuotaAdd(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	pl := p.(*Plugin)
	pl.groupQuotaManager.UpdateClusterTotalResource(createResourceList(501952056, 0))
	gqm := pl.groupQuotaManager
	quota := suit.AddQuota("1", "", 0, 0, 0, 0, 0, 0, false, "")
	assert.NotNil(t, gqm.GetQuotaInfoByName("1"))
	quota.DeletionTimestamp = &metav1.Time{Time: time.Now()}
	quota.Name = "2"
	pl.OnQuotaAdd(quota)
	assert.Nil(t, gqm.GetQuotaInfoByName("2"))
}

func (p *pluginTestSuit) AddQuota(name string, parentName string, maxCpu, maxMem int64,
	minCpu, minMem int64, scaleCpu, scaleMem int64, isParGroup bool, namespace string) *v1alpha1.ElasticQuota {
	quota := CreateQuota2(name, parentName, maxCpu, maxMem, minCpu, minMem, scaleCpu, scaleMem, isParGroup)
	p.client.SchedulingV1alpha1().ElasticQuotas(namespace).Create(context.TODO(), quota, metav1.CreateOptions{})
	time.Sleep(100 * time.Millisecond)
	return quota
}

func (g *Plugin) addQuota(name string, parentName string, maxCpu, maxMem int64,
	minCpu, minMem int64, scaleCpu, scaleMem int64, isParGroup bool, namespace string) *v1alpha1.ElasticQuota {
	quota := CreateQuota2(name, parentName, maxCpu, maxMem, minCpu, minMem, scaleCpu, scaleMem, isParGroup)
	g.OnQuotaAdd(quota)
	return quota
}

func CreateQuota2(name string, parentName string, maxCpu, maxMem int64, minCpu, minMem int64,
	scaleCpu, scaleMem int64, isParGroup bool) *v1alpha1.ElasticQuota {
	quota := &v1alpha1.ElasticQuota{
		ObjectMeta: metav1.ObjectMeta{
			Name:        name,
			Annotations: make(map[string]string),
			Labels:      make(map[string]string),
		},
		Spec: v1alpha1.ElasticQuotaSpec{
			Max: createResourceList(maxCpu, maxMem),
			Min: createResourceList(minCpu, minMem),
		},
	}
	quota.Annotations[extension.AnnotationSharedWeight] = fmt.Sprintf("{\"cpu\":%v, \"memory\":\"%v\"}", scaleCpu, scaleMem)
	quota.Labels[extension.LabelQuotaParent] = parentName
	if isParGroup {
		quota.Labels[extension.LabelQuotaIsParent] = "true"
	} else {
		quota.Labels[extension.LabelQuotaIsParent] = "false"
	}
	return quota
}

func TestPlugin_OnQuotaUpdate(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	plugin := p.(*Plugin)
	gqm := plugin.groupQuotaManager
	// test2 Max[96, 160]  Min[50,80] request[20,40]
	//   `-- test2-a Max[96, 160]  Min[50,80] request[20,40]
	// test1 Max[96, 160]  Min[50,80] request[60,100]
	//   `-- test1-a Max[96, 160]  Min[50,80] request[60,100]
	//         `-- a-123 Max[96, 160]  Min[50,80] request[60,100]
	plugin.addQuota("test1", "root", 96, 160, 100, 160, 96, 160, true, "")
	plugin.addQuota("test1-a", "test1", 96, 160, 50, 80, 96, 160, true, "")
	changeQuota := plugin.addQuota("a-123", "test1-a", 96, 160, 50, 80, 96, 160, false, "")
	plugin.addQuota("test2", "root", 96, 160, 100, 160, 96, 160, true, "")
	mmQuota := plugin.addQuota("test2-a", "test2", 96, 160, 50, 80, 96, 160, false, "")
	gqm.UpdateClusterTotalResource(createResourceList(96, 160))
	request := createResourceList(60, 100)
	pod := makePod2("pod", request)
	pod.Labels[extension.LabelQuotaName] = "a-123"
	plugin.OnPodAdd(pod)
	runtime := gqm.RefreshRuntime("a-123")
	assert.Equal(t, request, runtime)

	runtime = gqm.RefreshRuntime("test1-a")
	assert.Equal(t, request, runtime)

	runtime = gqm.RefreshRuntime("test1")
	assert.Equal(t, request, runtime)

	// test2-a request [20,40]
	request = createResourceList(20, 40)
	pod1 := makePod2("pod1", request)
	pod1.Labels[extension.LabelQuotaName] = "test2-a"
	plugin.OnPodAdd(pod1)
	runtime = gqm.RefreshRuntime("test2-a")
	assert.Equal(t, request, runtime)

	runtime = gqm.RefreshRuntime("test2")
	assert.Equal(t, request, runtime)

	// a-123 mv test2
	// test2 Max[96, 160]  Min[100,160] request[80,140]
	//   `-- test2-a Max[96, 160]  Min[50,80] request[20,40]
	//   `-- a-123 Max[96, 160]  Min[50,80] request[60,100]
	// test1 Max[96, 160]  Min[100,160] request[0,0]
	//   `-- test1-a Max[96, 160]  Min[50,80] request[0,0]
	oldQuota := changeQuota.DeepCopy()
	changeQuota.Labels[extension.LabelQuotaParent] = "test2"
	changeQuota.ResourceVersion = "2"
	gqm.GetQuotaInfoByName("test1-a").IsParent = true

	plugin.OnQuotaUpdate(oldQuota, changeQuota)
	quotaInfo := gqm.GetQuotaInfoByName("test1-a")
	gqm.RefreshRuntime("test1-a")
	assert.Equal(t, corev1.ResourceList{}, quotaInfo.GetRequest())
	assert.Equal(t, corev1.ResourceList{}, quotaInfo.GetUsed())
	assert.Equal(t, createResourceList(0, 0), quotaInfo.GetRuntime())

	quotaInfo = gqm.GetQuotaInfoByName("test1")
	gqm.RefreshRuntime("test1")
	assert.Equal(t, corev1.ResourceList{}, quotaInfo.GetRequest())
	assert.Equal(t, corev1.ResourceList{}, quotaInfo.GetUsed())
	assert.Equal(t, createResourceList(0, 0), quotaInfo.GetRuntime())

	quotaInfo = gqm.GetQuotaInfoByName("a-123")
	gqm.RefreshRuntime("a-123")
	assert.Equal(t, createResourceList(60, 100), quotaInfo.GetRequest())
	assert.Equal(t, createResourceList(60, 100), quotaInfo.GetUsed())
	assert.Equal(t, createResourceList(60, 100), quotaInfo.GetRuntime())
	assert.Equal(t, "test2", quotaInfo.ParentName)

	quotaInfo = gqm.GetQuotaInfoByName("test2-a")
	gqm.RefreshRuntime("test2-a")
	assert.Equal(t, createResourceList(20, 40), quotaInfo.GetRequest())
	assert.Equal(t, createResourceList(20, 40), quotaInfo.GetUsed())
	assert.Equal(t, createResourceList(20, 40), quotaInfo.GetRuntime())

	quotaInfo = gqm.GetQuotaInfoByName("test2")
	gqm.RefreshRuntime("test2")
	assert.Equal(t, createResourceList(80, 140), quotaInfo.GetRequest())
	assert.Equal(t, createResourceList(80, 140), quotaInfo.GetUsed())
	assert.Equal(t, createResourceList(80, 140), quotaInfo.GetRuntime())
	changeQuota.Name = "root"
	plugin.OnQuotaUpdate(oldQuota, changeQuota)
	changeQuota.DeletionTimestamp = &metav1.Time{Time: time.Now()}
	plugin.OnQuotaUpdate(oldQuota, changeQuota)
	changeQuota.ResourceVersion = "3"
	plugin.OnQuotaUpdate(oldQuota, changeQuota)
	plugin.OnQuotaDelete(mmQuota)
	assert.Nil(t, gqm.GetQuotaInfoByName("test2-a"))
	quotaInfo = gqm.GetQuotaInfoByName("test2")
	gqm.RefreshRuntime("test2")
	assert.Equal(t, createResourceList(60, 100), quotaInfo.GetRequest())
	assert.Equal(t, createResourceList(60, 100), quotaInfo.GetUsed())
	assert.Equal(t, createResourceList(60, 100), quotaInfo.GetRuntime())
}

func TestPlugin_OnPodAdd_Update_Delete(t *testing.T) {
	suit := newPluginTestSuitWithPod(t, nil, nil)
	plugin := suit.plugin.(*Plugin)
	gqm := plugin.groupQuotaManager
	plugin.addQuota("test1", "root", 96, 160, 100, 160, 96, 160, true, "")
	plugin.addQuota("test2", "root", 96, 160, 100, 160, 96, 160, true, "")
	pods := []*corev1.Pod{
		defaultCreatePodWithQuotaName("1", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("2", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("3", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("4", "test1", 10, 10, 10),
	}
	time.Sleep(100 * time.Millisecond)
	for _, pod := range pods {
		plugin.OnPodAdd(pod)
	}
	time.Sleep(100 * time.Millisecond)
	assert.Equal(t, gqm.GetQuotaInfoByName("test1").GetRequest(), createResourceList(40, 40))
	assert.Equal(t, 4, len(gqm.GetQuotaInfoByName("test1").PodCache))
	newPods := []*corev1.Pod{
		defaultCreatePodWithQuotaNameAndVersion("1", "test2", "2", 10, 10, 10),
		defaultCreatePodWithQuotaNameAndVersion("2", "test2", "2", 10, 10, 10),
		defaultCreatePodWithQuotaNameAndVersion("3", "test2", "2", 10, 10, 10),
		defaultCreatePodWithQuotaNameAndVersion("4", "test2", "2", 10, 10, 10),
	}
	for i, pod := range pods {
		plugin.OnPodUpdate(pod, newPods[i])
	}
	time.Sleep(100 * time.Millisecond)
	assert.Equal(t, 0, len(gqm.GetQuotaInfoByName("test1").GetPodCache()))
	assert.Equal(t, 4, len(gqm.GetQuotaInfoByName("test2").GetPodCache()))
	for _, pod := range newPods {
		plugin.OnPodDelete(pod)
	}
	time.Sleep(100 * time.Millisecond)
	assert.Equal(t, len(gqm.GetQuotaInfoByName("test2").GetPodCache()), 0)
}

func setLoglevel(logLevel string) {
	var level klog.Level
	if err := level.Set(logLevel); err != nil {
		fmt.Printf("failed set klog.logging.verbosity %v: %v", logLevel, err)
	}
	fmt.Printf("successfully set klog.logging.verbosity to %v", logLevel)
}

func TestPlugin_PreFilter(t *testing.T) {
	test := []struct {
		name           string
		pod            *corev1.Pod
		quotaInfo      *core.QuotaInfo
		expectedStatus framework.Status
		checkParent    bool
	}{
		{
			name: "default",
			pod: MakePod("t1-ns1", "pod1").Container(
				MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).Obj(),
			quotaInfo: &core.QuotaInfo{
				Name: extension.DefaultQuotaName,
				CalculateInfo: core.QuotaCalculateInfo{
					Runtime: MakeResourceList().CPU(0).Mem(20).GPU(10).Obj(),
				},
			},
			expectedStatus: *framework.NewStatus(framework.Unschedulable, fmt.Sprintf("Scheduling refused due to insufficient quotas, "+
				"quotaName: %v, runtime: %v, used: %v, pod's request: %v, exceedDimensions: [cpu]",
				"default", printResourceList(MakeResourceList().CPU(0).Mem(20).GPU(10).Obj()),
				printResourceList(corev1.ResourceList{}), printResourceList(MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()))),
		},
		{
			name: "used dimension larger than runtime, but value is enough",
			pod: MakePod("t1-ns1", "pod1").Container(
				MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).Obj(),
			quotaInfo: &core.QuotaInfo{
				Name: extension.DefaultQuotaName,
				CalculateInfo: core.QuotaCalculateInfo{
					Runtime: MakeResourceList().CPU(10).Mem(20).GPU(10).Obj(),
				},
			},
			expectedStatus: *framework.NewStatus(framework.Success, ""),
		},
		{
			name: "value not enough",
			pod: MakePod("t1-ns1", "pod1").Container(
				MakeResourceList().CPU(1).Mem(3).GPU(1).Obj()).Obj(),
			quotaInfo: &core.QuotaInfo{
				Name: extension.DefaultQuotaName,
				CalculateInfo: core.QuotaCalculateInfo{
					Runtime: MakeResourceList().CPU(1).Mem(2).Obj(),
				},
			},
			expectedStatus: *framework.NewStatus(framework.Unschedulable,
				fmt.Sprintf("Scheduling refused due to insufficient quotas, "+
					"quotaName: %v, runtime: %v, used: %v, pod's request: %v, exceedDimensions: [memory]",
					"default", printResourceList(MakeResourceList().CPU(1).Mem(2).Obj()),
					printResourceList(corev1.ResourceList{}), printResourceList(MakeResourceList().CPU(1).Mem(3).GPU(1).Obj()))),
		},
		{
			name: "used dimension larger than runtime, but value is enough",
			pod: MakePod("t1-ns1", "pod1").Container(
				MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).Obj(),
			quotaInfo: &core.QuotaInfo{
				Name: extension.DefaultQuotaName,
				CalculateInfo: core.QuotaCalculateInfo{
					Runtime: MakeResourceList().CPU(10).Mem(20).GPU(10).Obj(),
				},
			},
			expectedStatus: *framework.NewStatus(framework.Success, ""),
		},
	}
	for _, tt := range test {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
			gp := p.(*Plugin)
			qi := gp.groupQuotaManager.GetQuotaInfoByName(tt.quotaInfo.Name)
			qi.Lock()
			qi.CalculateInfo.Runtime = tt.quotaInfo.CalculateInfo.Runtime.DeepCopy()
			qi.UnLock()
			state := framework.NewCycleState()
			ctx := context.TODO()
			status := *gp.PreFilter(ctx, state, tt.pod)
			assert.Equal(t, status, tt.expectedStatus)
		})
	}
}

func TestPlugin_PreFilter_CheckParent(t *testing.T) {
	test := []struct {
		name           string
		pod            *corev1.Pod
		quotaInfo      *v1alpha1.ElasticQuota
		childRuntime   corev1.ResourceList
		parQuotaInfo   *v1alpha1.ElasticQuota
		parentRuntime  corev1.ResourceList
		expectedStatus framework.Status
	}{
		{
			name: "parent reject",
			pod: MakePod("t1-ns1", "pod1").Label(extension.LabelQuotaName, "test-child").Container(
				MakeResourceList().CPU(1).Mem(3).GPU(1).Obj()).Obj(),
			quotaInfo: &v1alpha1.ElasticQuota{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-child",
					Labels: map[string]string{
						extension.LabelQuotaParent: "test",
					},
				},
				Spec: v1alpha1.ElasticQuotaSpec{
					Max: MakeResourceList().CPU(10).Mem(30).GPU(10).Obj(),
					Min: MakeResourceList().CPU(0).Mem(0).GPU(0).Obj(),
				},
			},
			childRuntime:  MakeResourceList().CPU(1).Mem(3).GPU(1).Obj(),
			parentRuntime: MakeResourceList().CPU(1).Mem(2).GPU(1).Obj(),
			parQuotaInfo: &v1alpha1.ElasticQuota{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test",
				},
				Spec: v1alpha1.ElasticQuotaSpec{
					Max: MakeResourceList().CPU(10).Mem(30).GPU(10).Obj(),
					Min: MakeResourceList().CPU(0).Mem(0).GPU(0).Obj(),
				},
			},
			expectedStatus: *framework.NewStatus(framework.Unschedulable,
				fmt.Sprintf("Scheduling refused due to insufficient quotas, "+
					"quotaNameTopo: %v, runtime: %v, used: %v, pod's request: %v, exceedDimensions: [memory]",
					[]string{"test", "test-child"}, printResourceList(MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()),
					printResourceList(corev1.ResourceList{}), printResourceList(MakeResourceList().CPU(1).Mem(3).GPU(1).Obj()))),
		},
	}
	for _, tt := range test {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
			gp := p.(*Plugin)
			gp.pluginArgs.EnableCheckParentQuota = pointer.Bool(true)
			gp.OnQuotaAdd(tt.parQuotaInfo)
			gp.OnQuotaAdd(tt.quotaInfo)
			qi := gp.groupQuotaManager.GetQuotaInfoByName(tt.quotaInfo.Name)
			qi.Lock()
			qi.CalculateInfo.Runtime = tt.childRuntime.DeepCopy()
			qi.UnLock()
			qi1 := gp.groupQuotaManager.GetQuotaInfoByName(tt.parQuotaInfo.Name)
			qi1.Lock()
			qi1.CalculateInfo.Runtime = tt.parentRuntime.DeepCopy()
			qi1.UnLock()
			state := framework.NewCycleState()
			ctx := context.TODO()
			status := *gp.PreFilter(ctx, state, tt.pod)
			assert.Equal(t, status, tt.expectedStatus)
			klog.Infof("%v", tt.expectedStatus)
		})
	}
}

func TestPlugin_Reserve(t *testing.T) {
	test := []struct {
		name         string
		pod          *corev1.Pod
		quotaInfo    *core.QuotaInfo
		expectedUsed corev1.ResourceList
	}{
		{
			name: "basic",
			pod: MakePod("t1-ns1", "pod1").Container(
				MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).UID("pod1").Obj(),
			quotaInfo: &core.QuotaInfo{
				Name: extension.DefaultQuotaName,
				CalculateInfo: core.QuotaCalculateInfo{
					Used: MakeResourceList().CPU(10).Mem(20).GPU(10).Obj(),
				},
			},
			expectedUsed: MakeResourceList().CPU(11).Mem(22).GPU(11).Obj(),
		},
	}
	for _, tt := range test {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
			gp := p.(*Plugin)
			pod := makePod2("pod", tt.quotaInfo.CalculateInfo.Used)
			gp.OnPodAdd(pod)
			gp.OnPodAdd(tt.pod)
			ctx := context.TODO()
			gp.Reserve(ctx, framework.NewCycleState(), tt.pod, "")
			assert.Equal(t, gp.groupQuotaManager.GetQuotaInfoByName(tt.quotaInfo.Name).GetUsed(), tt.expectedUsed)
		})
	}
}

func TestPlugin_Unreserve(t *testing.T) {
	test := []struct {
		name         string
		pod          *corev1.Pod
		quotaInfo    *core.QuotaInfo
		expectStatus bool
	}{
		{
			name: "basic",
			pod: MakePod("t1-ns1", "pod1").Container(
				MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).Phase(corev1.PodRunning).UID("pod1").Obj(),
			quotaInfo: &core.QuotaInfo{
				Name: extension.DefaultQuotaName,
				CalculateInfo: core.QuotaCalculateInfo{
					Used: MakeResourceList().CPU(10).Mem(20).GPU(10).Obj(),
				},
				PodCache: make(map[string]*core.PodInfo),
			},
			expectStatus: false,
		},
	}
	for _, tt := range test {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
			gp := p.(*Plugin)
			ctx := context.TODO()
			gp.OnPodAdd(tt.pod)
			gp.Reserve(ctx, framework.NewCycleState(), tt.pod, "")
			assert.True(t, gp.groupQuotaManager.GetQuotaInfoByName(tt.quotaInfo.Name).GetPodIsAssigned(tt.pod))
			gp.Unreserve(ctx, framework.NewCycleState(), tt.pod, "")
			assert.False(t, gp.groupQuotaManager.GetQuotaInfoByName(tt.quotaInfo.Name).GetPodIsAssigned(tt.pod))
		})
	}
}

func TestPlugin_AddPod(t *testing.T) {
	test := []struct {
		name         string
		podInfo      *framework.PodInfo
		quotaInfo    *core.QuotaInfo
		expectedUsed corev1.ResourceList
	}{
		{
			name: "basic",
			podInfo: &framework.PodInfo{
				Pod: MakePod("t1-ns1", "pod1").Container(
					MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).
					Label(extension.LabelQuotaName, "t1-eq1").UID("1").Obj(),
			},
			quotaInfo: &core.QuotaInfo{
				Name: "default",
				CalculateInfo: core.QuotaCalculateInfo{
					Used: MakeResourceList().CPU(10).Mem(20).GPU(10).Obj(),
				},
				PodCache: make(map[string]*core.PodInfo),
			},
			expectedUsed: MakeResourceList().CPU(11).Mem(22).GPU(11).Obj(),
		},
	}
	for _, tt := range test {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
			gp := p.(*Plugin)
			pod := makePod2("test", tt.quotaInfo.CalculateInfo.Used)
			gp.OnPodAdd(pod)
			gp.OnPodAdd(tt.podInfo.Pod)
			state := framework.NewCycleState()
			ctx := context.TODO()
			gp.snapshotPostFilterState(gp.getPodAssociateQuotaName(tt.podInfo.Pod), state)
			gp.AddPod(ctx, state, nil, tt.podInfo, nil)
			data, _ := getPostFilterState(state)
			assert.Equal(t, data.quotaInfo.GetUsed(), tt.expectedUsed)
		})
	}
}

func TestPlugin_RemovePod(t *testing.T) {
	test := []struct {
		name         string
		podInfo      *framework.PodInfo
		quotaInfo    *core.QuotaInfo
		expectedUsed corev1.ResourceList
	}{
		{
			name: "basic",
			podInfo: &framework.PodInfo{
				Pod: MakePod("t1-ns1", "pod1").Container(
					MakeResourceList().CPU(1).Mem(2).GPU(1).Obj()).
					Label(extension.LabelQuotaName, "t1-eq1").UID("1").Phase(corev1.PodRunning).Obj(),
			},
			quotaInfo: &core.QuotaInfo{
				Name: "default",
				CalculateInfo: core.QuotaCalculateInfo{
					Used: MakeResourceList().CPU(10).Mem(20).GPU(10).Obj(),
				},
			},
			expectedUsed: MakeResourceList().CPU(10).Mem(20).GPU(10).Obj(),
		},
	}
	for _, tt := range test {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t, nil)
			p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
			gp := p.(*Plugin)
			pod := makePod2("pod", tt.quotaInfo.CalculateInfo.Used)
			gp.OnPodAdd(pod)
			gp.OnPodAdd(tt.podInfo.Pod)
			state := framework.NewCycleState()
			ctx := context.TODO()
			gp.snapshotPostFilterState(gp.getPodAssociateQuotaName(tt.podInfo.Pod), state)
			gp.RemovePod(ctx, state, nil, tt.podInfo, nil)
			data, _ := getPostFilterState(state)
			assert.Equal(t, data.quotaInfo.GetUsed(), tt.expectedUsed)
		})
	}
}

var (
	lowPriority, midPriority, highPriority = int32(0), int32(100), int32(1000)
)

func TestPlugin_DryRunPreemption(t *testing.T) {
	res := map[corev1.ResourceName]string{corev1.ResourceMemory: "150"}
	tests := []struct {
		name            string
		args            *config.ElasticQuotaArgs
		pod             *corev1.Pod
		pods            []*corev1.Pod
		nodes           []*corev1.Node
		quotaInfos      []*core.QuotaInfo
		clusterResource corev1.ResourceList
		nodesStatuses   framework.NodeToStatusMap
		want            []defaultpreemption.Candidate
	}{
		{
			name: "in same quota preemption",
			pod:  makePod("t1-p", "ns1", 50, 0, 0, highPriority, "", "t1-p"),
			pods: []*corev1.Pod{
				makePod("t1-p1", "ns1", 50, 0, 0, midPriority, "t1-p1", "node-a"),
				makePod("t1-p2", "ns2", 50, 0, 0, midPriority, "t1-p2", "node-a"),
				makePod("t1-p3", "ns2", 50, 0, 0, midPriority, "t1-p3", "node-a"),
			},
			nodes: []*corev1.Node{
				schedulertesting.MakeNode().Name("node-a").Capacity(res).Obj(),
			},
			nodesStatuses: framework.NodeToStatusMap{
				"node-a": framework.NewStatus(framework.Unschedulable),
			},
			want: []defaultpreemption.Candidate{
				&candidate{
					victims: &extenderv1.Victims{
						Pods: []*corev1.Pod{
							makePod("t1-p1", "ns1", 50, 0, 0, midPriority, "t1-p1", "node-a"),
						},
						NumPDBViolations: 0,
					},
					name: "node-a",
				},
			},
			quotaInfos: []*core.QuotaInfo{
				{
					Name:       "ns1",
					ParentName: "root",
					PodCache:   make(map[string]*core.PodInfo),
				},
				{
					Name:       "ns2",
					ParentName: "root",
					PodCache:   make(map[string]*core.PodInfo),
				},
			},
			clusterResource: createResourceList(0, 400),
		},
		{
			name: "preempt same quotaGroup, although its priority is higher than others",
			pod:  makePod("t1-p", "ns1", 50, 0, 0, highPriority, "", "t1-p"),
			pods: []*corev1.Pod{
				makePod("t1-p1", "ns1", 50, 0, 0, midPriority, "t1-p1", "node-a"),
				makePod("t1-p2", "ns2", 50, 0, 0, lowPriority, "t1-p2", "node-a"),
				makePod("t1-p3", "ns2", 50, 0, 0, lowPriority, "t1-p3", "node-a"),
			},
			nodes: []*corev1.Node{
				schedulertesting.MakeNode().Name("node-a").Capacity(res).Obj(),
			},
			nodesStatuses: framework.NodeToStatusMap{
				"node-a": framework.NewStatus(framework.Unschedulable),
			},
			want: []defaultpreemption.Candidate{
				&candidate{
					victims: &extenderv1.Victims{
						Pods: []*corev1.Pod{
							makePod("t1-p1", "ns1", 50, 0, 0, midPriority, "t1-p1", "node-a"),
						},
						NumPDBViolations: 0,
					},
					name: "node-a",
				},
			},
			quotaInfos: []*core.QuotaInfo{
				{
					Name:       "ns1",
					ParentName: "root",
					PodCache:   make(map[string]*core.PodInfo),
				},
				{
					Name:       "ns2",
					ParentName: "root",
					PodCache:   make(map[string]*core.PodInfo),
				},
			},
			clusterResource: createResourceList(0, 400),
		},
		{
			name: "preempt same quotaGroup, quota not enough",
			pod:  makePod("t1-p", "ns1", 50, 0, 0, highPriority, "", "t1-p"),
			pods: []*corev1.Pod{
				makePod("t1-p1", "ns1", 50, 0, 0, midPriority, "t1-p1", "node-a"),
				makePod("t1-p2", "ns2", 50, 0, 0, lowPriority, "t1-p2", "node-a"),
			},
			nodes: []*corev1.Node{
				schedulertesting.MakeNode().Name("node-a").Capacity(res).Obj(),
			},
			nodesStatuses: framework.NodeToStatusMap{
				"node-a": framework.NewStatus(framework.Unschedulable),
			},
			want: []defaultpreemption.Candidate{
				&candidate{
					victims: &extenderv1.Victims{
						Pods: []*corev1.Pod{
							makePod("t1-p1", "ns1", 50, 0, 0, midPriority, "t1-p1", "node-a"),
						},
						NumPDBViolations: 0,
					},
					name: "node-a",
				},
			},
			quotaInfos: []*core.QuotaInfo{
				{
					Name:       "ns1",
					ParentName: "root",
					PodCache:   make(map[string]*core.PodInfo),
				},
				{
					Name:       "ns2",
					ParentName: "root",
					PodCache:   make(map[string]*core.PodInfo),
				},
			},
			clusterResource: createResourceList(0, 100),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuitWithPod(t, tt.nodes, tt.pods)
			pl := suit.plugin.(*Plugin)
			gqm := pl.groupQuotaManager
			state := framework.NewCycleState()
			ctx := context.Background()

			gqm.UpdateClusterTotalResource(tt.clusterResource)
			for _, quota := range tt.quotaInfos {
				q := CreateQuota2(quota.Name, quota.ParentName, 1000, 1000, 0, 0, 1000, 1000, false)
				pl.OnQuotaAdd(q)
			}
			for _, pod := range tt.pods {
				pod.Labels = make(map[string]string)
				pod.Labels[extension.LabelQuotaName] = pod.Namespace
				pl.OnPodAdd(pod)
				pl.groupQuotaManager.UpdatePodIsAssigned(pod.Namespace, pod, true)
			}
			tt.pod.Labels = make(map[string]string)
			tt.pod.Labels[extension.LabelQuotaName] = tt.pod.Namespace

			// Some tests rely on PreFilter plugin to compute its CycleState.
			preFilterStatus := suit.Framework.RunPreFilterPlugins(ctx, state, tt.pod)
			if !preFilterStatus.IsSuccess() {
				t.Errorf("Unexpected preFilterStatus: %v", preFilterStatus)
			}

			quotaName := pl.getPodAssociateQuotaName(tt.pod)
			pl.groupQuotaManager.UpdatePodIsAssigned(quotaName, tt.pod, true)
			pl.groupQuotaManager.RefreshRuntime(tt.pod.Namespace)
			pl.snapshotPostFilterState(quotaName, state)

			got, status := pl.findCandidates(ctx, state, tt.pod, tt.nodesStatuses)
			if !status.IsSuccess() {
				t.Fatalf("unexpected error during FindCandidates(): %v", status)
			}

			// Sort the values (inner victims) and the candidate itself (by its NominatedNodeName).
			for i := range got {
				victims := got[i].Victims().Pods
				sort.Slice(victims, func(i, j int) bool {
					return victims[i].Name < victims[j].Name
				})
			}
			sort.Slice(got, func(i, j int) bool {
				return got[i].Name() < got[j].Name()
			})

			for _, victim := range tt.want {
				victim.Victims().Pods[0].Labels = make(map[string]string)
				victim.Victims().Pods[0].Labels[extension.LabelQuotaName] = victim.Victims().Pods[0].Namespace
			}
			if diff := gocmp.Diff(tt.want, got, gocmp.AllowUnexported(candidate{})); diff != "" {
				t.Errorf("Unexpected candidates (-want, +got): %s", diff)
			}
		})
	}
}

func TestPlugin_createDefaultQuotaIfNotPresent(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	eq, _ := suit.client.SchedulingV1alpha1().ElasticQuotas(suit.elasticQuotaArgs.QuotaGroupNamespace).Get(context.TODO(), "default", metav1.GetOptions{})
	if !v1.Equals(eq.Spec.Max, suit.elasticQuotaArgs.DefaultQuotaGroupMax) {
		t.Errorf("error")
	}
}

func TestPlugin_createSystemQuotaIfNotPresent(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	eq, _ := suit.client.SchedulingV1alpha1().ElasticQuotas(suit.elasticQuotaArgs.QuotaGroupNamespace).Get(context.TODO(), "system", metav1.GetOptions{})
	if !v1.Equals(eq.Spec.Max, suit.elasticQuotaArgs.SystemQuotaGroupMax) {
		t.Errorf("error")
	}
}

func makePod(podName string, namespace string, memReq int64, cpuReq int64, gpuReq int64, priority int32, uid string, nodeName string) *corev1.Pod {
	pause := imageutils.GetPauseImageName()
	pod := schedulertesting.MakePod().Namespace(namespace).Name(podName).Container(pause).
		Priority(priority).Node(nodeName).UID(uid).ZeroTerminationGracePeriod().Obj()
	pod.Spec.Containers[0].Resources = corev1.ResourceRequirements{
		Requests: corev1.ResourceList{
			corev1.ResourceMemory: *resource.NewQuantity(memReq, resource.DecimalSI),
			corev1.ResourceCPU:    *resource.NewMilliQuantity(cpuReq, resource.DecimalSI),
			"nvidia.com/gpu":      *resource.NewQuantity(gpuReq, resource.DecimalSI),
		},
	}
	pod.Status.Phase = corev1.PodRunning
	return pod
}

func makePod2(podName string, request corev1.ResourceList) *corev1.Pod {
	pause := imageutils.GetPauseImageName()
	pod := schedulertesting.MakePod().Namespace(extension.DefaultQuotaName).Name(podName).Container(pause).ZeroTerminationGracePeriod().Obj()
	pod.Spec.Containers[0].Resources = corev1.ResourceRequirements{
		Requests: request,
	}
	pod.Status.Phase = corev1.PodRunning
	pod.Spec.NodeName = "testNode"
	pod.Labels = make(map[string]string)
	return pod
}

// nominatedPodMap is a structure that stores pods nominated to run on nodes.
// It exists because nominatedNodeName of pod objects stored in the structure
// may be different than what scheduler has here. We should be able to find pods
// by their UID and update/delete them.
type nominatedPodMap struct {
	// nominatedPods is a map keyed by a node name and the value is a list of
	// pods which are nominated to run on the node. These are pods which can be in
	// the activeQ or unschedulableQ.
	nominatedPods map[string][]*framework.PodInfo
	// nominatedPodToNode is map keyed by a Pod UID to the node name where it is
	// nominated.
	nominatedPodToNode map[types.UID]string

	sync.RWMutex
}

func (npm *nominatedPodMap) add(pi *framework.PodInfo, nodeName string) {
	// always delete the pod if it already exist, to ensure we never store more than
	// one instance of the pod.
	npm.delete(pi.Pod)

	nnn := nodeName
	if len(nnn) == 0 {
		nnn = NominatedNodeName(pi.Pod)
		if len(nnn) == 0 {
			return
		}
	}
	npm.nominatedPodToNode[pi.Pod.UID] = nnn
	for _, npi := range npm.nominatedPods[nnn] {
		if npi.Pod.UID == pi.Pod.UID {
			klog.V(4).InfoS("Pod already exists in the nominated map", "pod", klog.KObj(npi.Pod))
			return
		}
	}
	npm.nominatedPods[nnn] = append(npm.nominatedPods[nnn], pi)
}

func (npm *nominatedPodMap) delete(p *corev1.Pod) {
	nnn, ok := npm.nominatedPodToNode[p.UID]
	if !ok {
		return
	}
	for i, np := range npm.nominatedPods[nnn] {
		if np.Pod.UID == p.UID {
			npm.nominatedPods[nnn] = append(npm.nominatedPods[nnn][:i], npm.nominatedPods[nnn][i+1:]...)
			if len(npm.nominatedPods[nnn]) == 0 {
				delete(npm.nominatedPods, nnn)
			}
			break
		}
	}
	delete(npm.nominatedPodToNode, p.UID)
}

// UpdateNominatedPod updates the <oldPod> with <newPod>.
func (npm *nominatedPodMap) UpdateNominatedPod(oldPod *corev1.Pod, newPodInfo *framework.PodInfo) {
	npm.Lock()
	defer npm.Unlock()
	// In some cases, an Update event with no "NominatedNode" present is received right
	// after a node("NominatedNode") is reserved for this pod in memory.
	// In this case, we need to keep reserving the NominatedNode when updating the pod pointer.
	nodeName := ""
	// We won't fall into below `if` block if the Update event represents:
	// (1) NominatedNode info is added
	// (2) NominatedNode info is updated
	// (3) NominatedNode info is removed
	if NominatedNodeName(oldPod) == "" && NominatedNodeName(newPodInfo.Pod) == "" {
		if nnn, ok := npm.nominatedPodToNode[oldPod.UID]; ok {
			// This is the only case we should continue reserving the NominatedNode
			nodeName = nnn
		}
	}
	// We update irrespective of the nominatedNodeName changed or not, to ensure
	// that pod pointer is updated.
	npm.delete(oldPod)
	npm.add(newPodInfo, nodeName)
}

// NewPodNominator creates a nominatedPodMap as a backing of framework.PodNominator.
func NewPodNominator() framework.PodNominator {
	return &nominatedPodMap{
		nominatedPods:      make(map[string][]*framework.PodInfo),
		nominatedPodToNode: make(map[types.UID]string),
	}
}

// NominatedNodeName returns nominated node name of a Pod.
func NominatedNodeName(pod *corev1.Pod) string {
	return pod.Status.NominatedNodeName
}

// DeleteNominatedPodIfExists deletes <pod> from nominatedPods.
func (npm *nominatedPodMap) DeleteNominatedPodIfExists(pod *corev1.Pod) {
	npm.Lock()
	npm.delete(pod)
	npm.Unlock()
}

// AddNominatedPod adds a pod to the nominated pods of the given node.
// This is called during the preemption process after a node is nominated to run
// the pod. We update the structure before sending a request to update the pod
// object to avoid races with the following scheduling cycles.
func (npm *nominatedPodMap) AddNominatedPod(pi *framework.PodInfo, nodeName string) {
	npm.Lock()
	npm.add(pi, nodeName)
	npm.Unlock()
}

// NominatedPodsForNode returns pods that are nominated to run on the given node,
// but they are waiting for other pods to be removed from the node.
func (npm *nominatedPodMap) NominatedPodsForNode(nodeName string) []*framework.PodInfo {
	npm.RLock()
	defer npm.RUnlock()
	// TODO: we may need to return a copy of []*Pods to avoid modification
	// on the caller side.
	return npm.nominatedPods[nodeName]
}

func TestPlugin_Recover(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	nodes := []*corev1.Node{
		{
			ObjectMeta: metav1.ObjectMeta{
				Name: "node1",
			},
			Status: corev1.NodeStatus{
				Allocatable: createResourceList(100, 1000),
			},
		},
		{
			ObjectMeta: metav1.ObjectMeta{
				Name: "node2",
			},
			Status: corev1.NodeStatus{
				Allocatable: createResourceList(100, 1000),
			},
		},
	}
	for _, node := range nodes {
		suit.Handle.ClientSet().CoreV1().Nodes().Create(context.TODO(), node, metav1.CreateOptions{})
	}
	time.Sleep(100 * time.Millisecond)
	suit.AddQuota("test1", "test-parent", 100, 1000, 0, 0, 0, 0, false, "")
	suit.AddQuota("test-parent", "root", 100, 1000, 0, 0, 0, 0, true, "")
	time.Sleep(100 * time.Millisecond)
	pods := []*corev1.Pod{
		defaultCreatePodWithQuotaName("1", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("2", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("3", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("4", "test1", 10, 10, 10),
	}
	for _, pod := range pods {
		suit.Handle.ClientSet().CoreV1().Pods("").Create(context.TODO(), pod, metav1.CreateOptions{})
	}
	time.Sleep(100 * time.Millisecond)
	p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	pl := p.(*Plugin)
	time.Sleep(100 * time.Millisecond)
	assert.Equal(t, pl.groupQuotaManager.GetQuotaInfoByName("test1").GetRequest(), createResourceList(40, 40))
	assert.Equal(t, pl.groupQuotaManager.GetQuotaInfoByName("test1").GetUsed(), createResourceList(40, 40))
	assert.True(t, v1.IsZero(pl.groupQuotaManager.GetQuotaInfoByName("default").GetRequest()))
	assert.Equal(t, len(pl.groupQuotaManager.GetAllQuotaNames()), 4)
}

func TestPlugin_migrateDefaultQuotaGroupsPod(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, _ := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	plugin := p.(*Plugin)
	gqm := plugin.groupQuotaManager
	plugin.addQuota("test2", "root", 96, 160, 100, 160, 96, 160, true, "")
	pods := []*corev1.Pod{
		defaultCreatePodWithQuotaName("1", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("2", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("3", "test1", 10, 10, 10),
		defaultCreatePodWithQuotaName("4", "test1", 10, 10, 10),
	}
	for _, pod := range pods {
		plugin.OnPodAdd(pod)
	}
	assert.Equal(t, gqm.GetQuotaInfoByName("default").GetRequest(), createResourceList(40, 40))
	assert.Equal(t, 4, len(gqm.GetQuotaInfoByName("default").PodCache))
	plugin.addQuota("test1", "root", 96, 160, 100, 160, 96, 160, true, "")
	time.Sleep(100 * time.Millisecond)
	go plugin.Start()
	for i := 0; i < 10; i++ {
		if len(gqm.GetQuotaInfoByName("default").GetPodCache()) != 0 || len(gqm.GetQuotaInfoByName("test1").GetPodCache()) != 4 {
			time.Sleep(1 * time.Second)
			continue
		}
		break
	}
	assert.Equal(t, 0, len(gqm.GetQuotaInfoByName("default").PodCache))
	assert.Equal(t, 4, len(gqm.GetQuotaInfoByName("test1").PodCache))
}

func defaultCreatePodWithQuotaNameAndVersion(name, quotaName, version string, priority int32, cpu, mem int64) *corev1.Pod {
	pod := defaultCreatePod(name, priority, cpu, mem)
	pod.Labels[extension.LabelQuotaName] = quotaName
	pod.ResourceVersion = version
	pod.UID = types.UID(name)
	return pod
}

func defaultCreatePodWithQuotaName(name, quotaName string, priority int32, cpu, mem int64) *corev1.Pod {
	pod := defaultCreatePod(name, priority, cpu, mem)
	pod.Labels[extension.LabelQuotaName] = quotaName
	pod.UID = types.UID(name)
	pod.Spec.NodeName = "test"
	return pod
}

func defaultCreatePod(name string, priority int32, cpu, mem int64) *corev1.Pod {
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Name:   name,
			Labels: make(map[string]string),
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: createResourceList(cpu, mem),
					},
				},
			},
			Priority: pointer.Int32(priority),
		},
	}
	pod.Status.Phase = corev1.PodRunning
	pod.Spec.NodeName = "test-node"
	return pod
}
