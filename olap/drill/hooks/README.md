This directory exists for Docker Hub's automatic image building. The hooks 
here override the default build, test and push commands used by Docker Hub
to build the published Docker images of Drill.  The reason they are overridden
is so that we can produce Docker images based on multiple OpenJDK base images,
all using a single Dockerfile.  Also see

../Dockerfile
https://docs.docker.com/docker-hub/builds/advanced/

