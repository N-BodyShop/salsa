#!/bin/sh
#./charmrun ++server ++server-port 1235 $* ./ResolutionServer ../ParallelGravity/scaling/lambs.00200_subsamp_10000.pos ../ParallelGravity/scaling/lambs.00200_subsamp_10000.potential -v -v -v -r -l
./charmrun ++server ++server-port 1235 $* ./ResolutionServer -l -r -v -v -v simlist
