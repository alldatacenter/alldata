package controllers

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/service"
)

func InitControllers() (err error) {
	modelSvc, err := service.GetService()
	if err != nil {
		return err
	}

	NodeController = newNodeController()
	ProjectController = newProjectController()
	SpiderController = newSpiderController()
	TaskController = newTaskController()
	UserController = newUserController()
	TagController = NewListControllerDelegate(ControllerIdTag, modelSvc.GetBaseService(interfaces.ModelIdTag))
	SettingController = newSettingController()
	LoginController = NewActionControllerDelegate(ControllerIdLogin, getLoginActions())
	ColorController = NewActionControllerDelegate(ControllerIdColor, getColorActions())
	PluginController = newPluginController()
	DataCollectionController = newDataCollectionController()
	ResultController = NewActionControllerDelegate(ControllerIdResult, getResultActions())
	ScheduleController = newScheduleController()
	StatsController = NewActionControllerDelegate(ControllerIdStats, getStatsActions())
	TokenController = newTokenController()
	FilerController = NewActionControllerDelegate(ControllerIdFiler, getFilerActions())
	PluginProxyController = NewActionControllerDelegate(ControllerIdPluginDo, getPluginProxyActions())
	GitController = NewListControllerDelegate(ControllerIdGit, modelSvc.GetBaseService(interfaces.ModelIdGit))
	VersionController = NewActionControllerDelegate(ControllerIdVersion, getVersionActions())
	I18nController = NewActionControllerDelegate(ControllerIdI18n, getI18nActions())
	SystemInfoController = NewActionControllerDelegate(ControllerIdSystemInfo, getSystemInfoActions())
	DemoController = NewActionControllerDelegate(ControllerIdDemo, getDemoActions())
	RoleController = NewListControllerDelegate(ControllerIdRole, modelSvc.GetBaseService(interfaces.ModelIdRole))
	PermissionController = NewListControllerDelegate(ControllerIdPermission, modelSvc.GetBaseService(interfaces.ModelIdPermission))
	ExportController = NewActionControllerDelegate(ControllerIdExport, getExportActions())
	EnvDepsController = NewActionControllerDelegate(ControllerIdEnvDeps, getEnvDepsActions())
	NotificationController = NewActionControllerDelegate(ControllerIdNotification, getNotificationActions())
	FilterController = NewActionControllerDelegate(ControllerIdFilter, getFilterActions())

	return nil
}
