#!/bin/sh
#  Run just the Salsa client
export LD_LIBRARY_PATH=`pwd`":$LD_LIBRARY_PATH"
java -Xmx512m -jar Salsa.jar localhost 1234 $@
