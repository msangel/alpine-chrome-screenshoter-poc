#!/bin/sh

mvn clean install
docker build --build-arg UID=$(id -u) -t  apline-seleium-poc .

docker run -it --rm --name apline-seleium-poc \
            --volume=$PWD/volume:/opt/volume \
            apline-seleium-poc 
#            --entrypoint /bin/sh \