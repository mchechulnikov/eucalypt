BASE_URL = http://localhost:8080

.DEFAULT_GOAL = help

help:				# prints this help
	@ echo Available targets are:
	@ grep -h -E "^[^\#].+:\s+\#\s+.+$$" ./makefile

build.images:
	@ docker build \
		-t eucalypt/executor:dotnet6 \
		-f ./src/images/dotnet6/dockerfile \
		./src/images/dotnet6

local.root:			# GET /
	@ curl -X GET $(BASE_URL)

local.dotnet.hw:		# POST /dotnet		<-- BODY: ./examples/hello-world.cs
	@ curl -X POST \
		--data-binary @./examples/hello-world.cs \
		$(BASE_URL)/dotnet

local.java.hw:			# POST /java		<-- not supported yet example
	@ curl -X POST \
		--data-binary '<some java code here>' \
		$(BASE_URL)/java
