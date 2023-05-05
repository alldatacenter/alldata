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

package query

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/olivere/elastic"
	"github.com/recallsong/go-utils/encoding/jsonx"
	"github.com/recallsong/go-utils/reflectx"

	mspDb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/tools/monitor/extensions/loghub/index/query/db"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

// log versions
const (
	LogVersion1 = "1.0.0"
	LogVersion2 = "2.0.0"
)

// ESClient .
type ESClient struct {
	*elastic.Client
	Cluster    string
	URLs       string
	LogVersion string
	Indices    []string
	Entrys     []*IndexEntry
}

func (c *ESClient) printSearchSource(searchSource *elastic.SearchSource) (string, error) {
	source, err := searchSource.Source()
	if err != nil {
		return "", fmt.Errorf("invalid search source: %s", err)
	}
	body := jsonx.MarshalAndIndent(source)
	body = c.URLs + "\n" + strings.Join(c.Indices, ",") + "\n" + body
	fmt.Println(body)
	return body, nil
}

func (p *provider) getAllESClients() []*ESClient {
	list, err := p.db.LogDeployment.List()
	if err != nil {
		return nil
	}
	return p.getESClientsFromLogAnalyticsByLogDeployment("", list...)
}

func (p *provider) getESClients(orgID int64, req *LogRequest) []*ESClient {
	if len(req.ClusterName) > 0 || len(req.Addon) > 0 {
		if len(req.ClusterName) <= 0 || len(req.Addon) <= 0 {
			return nil
		}
		clients := p.getESClientsFromLogAnalyticsByCluster(orgID, strings.ReplaceAll(req.Addon, "*", ""), req.ClusterName)
		return clients
	}
	filters := make(map[string]string)
	for _, item := range req.Filters {
		filters[item.Key] = item.Value
	}
	if filters["origin"] == "sls" {
		return p.getCenterESClients("sls-*")
	} else if filters["origin"] == "dice" {
		clients := p.getESClientsFromLogAnalytics(orgID)
		if len(clients) <= 0 {
			return p.getCenterESClients("rlogs-*")
		}
		return clients
	} else if filters["origin"] != "" {
		return p.getCenterESClients("__not-exist__*")
	}
	clients := append(p.getCenterESClients("sls-*"), p.getESClientsFromLogAnalytics(orgID)...)
	return clients
}

func (p *provider) getCenterESClients(indices ...string) []*ESClient {
	if p.C.QueryBackES {
		return []*ESClient{
			{Client: p.client, URLs: "-", Indices: indices},
			{Client: p.backClient, URLs: "-b", Indices: indices},
		}
	}
	return []*ESClient{
		{Client: p.client, URLs: "-", Indices: indices},
	}
}

func (p *provider) getESClientsFromLogAnalytics(orgID int64) []*ESClient {
	clusters, err := p.bdl.ListClusters("", uint64(orgID))
	if err != nil {
		return nil
	}
	var clusterNames []string
	for _, c := range clusters {
		clusterNames = append(clusterNames, c.Name)
	}
	return p.getESClientsFromLogAnalyticsByCluster(orgID, "", clusterNames...)
}

func (p *provider) getESClientsFromLogAnalyticsByCluster(orgID int64, addon string, clusterNames ...string) []*ESClient {
	list, err := p.db.LogDeployment.QueryByOrgIDAndClusters(orgID, clusterNames...)
	if err != nil {
		return nil
	}
	return p.getESClientsFromLogAnalyticsByLogDeployment(addon, list...)
}

