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
	"fmt"
	"os"
	"os/exec"
	"strconv"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
	"github.com/erda-project/erda/tools/cli/command"
)

const (
	sandboxImage         = "erdaproject/erda-mysql-migration-action:20210701-f84c33b"
	sandboxContainerName = "erda-sandbox-development"
)

var mysqlFlags = []command.Flag{
	command.StringFlag{
		Short:        "",
		Name:         "mysql-host",
		Doc:          "[MySQL] connect to host. env: ERDA_MYSQL_HOST",
		DefaultValue: os.Getenv("ERDA_MYSQL_HOST"),
	},
	command.IntFlag{
		Short:        "",
		Name:         "mysql-port",
		Doc:          "[MySQL] port number to use for connection. env: ERDA_MYSQL_PORT",
		DefaultValue: getPortFromEnv("ERDA_MYSQL_PORT"),
	},
	command.StringFlag{
		Short:        "",
		Name:         "mysql-username",
		Doc:          "[MySQl] user for login. env: ERDA_MYSQL_USERNAME",
		DefaultValue: os.Getenv("ERDA_MYSQL_USERNAME"),
	},
	command.StringFlag{
		Short:        "",
		Name:         "mysql-password",
		Doc:          "[MySQL] password to use then connecting to server. env: ERDA_MYSQL_PASSWORD",
		DefaultValue: os.Getenv("ERDA_MYSQL_PASSWORD"),
	},
	command.StringFlag{
		Short:        "",
		Name:         "database",
		Doc:          "[MySQL] database to use. env: ERDA_MYSQL_DATABASE",
		DefaultValue: os.Getenv("ERDA_MYSQL_DATABASE"),
	},
	command.IntFlag{
		Short:        "",
		Name:         "sandbox-port",
		Doc:          "[Sandbox] sandbox expose port. env: ERDA_SANDBOX_PORT",
		DefaultValue: getPortFromEnv("ERDA_SANDBOX_PORT"),
	},
}

var Migrate = command.Command{
	ParentName:     "",
	Name:           "migrate",
	ShortHelp:      "Erda MySQL Migrate",
	LongHelp:       "erda-cli migrate --mysql-host localhost --mysql-username root --mysql-password my_password --database erda",
	Example:        "$ erda-cli migrate --mysql-host localhost --mysql-username root --mysql-password my_password --database erda",
	Hidden:         false,
	DontHideCursor: false,
	Args:           nil,
	Flags: append(mysqlFlags,
		command.StringFlag{
			Short:        "",
			Name:         "lint-config",
			Doc:          "[Lint] Erda MySQL Lint config file",
			DefaultValue: "",
		},
		command.StringListFlag{
			Short:        "",
			Name:         "modules",
			Doc:          "[Lint] the modules for migrating",
			DefaultValue: nil,
		},
		command.BoolFlag{
			Short:        "",
			Name:         "debug-sql",
			Doc:          "[Migrate] print SQLs",
			DefaultValue: false,
		},
		command.BoolFlag{
			Short:        "",
			Name:         "skip-lint",
			Doc:          "[Lint] don't do Erda MySQL Lint",
			DefaultValue: false,
		},
		command.BoolFlag{
			Short:        "",
			Name:         "skip-sandbox",
			Doc:          "[Migrate] skip doing migration in sandbox",
			DefaultValue: false,
		},
		command.BoolFlag{
			Short:        "",
			Name:         "skip-pre-mig",
			Doc:          "[Migrate] skip doing pre-migration",
			DefaultValue: false,
		},
		command.BoolFlag{
			Short:        "",
			Name:         "skip-mig",
			Doc:          "[Migrate] skip doing pre-migration and real migration",
			DefaultValue: false,
		},
		command.StringFlag{
			Short:        "",
			Name:         "output",
			Doc:          "[Migrate] the directory for collecting SQLs",
			DefaultValue: "",
		},
	),
	Run: RunMigrate,
}

