BASE_URL = http://localhost:8080

.DEFAULT_GOAL = help

help:				# prints this help
	@ echo Available targets are:
	@ grep -h -E "^[^\#].+:\s+\#\s+.+$$" ./makefile

build.images:			# build runners' images
	@ docker build \
		-t eucalypt/executor:dotnet6 \
		-f ./src/src/images/dotnet6/dockerfile \
		./src/images/dotnet6

clean:				# clean all docker images
	@ docker container rm -f $$(docker ps -aq)

docker.events:			# show docker events to check situation
	@ docker events --format "{{.Actor.Attributes.name}},{{.Status}}" --filter 'container=eucalypt-executor-main' 

root:				# GET /			<-- default root result
	@ curl -X GET $(BASE_URL)

dotnet.hw:			# POST /dotnet		<-- BODY: ./examples/hello-world.cs
	@ curl -X POST \
		--data-binary @./examples/hello-world.cs \
		$(BASE_URL)/dotnet

dotnet.inf:			# POST /dotnet		<-- BODY: ./examples/infinite-operation.cs
	@ curl -X POST \
		--data-binary @./examples/infinite-operation.cs \
		$(BASE_URL)/dotnet

dotnet.inv:			# POST /dotnet		<-- BODY: ./examples/invalid.cs
	@ curl -X POST \
		--data-binary @./examples/invalid.cs \
		$(BASE_URL)/dotnet

dotnet.no:			# POST /dotnet		<-- BODY: ./examples/no-output.cs
	@ curl -X POST \
		--data-binary @./examples/no-output.cs \
		$(BASE_URL)/dotnet

dotnet.fio:			# POST /dotnet		<-- BODY: ./examples/file-io.cs
	@ curl -X POST \
		--data-binary @./examples/file-io.cs \
		$(BASE_URL)/dotnet

dotnet.fib:			# POST /dotnet		<-- BODY: ./examples/fib.cs
	@ curl -X POST \
		--data-binary @./examples/fib.cs \
		$(BASE_URL)/dotnet

java.hw:			# POST /java		<-- not supported yet example
	@ curl -X POST \
		--data-binary '<some java code here>' \
		$(BASE_URL)/java

shutdown:			# POST /shutdown	<-- shuts down server gracefully
	@ curl -X POST \
		$(BASE_URL)/shutdown
