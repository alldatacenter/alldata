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

package discover

import (
	"context"
	"fmt"
	"net"
	"os"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

type endpointInfo struct {
	endpoint  string
	expiresAt time.Time
}

const (
	defaultTimeout = 60 * time.Second
)

var endpointMap = &map[string]endpointInfo{}
var mutex sync.Mutex

func init() {
	go func() {
		for range time.Tick(defaultTimeout / 2) {
			if len(*endpointMap) == 0 {
				continue
			}
			mutex.Lock()
			newEndpointMap := map[string]endpointInfo{}
			for key, value := range *endpointMap {
				if time.Now().Before(value.expiresAt) {
					newEndpointMap[key] = value
					continue
				}
				endpoint, err := resolveEndpoint(key)
				if err != nil {
					logrus.Errorf("resolve endpoint failed, err:%+v", err)
					continue
				}
				newEndpointMap[key] = endpointInfo{
					endpoint:  endpoint,
					expiresAt: time.Now().Add(defaultTimeout),
				}
			}
			endpointMap = &newEndpointMap
			mutex.Unlock()
		}
	}()
}

func setEndpoint(serviceName, endpoint string) {
	mutex.Lock()
	defer mutex.Unlock()
	newEndpointMap := map[string]endpointInfo{}
	for key, value := range *endpointMap {
		newEndpointMap[key] = value
	}
	newEndpointMap[serviceName] = endpointInfo{
		endpoint:  endpoint,
		expiresAt: time.Now().Add(defaultTimeout),
	}
	endpointMap = &newEndpointMap
}

func getEndpoint(serviceName string) (string, bool) {
	endpoint, ok := (*endpointMap)[serviceName]
	return endpoint.endpoint, ok
}

func resolveEndpoint(serviceName string) (endpoint string, err error) {
	_, addrs, err := net.DefaultResolver.LookupSRV(context.Background(), "", "", serviceName)
	if err != nil {
		err = errors.Wrapf(err, "resolve service endpoint failed, service:%s", serviceName)
		return
	}
	if len(addrs) == 0 || addrs[0] == nil {
		err = errors.Errorf("can't find service endpoint, service:%s", serviceName)
		return
	}
	endpoint = fmt.Sprintf("%s:%d", strings.TrimSuffix(addrs[0].Target, "."), addrs[0].Port)
	logrus.Debugf("service: %s endpoint acquired: %s", serviceName, endpoint)
	return
}

func GetEndpoint(serviceName string) (endpoint string, err error) {
	if envKey, ok := ServicesEnvKeys[serviceName]; ok {
		v := os.Getenv(envKey)
		if v != "" {
			return v, nil
		}
	}
	endpoint, ok := getEndpoint(serviceName)
	if ok {
		return
	}
	endpoint, err = resolveEndpoint(serviceName)
	if err != nil {
		logrus.Infof("get endpoint failed, service name: %s, error: %s", serviceName, err)
		return
	}
	go setEndpoint(serviceName, endpoint)
	return
}
