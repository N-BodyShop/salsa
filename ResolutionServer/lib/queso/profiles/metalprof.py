import charm
from queso.globals import metals, getgroupradius, findcenter

def metalProfile(group,nbins=20,center='pot', family='bar'):
    if ((group in charm.getGroups())==False):
        print "Group does not exist, please try again."
        return
    if ((family in ['star','bar','gas'])==False):
        print 'Invalid family.  Need "bar","gas", or "star"' 
        return  
    center = findcenter.findCenter(group2=group,method=center)
    radialStep = getgroupradius.getGroupRadius(group,center)/nbins
    metalRad  = [0]*(nbins)
    metalProf = [0]*(nbins)
            
    numStar = charm.getNumParticles(group, 'star')
    numGas  = charm.getNumParticles(group, 'gas')
    
    if (numGas==0) & (numStar==0):
        return metalProf
    if (numStar !=0) & (family=='star'):
        charm.createGroup_Family('groupMetStar',group, 'star')
        metalStar = charm.reduceParticle('groupMetStar',massMetal,reduceMassMetal,(radialStep,center))
        for i in range(len(metalStar)): 
            metalProf[metalStar[i][0]] = metalStar[i][1]/metalStar[i][2]
    
    if (numGas !=0) & (family=='gas'):
        charm.createGroup_Family('groupMetGas',group , 'gas')
        metalGas  = charm.reduceParticle('groupMetGas' ,massMetal,reduceMassMetal,(radialStep,center))
        for i in range(len(metalGas)):
            metalProf[metalGas[i][0]] = metalGas[i][1]/metalGas[i][2]
    
    if (family=='bar'):
        charm.createGroup_Family('groupMetGas',group , 'gas')
        metalProfGas   = [[0,0]]*nbins
        metalProfStar  = [[0,0]]*nbins
        
        metalGas  = charm.reduceParticle('groupMetGas' ,massMetal,reduceMassMetal,(radialStep,center))
        for i in range(len(metalGas)):
            metalProfGas[metalGas[i][0]][0] = metalGas[i][1]
            metalProfGas[metalGas[i][0]][1] = metalGas[i][2]
        metalStar = charm.reduceParticle('groupMetStar',massMetal,reduceMassMetal,(radialStep,center))
        for i in range(len(metalStar)): 
            metalProfStar[metalStar[i][0]][0] = metalStar[i][1]
            metalProfStar[metalStar[i][0]][1] = metalStar[i][2]
        for i in range(len(metalProf)):
            metalTot = metalProfStar[i][0] + metalProfGas[i][0] 
            massTot  = metalProfStar[i][1] + metalProfGas[i][1]
            if massTot !=0: metalProf[i] = metalTot/massTot
    for i in range(nbins):
        metalRad[i] = (i+.5)*radialStep
    return(metalRad,metalProf)
    

massMetal              = """import math
def localparticle(p):
    _radialStep, _center  = p._param
    relPosition = (p.position[0]-_center[0],p.position[1]-_center[1],p.position[2]-_center[2])
    radius = math.sqrt(relPosition[0]**2+relPosition[1]**2+relPosition[2]**2)
    bin = int(math.floor(radius/_radialStep))
    if (math.floor(radius/_radialStep)==radius/_radialStep): bin-=1
    return  (bin,p.metals*p.mass,p.mass)
"""
reduceMassMetal       = """def localparticle(p):
    metalMassSum = 0
    massSum      = 0
    bin = p.list[0][0]
    for i in range(0, len(p.list)) :
        metalMassSum += p.list[i][1]
        massSum      += p.list[i][2]
    return     (bin,metalMassSum, massSum)
"""
