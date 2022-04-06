#!/bin/bash

docker exec $1 /app/entrypoint.sh "$(cat ./Example.cs)"