import charm, os
#===============================================================================
# Writes a given group to a tipsy ascii file.
# Last Updated: Eric Carlson 8/12/2010
#===============================================================================
def writeGroupAscii(group='All', filename='groupAscii.dat'):
    """Writes out a tipsy-ascii file for the group (filename='groupAscii.dat')."""
    numStars = charm.getNumParticles(group,'star')
    numGas   = charm.getNumParticles(group,'gas')
    numDark  = charm.getNumParticles(group,'dark')
    numTotal = numStars + numGas + numDark
    #===========================================================================
    # This region outputs the ascii file to be read in by tipsy
    #===========================================================================
    f = open(filename, 'w') # overwrites pre-existing file
    f.write(str(numTotal) + ' ' + str(numGas) + ' ' + str(numStars))
    f.write('\n3')
    f.write('\n'+ str(charm.getTime())+ '\n')
    f.close()
    for each in ['star','dark','gas']:charm.createScalarAttribute(each, 'tmpWorking')
    def writeAndAppend(attribute):
        charm.writeGroupArray('tmpGalBox', attribute, '/tmp/out.tmp')
        os.system('tail -n +2 /tmp/out.tmp >> ' + filename)
        return
    families = ['gas','dark','star']
    for each in families:
        charm.createGroup_Family('tmpGalBox', 'galBox', each)
        writeAndAppend('mass')
    # Positions
    for i in range(0,3):
        for each in families:
            charm.createGroup_Family('tmpGalBox', 'galBox', each)
            charm.runLocalParticleCodeGroup('tmpGalBox', vectorWriter, ('pos', i))
            writeAndAppend('tmpWorking')
    # Velocities
    for i in range(0,3):
        for each in families:
            charm.createGroup_Family('tmpGalBox', 'galBox', each)
            charm.runLocalParticleCodeGroup('tmpGalBox', vectorWriter, ('vel', i))
            writeAndAppend('tmpWorking')
    # Star and dark softening
    for each in ['dark', 'star']:
        charm.createGroup_Family('tmpGalBox', 'galBox', each)
        writeAndAppend('softening')        
    # Gas attributes
    charm.createGroup_Family('tmpGalBox', 'galBox', 'gas')
    for each in ['density','temperature','softening','metals']:
        writeAndAppend(each)
    #Star stuff
    charm.createGroup_Family('tmpGalBox', 'galBox', 'star')
    for each in ['metals','formationtime']:
        writeAndAppend(each)
    #potential
    for each in families:
        charm.createGroup_Family('tmpGalBox', 'galBox', each)
        writeAndAppend('potential')
    charm.deleteGroup('tmpGalBox')
    #print 'num lines in ' + filename + ' should be: ' + str(9*numTotal+3*numGas+2*numStars+3)
    return
vectorWriter              = """def localparticle(p):
    attr, comp  = p._param
    if (attr == 'pos'):
        p.tmpWorking = p.position[comp]
    elif (attr == 'vel'):
        p.tmpWorking = p.velocity[comp]
    return
"""
