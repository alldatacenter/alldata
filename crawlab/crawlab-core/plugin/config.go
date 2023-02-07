package plugin

import (
	"github.com/crawlab-team/crawlab-core/config"
	"path"
)

const DefaultPluginFsPathBase = "plugins"
const DefaultPluginDirName = "plugins"
const DefaultPluginBinName = "plugin"
const DefaultPluginInstallCmd = "go build -o ./build/start"
const DefaultWindowsPluginInstallCmd = DefaultPluginInstallCmd + ".exe"

var DefaultPluginDirPath = path.Join(config.DefaultConfigDirPath, DefaultPluginDirName)
