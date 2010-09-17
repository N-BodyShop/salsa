import charm, os
from queso import vectormath, quesoConfig
from queso.globals import findcenter, virialgroup,galcoord, angmom


def writeBoxAscii(boxRadius, filename, angMomGroup=None, centerMethod ='pot'):
    """Writes out an tipsy-ascii (filename) representation for a box aligned with the galactic coordinate system spanning +-boxRadius (given in kpc) in the galcoord directions. center can be 'pot' or 'com'.  Default angMomGroup is cold galactic gas."""
    if (angMomGroup==None):
        angMomGroup = 'angMomGroup'
        center = findcenter.findCenter(method=centerMethod)
        virialRadius = virialgroup.getVirialGroup()
        charm.createGroupAttributeSphere('angMomGroup', 'All', 'position', center[0], center[1], center[2], virialRadius*quesoConfig.rgalFraction)
        charm.createGroup_Family('angMomGroup', 'angMomGroup', 'gas')
        charm.createGroup_AttributeRange('angMomGroup', 'angMomGroup', 'temperature', 0, quesoConfig.coldGasTemp)
    else: 
        center = findcenter.findCenter(group2=angMomGroup,method=center)
        
    boxRad1 = boxRadius/(charm.getTime()*quesoConfig.kpcunit)
    print 'here'
    galcoord.calcGalacticCoordinates(angMomGroup=angMomGroup, center=centerMethod)
    galZ = vectormath.normalizeVector(angmom.getAngMomVector(angMomGroup))
    galX = vectormath.normalizeVector(vectormath.crossVectors((0,1,0),galZ))
    galY = vectormath.normalizeVector(vectormath.crossVectors(galZ,galX))
    #===========================================================================
    # Create the cube
    #===========================================================================
    cornerVec = vectormath.addVectors(vectormath.multVectorScalar(boxRad1, galX),vectormath.multVectorScalar(boxRad1, galY))
    cornerVec = vectormath.addVectors(cornerVec,vectormath.multVectorScalar(boxRad1, galZ))
    cornerVec = vectormath.subVectors(center, cornerVec)
    edge1 = vectormath.multVectorScalar(2*boxRad1,galX)
    edge2 = vectormath.multVectorScalar(2*boxRad1,galY)
    edge3 = vectormath.multVectorScalar(2*boxRad1,galZ)
    charm.createGroupAttributeBox('galBox', 'All', 'position',
                                  cornerVec[0],cornerVec[1],cornerVec[2],
                                  edge1[0]    ,edge1[1]    ,edge1[2],
                                  edge2[0]    ,edge2[1]    ,edge2[2],
                                  edge3[0]    ,edge3[1]    ,edge3[2])
    numStars = charm.getNumParticles('galBox','star')
    numGas   = charm.getNumParticles('galBox','gas')
    numDark  = charm.getNumParticles('galBox','dark')
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
        p.tmpWorking = p.galPosition[comp]
    elif (attr == 'vel'):
        p.tmpWorking = p.galVelocity[comp]
    return
"""
