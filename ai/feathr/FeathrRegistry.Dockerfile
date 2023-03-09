# Stage 1: build frontend ui
FROM node:16-alpine as ui-build
WORKDIR /usr/src/ui
COPY ./ui .

## Use api endpoint from same host and build production static bundle
RUN echo 'REACT_APP_API_ENDPOINT=' >> .env.production
RUN npm install && npm run build

# Stage 2: build backend and start nginx to as reserved proxy for both ui and backend
FROM python:3.9

## Install dependencies
RUN apt-get update -y && apt-get install -y nginx freetds-dev
COPY ./registry /usr/src/registry
WORKDIR /usr/src/registry/sql-registry
RUN pip install -r requirements.txt
WORKDIR /usr/src/registry/purview-registry
RUN pip install -r requirements.txt

## Remove default nginx index page and copy ui static bundle files
RUN rm -rf /usr/share/nginx/html/*
COPY --from=ui-build /usr/src/ui/build /usr/share/nginx/html
COPY ./deploy/nginx.conf /etc/nginx/nginx.conf

## Start service and then start nginx
WORKDIR /usr/src/registry
COPY ./deploy/start.sh .
RUN ["chmod", "+x", "./start.sh"]
CMD ["/bin/sh", "-c", "./start.sh"]