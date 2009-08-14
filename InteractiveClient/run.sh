#!/bin/sh
#  Run just the Salsa client
machine=`uname -m`
if test $machine = "x86_64" ; then
   ld_path=jogl-1.1.1-linux-amd64/lib
fi
export LD_LIBRARY_PATH=$ld_path:$LD_LIBRARY_PATH
java -Xmx512m -jar Salsa.jar localhost 1234 $@
