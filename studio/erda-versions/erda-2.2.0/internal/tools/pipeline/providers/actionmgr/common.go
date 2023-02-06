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

package actionmgr

import (
	"context"
	"strings"

	actionpb "github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/common/apis"
)

var (
	defaultVersion = "default"
)

func (s *provider) updateExtensionCache(extension apistructs.Extension) {
	// query
	extensionVersions, err := s.bdl.QueryExtensionVersions(apistructs.ExtensionVersionQueryRequest{
		Name:               extension.Name,
		All:                true,
		YamlFormat:         true,
		OrderByVersionDesc: true,
	})
	if err != nil {
		s.Log.Errorf("failed to query extension version, name: %s, err: %v", extension.Name, err)
		return
	}

	s.Lock()
	defer s.Unlock()

	// delete from defaultActionsCache by action name firstly, because maybe not have default versions in queried result
	delete(s.defaultActionsCache, extension.Name)
	// update
	for _, extensionVersion := range extensionVersions {
		s.actionsCache[makeActionNameVersion(extensionVersion.Name, extensionVersion.Version)] = extensionVersion
		if extensionVersion.IsDefault {
			s.defaultActionsCache[extension.Name] = extensionVersion
		}
	}
	// if not found the default version, set the first public version as default
	if _, ok := s.defaultActionsCache[extension.Name]; !ok && len(extensionVersions) > 0 {
		for _, extensionVersion := range extensionVersions {
			if extensionVersion.Public {
				s.defaultActionsCache[extension.Name] = extensionVersion
				break
			}
		}
	}
}

// getOrUpdateExtensionFromCache get the fitted extension from the cache
// if not exist, try to update the cache by the given extension name
func (s *provider) getOrUpdateExtensionFromCache(nameVersion string) (action apistructs.ExtensionVersion, found bool) {
	splits := strings.SplitN(nameVersion, "@", 2)
	name := splits[0]
	version := ""
	if len(splits) > 1 {
		version = splits[1]
	}
	if version == "" {
		s.Lock()
		action, found = s.defaultActionsCache[name]
		s.Unlock()
		if !found {
			newAction, err := s.bdl.GetExtensionVersion(apistructs.ExtensionVersionGetRequest{
				Name:       name,
				Version:    defaultVersion,
				YamlFormat: true,
			})
			if err != nil {
				found = false
				return
			}
			s.Lock()
			s.defaultActionsCache[name] = *newAction
			s.Unlock()
			return *newAction, true
		}
		return
	}
	s.Lock()
	action, found = s.actionsCache[nameVersion]
	s.Unlock()
	if !found {
		newAction, err := s.bdl.GetExtensionVersion(apistructs.ExtensionVersionGetRequest{
			Name:       name,
			Version:    version,
			YamlFormat: true,
		})
		if err != nil {
			found = false
			return
		}
		s.Lock()
		s.actionsCache[nameVersion] = *newAction
		s.Unlock()
		return *newAction, true
	}
	return
}

func (s *provider) searchPipelineActions(items []string, locations []string) (map[string]apistructs.ExtensionVersion, error) {
	if len(items) == 0 {
		return map[string]apistructs.ExtensionVersion{}, nil
	}

	if len(locations) == 0 {
		return map[string]apistructs.ExtensionVersion{}, nil
	}

	var pipelineActionListRequest actionpb.PipelineActionListRequest
	pipelineActionListRequest.YamlFormat = true
	pipelineActionListRequest.Locations = locations
	for _, nameVersion := range items {
		name, version := getActionNameVersion(nameVersion)
		query := &actionpb.ActionNameWithVersionQuery{
			Name:    name,
			Version: version,
		}
		pipelineActionListRequest.ActionNameWithVersionQuery = append(pipelineActionListRequest.ActionNameWithVersionQuery, query)
	}

	resp, err := s.actionService.List(apis.WithInternalClientContext(context.Background(), "pipeline"), &pipelineActionListRequest)
	if err != nil {
		return nil, err
	}

	var result = map[string]apistructs.ExtensionVersion{}
	for _, nameVersion := range items {
		name, version := getActionNameVersion(nameVersion)

		var findAction *actionpb.Action
		for _, action := range resp.Data {
			if action.Name != name {
				continue
			}

			if version == "" {
				if action.IsDefault {
					findAction = action
					break
				}
			} else {
				if action.Version == version {
					findAction = action
					break
				}
			}
		}

		// Set the first public action if the default cannot be found
		if findAction == nil && version == "" {
			for _, action := range resp.Data {
				if action.Name != name {
					continue
				}
				if !action.IsPublic {
					continue
				}
				findAction = action
				break
			}
		}

		if findAction == nil {
			continue
		}

		result[nameVersion] = apistructs.ExtensionVersion{
			Name:      findAction.Name,
			Version:   findAction.Version,
			Type:      "action",
			Spec:      findAction.Spec.GetStringValue(),
			Dice:      findAction.Dice.GetStringValue(),
			Readme:    findAction.Readme,
			CreatedAt: findAction.TimeCreated.AsTime(),
			UpdatedAt: findAction.TimeUpdated.AsTime(),
			IsDefault: findAction.IsDefault,
			Public:    findAction.IsPublic,
		}
	}
	return result, nil
}

func getActionNameVersion(nameVersion string) (string, string) {
	splits := strings.SplitN(nameVersion, "@", 2)
	name := splits[0]
	version := ""
	if len(splits) > 1 {
		version = splits[1]
	}
	if version == defaultVersion {
		version = ""
	}
	return name, version
}

func makeActionNameVersion(name, version string) string {
	if len(version) == 0 {
		return name
	}
	return name + "@" + version
}
