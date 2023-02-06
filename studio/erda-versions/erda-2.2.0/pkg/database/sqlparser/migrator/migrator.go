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

package migrator

import (
	"context"
	"fmt"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gorm.io/gorm"

	"github.com/erda-project/erda/pkg/database/sqlparser/pygrator"
	"github.com/erda-project/erda/pkg/database/sqlparser/snapshot"
	"github.com/erda-project/erda/pkg/strutil"
)

// installing type
const (
	firstTimeInstall installType = "first_time_install"
	normalUpdate     installType = "normal_update"
	firstTimeUpdate  installType = "first_time_update"

	patchPrefix = "patch-"
)

type installType string

const patchesKey = "patchesKey"

type Migrator struct {
	Parameters

	snap         *snapshot.Snapshot
	LocalScripts *Scripts
	reversing    []string

	installingType installType

	dbSettings      *pygrator.Settings // database settings
	sandboxSettings *pygrator.Settings // sandbox settings
	db              *gorm.DB
	sandbox         *gorm.DB

	collectorFilename string
}

func New(parameters Parameters) (mig *Migrator, err error) {
	if parameters == nil {
		return nil, errors.New("parameters is nil")
	}
	if parameters.MySQLParameters() == nil {
		return nil, errors.New("MySQL DSN is invalid, did you set the right DSN ?")
	}
	if parameters.SandboxParameters() == nil {
		return nil, errors.New("sandbox DSN is invalid, did you set the right DSN ?")
	}

	// init parameters
	mig = new(Migrator)
	mig.Parameters = parameters
	mig.dbSettings = &pygrator.Settings{
		Engine:   pygrator.MySQLConnectorEngine,
		User:     mig.MySQLParameters().Username,
		Password: mig.MySQLParameters().Password,
		Host:     mig.MySQLParameters().Host,
		Port:     mig.MySQLParameters().Port,
		Name:     mig.MySQLParameters().Database,
		TimeZone: pygrator.TimeZoneAsiaShanghai,
	}
	mig.sandboxSettings = &pygrator.Settings{
		Engine:   pygrator.MySQLConnectorEngine,
		User:     mig.SandboxParameters().Username,
		Password: mig.SandboxParameters().Password,
		Host:     mig.SandboxParameters().Host,
		Port:     mig.SandboxParameters().Port,
		Name:     mig.SandboxParameters().Database,
		TimeZone: pygrator.TimeZoneAsiaShanghai,
	}

	// set the SQLs collector filename
	mig.collectorFilename = SQLCollectorFilename()
	if collectDir, ok := mig.Parameters.(SQLCollectorDir); ok {
		mig.collectorFilename = filepath.Join(collectDir.SQLCollectorDir(), mig.collectorFilename)
	}

	// load scripts
	mig.LocalScripts, err = NewScripts(parameters)
	if err != nil {
		return nil, errors.Wrap(err, "failed to NewScripts")
	}

	return mig, nil
}

func (mig *Migrator) Run() (err error) {
	defer func() {
		if err == nil {
			logrus.Infoln("Erda MySQL Migrate Complete !")
		}
	}()

	// snapshot database schema structure
	mig.snap, err = snapshot.From(mig.DB(), SchemaMigrationHistory)
	if err != nil {
		return err
	}

	// if there is no table then goto new installation
	if !mig.snap.HasAnyTable() {
		logrus.Infoln("there is not any table, it is the first time installation...")
		mig.installingType = firstTimeInstall
		return mig.newInstallation()
	}

	// if there is any history record then goto normal update,
	// otherwise goto first time update
	var histories []HistoryModel
	tx := mig.DB().Find(&histories)
	if tx.Error == nil && len(histories) > 0 {
		logrus.Infoln("found migration histories, it is the normal update installation...")
		mig.installingType = normalUpdate
	} else {
		logrus.Infoln("there are some tables but no migration histories, it is the first time update installation by Erda MySQL Migration...")
		mig.installingType = firstTimeUpdate
	}
	return mig.normalUpdate()
}

