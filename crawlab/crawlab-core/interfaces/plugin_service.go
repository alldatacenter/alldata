package interfaces

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type PluginService interface {
	Module
	SetFsPathBase(path string)
	SetMonitorInterval(interval time.Duration)
	InstallPlugin(id primitive.ObjectID) (err error)
	UninstallPlugin(id primitive.ObjectID) (err error)
	StartPlugin(id primitive.ObjectID) (err error)
	StopPlugin(id primitive.ObjectID) (err error)
	GetPublicPluginList() (res interface{}, err error)
	GetPublicPluginInfo(fullName string) (res interface{}, err error)
}
