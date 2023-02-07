package models

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type PluginStatus struct {
	Id       primitive.ObjectID `json:"_id" bson:"_id"`
	PluginId primitive.ObjectID `json:"plugin_id" bson:"plugin_id"`
	NodeId   primitive.ObjectID `json:"node_id" bson:"node_id"`
	Status   string             `json:"status" bson:"status"`
	Pid      int                `json:"pid" bson:"pid"`
	Error    string             `json:"error" bson:"error"`
	Node     *Node              `json:"node,omitempty" bson:"-"`
}

func (ps *PluginStatus) GetId() (id primitive.ObjectID) {
	return ps.Id
}

func (ps *PluginStatus) SetId(id primitive.ObjectID) {
	ps.Id = id
}

func (ps *PluginStatus) GetPluginId() (id primitive.ObjectID) {
	return ps.PluginId
}

func (ps *PluginStatus) SetPluginId(id primitive.ObjectID) {
	ps.PluginId = id
}

func (ps *PluginStatus) GetNodeId() (id primitive.ObjectID) {
	return ps.NodeId
}

func (ps *PluginStatus) SetNodeId(id primitive.ObjectID) {
	ps.NodeId = id
}

func (ps *PluginStatus) GetStatus() (status string) {
	return ps.Status
}

func (ps *PluginStatus) SetStatus(status string) {
	ps.Status = status
}

func (ps *PluginStatus) GetPid() (pid int) {
	return ps.Pid
}

func (ps *PluginStatus) SetPid(pid int) {
	ps.Pid = pid
}

func (ps *PluginStatus) GetError() (e string) {
	return ps.Error
}

func (ps *PluginStatus) SetError(e string) {
	ps.Error = e
}

type PluginStatusList []PluginStatus

func (l *PluginStatusList) GetModels() (res []interfaces.Model) {
	for i := range *l {
		d := (*l)[i]
		res = append(res, &d)
	}
	return res
}
