FROM nginx:alpine

RUN mkdir -p /app/
COPY ./dist /app/
COPY ./nginx.conf /etc/nginx/nginx.conf


EXPOSE 80
