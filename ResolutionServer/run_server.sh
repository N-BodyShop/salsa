#!/bin/sh
./charmrun ++server ++server-port 1235 $* ./ResolutionServer ../TreeDataFormat/test/particles_subsamp.x86.pos ../TreeDataFormat/test/particles_subsamp.x86.potential -v -v -v -l
