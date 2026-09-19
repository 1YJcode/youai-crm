FROM docker.m.daocloud.io/library/nginx:1.27-alpine

COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY index.html app.js styles.css config.js /usr/share/nginx/html/
COPY logo-youai.png /usr/share/nginx/html/logo-youai.png
COPY docker/frontend-config.js /usr/share/nginx/html/config.js
EXPOSE 80
