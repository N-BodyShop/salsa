#!/bin/csh -f

set PORT = 16512
while ( `netstat -a | grep -c $PORT` != 0 )
  @ PORT = $PORT + 1
  #echo $PORT
end

set HOST = `hostname`
#./charmrun ++server ++server-port $PORT ./wrapper $* simlist < /dev/null >& /dev/pts/2 &
./charmrun ++server ++server-port $PORT ./ResolutionServer $* simlist
echo "$HOST $PORT"
