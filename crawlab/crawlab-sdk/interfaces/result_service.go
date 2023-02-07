package interfaces

import "github.com/crawlab-team/crawlab-sdk/entity"

type ResultService interface {
	SaveItem(item ...entity.Result)
	SaveItems(item []entity.Result)
}
