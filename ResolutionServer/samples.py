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
