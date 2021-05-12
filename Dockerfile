FROM openjdk:8-jdk-alpine
RUN apk add bind-tools 
RUN apk add --no-cache bash shadow

# Installs latest Chromium package.
RUN echo "http://dl-cdn.alpinelinux.org/alpine/v3.13/main" >> /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/v3.13/community" >> /etc/apk/repositories \
    && echo "http://dl-cdn.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories \
    && apk upgrade -U -a \
    && apk add \
    libstdc++ \
    chromium \
    harfbuzz \
    nss \
    freetype \
    ttf-freefont \
    font-noto-emoji \
    wqy-zenhei \
    && rm -rf /var/cache/* \
    && mkdir /var/cache/apk
    
RUN apk add chromium-chromedriver
RUN addgroup -S worker && adduser -S worker -G worker
RUN mkdir -p /opt/worker
RUN chown worker:worker -R /opt/worker
ARG UID=1000
RUN usermod -u $UID worker
USER worker:worker
WORKDIR /opt/worker
COPY src/main/docker/local.conf /etc/fonts/local.conf
ENV CHROME_BIN=/usr/bin/chromium-browser \
    CHROME_PATH=/usr/lib/chromium/

COPY target/alpine-chrome-screenshoter-poc-*.jar alpine-selenium-poc.jar
ENTRYPOINT ["java","-jar","alpine-selenium-poc.jar"]