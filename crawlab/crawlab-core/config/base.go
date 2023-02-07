package config

import (
	"github.com/mitchellh/go-homedir"
	"path"
)

var HomeDirPath, _ = homedir.Dir()

var configDirName = ".crawlab"

var DefaultConfigDirPath = path.Join(HomeDirPath, configDirName)

var ConfigName = "config.json"

var DefaultConfigPath = path.Join(HomeDirPath, configDirName, ConfigName)
