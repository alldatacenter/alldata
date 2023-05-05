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

package util

import (
	"encoding/base64"
	"fmt"
	"sort"
	"strconv"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type serviceDepends map[string]struct{}

func ParseServiceDependency(runtime *apistructs.ServiceGroup) ([][]*apistructs.Service, error) {
	if runtime == nil {
		return nil, errors.New("runtime could not be nil")
	}
	services := runtime.Services
	serviceMap := make(map[string]*apistructs.Service)

	svcSet := make(map[string]serviceDepends, len(services))
	for i, svc := range services {
		if _, ok := svcSet[svc.Name]; ok {
			return nil, errors.New("duplicate service name in runtime")
		}
		depends := serviceDepends{}
		for _, d := range svc.Depends {
			depends[d] = struct{}{}
		}
		svcSet[svc.Name] = depends
		serviceMap[svc.Name] = &services[i]
	}

	// Check if the dependency is in the service collection
	for _, depends := range svcSet {
		for d := range depends {
			if _, ok := svcSet[d]; !ok {
				return nil, errors.Errorf("not found service: %s, it's in depends but not in runtime services", d)
			}
		}
	}

	handleDepends := func(svcDepMap map[string]serviceDepends) ([]string, error) {
		independents := make([]string, 0)

		// Find out the service that has no dependencies
		for name, depends := range svcDepMap {
			if len(depends) == 0 {
				independents = append(independents, name)
			}
		}

		// If you can't find a non-dependent service, there is an infinite loop.
		if len(independents) == 0 {
			return nil, errors.New("dead loop in the service dependency")
		}

		// Clear this batch of non-dependent services
		for _, name := range independents {
			delete(svcDepMap, name)
		}

		// Cleanup dependencies
		for _, name := range independents {
			for job, depends := range svcDepMap {
				delete(depends, name)
				svcDepMap[job] = depends
			}
		}

		return independents, nil
	}

	layers := make([][]*apistructs.Service, 0, len(services))
	for len(svcSet) != 0 {
		// Sort out the nodes with degree 0 in the directed acyclic graph, and delete these nodes from svcSet
		independents, err := handleDepends(svcSet)
		if err != nil {
			return nil, err
		}
		sort.Strings(independents)

		// Services in the same layer can be created in parallel
		svcLayer := make([]*apistructs.Service, 0, len(independents))
		for _, name := range independents {
			if svcAddr, ok := serviceMap[name]; ok {
				svcLayer = append(svcLayer, svcAddr)
			}
		}
		if len(svcLayer) != 0 {
			layers = append(layers, svcLayer)
		}
	}

	return layers, nil
}

func ParseEnableTagOption(options map[string]string, key string, defaultValue bool) (bool, error) {
	enableTagStr, ok := options[key]
	if !ok {
		return defaultValue, nil
	}
	enableTag, err := strconv.ParseBool(enableTagStr)
	if err != nil {
		return false, err
	}
	return enableTag, nil
}

func ParsePreserveProjects(options map[string]string, key string) map[string]struct{} {
	ret := make(map[string]struct{})
	projectsStr, ok := options[key]
	if !ok {
		return map[string]struct{}{}
	}
	projects := splitTags(projectsStr)
	for _, p := range projects {
		ret[p] = struct{}{}
	}
	return ret
}

func BuildDcosConstraints(enable bool, labels map[string]string, preserveProjects map[string]struct{}, workspaceTags map[string]struct{}) [][]string {
	if !enable {
		return [][]string{}
	}
	matchTags := splitTags(labels[apistructs.LabelMatchTags])
	excludeTags := splitTags(labels[apistructs.LabelExcludeTags])
	var cs [][]string
	anyTagDisable := false
	if projectId, ok := labels["DICE_PROJECT"]; ok {
		_, exists := preserveProjects[projectId]
		if exists {
			anyTagDisable = true
			cs = append(cs, []string{"dice_tags", "LIKE", `.*\b` + apistructs.TagProjectPrefix + projectId + `\b.*`})
		} else {
			cs = append(cs, []string{"dice_tags", "UNLIKE", `.*\b` + apistructs.TagProjectPrefix + `[^,]+` + `\b.*`})
		}
	}

	if envTag, ok := labels["DICE_WORKSPACE"]; ok {
		_, exists := workspaceTags[envTag]
		if exists {
			cs = append(cs, []string{"dice_tags", "LIKE", `.*\b` + apistructs.TagWorkspacePrefix + envTag + `\b.*`})
			anyTagDisable = true
		} else {
			cs = append(cs, []string{"dice_tags", "UNLIKE", `.*\b` + apistructs.TagWorkspacePrefix + `[^,]+` + `\b.*`})
		}
	}
	if len(matchTags) == 0 && !anyTagDisable {
		cs = append(cs, []string{"dice_tags", "LIKE", `.*\bany\b.*`})
	} else if len(matchTags) != 0 && anyTagDisable {
		for _, matchTag := range matchTags {
			cs = append(cs, []string{"dice_tags", "LIKE", `.*\b` + matchTag + `\b.*`})
		}
	} else if len(matchTags) != 0 && !anyTagDisable {
		for _, matchTag := range matchTags {
			// The bigdata tag does not coexist with any
			if matchTag == "bigdata" {
				cs = append(cs, []string{"dice_tags", "LIKE", `.*\b` + matchTag + `\b.*`})
			} else {
				cs = append(cs, []string{"dice_tags", "LIKE", `.*\b` + apistructs.TagAny + `\b.*|.*\b` + matchTag + `\b.*`})
			}
		}
	}
	for _, excludeTag := range excludeTags {
		cs = append(cs, []string{"dice_tags", "UNLIKE", `.*\b` + excludeTag + `\b.*`})
	}
	return cs
}

func CombineLabels(parent, child map[string]string) map[string]string {
	ret := make(map[string]string)
	for k, v := range parent {
		ret[k] = v
	}
	for k, v := range child {
		ret[k] = v
	}
	return ret
}

func splitTags(tagStr string) []string {
	return strings.FieldsFunc(tagStr, func(c rune) bool {
		return c == ','
	})
}

func IsNotFound(err error) bool {
	if strings.Contains(err.Error(), "not found") {
		return true
	}
	return false
}

// GetClient get http client with cluster info.
func GetClient(clusterName string, manageConfig *apistructs.ManageConfig) (string, *httpclient.HTTPClient, error) {
	if manageConfig == nil {
		return "", nil, fmt.Errorf("cluster %s manage config is nil", clusterName)
	}

	inetPortal := "inet://"

	hcOptions := []httpclient.OpOption{
		httpclient.WithHTTPS(),
		httpclient.WithTimeout(conf.ExecutorClientTimeout(), conf.ExecutorClientTimeout()),
	}

	// check mange config type
	switch manageConfig.Type {
	case apistructs.ManageProxy, apistructs.ManageToken:
		// cluster-agent -> (patch) cluster-manager
		// -> (update) eventBox -> (update) scheduler -> scheduler reload executor
		if manageConfig.Token == "" || manageConfig.Address == "" {
			return "", nil, fmt.Errorf("token or address is empty")
		}

		hc := httpclient.New(hcOptions...)
		hc.BearerTokenAuth(manageConfig.Token)

		if manageConfig.Type == apistructs.ManageToken {
			return manageConfig.Address, hc, nil
		}

		// parseInetAddr parse inet addr, will add proxy header in custom http request
		return fmt.Sprintf("%s%s/%s", inetPortal, clusterName, manageConfig.Address), hc, nil
	case apistructs.ManageCert:
		if len(manageConfig.KeyData) == 0 ||
			len(manageConfig.CertData) == 0 {
			return "", nil, fmt.Errorf("cert or key is empty")
		}

		certBase64, err := base64.StdEncoding.DecodeString(manageConfig.CertData)
		if err != nil {
			return "", nil, err
		}
		keyBase64, err := base64.StdEncoding.DecodeString(manageConfig.KeyData)
		if err != nil {
			return "", nil, err
		}

		var certOption httpclient.OpOption

		certOption = httpclient.WithHttpsCertFromJSON(certBase64, keyBase64, nil)

		if len(manageConfig.CaData) != 0 {
			caBase64, err := base64.StdEncoding.DecodeString(manageConfig.CaData)
			if err != nil {
				return "", nil, err
			}
			certOption = httpclient.WithHttpsCertFromJSON(certBase64, keyBase64, caBase64)
		}
		hcOptions = append(hcOptions, certOption)

		return manageConfig.Address, httpclient.New(hcOptions...), nil
	default:
		return "", nil, fmt.Errorf("manage type is not support")
	}
}
