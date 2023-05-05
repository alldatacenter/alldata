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

package registryhelper

import (
	"fmt"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/http/httpclient"
)

type RemoveManifestsRequest struct {
	RegistryURL string
	Images      []string
	ClusterKey  string
}

type RemoveManifestsResponse struct {
	Succeed []string
	Failed  map[string]string
}

func (req RemoveManifestsRequest) removeManifests(s, clusterKey string) string {
	var name, tag string
	if i := strings.IndexByte(s, '/'); i != -1 {
		name = s[i+1:]
		if i := strings.LastIndexByte(name, ':'); i != -1 {
			name, tag = name[:i], name[i+1:]
		}
	}
	if name == "" {
		return "image name is empty"
	}
	if tag == "" {
		tag = "latest"
	}
	c := httpclient.New(httpclient.WithClusterDialer(clusterKey))
	res, err := c.Get(req.RegistryURL).Path(fmt.Sprintf("/v2/%s/manifests/%s", name, tag)).
		Header("Content-Type", "application/json").
		Header("Accept", "application/vnd.docker.distribution.manifest.v2+json").
		Do().DiscardBody()
	if err != nil {
		return "get manifests failed: " + err.Error()
	}
	if sc := res.StatusCode(); sc != http.StatusOK {
		if sc == http.StatusNotFound { // does not exist
			return ""
		} else {
			return "get manifests failed: status code is " + strconv.Itoa(sc)
		}
	}
	dcd := res.ResponseHeader("Docker-Content-Digest")
	if dcd == "" {
		return "get manifests failed: header Docker-Content-Digest is empty"
	}
	res, err = c.Delete(req.RegistryURL).Path(fmt.Sprintf("/v2/%s/manifests/%s", name, dcd)).
		Header("Content-Type", "application/json").
		Do().DiscardBody()
	if err != nil {
		return "delete manifests failed: " + err.Error()
	}
	if sc := res.StatusCode(); sc != http.StatusOK && sc != http.StatusAccepted {
		return "delete manifests failed: status code is " + strconv.Itoa(sc)
	}
	return ""
}

func RemoveManifests(req RemoveManifestsRequest) (*RemoveManifestsResponse, error) {
	if req.RegistryURL == "" {
		req.RegistryURL = os.Getenv("REGISTRY_ADDR")
	}
	if req.RegistryURL == "" {
		return nil, errors.New("no registry url")
	}

	m := make(map[string]string, len(req.Images))
	for _, image := range req.Images {
		if _, ok := m[image]; !ok {
			m[image] = req.removeManifests(image, req.ClusterKey)
		}
	}
	var res RemoveManifestsResponse
	for k, v := range m {
		if v == "" {
			logrus.Infof("%s: delete image succeed\n", k)
			res.Succeed = append(res.Succeed, k)
		} else {
			if res.Failed == nil {
				res.Failed = make(map[string]string)
			}
			logrus.Warningf("%s: %s\n", k, v)
			res.Failed[k] = v
		}
	}
	return &res, nil
}

type State struct {
	Running   bool      `json:"running"`
	StartTime time.Time `json:"startTime"`
	EndTime   time.Time `json:"endTime"`
	LastError string    `json:"lastError"`
}

type StateReply struct {
	Success bool   `json:"success"`
	Data    *State `json:"data"`
	Err     *struct {
		Code string      `json:"code"`
		Msg  string      `json:"msg"`
		Ctx  interface{} `json:"ctx"`
	} `json:"err"`
}

func GetGcState(registryctlURL, clusterKey string) (*State, error) {
	if registryctlURL == "" {
		registryctlURL = os.Getenv("REGISTRY_ADDR")
		if registryctlURL != "" {
			i := strings.LastIndexByte(registryctlURL, ':')
			if i != -1 {
				registryctlURL = registryctlURL[0:i]
			}
			registryctlURL = registryctlURL + ":5050"
		}
	}
	if registryctlURL == "" {
		return nil, errors.New("no registryctl url")
	}

	var v StateReply
	c := httpclient.New(httpclient.WithClusterDialer(clusterKey))
	res, err := c.Get(registryctlURL).Path("/gc").Do().JSON(&v)
	if err != nil {
		return nil, err
	}
	if sc := res.StatusCode(); sc != http.StatusOK {
		return nil, errors.Errorf("get gc state failed: status code is:%d", sc)
	}
	if !v.Success {
		return nil, errors.Errorf("get gc state failed:%s", v.Err.Msg)
	}
	return v.Data, nil
}
