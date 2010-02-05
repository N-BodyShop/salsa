# Sum of masses:

ck.printclient(str(charm.getAttributeSum('All', 'mass'))+'\n')

# Number of particles:
ck.printclient(str(charm.getNumParticles('dark'))+'\n')

# "setsphere"
charm.createGroupAttributeSphere("test2", "All", "position", 0, 0, 0, .1)

# "setbox"
charm.createGroupAttributeBox("test3", "All", "position", 0, 0, 0, .1, 0, 0, 0, .1, 0, 0, 0, .1)

# Some info
ck.printclient(str(charm.getCenterOfMass("test2")))

# testing importing data
tstdat = [1,2,3,4,5,6,7,8,9,10]
charm.importData('testf', 'data', tstdat)

def boxstat(group, family):

# reduce particle    
mapcode = 'def localparticle(p):\n\treturn (int(p.position[0]), p.mass)'
reducecode = 'def localparticle(p):\n\tsum = 0.0\n\tfor i in range(0,len(p.list)) :\n\t\tsum += p.list[i][1]\n\treturn (p.list[0][0], sum)'

x = charm.reduceParticle('All', mapcode, reducecode, None)

# -------------------------------------------
# Black hole merger analysis
# Identify black holes by their negative formation time 
#
charm.loadSimulation('bhmerger2.3.00006')
charm.createGroup_AttributeRange('blackholes', 'All', 'formationtime', -1e38, -1.0e-38)
print charm.getNumParticles('blackholes', 'star')

