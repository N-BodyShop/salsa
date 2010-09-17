import charm, math, angmom, findcenter, virialgroup
from queso import quesoConfig, vectormath

def calcGalacticCoordinates(angMomGroup=None, center ='pot'):
    '''Assigns particles a position in the galactic coordinate system. galVelocity = (v+Hr).  angMomGroup'''
    #===========================================================================
    # --Overview--
    # 1.Form transformation matrix
    # 2.Translate the coordinates to the center of the galaxy (or COM velocity)
    # 3.Rotate the coordinate system to be aligned with the angular momentum vector
    # 4.Write information to vector attributes
    #===========================================================================
    if ((angMomGroup in charm.getGroups())==False) & (angMomGroup!=None):
        print "Group does not exist, please try again."
        return
    ##############################
    # Actual Routine
    ##############################
    if (angMomGroup==None):
        angMomGroup = 'angMomGroup'
        cnt = findcenter.findCenter(method=center)
        virialRadius = virialgroup.getVirialGroup()
        charm.createGroupAttributeSphere('angMomGroup', 'All', 'position', cnt[0], cnt[1], cnt[2], virialRadius*quesoConfig.rgalFraction)
        charm.createGroup_Family('angMomGroup', 'angMomGroup', 'gas')
        charm.createGroup_AttributeRange('angMomGroup', 'angMomGroup', 'temperature', 0, quesoConfig.coldGasTemp)
    else: 
        cnt = findcenter.findCenter(group2=angMomGroup,method=center)
        
    angmom.getAngMomVector(group=angMomGroup,center=center)
    # Transformation Matrix
    galZ = vectormath.normalizeVector(angmom.getAngMomVector(group=angMomGroup,center=center))
    galX = vectormath.normalizeVector(vectormath.crossVectors((0,1,0),galZ))
    galY = vectormath.normalizeVector(vectormath.crossVectors(galZ,galX))
    transMatrix = (galX,
                   galY,
                   galZ)
    
    fHubble       = quesoConfig.fHubble0*math.sqrt(quesoConfig.omega0/charm.getTime()**3+(1-quesoConfig.omega0))
    # Galactic Position                    
    charm.createVectorAttribute('star','galPosition')
    charm.createVectorAttribute('gas', 'galPosition')
    charm.createVectorAttribute('dark','galPosition')
    param = (cnt, transMatrix)
    charm.runLocalParticleCodeGroup('All', calcGalPosition, param)
    # Galactic velocity
    vCM=angmom.getVelCM(angMomGroup)
    param = (cnt, transMatrix,vCM, fHubble)
    charm.createVectorAttribute('star','galVelocity')
    charm.createVectorAttribute('gas', 'galVelocity')
    charm.createVectorAttribute('dark','galVelocity')
    charm.runLocalParticleCodeGroup('All', calcGalVelocity, param)
    return

calcGalPosition           = """def localparticle(p):
    def dotProd(v1,v2):
        return (v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2])
    _center, _transMatrix = p._param
    # Shift Relative Position
    relPosition = (p.position[0]-_center[0],p.position[1]-_center[1],p.position[2]-_center[2])
    galPos = [0]*3
    galPos[0] = dotProd(_transMatrix[0],relPosition)
    galPos[1] = dotProd(_transMatrix[1],relPosition)
    galPos[2] = dotProd(_transMatrix[2],relPosition)
    
    p.galPosition = tuple(galPos)
    return
"""
calcGalVelocity           = """def localparticle(p):
    def dotProd(v1,v2):
        return (v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2])
    _center, _transMatrix, _vCM ,_fHubble= p._param
    # Shift Relative Velocity
    relVel = (p.velocity[0]-_vCM[0]+_fHubble*(p.position[0]-_center[0]),
              p.velocity[1]-_vCM[1]+_fHubble*(p.position[1]-_center[1]),
              p.velocity[2]-_vCM[2]+_fHubble*(p.position[2]-_center[2]))
    galVel = [0]*3
    galVel[0] = dotProd(_transMatrix[0],relVel)
    galVel[1] = dotProd(_transMatrix[1],relVel)
    galVel[2] = dotProd(_transMatrix[2],relVel)
    
    p.galVelocity = tuple(galVel)
    return
"""

