package services

type TaskService struct {
	parent *Service
}

func (svc *TaskService) Init() {
}

func NewTaskService(parent *Service) (svc *TaskService) {
	svc = &TaskService{
		parent: parent,
	}

	return svc
}
