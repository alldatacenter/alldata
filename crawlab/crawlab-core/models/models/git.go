package models

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type Git struct {
	Id            primitive.ObjectID `json:"_id" bson:"_id"`
	Url           string             `json:"url" bson:"url"`
	AuthType      string             `json:"auth_type" bson:"auth_type"`
	Username      string             `json:"username" bson:"username"`
	Password      string             `json:"password" bson:"password"`
	CurrentBranch string             `json:"current_branch" bson:"current_branch"`
	AutoPull      bool               `json:"auto_pull" bson:"auto_pull"`
}

func (t *Git) GetId() (id primitive.ObjectID) {
	return t.Id
}

func (t *Git) SetId(id primitive.ObjectID) {
	t.Id = id
}

type GitList []Git

func (l *GitList) GetModels() (res []interfaces.Model) {
	for i := range *l {
		d := (*l)[i]
		res = append(res, &d)
	}
	return res
}
