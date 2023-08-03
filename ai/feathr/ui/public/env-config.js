// This is a placeholder for environment variables that will get injected during runtime
// React code is running on browser side, there is no such thing as environment variable. 
// However, like client id, tenant id are only fetchable during runtime, they can not be pre-packaged
// To resolve this, a bash script env.sh will run when container starts and injects runtime environments in this file.
// Then react app can get these runtime variables on the flying on client side.
window.environment = {}
