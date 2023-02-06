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

package customhttp

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/cache"
	"github.com/erda-project/erda/pkg/discover"
)

var (
	ErrInvalidAddr = Error{"customhttp: invalid inetaddr"}
	ipCache        = cache.New("clusterManagerEndpoint", time.Second*30, queryClusterManagerIP)
)

type Error struct {
	msg string
}

func (e Error) Error() string {
	return e.msg
}

const (
	portalHostHeader = "X-Portal-Host"
	portalDestHeader = "X-Portal-Dest"
)

func parseInetUrl(url string) (portalHost string, portalDest string, portalUrl string, portalArgs map[string]string, err error) {
	url = strings.TrimPrefix(url, "inet://")
	url = strings.Replace(url, "//", "/", -1)
	portalArgs = map[string]string{}
	parts := strings.SplitN(url, "/", 3)
	if len(parts) < 2 {
		err = errors.Wrapf(ErrInvalidAddr, "addr:%s", url)
		return
	}
	portalHost = parts[0]
	portalDest = parts[1]
	portalUrl = ""
	if len(parts) > 2 {
		portalUrl = parts[2]
	}
	portalArgsPos := strings.Index(portalHost, "?")
	if portalArgsPos == -1 {
		return
	}
	argStr := portalHost[portalArgsPos+1:]
	portalHost = portalHost[:portalArgsPos]
	argPairs := strings.Split(argStr, "&")
	for _, pair := range argPairs {
		argParts := strings.Split(pair, "=")
		if len(argParts) == 2 {
			portalArgs[argParts[0]] = argParts[1]
		}
	}
	return
}

func GetNetPortalUrl(clusterName string) string {
	return "inet://" + clusterName
}

func NewRequest(method, url string, body io.Reader) (*http.Request, error) {
	if !strings.HasPrefix(url, "inet://") {
		return http.NewRequest(method, url, body)
	}
	portalHost, portalDest, portalUrl, _, err := parseInetUrl(url)
	if err != nil {
		return nil, err
	}

	clusterManagerEndpoint, ok := ipCache.LoadWithUpdateSync(portalHost)
	if !ok {
		logrus.Errorf("failed to get clusterManager endpoint for portal host %s", portalHost)
		return nil, errors.Errorf("failed to get clusterManager endpoint for portal host %s", portalHost)
	}
	logrus.Infof("[DEBUG] get cluster manager endpoint succeeded, IP: %s", clusterManagerEndpoint)

	url = fmt.Sprintf("http://%s/%s", clusterManagerEndpoint, portalUrl)
	request, err := http.NewRequest(method, url, body)
	if err != nil {
		return nil, errors.WithStack(err)
	}
	request.Header.Set(portalHostHeader, portalHost)
	request.Header.Set(portalDestHeader, portalDest)
	return request, nil
}

func queryClusterManagerIP(clusterKey interface{}) (interface{}, bool) {
	log := logrus.WithField("func", "NetPortal NewRequest")
	log.Debug("start querying clusterManager IP in netPortal NewRequest...")

	splits := strings.Split(discover.ClusterDialer(), ":")
	if len(splits) != 2 {
		log.Errorf("invalid clusterManager addr: %s", discover.ClusterDialer())
		return "", false
	}
	addr := splits[0]
	port := splits[1]
	host := fmt.Sprintf("http://%s:%s", addr, port)
	resp, err := http.Get(host + fmt.Sprintf("/clusterdialer/ip?clusterKey=%s", clusterKey))
	if err != nil {
		log.Errorf("failed to request clsuterdialer in cache updating in netPortal NewRequest, %v", err)
		return "", false
	}

	data, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Errorf("failed to read from resp in cache updating , %v", err)
		return "", false
	}
	r := make(map[string]interface{})
	if err = json.Unmarshal(data, &r); err != nil {
		log.Errorf("failed to unmarshal resp, %v", err)
		return "", false
	}

	succeeded, _ := r["succeeded"].(bool)
	if !succeeded {
		errStr, _ := r["error"].(string)
		log.Errorf("reutrn error from clusterManager in cache updating, %s", errStr)
		return "", false
	}

	ip, _ := r["IP"].(string)
	return fmt.Sprintf("%s:%s", ip, port), true
}
