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

package legacy

import (
	"math/rand"
	"strconv"
	"time"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/core/legacy/conf"
)

const testClusterName = "core-services-test"

var (
	hostIps = randomIP(10)
	header  = map[string]string{"Accept": "application/vnd.dice+json;version=1.0", "Previleged": "true"}
)

// func init() {
// 	PatchParseConf()
// 	PatchRunConsumer()
// 	PatchNewServer()
// 	defer monkey.UnpatchAll()
// 	go func() {
// 		err := Initialize()
// 		if err != nil {
// 			panic("start cmdb server failed")
// 		}
// 	}()
// 	time.Sleep(5 * time.Second)
// }
//
// func initEndpoint(t *testing.T) *httpexpect.Expect {
// 	endpoint := httpexpect.New(t, "http://127.0.0.1:9093/api")
// 	return endpoint
// }

func randomIP(size int) []string {
	var result []string

	rand.Seed(time.Now().UnixNano())
	for i := 1; i <= size; i++ {
		a := strconv.Itoa(rand.Intn(254))
		b := strconv.Itoa(rand.Intn(254))
		c := strconv.Itoa(rand.Intn(254))
		d := strconv.Itoa(rand.Intn(254))
		ip := a + "." + b + "." + c + "." + d
		result = append(result, ip)
	}
	return result
}

func RandStringBytes(n, size int) []string {
	var result []string
	letterBytes := "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
	rand.Seed(time.Now().UnixNano())
	for i := 1; i <= size; i++ {
		b := make([]byte, n)
		for i := range b {
			b[i] = letterBytes[rand.Intn(len(letterBytes))]
		}
		result = append(result, string(b))
	}
	return result
}

func PatchParseConf() {
	monkey.Patch(conf.Load, func() error {
		return nil
	})
}

// func PatchNewServer() {
// 	monkey.Patch(NewServer, httpserver.New(":9093"))
// }

// func PatchRunConsumer() {
// 	var server *endpoints.Server
// 	monkey.PatchInstanceMethod(reflect.TypeOf(server), "RunConsumer",
// 		func(server *endpoints.Server) {})
// }

// func TestClusterUsage(t *testing.T) {
// 	PatchAllContainersByHost()
// 	PatchAllHostsByCluster()
// 	defer monkey.UnpatchAll()
// 	repos := initEndpoint(t).GET("/clusters/cmdb-test/usage").
// 		WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 	repos.Object().Value("success").Boolean().Equal(true)
// 	result := repos.Object().Value("data").Raw()
// 	var usage *apistructs.GetClusterUsageResponseData
// 	bytes, err := json.Marshal(result)
// 	assert.Nil(t, err)
// 	err = json.Unmarshal(bytes, &usage)
// 	assert.Nil(t, err)
// 	// check total resource
// 	assert.Equal(t, usage.TotalCPU, float64(80))
// 	assert.Equal(t, usage.TotalMemory, float64(320))
// 	assert.Equal(t, usage.TotalDisk, float64(1000))
// 	// check used resource
// 	assert.Equal(t, usage.UsedCPU, float64(4))
// 	assert.Equal(t, usage.UsedMemory, float64(100))
// 	assert.Equal(t, usage.UsedDisk, float64(50))
// }

// func TestHostsUsage(t *testing.T) {
// 	PatchAllHostsByCluster()
// 	PatchOneContainerByHost()
// 	defer monkey.UnpatchAll()
// 	repos := initEndpoint(t).GET("/clusters/cmdb-test/hosts-usage").
// 		WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 	repos.Object().Value("success").Boolean().Equal(true)
// 	result := repos.Object().Value("data").Raw()
// 	var usages []*apistructs.GetHostUsageResponseData
// 	bytes, err := json.Marshal(result)
// 	assert.Nil(t, err)
// 	err = json.Unmarshal(bytes, &usages)
// 	assert.Nil(t, err)
// 	// check amount
// 	assert.Equal(t, len(usages), 10)
// 	// check resource
// 	for _, usage := range usages {
// 		assert.Equal(t, usage.TotalCPU, float64(8))
// 		assert.Equal(t, usage.TotalMemory, float64(32))
// 		assert.Equal(t, usage.TotalDisk, float64(100))
// 		assert.Equal(t, usage.UsedCPU, float64(0.4))
// 		assert.Equal(t, usage.UsedMemory, float64(10))
// 		assert.Equal(t, usage.UsedDisk, float64(5))
// 	}
// }

