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

package cmd

import (
	"archive/zip"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"path"
	"strconv"
	"strings"
	"time"

	"github.com/mholt/archiver"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

var PROJECTCREATE = command.Command{
	Name:      "create",
	ShortHelp: "create project",
	Example:   "erda-cli create --name=<name>",
	Flags: []command.Flag{
		command.StringFlag{Short: "", Name: "org", Doc: "the name of an organization", DefaultValue: ""},
		command.StringFlag{Short: "n", Name: "name", Doc: "the name of the project ", DefaultValue: ""},
		command.StringFlag{Short: "d", Name: "description", Doc: "description of the project", DefaultValue: ""},
		command.StringFlag{Short: "", Name: "init-package", Doc: "package for init the project", DefaultValue: ""},
		command.StringListFlag{Short: "", Name: "init-env", Doc: "environment (DEV/TEST/STAGING/PROD) to init", DefaultValue: nil},
		command.IntFlag{Short: "", Name: "wait-import", Doc: "minutes wait for package to be import", DefaultValue: 1},
	},
	Run: ProjectCreate,
}

func ProjectCreate(ctx *command.Context, org, project, desc, pkg string, envs []string, waitImport int) error {
	org, orgID, err := common.GetOrgID(ctx, org)
	if err != nil {
		return err
	}

	var values map[string]interface{}
	workspaces := make(map[apistructs.DiceWorkspace]interface{}, 4)
	if pkg != "" {
		s, err := os.Stat(pkg)
		if err != nil {
			return errors.Errorf("Invalid package %v", err)
		}

		if s.IsDir() {
			files, err := utils.ListDir(pkg)
			if err != nil {
				return err
			}
			zipTmpFile, err := ioutil.TempFile("", "project-package-*.zip")
			if err != nil {
				return err
			}
			defer os.Remove(zipTmpFile.Name())
			defer zipTmpFile.Close()
			err = archiver.Zip.Write(zipTmpFile, files)
			if err != nil {
				return err
			}
			pkg = zipTmpFile.Name()
		}
		if strings.HasSuffix(pkg, ".zip") {
			values, err = readValues(pkg)
			if err != nil {
				return errors.Errorf("Invalid package %v", err)
			}
		} else {
			return errors.Errorf("Invalid package %v, neither a dirctory nor a zip file", err)
		}

		if envs != nil {
			for _, e := range envs {
				w := apistructs.DiceWorkspace(strings.ToUpper(e))
				if !w.Deployable() {
					return errors.Errorf("Invalid environment '%s', should be one of DEV/TEST/STAGING/PROD.", e)
				}
				workspaces[w] = struct{}{}
			}
		} else {
			for _, e := range apistructs.DiceWorkspaceSlice {
				workspaces[e] = struct{}{}
			}
		}
	}

	var resourceConfigs *apistructs.ResourceConfigs
	if values != nil {
		resourceConfigs = apistructs.NewResourceConfigs()
		err = parseValues(resourceConfigs, values, workspaces)
		if err != nil {
			return err
		}
	}

	ctx.Info("Devops project %s creating...", project)
	projectID, err := common.CreateProject(ctx, orgID, project, desc, resourceConfigs)
	if err != nil {
		return err
	}
	ctx.Info("Devops project %s created.", project)

	ctx.Info("Msp tenant %s creating...", project)
	_, err = common.CreateMSPProject(ctx, projectID, project)
	if err != nil {
		return err
	}
	ctx.Info("Msp tenant %s created.", project)

	if pkg != "" {
		ctx.Info("Project package importing...")
		fileId, err := common.ImportPackage(ctx, orgID, projectID, pkg)
		if err != nil {
			return errors.Errorf("Import package %s failed %v", pkg, err)
		}

		exportSuccess := false
		for i := 0; i <= 12*waitImport; i++ {
			record, err := common.GetRecord(ctx, orgID, fileId)
			if err != nil {
				return err
			}

			switch record.State {
			case apistructs.FileRecordStateFail:
				return errors.Errorf("Import package %s failed, error %s", pkg, record.ErrorInfo)
			case apistructs.FileRecordStateSuccess:
				exportSuccess = true
			case apistructs.FileRecordStatePending, apistructs.FileRecordStateProcessing:
				ctx.Info("Project package importing...")
				time.Sleep(5 * time.Second)
			}
			if exportSuccess {
				break
			}
		}
		if exportSuccess {
			ctx.Info("Project package imported.")
		} else {
			return errors.Errorf("Import package %s timeout.", pkg)
		}
	}

	ctx.Succ("Project '%s' created.", project)
	return nil
}

func parseValues(resourceConfigs *apistructs.ResourceConfigs, values map[string]interface{},
	workspaces map[apistructs.DiceWorkspace]interface{}) error {
	for k, v := range values {
		splits := strings.SplitN(k, ".", 4)
		if len(splits) < 4 {
			continue
		}
		env := apistructs.DiceWorkspace(strings.ToUpper(splits[1]))

		if len(splits) == 4 && splits[0] == "values" && splits[2] == "addons" {
			_, ok := workspaces[env]
			if ok && v == "" {
				return errors.Errorf("Invalid package, found value of '%s' not configed", k)
			}
		}

		if len(splits) == 4 && splits[0] == "values" && splits[2] == "cluster" {
			if v == "" {
				return errors.Errorf("Invalid package, found value of '%s' not configed", k)
			}

			if splits[3] == "name" {
				resourceConfigs.GetClusterConfig(env).ClusterName = fmt.Sprintf("%v", v)
			} else if splits[3] == "quota.cpuQuota" {
				cpuQuotaStr := fmt.Sprintf("%v", v)
				cpuQuota, err := strconv.ParseFloat(cpuQuotaStr, 64)
				if err != nil {
					return errors.Errorf("Invalid package, found value of '%s' not a float", k)
				}
				resourceConfigs.GetClusterConfig(env).CPUQuota = cpuQuota
			} else if splits[3] == "quota.memoryQuota" {
				memoryQuotaStr := fmt.Sprintf("%v", v)
				memoryQuota, err := strconv.ParseFloat(memoryQuotaStr, 64)
				if err != nil {
					return errors.Errorf("Invalid package, found value of '%s' not a float", k)
				}
				resourceConfigs.GetClusterConfig(env).MemQuota = memoryQuota
			}
		}
	}
	return nil
}

func readValues(pkg string) (map[string]interface{}, error) {
	zipReader, err := zip.OpenReader(pkg)
	if err != nil {
		return nil, err
	}
	defer zipReader.Close()

	valuesFile := "values.yml"
	if prefix, inDir := zipInDirectory(zipReader); inDir {
		valuesFile = path.Join(prefix, valuesFile)
	}

	valueYml, err := zipReader.Open(valuesFile)
	if err != nil {
		return nil, err
	}
	defer valueYml.Close()

	yamlBytes, err := io.ReadAll(valueYml)
	if err != nil {
		return nil, err
	}

	values := map[string]interface{}{}
	if err := yaml.Unmarshal(yamlBytes, values); err != nil {
		return nil, err
	}

	return values, nil
}

func zipInDirectory(zipReader *zip.ReadCloser) (string, bool) {
	var prefix string
	var hasProjectYml, hasMetadataYml, hasValuesYml bool
	for _, f := range zipReader.File {
		if !strings.HasPrefix(f.Name, "__") {
			splits := strings.SplitN(f.Name, "/", 2)
			if len(splits) == 2 {
				if prefix == "" {
					prefix = splits[0]
				} else {
					if prefix != splits[0] {
						return "", false
					}
				}

				switch splits[1] {
				case "project.yml":
					hasProjectYml = true
				case "metadata.yml":
					hasMetadataYml = true
				case "values.yml":
					hasValuesYml = true
				}
			} else {
				return "", false
			}
		}
	}

	if hasValuesYml && hasMetadataYml && hasProjectYml {
		return prefix, true
	} else {
		return "", false
	}
}
