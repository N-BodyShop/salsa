import charm, findcenter, virialgroup, queso, math
from queso import quesoConfig

def getAngMomVector(group, center='pot'):
    """Returns specific angular momentum vectors in physical units.  Center can be 'pot' or 'com'"""
    if ((group in charm.getGroups())==False):
        print "Group does not exist, please try again."
        return
    def multVectorScalar(scalar,vector):
        return (vector[0]*scalar,vector[1]*scalar,vector[2]*scalar)
    center = findcenter.findCenter(group2=group,method=center)
    #find COM Velocity
    vCM=getVelCM(group)
    
    angMomVector  = charm.reduceParticle(group, getParticleAngMomentum, sumAngMomentum, [center,vCM]) 
    mass   = charm.getAttributeSum(group, 'gas',  'mass')
    mass  += charm.getAttributeSum(group, 'star', 'mass')
    mass  += charm.getAttributeSum(group, 'dark', 'mass')
    unitConvert = quesoConfig.kpcunit*quesoConfig.velocityunit #masses are divided out anyway.
    #Convert this into a specific angular momentum 3-vector
    angMomVector    = multVectorScalar(unitConvert/mass, (angMomVector[0][1] ,angMomVector[0][2] ,angMomVector[0][3]))
    return angMomVector

def getEulerAngles(vz):
    """Takes the z angular momentum vector and returns the first two Euler angles w.r.t. the standard coordinate system."""    
    
    vlen = math.sqrt(vz[0]**2+vz[1]**2+vz[2]**2)
    vz = vz[0]/vlen,vz[1]/vlen,vz[2]/vlen
    
    if (vz[2]==1):
        return (0,0)
    if (vz[2] == -1):
        return (math.pi,0)

    theta = math.acos(vz[2])
    phi = math.atan2(vz[0], vz[1])
    return (theta,phi)

def getVelCM(group):
    """Finds and returns a COM velocity vector for a given group."""
    mass  = charm.getAttributeSum(group, 'gas', 'mass')
    mass += charm.getAttributeSum(group, 'star', 'mass')
    mass += charm.getAttributeSum(group, 'dark', 'mass')
    vmoment  = charm.reduceParticle(group, centMassVelMap, centMassReduce, None)[0][1:4]
    vcm = (vmoment[0]/mass, vmoment[1]/mass, vmoment[2]/mass)
    return vcm #return the COM velocity vector

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