func (mig *Migrator) newInstallation() (err error) {
	new(HistoryModel).CreateTable(mig.DB())
	mig.LocalScripts.MarkPending(mig.DB())

	// Erda mysql lint
	if !mig.SkipMigrationLint() {
		logrus.Infoln("DO ERDA MYSQL LINT...")
		if err = mig.LocalScripts.Lint(); err != nil {
			return err
		}
		logrus.Infoln("OK")
	}

	// same name lint
	logrus.Infoln("DO SAME NAME LINT....")
	if err = mig.LocalScripts.SameNameLint(); err != nil {
		return err
	}
	logrus.Infoln("OK")

	// alter permission lint
	logrus.Infoln("DO ALTER PERMISSION LINT...")
	if err = mig.LocalScripts.AlterPermissionLint(); err != nil {
		return err
	}
	logrus.Infoln("OK")

	var ctx = context.Background()
	// execute in sandbox
	if !mig.SkipSandbox() {
		logrus.Infoln("DO MIGRATION IN SANDBOX...")
		if err = mig.migrateSandbox(ctx); err != nil {
			return err
		}
		logrus.Infoln("OK")
	}

	// migrate
	if !mig.SkipMigrate() {
		logrus.Infoln("DO MIGRATION...")
		if err = mig.migrate(ctx); err != nil {
			return err
		}
		logrus.Infoln("MIGRATE OK")
	}

	return nil
}

func (mig *Migrator) normalUpdate() (err error) {
	mig.LocalScripts.MarkPending(mig.DB())

	// Erda mysql lint
	if !mig.SkipMigrationLint() {
		logrus.Infoln("DO ERDA MYSQL LINT....")
		if err = mig.LocalScripts.Lint(); err != nil {
			return err
		}
		logrus.Infoln("OK")
	}

	// same name lint
	logrus.Infoln("DO SAME NAME LINT....")
	if err = mig.LocalScripts.SameNameLint(); err != nil {
		return err
	}
	logrus.Infoln("OK")

	// alter permission lint
	logrus.Infoln("DO ALTER PERMISSION LINT....")
	if err = mig.LocalScripts.AlterPermissionLint(); err != nil {
		return err
	}
	logrus.Infoln("OK")

	ctx := context.Background()

	// installed script changes lint
	logrus.Infoln("DO INSTALLED CHANGES LINT....")
	if err = mig.LocalScripts.InstalledChangesLint(&ctx, mig.DB()); err != nil {
		return err
	}
	logrus.Infoln("OK")

	if !mig.SkipSandbox() {
		// migrate in sandbox
		logrus.Infoln("DO MIGRATION IN SANDBOX....")
		if err = mig.migrateSandbox(ctx); err != nil {
			return err
		}
		logrus.Infoln("OK")
	}

	// migrate data
	if !mig.SkipMigrate() {
		logrus.Infoln("DO MIGRATION....")
		if err = mig.migrate(ctx); err != nil {
			return err
		}
		logrus.Infoln("MIGRATE OK")
	}

	return nil
}

func (mig *Migrator) reverse(reversing []string, reverseSlice bool) error {
	if len(reversing) == 0 {
		return nil
	}
	if reverseSlice {
		strutil.ReverseSlice(reversing)
	}
	for _, s := range mig.reversing {
		if err := mig.DB().Exec(s).Error; err != nil {
			return errors.Errorf("failed to exec reversing SQL: %s", s)
		}
	}
	return nil
}

