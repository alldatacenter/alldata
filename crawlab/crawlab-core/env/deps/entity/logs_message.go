package entity

import "go.mongodb.org/mongo-driver/bson/primitive"

type LogsMessage struct {
	TaskId primitive.ObjectID `json:"task_id"`
	Lines  []string           `json:"lines"`
}
