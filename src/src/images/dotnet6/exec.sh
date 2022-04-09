#!/bin/bash

cd /exec-dir || exit

cp -R /restore-dir/. .
echo "$1" > ./Program.cs

if dotnet build -v q > build_log.txt; then
    dotnet ./bin/Debug/net6.0/Main.dll
else
   cat build_log.txt
fi