func (mig *Migrator) migrateSandbox(ctx context.Context) (err error) {
	snap, err := snapshot.From(mig.DB())
	if err != nil {
		return err
	}
	// copy database snapshot to sandbox
	logrus.Infoln("copy current database structure to sandbox")
	if err = snap.RecoverTo(mig.SandBox()); err != nil {
		return err
	}
	logrus.Infoln("ok")

	rows, err := mig.SandBox().Raw("show tables").Rows()
	if err != nil {
		return err
	}
	logrus.Infoln("tables in sandbox:")
	for rows.Next() {
		var tableName string
		if err := rows.Scan(&tableName); err != nil {
			return errors.Wrapf(err, "scan error")
		}
		logrus.Infoln(tableName)
	}

	if mig.needCompare() {
		modules := mig.LocalScripts.FreshBaselineModules(mig.DB())
		reason, ok := compareSchemas(mig.SandBox(), modules)
		if !ok {
			logrus.Warnf("local schema is not equal with cloud schema, try to resolve it:\n%s", reason)
			if err := mig.patchBeforeMigrating(mig.SandBox(), []string{patchInit}); err != nil {
				return errors.Wrap(err, "failed to patch init")
			}
			reason, ok := compareSchemas(mig.SandBox(), modules)
			if !ok {
				return errors.Errorf("local base schema is not equal with cloud schema:\n%s", reason)
			}
		}

		// record base
		logrus.Infoln("RECORD BASE... ..")
		if err := recordModules(mig.SandBox(), modules); err != nil {
			return errors.Wrapf(err, "failed to record base after comparing")
		}
	}

	files, err := retrievePatchesFiles(ctx)
	if err != nil {
		return errors.New("failed to retrieve patches files")
	}
	logrus.Infoln("retrieve files that needs to patch:", files)
	if err = mig.patchBeforeMigrating(mig.SandBox(), files); err != nil {
		return errors.Wrapf(err, "failed to patch before migrating in sandbox")
	}

	// install every module
	var scripts = SortedScripts(mig.LocalScripts.Services)
	for _, scr := range scripts {
		var (
			moduleName = scr.ModuleName
			module     = scr.Module
			script     = scr.Script
		)

		logrus.WithField("module", moduleName).
			WithField("script", script.GetName()).
			Infof("[Sandbox] %s", map[bool]string{true: "    to install", false: "not to install"}[script.Pending])
		if !script.Pending {
			continue
		}
		switch script.Type {
		case ScriptTypeSQL:
			after := func(tx *gorm.DB, err error) {
				tx.Commit()
			}
			if err := mig.installSQL(script, mig.SandBox(), func() (tx *gorm.DB) {
				return mig.SandBox().Begin()
			}, after); err != nil {
				return errors.Wrapf(err, "failed to migrate in sandbox:  module name: %s, filename: %s, type: %s",
					moduleName, script.GetName(), ScriptTypeSQL)
			}
		case ScriptTypePython:
			if err := mig.installPy(script, module, mig.sandboxSettings, true); err != nil {
				return errors.Wrapf(err, "failed to migrate in sandbox: %+v",
					map[string]interface{}{"moduleName": moduleName, "filename": script.GetName(), "type": ScriptTypePython})
			}
		}
	}

	return err
}

func (mig *Migrator) migrate(ctx context.Context) error {
	now := time.Now()

	if mig.needCompare() {
		modules := mig.LocalScripts.FreshBaselineModules(mig.DB())
		reason, ok := compareSchemas(mig.DB(), modules)
		if !ok {
			logrus.Warnf("local schema is not equal with cloud schema, try to resolve it:\n%s", reason)
			if err := mig.patchBeforeMigrating(mig.DB(), []string{patchInit}); err != nil {
				return errors.Wrap(err, "failed to patch init")
			}
			reason, ok := compareSchemas(mig.DB(), modules)
			if !ok {
				return errors.Errorf("local base schema is not equal with cloud schema:\n%s", reason)
			}
		}

		// record base
		logrus.Infoln("RECORD BASE... ..")
		if err := recordModules(mig.DB(), modules); err != nil {
			return errors.Wrapf(err, "failed to record base after comparing")
		}
	}

	files, err := retrievePatchesFiles(ctx)
	if err != nil {
		return err
	}
	logrus.Infoln("retrieve files that needs to patch:", files)
	if err := mig.patchBeforeMigrating(mig.DB(), files); err != nil {
		return errors.Wrap(err, "failed to patch before migrating")
	}

	// install every service
	var scripts = SortedScripts(mig.LocalScripts.Services)
	for _, scr := range scripts {
		var (
			moduleName = scr.ModuleName
			mod        = scr.Module
			script     = scr.Script
		)
		logrus.WithField("module", moduleName).
			WithField("scriptName", script.GetName()).
			Infof("[MySQL Server] %s", map[bool]string{true: "    to install", false: "not to install"}[script.Pending])

		if !script.Pending {
			continue
		}

		switch script.Type {
		case ScriptTypeSQL:
			after := func(tx *gorm.DB, err error) {
				if err != nil {
					tx.Rollback()
					mig.reverse(script.Reversing, true)
				} else {
					tx.Commit()
				}
			}
			if err := mig.installSQL(script, mig.DB(), func() (tx *gorm.DB) {
				return mig.DB().Begin()
			}, after); err != nil {
				return errors.Wrapf(err, "failed to migrate: %+v",
					map[string]interface{}{"module name": moduleName, "script name": script.GetName(), "type": ScriptTypeSQL})
			}

		case ScriptTypePython:
			if err := mig.installPy(script, mod, mig.dbSettings, true); err != nil {
				return errors.Wrapf(err, "failed to migrate: %+v",
					map[string]interface{}{"module name": moduleName, "script name": script.GetName(), "type": ScriptTypePython})
			}
		}

		// record it
		strutil.ReverseSlice(script.Reversing)
		record := HistoryModel{
			ID:           0,
			CreatedAt:    now,
			UpdatedAt:    now,
			ServiceName:  moduleName,
			Filename:     script.GetName(),
			Checksum:     script.Checksum(),
			InstalledBy:  "",
			InstalledOn:  "",
			LanguageType: string(script.Type),
			Reversed:     "",
		}
		if err := mig.DB().Create(&record).Error; err != nil {
			return errors.Wrapf(err, "internal error: failed to record migration")
		}
	}

	return nil
}

