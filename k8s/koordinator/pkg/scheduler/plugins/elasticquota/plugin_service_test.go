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
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/elasticquota/core"
)

func TestEndpointsQueryQuotaInfo(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(suit.elasticQuotaArgs, suit.Handle)
	assert.NotNil(t, p)
	assert.Nil(t, err)

	eq := p.(*Plugin)
	quota := CreateQuota2("test1", "", 100, 100, 10, 10, 20, 20, false)
	eq.OnQuotaAdd(quota)

	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "node1",
		},
		Status: corev1.NodeStatus{
			Allocatable: createResourceList(1000, 1000),
		},
	}
	eq.OnNodeAdd(node)

	podToCreate := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "ns",
			Name:      "pod1",
			Labels: map[string]string{
				extension.LabelQuotaName: "test1",
			},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: createResourceList(33, 33),
					},
				},
			},
		},
	}
	podToCreate.Spec.NodeName = "n1"
	eq.OnPodAdd(podToCreate)

	quotaExpected := core.QuotaInfoSummary{
		Name:              "test1",
		ParentName:        "root",
		IsParent:          false,
		AllowLentResource: true,
		Max:               createResourceList(100, 100),
		Min:               createResourceList(10, 10),
		AutoScaleMin:      createResourceList(10, 10),
		Used:              createResourceList(33, 33),
		Request:           createResourceList(33, 33),
		SharedWeight:      createResourceList(20, 20),
		Runtime:           createResourceList(33, 33),

		PodCache: map[string]*core.SimplePodInfo{
			"pod1": {
				IsAssigned: true,
				Resource:   createResourceList(33, 33),
			},
		},
	}
	{
		engine := gin.Default()
		eq.RegisterEndpoints(engine.Group("/"))
		w := httptest.NewRecorder()
		req, _ := http.NewRequest("GET", "/quota/test1", nil)
		engine.ServeHTTP(w, req)
		assert.Equal(t, http.StatusOK, w.Result().StatusCode)
		quotaSummary := core.NewQuotaInfoSummary()
		err = json.NewDecoder(w.Result().Body).Decode(quotaSummary)
		assert.NoError(t, err)

		assert.True(t, quotav1.Equals(quotaSummary.Max, quotaExpected.Max))
		assert.True(t, quotav1.Equals(quotaSummary.Min, quotaExpected.Min))
		assert.True(t, quotav1.Equals(quotaSummary.AutoScaleMin, quotaExpected.AutoScaleMin))
		assert.True(t, quotav1.Equals(quotaSummary.Used, quotaExpected.Used))
		assert.True(t, quotav1.Equals(quotaSummary.Request, quotaExpected.Request))
		assert.True(t, quotav1.Equals(quotaSummary.SharedWeight, quotaExpected.SharedWeight))
		assert.True(t, quotav1.Equals(quotaSummary.Runtime, quotaExpected.Runtime))

		assert.Equal(t, quotaSummary.Name, quotaExpected.Name)
		assert.Equal(t, quotaSummary.ParentName, quotaExpected.ParentName)
		assert.Equal(t, quotaSummary.IsParent, quotaExpected.IsParent)
		assert.Equal(t, quotaSummary.AllowLentResource, quotaExpected.AllowLentResource)
		assert.Equal(t, len(quotaSummary.PodCache), 1)
		assert.Equal(t, quotaSummary.PodCache[podToCreate.Namespace+"/"+podToCreate.Name].IsAssigned, true)
		assert.True(t, quotav1.Equals(quotaSummary.PodCache[podToCreate.Namespace+"/"+podToCreate.Name].Resource, createResourceList(33, 33)))
	}
	{
		engine := gin.Default()
		eq.RegisterEndpoints(engine.Group("/"))
		w := httptest.NewRecorder()
		req, _ := http.NewRequest("GET", "/quotas", nil)
		engine.ServeHTTP(w, req)
		assert.Equal(t, http.StatusOK, w.Result().StatusCode)
		quotaSummaries := make(map[string]*core.QuotaInfoSummary)
		err = json.Unmarshal([]byte(w.Body.String()), &quotaSummaries)
		assert.NoError(t, err)

		quotaSummary := quotaSummaries["test1"]
		assert.True(t, quotav1.Equals(quotaSummary.Max, quotaExpected.Max))
		assert.True(t, quotav1.Equals(quotaSummary.Min, quotaExpected.Min))
		assert.True(t, quotav1.Equals(quotaSummary.AutoScaleMin, quotaExpected.AutoScaleMin))
		assert.True(t, quotav1.Equals(quotaSummary.Used, quotaExpected.Used))
		assert.True(t, quotav1.Equals(quotaSummary.Request, quotaExpected.Request))
		assert.True(t, quotav1.Equals(quotaSummary.SharedWeight, quotaExpected.SharedWeight))
		assert.True(t, quotav1.Equals(quotaSummary.Runtime, quotaExpected.Runtime))

		assert.Equal(t, quotaSummary.Name, quotaExpected.Name)
		assert.Equal(t, quotaSummary.ParentName, quotaExpected.ParentName)
		assert.Equal(t, quotaSummary.IsParent, quotaExpected.IsParent)
		assert.Equal(t, quotaSummary.AllowLentResource, quotaExpected.AllowLentResource)
		assert.Equal(t, len(quotaSummary.PodCache), 1)
		assert.Equal(t, quotaSummary.PodCache[podToCreate.Namespace+"/"+podToCreate.Name].IsAssigned, true)
		assert.True(t, quotav1.Equals(quotaSummary.PodCache[podToCreate.Namespace+"/"+podToCreate.Name].Resource, createResourceList(33, 33)))
	}
}
