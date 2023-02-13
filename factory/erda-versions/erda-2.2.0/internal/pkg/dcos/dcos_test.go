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

package dcos

import (
	"encoding/json"
	"fmt"
	"os"
	"testing"
	"time"
)

func TestGetVersion(t *testing.T) {
	masterIP := os.Getenv("MASTER_IP")
	if masterIP == "" {
		t.SkipNow()
	}
	v, err := GetVersion(masterIP)
	if err != nil {
		t.Fatal(err)
	}
	t.Log(v)
}

func TestGetApps(t *testing.T) {
	masterIP := os.Getenv("MASTER_IP")
	if masterIP == "" {
		t.SkipNow()
	}
	a, err := GetApps(masterIP)
	if err != nil {
		t.Fatal(err)
	}
	OutputApps(a)
}

func TestRestartAndGetApp(t *testing.T) {
	masterIP := os.Getenv("MASTER_IP")
	if masterIP == "" {
		t.SkipNow()
	}
	appID := os.Getenv("APP_ID")
	if appID == "" {
		t.SkipNow()
	}

	deploymentId, err := RestartApp(masterIP, appID)
	if err != nil {
		t.Fatal(err)
	}
	fmt.Println(appID, deploymentId)

	time.Sleep(time.Second)

	m, err := GetApp(masterIP, appID)
	if err != nil {
		t.Fatal(err)
	}
	if appID != m["id"].(string) {
		t.Fatal(appID)
	}
	b, err := json.MarshalIndent(m, "", "\t")
	if err != nil {
		t.Fatal(err)
	}
	fmt.Println(string(b))
}