func (mig *Migrator) patchBeforeMigrating(db *gorm.DB, files []string) error {
	mod := mig.LocalScripts.GetPatches()
	if mod == nil {
		return nil
	}

	logrus.WithField("module", mod.Name)
	for _, script := range mod.Scripts {
		var (
			record   HistoryModel
			filename = strings.TrimPrefix(script.GetName(), patchPrefix)
			where    = map[string]interface{}{"filename": filename}
		)
		// if the script is not in the diff checksum file list, skip
		var in = false
		for _, file := range files {
			if file == script.GetName() {
				in = true
				break
			}
		}
		if !in {
			continue
		}

		// if the file is going to be patched is not installed, do not patch it
		if db := db.Where(where).First(&record); (db.Error != nil || db.RowsAffected == 0) && script.GetName() != patchInit {
			continue
		}

		switch script.Type {
		case ScriptTypeSQL:
			logrus.WithField("script name", script.GetName()).Infoln("patch it before all migrating")
			logrus.Infof("script Rawtext: %s", string(script.GetData()))
			if !script.IsEmpty() {
				if err := db.Exec(string(script.GetData())).Error; err != nil {
					return errors.Wrapf(err, "failed to patch, module name: %s, script name: %s, type: %s",
						mod.Name, script.GetName(), ScriptTypeSQL)
				}
			}

			// correct the checksum
			// if there is no corresponding original file, skip
			_, originalScript, ok := mig.LocalScripts.GetScriptByFilename(filename)
			if ok {
				if err := db.Model(new(HistoryModel)).Where(where).
					Update("checksum", originalScript.Checksum()).Error; err != nil {
					return errors.Wrapf(err, "failed to patch new checksum, modeule name: %s, script name: %s, type: %s",
						mod.Name, script.GetName(), ScriptTypeSQL)
				}
			}
		default:
			return errors.New("only support .sql patch file")
		}
	}

	return nil
}

func (mig *Migrator) installSQL(s *Script, exec *gorm.DB, begin func() (tx *gorm.DB), after func(tx *gorm.DB, err error)) (err error) {
	defer func() {
		mig.reversing = append(mig.reversing, s.Reversing...)
	}()

	s.Reversing = nil

	for i := range s.Blocks {
		switch s.Blocks[i].Type() {
		case DDL:
			if err := mig.installDDLBlock(s, i, exec); err != nil {
				return err
			}
		case DML:
			if err := mig.installDMLBlocks(s, i, begin(), after); err != nil {
				return err
			}
		}
	}

	return nil
}

func (mig *Migrator) installDDLBlock(s *Script, i int, exec *gorm.DB) (err error) {
	for _, node := range s.Blocks[i].Nodes() {
		if err = exec.Exec(node.Text()).Error; err != nil {
			return errors.Wrapf(err, "failed to do schema migration: %+v",
				map[string]string{"scriptName": s.GetName(), "SQL": node.Text()})
		}
	}
	return nil
}

