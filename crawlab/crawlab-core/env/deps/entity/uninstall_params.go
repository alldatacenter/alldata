package entity

import "go.mongodb.org/mongo-driver/bson/primitive"

type UninstallParams struct {
	TaskId primitive.ObjectID `json:"task_id"`
	Names  []string           `json:"names"`
	Cmd    string             `json:"cmd"`
}