func (p *provider) getESClientsFromLogAnalyticsByLogDeployment(addon string, logDeployments ...*db.LogDeployment) []*ESClient {
	type ESConfig struct {
		Security bool   `json:"securityEnable"`
		Username string `json:"securityUsername"`
		Password string `json:"securityPassword"`
	}
	var indices map[string]map[string][]*IndexEntry
	if p.C.IndexPreload {
		v := p.indices.Load()
		if v != nil {
			indices = v.(map[string]map[string][]*IndexEntry)
		}
	}
	var clients []*ESClient
	for _, d := range logDeployments {
		if len(d.ESURL) <= 0 {
			continue
		}

		// get all other addons in same cluster, project and workspace
		var addons []string
		if len(addon) > 0 {
			logInstance, err := p.db.LogInstanceDB.GetByLogKey(addon)
			if err != nil {
				p.L.Warnf("fail to get logInstance for logKey: %s", addon)
			}
			if logInstance != nil {
				keyCaches := map[string]bool{}
				sameGroupLogInstances, err := p.db.LogInstanceDB.
					GetListByClusterAndProjectIdAndWorkspace(logInstance.ClusterName, logInstance.ProjectId, logInstance.Workspace)
				if err != nil {
					p.L.Warnf("fail to get logInstance")
				}
				for _, instance := range sameGroupLogInstances {
					logKey := instance.LogKey
					if instance.LogType == string(mspDb.LogTypeLogService) {
						var instanceConfig = struct {
							MspEnvID string `json:"MSP_ENV_ID"`
						}{}

						json.Unmarshal([]byte(instance.Config), &instanceConfig)
						logKey = instanceConfig.MspEnvID
					}

					if _, ok := keyCaches[logKey]; ok {
						continue
					}
					addons = append(addons, logKey)
					keyCaches[logKey] = true
				}
			} else {
				addons = append(addons, addon)
			}
		}

		options := []elastic.ClientOptionFunc{
			elastic.SetURL(strings.Split(d.ESURL, ",")...),
			elastic.SetSniff(false),
			elastic.SetHealthcheck(false),
		}
		if len(d.ESConfig) > 0 {
			var cfg ESConfig
			err := json.Unmarshal(reflectx.StringToBytes(d.ESConfig), &cfg)
			if err == nil {
				if cfg.Security && (cfg.Username != "" || cfg.Password != "") {
					options = append(options, elastic.SetBasicAuth(cfg.Username, cfg.Password))
				}
			}
		}
		if d.ClusterType == 1 {
			options = append(options, elastic.SetHttpClient(newHTTPClient(d.ClusterName)))
		}

		orgId := d.OrgID
		if d.LogType == string(mspDb.LogTypeLogAnalytics) {
			// omit the orgId alias, if deployed by log-analytics addon，specially for old versions, there's no orgId alias
			orgId = ""
		}

		client, err := elastic.NewClient(options...)
		if err != nil {
			p.L.Errorf("failed to create elasticsearch client: %s", err)
			continue
		}
		d.CollectorURL = strings.TrimSpace(d.CollectorURL)
		if len(d.CollectorURL) > 0 || d.LogType == string(mspDb.LogTypeLogService) {
			c := &ESClient{
				Client:     client,
				Cluster:    d.ClusterName,
				LogVersion: LogVersion2,
				URLs:       d.ESURL,
				Indices:    getLogIndices("rlogs-", orgId, addons...),
			}
			clients = append(clients, c)

			if p.C.IndexPreload && indices != nil && len(addons) > 0 {
				cacheKey := p.calcIndexCacheBucketKey(c.Cluster, c.URLs)
				if indexAddons, ok := indices[cacheKey]; ok {
					for _, addon := range addons {
						c.Entrys = append(c.Entrys, indexAddons[addon]...)
					}
				}
			}
		} else {
			clients = append(clients, &ESClient{
				Client:     client,
				Cluster:    d.ClusterName,
				LogVersion: LogVersion1,
				URLs:       d.ESURL,
				Indices:    getLogIndices("spotlogs-", orgId, addons...),
			})
		}
	}
	return clients
}

func getLogIndices(prefix, orgId string, addons ...string) []string {
	if len(addons) > 0 {
		var indices []string
		for _, addon := range addons {
			indices = append(indices, prefix+addon, prefix+addon+"-*")
		}
		return indices
	}
	if len(orgId) > 0 {
		return []string{prefix + "org-" + orgId}
	}
	return []string{prefix + "*"}
}

func newHTTPClient(clusterName string) *http.Client {
	hclient := httpclient.New(httpclient.WithClusterDialer(clusterName))
	t := hclient.BackendClient().Transport.(*http.Transport)
	return &http.Client{
		Transport: &http.Transport{
			Proxy:                 http.ProxyFromEnvironment,
			DialContext:           t.DialContext,
			ForceAttemptHTTP2:     true,
			MaxIdleConns:          -1,
			IdleConnTimeout:       90 * time.Second,
			TLSHandshakeTimeout:   10 * time.Second,
			ExpectContinueTimeout: 1 * time.Second,
		},
	}
}
