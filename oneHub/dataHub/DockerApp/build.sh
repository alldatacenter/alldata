docker pull nginx:alpine
docker image build -t react-app .
docker container run -p 3000:3000 react-app