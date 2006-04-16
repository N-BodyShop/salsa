#!/bin/sh -f

echo "group main ++shell /usr/bin/ssh -X" > nodelist
echo host localhost >> nodelist

charmrun ++server ResolutionServer run99.std >& /tmp/Resolution.out &

sleep 4
PORT=`awk '/port/ {print $9}' < /tmp/Resolution.out`

echo PORT is $PORT

java -jar ../InteractiveClient/Salsa.jar localhost $PORT $*