def checkics() :
    """Function to do an overall check of an IC file for sanity.
    Basic statistics of the particle distribution are printed."""
    print "Gas particles: ", charm.getNumParticles('All', 'gas')
    print "Dark particles: ", charm.getNumParticles('All', 'dark')
    gasmass = charm.getAttributeSum('All', 'gas', 'mass')
    print "Total Gas mass: ", gasmass
    print "Gas mass range: ", charm.getAttributeRangeGroup('All', 'gas', 'mass')
    darkmass = charm.getAttributeSum('All', 'dark', 'mass')
    print "Total Gas mass: ", darkmass
    print "Dark mass range: ", charm.getAttributeRangeGroup('All', 'dark', 'mass')
    print "Total mass: ", gasmass + darkmass
    print "Gas position range: ", charm.getAttributeRangeGroup('All', 'gas', 'position')
    print "Dark position range: ", charm.getAttributeRangeGroup('All', 'dark', 'position')
    print "Gas softening range: ", charm.getAttributeRangeGroup('All', 'gas', 'softening')
    print "Dark softening range: ", charm.getAttributeRangeGroup('All', 'dark', 'softening')
    print "Gas velocity range: ", charm.getAttributeRangeGroup('All', 'gas', 'velocity')
    print "Dark velocity range: ", charm.getAttributeRangeGroup('All', 'dark', 'velocity')
    print "Gas temperature range: ", charm.getAttributeRangeGroup('All', 'gas', 'temperature')
    print "Gas metals range: ", charm.getAttributeRangeGroup('All', 'gas', 'metals')
    
    