func (mig *Migrator) installDMLBlocks(s *Script, i int, tx *gorm.DB, after func(tx *gorm.DB, err error)) (err error) {
	defer after(tx, err)
	for _, node := range s.Blocks[i].Nodes() {
		if err = tx.Exec(node.Text()).Error; err != nil {
			return errors.Wrapf(err, "failed to do data migrations, scritp name: %s, block index: %v, SQL: %s",
				s.GetName(), i, node.Text())
		}
	}
	return nil
}

func (mig *Migrator) installPy(s *Script, module *Module, settings *pygrator.Settings, commit bool) error {
	var p = pygrator.Package{
		DeveloperScript:   s,
		Requirements:      module.PythonRequirementsText,
		Settings:          *settings,
		Commit:            commit,
		CollectorFilename: mig.collectorFilename,
	}
	if len(p.Requirements) == 0 {
		p.Requirements = []byte(pygrator.BaseRequirements)
	}

	if err := p.Make(); err != nil {
		return err
	}
	defer p.Remove()

	return p.Run()
}

func (mig *Migrator) needCompare() bool {
	return mig.installingType == firstTimeUpdate
}

type moduleScript struct {
	ModuleName string
	Module     *Module
	Script     *Script
}

// SortedScripts 对所有脚本进行总排序
func SortedScripts(services map[string]*Module) []moduleScript {
	var scripts []moduleScript
	for moduleName, mod := range services {
		for _, script := range mod.Scripts {
			scripts = append(scripts, moduleScript{
				ModuleName: moduleName,
				Module:     mod,
				Script:     script,
			})
		}
	}
	sort.Slice(scripts, func(i, j int) bool {
		if scripts[i].Script.IsBaseline() && !scripts[j].Script.IsBaseline() {
			return true
		}
		if !scripts[i].Script.IsBaseline() && scripts[j].Script.IsBaseline() {
			return false
		}
		return strings.TrimSuffix(scripts[i].Script.GetName(), filepath.Ext(scripts[i].Script.GetName())) <
			strings.TrimSuffix(scripts[j].Script.GetName(), filepath.Ext(scripts[j].Script.GetName()))
	})
	return scripts
}

func compareSchemas(db *gorm.DB, modules map[string]*Module) (string, bool) {
	logrus.Infoln("compare local schema and cloud schema for baseline ...")
	if len(modules) == 0 {
		logrus.Infoln("no new baseline file, exit comparing")
		return "", true
	}
	logrus.Infoln("there are new baseline files in some module, to compare them")
	var (
		reasons string
		eq      = true
	)
	for modName, module := range modules {
		equal := module.Schema().EqualWith(db)
		if !equal.Equal() {
			eq = false
			reasons += fmt.Sprintf("module name: %s:\n%s", modName, equal.Reason())
		}
	}
	return reasons, eq
}

func recordModules(db *gorm.DB, modules map[string]*Module) error {
	new(HistoryModel).CreateTable(db)
	now := time.Now()
	for moduleName, module := range modules {
		for i := 0; i < len(module.Scripts); i++ {
			module.Scripts[i].Pending = false
			record := HistoryModel{
				ID:           0,
				CreatedAt:    now,
				UpdatedAt:    now,
				ServiceName:  moduleName,
				Filename:     module.Scripts[i].GetName(),
				Checksum:     module.Scripts[i].Checksum(),
				InstalledBy:  "",
				InstalledOn:  "",
				LanguageType: string(module.Scripts[i].Type),
				Reversed:     "",
			}
			if err := db.Create(&record).Error; err != nil {
				return errors.Wrapf(err, "failed to record module, module name: %s, script name: %s",
					moduleName, module.Scripts[i].GetName())
			}
		}
	}

	return nil
}

func retrievePatchesFiles(ctx context.Context) ([]string, error) {
	value := ctx.Value(patchesKey)
	if value == nil {
		logrus.Infoln("retrievePatchesFiles value is nil")
		return nil, nil
	}
	var (
		files []string
		ok    bool
	)
	files, ok = value.([]string)
	if !ok {
		return nil, errors.New("failed to retrieve patches files list")
	}
	return files, nil
}
