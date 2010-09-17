import charm, findcenter, math, virialgroup, queso,getgroupradius
from queso import quesoConfig

def getEpsilonDist(group='gal', nBins=50, epsCut=2., center='pot'):
    """Returns a histogram of dimension 2 x nbins of stellar epsilons (jz/jcirc) and mass-fraction.  epsCut defines the histogram bound in both directions. Center can be either 'pot' or 'com'"""
    numRotBins   = 400 #number of points at which we linearly interpolate 
    radiusVCM    = 0.25 #fraction of group radius to use when calculating COM velocity
    radiusAngMom = 0.25 #fraction of group radius to use when calculating stellar angular momentum
    
    
    a = charm.getTime()
    if ((group in charm.getGroups()==False)) & (group !='gal'):
        print "Group does not exist, please try again."
        return
    if (group == 'gal'): 
        rgal = quesoConfig.rgalFraction*virialgroup.getVirialGroup()
        center=findcenter.findCenter(group2='virialGroup', method=center)
        charm.createGroupAttributeSphere('gal', 'All', 'position', center[0], center[1], center[2], rgal)
    else:
        center=findcenter.findCenter(group2=group, method=center)
        
    # Find maximum radius
    charm.createGroup_Family('tmp',group,'star')
    maxRad = getgroupradius.getGroupRadius('tmp', center)
    massDist = [0]*(numRotBins+1)
    rad = [0]*(numRotBins+1)
    radialStep = maxRad/numRotBins
    #populate mass bins
    for i in range(numRotBins+1): 
        charm.createGroupAttributeSphere('tmp', 'All', 'position', center[0], center[1], center[2], radialStep*i)
        massDist[i]  = charm.getAttributeSum('tmp','star','mass')
        massDist[i] += charm.getAttributeSum('tmp','dark','mass')
        massDist[i] += charm.getAttributeSum('tmp','gas' ,'mass')
        #Note: I tried shells instead of spheres and it made no real difference.
        rad[i] = radialStep*i
        
    vCM = getVelCMStars(center2=center, radius=radiusVCM*maxRad) #COM velocity
    angMomVec = getNormalStarAngMom(center2=center, radius=radiusAngMom*maxRad, comVel=vCM) #Ang Mom Velocity
    #vCM = getVelCMStars(center2=center, radius=10/(a*quesoConfig.kpcunit)) #COM velocity
    #angMomVec = getNormalStarAngMom(center2=center, radius=6/(a*quesoConfig.kpcunit), comVel=vCM) #Ang Mom Velocity
    
    param = (center, vCM, angMomVec, maxRad, numRotBins,massDist, nBins,epsCut)
    charm.createGroupAttributeSphere('tmp', 'All', 'position', center[0], center[1], center[2], maxRad)
    charm.createGroup_Family('tmp',group,'star')
    #calc total stellar mass for mass weighted average
    reduceResult = charm.reduceParticle('tmp', mapEpsilon, reduceEpsilon, param) 
    sMass = charm.getAttributeSum('tmp','star', 'mass')
    epsilonDist = [0]*nBins
    eps  = [0]*nBins
    for i in range(nBins): eps[i] = -epsCut+ 2*epsCut/nBins*(i)
    for i in range(len(reduceResult)):
        try:
            bin = reduceResult[i][0]
            epsilonDist[bin] = reduceResult[i][1]/sMass #return the mass fraction
        except:{}
    return (eps, epsilonDist)
def getVelCMStars(center2, radius):
    "Returns the vector for the center of mass velocity of a group of stars defined by center2 coordinates and a radius."
    charm.createGroupAttributeSphere('tmpGal', 'All', 'position', center2[0], center2[1], center2[2], radius) #use cmVelCompare2.py to choose length
    charm.createGroup_Family('tmpStarGroup', 'tmpGal', 'star') #select star particles only
    massStar = charm.getAttributeSum('tmpGal', 'star', 'mass')
    mass = massStar #calculate total mass
    vmomentStar = charm.reduceParticle('tmpStarGroup', centMassVelMap, centMassReduce, None)
    vmoment = [vmomentStar[0][1],vmomentStar[0][2],vmomentStar[0][3]]
    vcm = (vmoment[0]/mass, vmoment[1]/mass, vmoment[2]/mass)
    #Clean up
    charm.deleteGroup('tmpGal')
    charm.deleteGroup('tmpStarGroup')
    return vcm #return the COM velocity vector    
