#!/bin/sh -f

echo "group main ++shell /usr/bin/ssh -X" > nodelist
echo host localhost >> nodelist

rm -f /tmp/Resolution.out

charmrun ++server ResolutionServer run99.std >& /tmp/Resolution.out &

PORT=""
while test -z "$PORT" ; do
PORT=`awk '/port/ {print $9}' < /tmp/Resolution.out`
sleep 1
echo trying $PORT
done

echo PORT is $PORT

java -jar ../InteractiveClient/Salsa.jar localhost $PORT $*
