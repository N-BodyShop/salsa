#!/bin/csh -f

set PORT = 16435
while ( `netstat -a | grep -c $PORT` != 0 )
  @ PORT = $PORT + 1
  echo $PORT
end

set HOST = `hostname`
./charmrun ++server ++server-port $PORT $* ./wrapper simlist < /dev/null >& /dev/null &
echo "$HOST $PORT"
