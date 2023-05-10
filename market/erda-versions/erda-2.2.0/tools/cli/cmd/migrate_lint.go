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
	"log"
	"path"
	"path/filepath"
	"strings"

	"github.com/erda-project/erda/pkg/database/sqllint"
	_ "github.com/erda-project/erda/pkg/database/sqllint/linters"
	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
	"github.com/erda-project/erda/tools/cli/command"
)

const (
	baseScriptLabel  = "# MIGRATION_BASE"
	baseScriptLabel2 = "-- MIGRATION_BASE"
	baseScriptLabel3 = "/* MIGRATION_BASE */"
)

var MigrationLint = command.Command{
	ParentName:     "Migrate",
	Name:           "lint",
	ShortHelp:      "Erda MySQL Migration lint",
	LongHelp:       "Erda MySQL Migration lint",
	Example:        "$ erda-cli migrate lint --input=. --config=default.yaml --detail",
	Hidden:         false,
	DontHideCursor: false,
	Args:           nil,
	Flags: []command.Flag{
		command.StringFlag{
			Short:        "",
			Name:         "input",
			Doc:          "[Optional] the file or directory for linting",
			DefaultValue: ".",
		},
		command.StringFlag{
			Short:        "",
			Name:         "lint-config",
			Doc:          "[Optional] the lint config file",
			DefaultValue: "",
		},
	},
	Run: RunMigrateLint,
}

func RunMigrateLint(ctx *command.Context, input, filename string) (err error) {
	log.Printf("Erda MySQL Lint on directory: %s", input)
	if filename == "" {
		filename = filepath.Join(input, "config.yml")
	}
	log.Printf("Erda MySQL Lint with config file: %s", filename)
	return StandardMigrateLint(ctx, input, filename)
}

func StandardMigrateLint(ctx *command.Context, input, filename string) (err error) {
	defer func() {
		if err != nil {
			log.Fatalln(err)
		}
	}()

	cfg, err := sqllint.LoadConfigFromLocal(filename)
	if err != nil {
		return err
	}
	var p = scriptsParameters{
		migrationDir: input,
		cfg:          cfg,
	}

	scripts, err := migrator.NewScripts(&p)
	if err != nil {
		return err
	}
	scripts.IgnoreMarkPending()

	if err = scripts.SameNameLint(); err != nil {
		return err
	}

	if err = scripts.AlterPermissionLint(); err != nil {
		return err
	}

	if err = scripts.Lint(); err != nil {
		return err
	}

	log.Println("Erda MySQL Migration Lint OK")
	return nil
}

type walk struct {
	files []string
}

func (w *walk) filenames() []string {
	return w.files
}

func (w *walk) walk(input, suffix string) *walk {
	infos, err := ioutil.ReadDir(input)
	if err != nil {
		w.files = append(w.files, input)
		return w
	}

	for _, info := range infos {
		if info.IsDir() {
			w.walk(filepath.Join(input, info.Name()), suffix)
			continue
		}
		if strings.ToLower(path.Ext(info.Name())) == strings.ToLower(suffix) {
			file := filepath.Join(input, info.Name())
			w.files = append(w.files, file)
		}
	}

	return w
}

type scriptsParameters struct {
	migrationDir string
	cfg          map[string]sqllint.Config
}

func (p *scriptsParameters) Workdir() string {
	return "."
}

func (p *scriptsParameters) MigrationDir() string {
	return p.migrationDir
}

func (p *scriptsParameters) Modules() []string {
	return nil
}

func (p *scriptsParameters) LintConfig() map[string]sqllint.Config {
	return p.cfg
}

func isBaseScript(data []byte) bool {
	return bytes.HasPrefix(data, []byte(baseScriptLabel)) ||
		bytes.HasPrefix(data, []byte(baseScriptLabel2)) ||
		bytes.HasPrefix(data, []byte(baseScriptLabel3))
}
