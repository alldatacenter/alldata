package models

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type Log struct {
	Id       primitive.ObjectID `json:"_id" bson:"_id"`
	TaskId   primitive.ObjectID `json:"task_id" bson:"task_id"`
	Content  string             `json:"content" bson:"content"`
	UpdateTs time.Time          `json:"update_ts" bson:"update_ts"`
}
