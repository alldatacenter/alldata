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

package router

import "fmt"

func ExamplePrint() {
	r := New()
	r.Add("application_*", nil, "key1")
	r.Add("application_*", []*KeyValue{
		{
			Key:   "terminus_key",
			Value: "xxxx",
		},
	}, "key2")
	r.Add("application_*", []*KeyValue{
		{
			Key:   "terminus_key",
			Value: "yyyy",
		},
		{
			Key:   "env",
			Value: "abc",
		},
	}, "key3")
	r.Add("application_http", []*KeyValue{
		{
			Key:   "terminus_key",
			Value: "xxxxxxx",
		},
	}, "key4")
	r.Add("application_db", []*KeyValue{
		{
			Key:   "terminus_key",
			Value: "xxxxxxx",
		},
	}, "key5")
	r.Add("*_db", []*KeyValue{
		{
			Key:   "terminus_key",
			Value: "xxxxxxx",
		},
	}, "key6")
	r.Add("docker_container_*", []*KeyValue{
		{
			Key:   "terminus_key",
			Value: "xxxxxxx",
		},
	}, "key7")
	r.Add("docker_*_mem", []*KeyValue{
		{
			Key:   "terminus_key",
			Value: "xxxxxxx",
		},
	}, "key8")
	r.Add("*", nil, "key9")
	r.Add("*", []*KeyValue{
		{
			Key:   "cluster_name",
			Value: "c1",
		},
	}, "key10")
	r.PrintTree(false)

	printFind(r, "app_not_exist", nil) // key9

	printFind(r, "app_not_exist", map[string]string{
		"terminus_key": "xxxxxxx",
	}) // key9

	printFind(r, "app_not_exist", map[string]string{
		"cluster_name": "c1",
	}) // key10

	printFind(r, "application_xxx", map[string]string{
		"terminus_key": "xxxx",
		"test_key":     "test_value",
	}) // key2

	printFind(r, "application_xxx", map[string]string{
		"terminus_key": "not_match",
		"test_key":     "test_value",
	}) // key1

	printFind(r, "docker_container_mem", map[string]string{
		"terminus_key": "xxxxxxx",
	}) // key7

	printFind(r, "docker_xxxx_mem", map[string]string{
		"terminus_key": "xxxxxxx",
	}) // key8

	printFind(r, "docker_xxxx_mem", map[string]string{
		"terminus_key": "not_exist",
	}) // key9

	// Output:
	// ├── *: kind=1, target=key9
	// │   ├── application_: kind=0, target=<nil>
	// │   │   ├── *: kind=1, target=key1
	// │   │   │   └── terminus_key: kind=2, target=<nil>
	// │   │   │       ├── xxxx: kind=3, target=key2
	// │   │   │       └── yyyy: kind=3, target=<nil>
	// │   │   │           └── env: kind=2, target=<nil>
	// │   │   │               └── abc: kind=3, target=key3
	// │   │   ├── http: kind=0, target=<nil>
	// │   │   │   └── terminus_key: kind=2, target=<nil>
	// │   │   │       └── xxxxxxx: kind=3, target=key4
	// │   │   └── db: kind=0, target=<nil>
	// │   │       └── terminus_key: kind=2, target=<nil>
	// │   │           └── xxxxxxx: kind=3, target=key5
	// │   ├── _db: kind=0, target=<nil>
	// │   │   └── terminus_key: kind=2, target=<nil>
	// │   │       └── xxxxxxx: kind=3, target=key6
	// │   ├── docker_: kind=0, target=<nil>
	// │   │   ├── container_: kind=0, target=<nil>
	// │   │   │   └── *: kind=1, target=<nil>
	// │   │   │       └── terminus_key: kind=2, target=<nil>
	// │   │   │           └── xxxxxxx: kind=3, target=key7
	// │   │   └── *: kind=1, target=<nil>
	// │   │       └── _mem: kind=0, target=<nil>
	// │   │           └── terminus_key: kind=2, target=<nil>
	// │   │               └── xxxxxxx: kind=3, target=key8
	// │   └── cluster_name: kind=2, target=<nil>
	// │       └── c1: kind=3, target=key10
	// app_not_exist map[] -> key9
	// app_not_exist map[terminus_key:xxxxxxx] -> key9
	// app_not_exist map[cluster_name:c1] -> key10
	// application_xxx map[terminus_key:xxxx test_key:test_value] -> key2
	// application_xxx map[terminus_key:not_match test_key:test_value] -> key1
	// docker_container_mem map[terminus_key:xxxxxxx] -> key7
	// docker_xxxx_mem map[terminus_key:xxxxxxx] -> key8
	// docker_xxxx_mem map[terminus_key:not_exist] -> key9
}

func printFind(r *Router, key string, kvs map[string]string) interface{} {
	result := r.Find(key, kvs)
	fmt.Println(key, kvs, "->", result)
	return result
}

func ExamplePrint_application_http() {
	r := New()
	key := "2068c6f11ccfa3e8"
	r.Add("application_*", []*KeyValue{
		{
			Key:   "target_terminus_key",
			Value: "bd717ad15bc8542588bde9ff0c7b4cf78",
		},
	}, key)
	r.Add("application_*", []*KeyValue{
		{
			Key:   "source_terminus_key",
			Value: "bd717ad15bc8542588bde9ff0c7b4cf78",
		},
	}, key)
	r.PrintTree(false)

	name := "application_http"
	kvs := map[string]string{
		"_meta":                      "true",
		"_metric_scope":              "micro_service",
		"_metric_scope_id":           "bd717ad15bc8542588bde9ff0c7b4cf78",
		"cluster_name":               "terminus-test",
		"component":                  "Http",
		"host":                       "10.123.254.40:8095",
		"host_ip":                    "10.0.6.198",
		"http_method":                "GET",
		"http_path":                  "/health/check",
		"http_status_code":           "200",
		"http_url":                   "http://10.123.254.40:8095/health/check",
		"org_name":                   "mobile",
		"peer_hostname":              "10.0.6.198",
		"span_kind":                  "server",
		"target_application_id":      "1",
		"target_application_name":    "apm-demo",
		"target_org_id":              "1",
		"target_project_id":          "1",
		"target_project_name":        "dice",
		"target_runtime_id":          "1",
		"target_runtime_name":        "feature/simple",
		"target_service_id":          "1_feature/simple_apm-demo-api",
		"target_service_instance_id": "f3da4d5f-d035-4d15-993f-e88171d6b140",
		"target_service_name":        "apm-demo-api",
		"target_terminus_key":        "bd717ad15bc8542588bde9ff0c7b4cf78",
		"target_workspace":           "DEV",
	}
	for i := 0; i < 10000; i++ {
		result := r.Find(name, kvs)
		if result != key {
			fmt.Println(name, "->", result)
		}
	}

	// Output:
	// ├── *: kind=1, target=<nil>
	// │   └── application_: kind=0, target=<nil>
	// │       └── *: kind=1, target=<nil>
	// │           ├── target_terminus_key: kind=2, target=<nil>
	// │           │   └── bd717ad15bc8542588bde9ff0c7b4cf78: kind=3, target=2068c6f11ccfa3e8
	// │           └── source_terminus_key: kind=2, target=<nil>
	// │               └── bd717ad15bc8542588bde9ff0c7b4cf78: kind=3, target=2068c6f11ccfa3e8
}
