centmassmap = """def localparticle(p):
	return (0, p.mass*p.position[0], p.mass*p.position[1],p.mass*p.position[2])
""" 
centmassVelmap = """def localparticle(p):
	return (0, p.mass*p.velocity[0], p.mass*p.velocity[1],p.mass*p.velocity[2])
""" 
centmassreduce = """def localparticle(p):
	x = [0.0, 0.0, 0.0]
	for i in range(0, len(p.list)) :
		x[0] += p.list[i][1]
		x[1] += p.list[i][2]
		x[2] += p.list[i][3]
	return (0, x[0], x[1], x[2])
"""

def boxstat(group, family) :
    """Print statistics for a group as in the tipsy boxstat command
    Arguments are group and family"""
    nPart = charm.getNumParticles(group, family)
    mass = charm.getAttributeSum(group, family, 'mass')
    bBox = charm.getAttributeRangeGroup(group, family, 'position')
    size = map(lambda min, max : max - min, bBox[0], bBox[1])
    center = map(lambda min, max : 0.5*(max + min), bBox[1], bBox[0])
    groupfamname = group + 'FAM' + family
    charm.createGroup_Family(groupfamname, group, family)
    mmoment = charm.reduceParticle(groupfamname, centmassmap, centmassreduce, None)
    cm = [mmoment[0][1]/mass, mmoment[0][2]/mass, mmoment[0][3]/mass]
    vmoment = charm.reduceParticle(groupfamname, centmassVelmap, centmassreduce, None)
    vcm = [vmoment[0][1]/mass, vmoment[0][2]/mass, vmoment[0][3]/mass]
    print 'number of', family, 'particles =', nPart
    print 'mass =', mass
    print 'center coordinates =', center
    print 'size =', size
    print 'center of mass coordinates =', cm
    print 'center of mass velocity =', vcm

