import charm, findcenter
from queso import quesoConfig, vectormath
def getMomentInertiaTensor(group2,center='pot'):
    '''Return the moment of inertia tensor for given group.  center can be 'pot' or 'com'.  This is in physical units.
        [[Ixx, Ixy, Ixz],
         [Iyx, Iyy, Iyz],
         [Izx, Izy, Izz]]
    '''
    group = group2
    if ((group2 in charm.getGroups())==False):
        print "Group does not exist, please try again."
        return
    center = findcenter.findCenter(group2=group,method=center)
    triangleTensor = charm.reduceParticle(group , mapMomentInertiaTensor, reduceMomentInertiaTensor, center)[0][1]
    fullTensor = [[0]*3,[0]*3,[0]*3]
    units = quesoConfig.msolunit*((charm.getTime()*quesoConfig.kpcunit)**2)
    fullTensor[0] = vectormath.multVectorScalar(units, triangleTensor[0][0:3])
    fullTensor[1] = vectormath.multVectorScalar(units, [triangleTensor[0][1],triangleTensor[1][0],triangleTensor[1][1]])
    fullTensor[2] = vectormath.multVectorScalar(units, [triangleTensor[0][2],triangleTensor[1][1],triangleTensor[2][0]])
    return fullTensor

mapMomentInertiaTensor       = """def localparticle(p):
    #all dark matter particles inside virialRadius
    #find relative position -> calc components on and above diagonal (since symmetric tensor)
    #pass tuple with 6 components to reduce function 3 other components are implied
    centPosition = p._param
    relPosition = [p.position[0]-centPosition[0],p.position[1]-centPosition[1],p.position[2]-centPosition[2]]
    tensor = [[0]*3,[0]*2,[0]]
    xSquared = relPosition[0]*relPosition[0]
    ySquared = relPosition[1]*relPosition[1]
    zSquared = relPosition[2]*relPosition[2]
    tensor[0][0] = p.mass*(ySquared+zSquared) #Ixx
    tensor[1][0] = p.mass*(xSquared+zSquared) #Iyy
    tensor[2][0] = p.mass*(xSquared+ySquared) #Izz
    tensor[0][1] = -p.mass*relPosition[0]*relPosition[1] #Ixy
    tensor[0][2] = -p.mass*relPosition[0]*relPosition[2] #Ixz
    tensor[1][1] = -p.mass*relPosition[1]*relPosition[2] #Iyz

    return(0,tensor)
"""
reduceMomentInertiaTensor = """def localparticle(p):
    tensor = [[0]*3,[0]*2,[0]]
    for i in range(0,len(p.list)):
        for j in range(0,3):
            for k in range(0,3-j):
                tensor[j][k]+=p.list[i][1][j][k]
    return (0,tensor)
"""
