#!/bin/sh
./charmrun ++server ++server-port 1235 $* ./ResolutionServer ../ParallelGravity/scaling/lambs.00200_subsamp_1M.pos ../ParallelGravity/scaling/lambs.00200_subsamp_1M.potential -v -v -v -r -l
