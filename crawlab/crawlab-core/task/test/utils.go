package test

func startAllServices() {
	go T.schedulerSvc.Start()
	go T.handlerSvc.Start()
}

func stopAllServices() {
	go T.schedulerSvc.Stop()
	go T.handlerSvc.Stop()
}
