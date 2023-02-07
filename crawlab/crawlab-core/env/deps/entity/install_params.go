package entity

import "go.mongodb.org/mongo-driver/bson/primitive"

type InstallParams struct {
	TaskId    primitive.ObjectID `json:"task_id"`
	Cmd       string             `json:"cmd"`
	Names     []string           `json:"names"`
	Upgrade   bool               `json:"upgrade"`
	Proxy     string             `json:"proxy"`
	UseConfig bool               `json:"use_config"`
	SpiderId  primitive.ObjectID `json:"spider_id"`
}
