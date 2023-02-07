package constants

const (
	PluginTypeApi  = "api"
	PluginTypeWeb  = "web"
	PluginTypeTask = "task"
)

const (
	PluginProtoGrpc = "grpc"
	PluginProtoHttp = "http"
)

const (
	PluginUIComponentTypeView = "view"
	PluginUIComponentTypeTab  = "tab"
)

const (
	PluginDeployModeMaster = "master"
	PluginDeployModeAll    = "all"
)

const (
	PluginInstallTypePublic = "public"
	PluginInstallTypeGit    = "git"
	PluginInstallTypeLocal  = "local"
)

const (
	PluginStatusInstalling   = "installing"
	PluginStatusInstallError = "install_error"
	PluginStatusStopped      = "stopped"
	PluginStatusRunning      = "running"
	PluginStatusError        = "error"
)
