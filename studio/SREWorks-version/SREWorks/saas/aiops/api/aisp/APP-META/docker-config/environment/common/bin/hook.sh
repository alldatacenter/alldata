# add hooks for jbossctl when app start, stop.
# apps can add custom actions in these functions, default are empty.

# before application server start, jvm process not exists.
beforeStartApp() {
	return
}

# after application server start, localhost:8080 is ready.
afterStartApp() {
	return
}

# before application server stop, localhost:8080 is available.
beforeStopApp() {
	return
}

# after application server stop, jvm process has exited.
afterStopApp() {
	return
}