// func TestComponentUsage(t *testing.T) {
// 	PatchAllComponentsByCluster()
// 	defer monkey.UnpatchAll()
// 	repos := initEndpoint(t).GET("/clusters/cmdb-test/instances-usage").
// 		WithQuery("type", "component").
// 		WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 	repos.Object().Value("success").Boolean().Equal(true)
// 	result := repos.Object().Value("data").Raw()
// 	var usages []*apistructs.GetComponentUsageResponseData
// 	bytes, err := json.Marshal(result)
// 	assert.Nil(t, err)
// 	err = json.Unmarshal(bytes, &usages)
// 	assert.Nil(t, err)
// 	// check total number
// 	var amount int
// 	for _, usage := range usages {
// 		amount += usage.Instance
// 		assert.Equal(t, usage.Memory, float64(1024))
// 		assert.Equal(t, usage.CPU, float64(0.04))
// 		assert.Equal(t, usage.Disk, float64(512))
// 	}
// 	assert.Equal(t, amount, 10)
// }

// func TestAddonUsage(t *testing.T) {
// 	PatchAllContainersByAddon()
// 	defer monkey.UnpatchAll()
// 	repos := initEndpoint(t).GET("/clusters/cmdb-test/instances-usage").
// 		WithQuery("type", "addon").
// 		WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 	repos.Object().Value("success").Boolean().Equal(true)
// 	result := repos.Object().Value("data").Raw()
// 	var usages []*apistructs.GetAddOnUsageResponseData
// 	bytes, err := json.Marshal(result)
// 	assert.Nil(t, err)
// 	err = json.Unmarshal(bytes, &usages)
// 	assert.Nil(t, err)
// 	// check total number
// 	var amount int
// 	for _, usage := range usages {
// 		amount += usage.Instance
// 		assert.Equal(t, usage.Memory, float64(1024))
// 		assert.Equal(t, usage.CPU, float64(0.04))
// 		assert.Equal(t, usage.Disk, float64(512))
// 	}
// 	assert.Equal(t, amount, 100)
// }

