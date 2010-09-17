import charm, findcenter, virialgroup, queso
from queso import quesoConfig

def getMeanMetals(group , family='bar'):
    if ((group in charm.getGroups())==False):
        print "Group does not exist, please try again."
        return
    metalStar = [(0,0,0)]
    metalGas  = [(0,0,0)]        
    numStar = charm.getNumParticles(group, 'star')
    numGas  = charm.getNumParticles(group, 'gas')
    
    if (numStar !=0) & (family=='bar' or family=='star'):
        charm.createGroup_Family('groupMetStar',group, 'star')
        metalStar = charm.reduceParticle('groupMetStar',massMetal,reduceMassMetal,None)
    if (numGas !=0) & (family=='bar' or family=='gas'):
        charm.createGroup_Family('groupMetGas',group , 'gas')
        metalGas  = charm.reduceParticle('groupMetGas' ,massMetal,reduceMassMetal,None)
    if (numGas==0) & (numStar==0):
        return 0
    metals = (metalStar[0][1] + metalGas[0][1])/(metalStar[0][2]+metalGas[0][2])
    return metals
    
massMetal              = """def localparticle(p):
    return     (0,p.metals*p.mass,p.mass)
"""
reduceMassMetal       = """def localparticle(p):
    metalMassSum = 0
    massSum      = 0
    for i in range(0, len(p.list)) :
        metalMassSum += p.list[i][1]
        massSum      += p.list[i][2]
    return     (0,metalMassSum, massSum)
"""