def getNormalStarAngMom(center2, radius, comVel=None):
    """Calculates the stellar angular momentum vector given a center and radius. By default the COM velocity is calculated unless it is passed."""
    if (comVel==None):vCM=getVelCMStars(center2, radius)
    else: vCM = comVel 
    charm.createGroupAttributeSphere('groupAll', 'All', 'position', center2[0], center2[1], center2[2], radius) 
    charm.createGroup_Family('groupStar', 'groupAll', 'star')
    starAngMom1  = charm.reduceParticle('groupStar', getParticleAngMomentum, sumAngMomentum, [center2,vCM])
    starAngMom = (starAngMom1[0][1],starAngMom1[0][2],starAngMom1[0][3])
    starAngMomMag = math.sqrt(starAngMom[0]**2+starAngMom[1]**2+starAngMom[2]**2)
    starAngMom = (starAngMom[0]/starAngMomMag,starAngMom[1]/starAngMomMag,starAngMom[2]/starAngMomMag)
    return  starAngMom


mapEpsilon               = """import math
def localparticle(p):
    def crossprod(a, b) :
        return [a[1]*b[2] - a[2]*b[1], a[2]*b[0] - a[0]*b[2], a[0]*b[1] - a[1]*b[0]]
    _center, _vcm, _zAngMomNorm, _maxRad, _numRotBins, _massDist, _nBins, _epsCut = p._param #maxRad is in simulation units
    j  = [0,0,0]
    relPosition = [p.position[0]-_center[0],p.position[1]-_center[1],p.position[2]-_center[2]]
    relVelocity = [p.velocity[0]-_vcm[0],p.velocity[1]-_vcm[1],p.velocity[2]-_vcm[2]]
    #take cross-product of relative position and velocity
    j = crossprod(relPosition, relVelocity)
    #take the dot product with the normalized angular momentum. 
    jZ = j[0]*_zAngMomNorm[0]  +  j[1]*_zAngMomNorm[1]  +  j[2]*_zAngMomNorm[2]
    #now do linear interpolation to determine M(r) and thus jCirc from the _massDist list
    radius     = math.sqrt(relPosition[0]*relPosition[0] + relPosition[1]*relPosition[1] + relPosition[2]*relPosition[2])
    radiusStep = _maxRad/_numRotBins
    radiusInSteps  = radius/radiusStep
    floor = int(math.floor(radiusInSteps))
    ceil  = int(math.ceil(radiusInSteps))
    #Rare case they are the same, radius excess will be zero so just let the calculation go as normal.
    if (ceil==floor): ceil += 1  
    
    
    if (ceil==_numRotBins+1): ceil -=1 
    slope = (_massDist[ceil]-_massDist[floor]) #denominator always 1 in bin space so just take difference
    
    radiusExcess = radiusInSteps-float(floor)
    jCirc = math.sqrt((_massDist[floor]+slope*radiusExcess)*radius)
    epsilon = 0.
    bin = 0
    if (jCirc == 0): 
        #Central particle has no angular momentum
        return (0,epsilon)
    epsilon = jZ/jCirc
    p.epsilon = epsilon
    
    epsStep = 2.0*_epsCut/_nBins
    #if (math.fabs(epsilon) > _epsCut):return(0,0)
    #else:
    #    bin = int(math.floor((_epsCut+epsilon)/epsStep))
    bin = int(math.floor((_epsCut+epsilon)/epsStep))
    return (bin,p.mass)
"""    
reduceEpsilon            = """def localparticle(p):
        mass = 0.
        bin = p.list[0][0]    
        for i in range(len(p.list)):
            mass +=p.list[i][1]
        return (bin,mass)
"""
centMassVelMap             = """def localparticle(p):
    return (0, p.mass*p.velocity[0], p.mass*p.velocity[1], p.mass*p.velocity[2])
""" 
centMassReduce             = """def localparticle(p):
    x = [0.0, 0.0, 0.0]

    for i in range(0, len(p.list)) :
        x[0] += p.list[i][1]
        x[1] += p.list[i][2]
        x[2] += p.list[i][3]
    return (0, x[0], x[1], x[2])
"""
getParticleAngMomentum    = """def localparticle(p):
    #used to find each particles angular momentum.
    j = [0,0,0]
    _centPosition, _comVel = p._param
    relPosition = [p.position[0]-_centPosition[0],p.position[1]-_centPosition[1],p.position[2]-_centPosition[2]]
    relVelocity = [p.velocity[0]-_comVel[0],p.velocity[1]-_comVel[1],p.velocity[2]-_comVel[2]]
    #take cross-product of relative position and velocity
    j[0] = relPosition[1]*relVelocity[2] - relPosition[2]*relVelocity[1]
    j[1] = relPosition[2]*relVelocity[0] - relPosition[0]*relVelocity[2]
    j[2] = relPosition[0]*relVelocity[1] - relPosition[1]*relVelocity[0]
    return (0, p.mass*j[0], p.mass*j[1], p.mass*j[2])
"""
sumAngMomentum            = """def localparticle(p):
    #Totals Angular momentum vector.
    x = [0.0, 0.0, 0.0]
    for i in range(0, len(p.list)) :
        x[0] += p.list[i][1]
        x[1] += p.list[i][2]
        x[2] += p.list[i][3]
    return (0, x[0], x[1], x[2])
"""