func RunMigrate(ctx *command.Context, host string, port int, username, password, database string, sandboxPort int,
	lintConfig string, modules []string, debugSQL, skipLint, skipSandbox, skipPreMig, skipMig bool, output string) error {
	logrus.Infoln("Erda Migrator is working")

	var p = parameters{
		mySQLParams: &migrator.DSNParameters{
			Username:  username,
			Password:  password,
			Host:      host,
			Port:      port,
			Database:  database,
			ParseTime: true,
			Timeout:   time.Second * 150,
		},
		sandboxParams: &migrator.DSNParameters{
			Username:  "root",
			Password:  "12345678",
			Host:      "0.0.0.0",
			Port:      sandboxPort,
			Database:  database,
			ParseTime: true,
			Timeout:   time.Second * 150,
		},
		migrationDir:   ".",
		modules:        nil,
		workdir:        "",
		debugSQL:       debugSQL,
		cfg:            make(map[string]sqllint.Config),
		skipLint:       skipLint,
		skipSandbox:    skipSandbox,
		skipPreMigrate: skipPreMig,
		skipMigrate:    skipMig,
		output:         output,
	}

	for _, module := range modules {
		if module != "" {
			p.modules = append(p.modules, module)
		}
	}

	cfg, err := sqllint.LoadConfigFromLocal(lintConfig)
	if err != nil {
		logrus.WithError(err).Warnln("failed to load lint config from local config file. use default!")
		cfg = make(map[string]sqllint.Config)
	}
	p.cfg = cfg

	if !skipSandbox {
		go func() {
			if err := StartSandbox(sandboxPort, sandboxContainerName); err != nil {
				logrus.WithField("err", err).Fatalln("failed to start sandbox")
			}
		}()
		defer StopSandbox(sandboxContainerName)
	}

	mig, err := migrator.New(p)
	if err != nil {
		return errors.Wrap(err, "failed to make new migrator")
	}
	mig.ClearSandbox()
	if err = mig.Run(); err != nil {
		return errors.Wrap(err, "failed to run migrator")
	}

	return nil
}

func getPortFromEnv(key string) int {
	port := os.Getenv(key)
	i, err := strconv.ParseInt(port, 10, 32)
	if err != nil {
		return 3306
	}
	return int(i)
}

func StartSandbox(exposePort int, name string) error {
	if err := StopSandbox(name); err != nil {
		logrus.Warnln(err)
	}
	if err := RmSandbox(name); err != nil {
		logrus.Warnln(err)
	}

	cmd := exec.Command("docker", "run", "-i", "--entrypoint", "run-mysqld",
		"-p", "0.0.0.0:"+strconv.FormatInt(int64(exposePort), 10)+":3306",
		"--name", name, sandboxImage)
	fmt.Println(cmd.String())
	if err := cmd.Start(); err != nil {
		return err
	}
	if err := cmd.Wait(); err != nil {
		return err
	}
	return nil
}

func StopSandbox(name string) error {
	cmd := exec.Command("docker", "stop", name)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func RmSandbox(name string) error {
	cmd := exec.Command("docker", "rm", name)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

type parameters struct {
	mySQLParams   *migrator.DSNParameters
	sandboxParams *migrator.DSNParameters
	migrationDir  string
	modules       []string
	workdir       string
	debugSQL      bool
	cfg           map[string]sqllint.Config

	skipLint       bool
	skipSandbox    bool
	skipPreMigrate bool
	skipMigrate    bool
	output         string
}

// MySQLParameters gets MySQL DSN
func (p parameters) MySQLParameters() *migrator.DSNParameters {
	return p.mySQLParams
}

// SandboxParameters gets sandbox DSN
func (p parameters) SandboxParameters() *migrator.DSNParameters {
	return p.sandboxParams
}

// MigrationDir gets migration scripts direction from repo, like .dice/migrations or 4.1/sqls
func (p parameters) MigrationDir() string {
	return p.migrationDir
}

// Modules is the modules for installing.
// if is nil, to install all modules in the MigrationDir()
func (p parameters) Modules() []string {
	return p.modules
}

// Workdir gets pipeline node workdir
func (p parameters) Workdir() string {
	return p.workdir
}

// DebugSQL gets weather to debug SQL executing
func (p parameters) DebugSQL() bool {
	return p.debugSQL
}

func (p parameters) SkipMigrationLint() bool {
	return p.skipLint
}

func (p parameters) SkipSandbox() bool {
	return p.skipSandbox
}

func (p parameters) SkipPreMigrate() bool {
	return p.skipPreMigrate
}

func (p parameters) SkipMigrate() bool {
	return p.skipMigrate
}

func (p parameters) LintConfig() map[string]sqllint.Config {
	if len(p.cfg) == 0 {
		return make(map[string]sqllint.Config)
	}
	return p.cfg
}

func (p parameters) SQLCollectorDir() string {
	return p.output
}

func (p parameters) RetryTimeout() uint64 {
	return 150
}
