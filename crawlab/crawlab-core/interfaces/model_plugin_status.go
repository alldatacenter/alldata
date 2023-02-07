package interfaces

import "go.mongodb.org/mongo-driver/bson/primitive"

type PluginStatus interface {
	Model
	GetPluginId() (id primitive.ObjectID)
	SetPluginId(id primitive.ObjectID)
	GetNodeId() (id primitive.ObjectID)
	SetNodeId(id primitive.ObjectID)
	GetStatus() (status string)
	SetStatus(status string)
	GetPid() (pid int)
	SetPid(pid int)
	GetError() (e string)
	SetError(e string)
}
