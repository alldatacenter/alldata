FROM node:15-alpine as builder

WORKDIR /react-app
ADD . /react-app
ADD package.json package-lock.json  /react-app/
RUN npm ci
RUN  npm run build

FROM nginx:alpine
COPY --from=builder /react-app/build /usr/share/nginx/html