// func TestProjectUsage(t *testing.T) {
// 	projectID := (*diceContainers)[0].DiceProject
// 	PatchAllContainersByProject(projectID)
//
// 	applicationID := (*diceContainers)[0].DiceApplication
// 	PatchAllContainersByApplication(applicationID)
//
// 	runtimeID := (*diceContainers)[0].DiceRuntime
// 	PatchAllContainersByRuntime(runtimeID)
//
// 	serviceName := (*diceContainers)[0].DiceService
// 	PatchAllContainersByService(serviceName)
//
// 	PatchAllProjectsContainersByCluster()
// 	defer monkey.UnpatchAll()
// 	repos := initEndpoint(t).GET("/clusters/cmdb-test/instances-usage").
// 		WithQuery("type", "project").
// 		WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 	repos.Object().Value("success").Boolean().Equal(true)
// 	result := repos.Object().Value("data").Raw()
// 	var usages []*apistructs.GetProjectUsageResponseData
// 	bytes, err := json.Marshal(result)
// 	assert.Nil(t, err)
// 	err = json.Unmarshal(bytes, &usages)
// 	assert.Nil(t, err)
// 	// check total number
// 	var amount int
// 	for _, usage := range usages {
// 		amount += usage.Instance
// 		assert.Equal(t, usage.Memory, float64(10240))
// 		assert.Equal(t, usage.CPU, float64(0.4))
// 		assert.Equal(t, usage.Disk, float64(5120))
// 	}
// 	assert.Equal(t, amount, 100)
//
// 	{
// 		repos := initEndpoint(t).GET("/clusters/cmdb-test/instances-usage").
// 			WithQuery("type", "application").
// 			WithQuery("project", projectID).
// 			WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 		repos.Object().Value("success").Boolean().Equal(true)
// 		result := repos.Object().Value("data").Raw()
// 		var usages []*apistructs.GetApplicationUsageResponseData
// 		bytes, err := json.Marshal(result)
// 		assert.Nil(t, err)
// 		err = json.Unmarshal(bytes, &usages)
// 		assert.Nil(t, err)
// 		// check total number
// 		var amount int
// 		for _, usage := range usages {
// 			amount += usage.Instance
// 			assert.Equal(t, usage.Memory, float64(1024))
// 			assert.Equal(t, usage.CPU, float64(0.04))
// 			assert.Equal(t, usage.Disk, float64(512))
// 		}
// 		assert.Equal(t, amount, 10)
// 	}
//
// 	{
// 		repos := initEndpoint(t).GET("/clusters/cmdb-test/instances-usage").
// 			WithQuery("type", "runtime").
// 			WithQuery("project", projectID).
// 			WithQuery("application", applicationID).
// 			WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 		repos.Object().Value("success").Boolean().Equal(true)
// 		result := repos.Object().Value("data").Raw()
// 		var usages []*apistructs.GetRuntimeUsageResponseData
// 		bytes, err := json.Marshal(result)
// 		assert.Nil(t, err)
// 		err = json.Unmarshal(bytes, &usages)
// 		assert.Nil(t, err)
// 		// check total number
// 		var amount int
// 		for _, usage := range usages {
// 			amount += usage.Instance
// 			assert.Equal(t, usage.Memory, float64(1024))
// 			assert.Equal(t, usage.CPU, float64(0.04))
// 			assert.Equal(t, usage.Disk, float64(512))
// 		}
// 		assert.Equal(t, amount, 1)
// 	}
//
// 	{
// 		repos := initEndpoint(t).GET("/clusters/cmdb-test/instances-usage").
// 			WithQuery("type", "service").
// 			WithQuery("project", projectID).
// 			WithQuery("application", applicationID).
// 			WithQuery("runtime", runtimeID).
// 			WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 		repos.Object().Value("success").Boolean().Equal(true)
// 		result := repos.Object().Value("data").Raw()
// 		var usages []*apistructs.GetServiceUsageResponseData
// 		bytes, err := json.Marshal(result)
// 		assert.Nil(t, err)
// 		err = json.Unmarshal(bytes, &usages)
// 		assert.Nil(t, err)
// 		// check total number
// 		var amount int
// 		for _, usage := range usages {
// 			amount += usage.Instance
// 			assert.Equal(t, usage.Memory, float64(1024))
// 			assert.Equal(t, usage.CPU, float64(0.04))
// 			assert.Equal(t, usage.Disk, float64(512))
// 		}
// 		assert.Equal(t, amount, 1)
// 	}
//
// 	{
// 		repos := initEndpoint(t).GET("/clusters/cmdb-test/instances").
// 			WithQuery("type", "service").
// 			WithQuery("project", projectID).
// 			WithQuery("application", applicationID).
// 			WithQuery("runtime", runtimeID).
// 			WithQuery("service", serviceName).
// 			WithHeaders(header).Expect().Status(http.StatusOK).JSON()
// 		repos.Object().Value("success").Boolean().Equal(true)
// 		result := repos.Object().Value("data").Raw()
// 		var containers []*types.CmContainer
// 		bytes, err := json.Marshal(result)
// 		assert.Nil(t, err)
// 		err = json.Unmarshal(bytes, &containers)
// 		assert.Nil(t, err)
// 		// check total number
// 		for _, container := range containers {
// 			assert.Equal(t, container.Memory, float64(1024))
// 			assert.Equal(t, container.CPU, float64(0.04))
// 			assert.Equal(t, container.Disk, float64(512))
// 			assert.Equal(t, container.DiceProject, projectID)
// 			assert.Equal(t, container.DiceApplication, applicationID)
// 			assert.Equal(t, container.DiceRuntime, runtimeID)
// 			assert.Equal(t, container.DiceService, serviceName)
// 		}
// 	}
// }
