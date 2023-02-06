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
	"bytes"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
	"github.com/erda-project/erda/pkg/database/sqlparser/pygrator"
	"github.com/erda-project/erda/tools/cli/command"
)

var MigratePy = command.Command{
	ParentName:     "Migrate",
	Name:           "mkpy",
	ShortHelp:      "make a python migration script pattern",
	LongHelp:       "make a python migration scritp pattern.",
	Example:        "$ erda-cli migrate mkpy --module my_module --name my_script_name",
	Hidden:         false,
	DontHideCursor: false,
	Args:           nil,
	Flags: []command.Flag{
		command.StringFlag{
			Short:        "",
			Name:         "workdir",
			Doc:          "workdir",
			DefaultValue: ".",
		},
		command.StringFlag{
			Short:        "m",
			Name:         "module",
			Doc:          "migration module name",
			DefaultValue: "",
		},
		command.StringFlag{
			Short:        "n",
			Name:         "name",
			Doc:          "script name",
			DefaultValue: "",
		},
		command.StringListFlag{
			Short:        "",
			Name:         "tables",
			Doc:          "dependency tables",
			DefaultValue: nil,
		},
	},
	Run: RunMigrateMkPy,
}

func RunMigrateMkPy(ctx *command.Context, workdir, module, name string, tables []string) error {
	moduleInfos, err := ioutil.ReadDir(workdir)
	if err != nil {
		return err
	}

	var (
		m                  migrator.Module
		validModule        = false
		requirementsExists bool
	)

	for _, moduleInfo := range moduleInfos {
		if !moduleInfo.IsDir() {
			continue
		}
		if moduleInfo.Name() == module {
			validModule = true
		}
		fileInfos, err := ioutil.ReadDir(moduleInfo.Name())
		if err != nil {
			return errors.Wrap(err, "failed to ReadDir, directory name: %s")
		}
		for _, fileInfo := range fileInfos {
			if fileInfo.IsDir() {
				continue
			}

			// is there already exists requirements.txt
			if moduleInfo.Name() == module && fileInfo.Name() == pygrator.RequirementsFilename {
				requirementsExists = true
			}

			if ext := filepath.Ext(fileInfo.Name()); strings.EqualFold(ext, string(migrator.ScriptTypeSQL)) {
				script, err := migrator.NewScript(workdir, filepath.Join(workdir, moduleInfo.Name(), fileInfo.Name()))
				if err != nil {
					return errors.Wrapf(err, "failed to NewScript")
				}
				m.Scripts = append(m.Scripts, script)
			}
		}
	}
	if !validModule {
		return errors.Errorf("invalid module name: %s", module)
	}

	if !requirementsExists {
		if err = ioutil.WriteFile(filepath.Join(workdir, module, pygrator.RequirementsFilename),
			[]byte(pygrator.BaseRequirements), 0644); err != nil {
			return errors.Wrap(err, "failed to generate requirements.txt")
		}
	}

	m.Sort()

	// transform table definitions to django models
	var (
		schema  = m.Schema()
		script  pygrator.DeveloperScript
		tablesM = make(map[string]bool)
	)
	for _, tableName := range tables {
		tablesM[tableName] = true
	}
	for _, definition := range schema.TableDefinitions {
		// if user specifies the tables, skip the table which is not in the tables
		if definition.CreateStmt == nil {
			continue
		}
		if _, ok := tablesM[definition.CreateStmt.Table.Name.String()]; !ok {
			continue
		}

		// make table definition to Django Model
		model, err := pygrator.CreateTableStmtToModel(definition.CreateStmt)
		if err != nil {
			return errors.Wrapf(err, "failed to CreateTableStmtToModel")
		}
		var buf = bytes.NewBuffer(nil)
		if err = pygrator.GenModel(buf, *model); err != nil {
			return errors.Wrapf(err, "failed to GenModel")
		}
		script.Models = append(script.Models, buf.String())
	}

	// write developer's python pattern script
	name = strings.TrimSuffix(name, filepath.Ext(name)) + string(migrator.ScriptTypePython)
	var filename = filepath.Join(workdir, module, name)
	f, err := os.OpenFile(filename, os.O_CREATE|os.O_TRUNC|os.O_RDWR, 0644)
	if err != nil {
		return errors.Wrapf(err, "failed to OpenFile: %s", filename)
	}
	defer f.Close()
	if err = pygrator.GenDeveloperScript(f, script); err != nil {
		return err
	}

	return nil
}
