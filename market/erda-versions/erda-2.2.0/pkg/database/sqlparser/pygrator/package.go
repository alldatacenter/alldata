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

package pygrator

import (
	"io"
	"os"
	"os/exec"
	"path/filepath"

	"github.com/pkg/errors"
)

const (
	packageName = ".erda_migration_in_py"
)

const (
	EntryFilename   = "entry.py"
	FeatureFilename = "feature.py"
)

type TextFile interface {
	// GetName returns the file's base name
	GetName() string
	GetData() []byte
}

type Package struct {
	DeveloperScript   TextFile
	Requirements      []byte
	Settings          Settings
	Commit            bool
	CollectorFilename string

	entrypoint Entrypoint
}

func (p *Package) Make() (err error) {
	_ = p.Remove()

	if err = os.Mkdir(packageName, os.ModeSticky|os.ModePerm); err != nil {
		return errors.Wrap(err, "failed to make temp python package for migration")
	}
	defer func() {
		_ = p.Remove()
	}()

	msg := "failed to make python file for migration"

	p.entrypoint = Entrypoint{DeveloperScriptFilename: p.DeveloperScript.GetName(), CollectorFilename: p.CollectorFilename}

	if err = p.writeDeveloperScript(); err != nil {
		return errors.Wrap(err, msg)
	}

	if err = p.writeEntrypoint(); err != nil {
		return errors.Wrap(err, msg)
	}

	if err = p.writeRequirements(); err != nil {
		return errors.Wrap(err, msg)
	}

	return nil
}

func (p *Package) Remove() error {
	return os.RemoveAll(packageName)
}

func (p *Package) Run() error {
	python := os.Getenv("PYTHON_HOME")
	if python == "" {
		python = "python3"
	}

	cmd := exec.Command(python, "-m", "pip", "install", "-r", filepath.Join(packageName, RequirementsFilename))
	cmd.Stderr = io.MultiWriter(os.Stdout, os.Stderr)
	if err := cmd.Start(); err != nil {
		return err
	}
	if err := cmd.Wait(); err != nil {
		return err
	}

	cmd = exec.Command(python, filepath.Join(packageName, EntryFilename))
	cmd.Stdout = os.Stdout
	cmd.Stderr = io.MultiWriter(os.Stdout, os.Stderr)
	if err := cmd.Start(); err != nil {
		return err
	}
	if err := cmd.Wait(); err != nil {
		return err
	}

	return nil
}

func (p *Package) writeDeveloperScript() error {
	filename := filepath.Join(packageName, FeatureFilename)
	f, err := os.OpenFile(filename, os.O_CREATE|os.O_TRUNC|os.O_RDWR, 777)
	if err != nil {
		return err
	}
	defer f.Close()

	if err = GenSettings(f, p.Settings); err != nil {
		return err
	}
	if _, err = f.Write(p.DeveloperScript.GetData()); err != nil {
		return err
	}
	return nil
}

func (p *Package) writeEntrypoint() error {
	f, err := os.OpenFile(filepath.Join(packageName, EntryFilename), os.O_CREATE|os.O_TRUNC|os.O_RDWR, 777)
	if err != nil {
		return err
	}
	defer f.Close()

	return GenEntrypoint(f, p.entrypoint, p.Commit)
}

func (p *Package) writeRequirements() error {
	f, err := os.OpenFile(filepath.Join(packageName, RequirementsFilename), os.O_CREATE|os.O_TRUNC|os.O_RDWR, 777)
	if err != nil {
		return err
	}
	defer f.Close()

	_, err = f.Write(p.Requirements)
	return err
}
