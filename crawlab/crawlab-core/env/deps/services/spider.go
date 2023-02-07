package services

type SpiderService struct {
	parent *Service
}

func (svc *SpiderService) Init() {
}

func NewSpiderService(parent *Service) (svc *SpiderService) {
	svc = &SpiderService{
		parent: parent,
	}
	return svc
}
