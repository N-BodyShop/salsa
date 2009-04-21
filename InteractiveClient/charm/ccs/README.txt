These .java source files began life in the Charm++ distribution, in
	charm/java/charm/ccs/
back in about 2004.  These files track the Charm++ versions, except for CcsThread.java,
which evolved differently in the two copies.

A good project for somebody, someday, would be to merge the doBlockingRequest method
of this CcsThread into the mainline Charm++ CcsThread, and update the rest of this code
to call the updated CcsThread code.

Orion Sky Lawlor, olawlor@acm.org, 2009-04-20
