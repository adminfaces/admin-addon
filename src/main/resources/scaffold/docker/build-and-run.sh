#!/bin/sh
mvn clean package && cd docker && ./build.sh && ./run.sh