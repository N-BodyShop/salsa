#!/bin/sh -f
#
# Shell script to fire up ResolutionServer, the java GUI (Salsa), and
# a command line client (taco).
#
# This is a "remote" version: the server will be started on some remote
# cluster via ssh, and we will connect across to the port.
#
# "charmrun", "ResolutionServer", "taco" and "Salsa.jar" should be installed
# in BINDIR.
# The jogl (Java Open GL) libraries should also be installed in
# BINDIR/jogl-1.1.1-linux-XXX/lib.  Where the XXX indicates the
# architecture: amd64 or i586
# Edit the BINDIR to point at this installation.
#
# Edit the REMOTE_BINDIR to point at the directory containing the charmrun and
# ResolutionServer executables on the remote host.  Also be aware that the
# snapshot file must be a valid path on the REMOTE host.

rm -f /tmp/Resolution-$UID.out

if [ $# -ne 3 ]
then
	echo "Usage: $0 hostname ncpus snapshotfile"
	exit
fi

HOST=$1
CPUS=$2
FILE=$3
BINDIR=/astro/users/trq/bin.x86_64
REMOTE_BINDIR=/astro/users/trq/bin.x86_64

# Build remote script
cat << EOF > /tmp/Resolution_remote.sh
echo "group main ++shell /usr/bin/ssh -X" > /tmp/nodelist.salsa$UID
echo host localhost cpus 1 >> /tmp/nodelist.salsa$UID

$REMOTE_BINDIR/charmrun ++nodelist /tmp/nodelist.salsa$UID ++server ++batch 4 $REMOTE_BINDIR/ResolutionServer +stacksize 131072 +p $CPUS junk
EOF

cat /tmp/Resolution_remote.sh | ssh $HOST >& /tmp/Resolution-$UID.out &

PORT=""
while test -z "$PORT" ; do
PORT=`awk '/port/ {print $9}' < /tmp/Resolution-$UID.out`
sleep 1
echo trying $PORT
done

echo PORT is $PORT

# detect if we have 32 bit or 64 bit Java
if ( /usr/bin/java -version 2>&1 | grep 64-Bit)
then
   echo found 64 bit Java
   ld_path=$BINDIR/jogl-1.1.1-linux-amd64/lib
else
   ld_path=$BINDIR/jogl-1.1.1-linux-i586/lib
   echo assuming 32 bit Java
fi
if test ! -f $ld_path/libgluegen-rt.so ;
then
   echo GL runtime libs not found, expected in $ld_path
   exit
fi	
export LD_LIBRARY_PATH=$ld_path:$LD_LIBRARY_PATH
echo $LD_LIBRARY_PATH
sleep 2
/usr/bin/java -Xmx512m -jar $BINDIR/Salsa.jar $HOST $PORT $FILE &
sleep 1
xterm -e "$BINDIR/taco $HOST $PORT"
