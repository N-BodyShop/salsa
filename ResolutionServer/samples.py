# Sum of masses:

ck.printclient(str(charm.getAttributeSum('All', 'mass'))+'\n')

# "setsphere"
charm.createGroupAttributeSphere("test2", "All", "position", 0, 0, 0, .1)

# "setbox"
charm.createGroupAttributeBox("test3", "All", "position", 0, 0, 0, .1, 0, 0, 0, .1, 0, 0, 0, .1)

# Some info
ck.printclient(str(charm.getCenterOfMass("test2")))
