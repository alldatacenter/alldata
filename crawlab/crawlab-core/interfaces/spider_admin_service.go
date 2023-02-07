package interfaces

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type SpiderAdminService interface {
	WithConfigPath
	Start() (err error)
	// Schedule a new task of the spider
	Schedule(id primitive.ObjectID, opts *SpiderRunOptions) (err error)
	// Clone the spider
	Clone(id primitive.ObjectID, opts *SpiderCloneOptions) (err error)
	// Delete the spider
	Delete(id primitive.ObjectID) (err error)
	// SyncGit syncs the git repository of the spider
	SyncGit() (err error)
}
