package models

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type Task struct {
	Id        primitive.ObjectID `json:"_id" bson:"_id"`
	Status    string             `json:"status" bson:"status"`
	Error     string             `json:"error" bson:"error"`
	SettingId primitive.ObjectID `json:"setting_id" bson:"setting_id"`
	Type      string             `json:"type" bson:"type"`
	NodeId    primitive.ObjectID `json:"node_id" bson:"node_id"`
	Action    string             `json:"action" bson:"action"`
	DepNames  []string           `json:"dep_names" bson:"dep_names"`
	Upgrade   bool               `json:"upgrade" bson:"upgrade"`
	UpdateTs  time.Time          `json:"update_ts" bson:"update_ts"`
}
