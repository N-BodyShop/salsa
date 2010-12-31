import math, traceback

centmassmap = """def localparticle(p):
	return (0, p.mass*p.position[0], p.mass*p.position[1], p.mass*p.position[2])
""" 
centmassVelmap = """def localparticle(p):
	return (0, p.mass*p.velocity[0], p.mass*p.velocity[1], p.mass*p.velocity[2])
""" 
angmomMap = """def localparticle(p):
	j = [0,0,0]
	j[0] = p.position[1]*p.velocity[2] - p.position[2]*p.velocity[1]
	j[1] = p.position[2]*p.velocity[0] - p.position[0]*p.velocity[2]
	j[2] = p.position[0]*p.velocity[1] - p.position[1]*p.velocity[0]
	return (0, p.mass*j[0], p.mass*j[1], p.mass*j[2])
"""
centmassreduce = """def localparticle(p):
	x = [0.0, 0.0, 0.0]
	for i in range(0, len(p.list)) :
		x[0] += p.list[i][1]
		x[1] += p.list[i][2]
		x[2] += p.list[i][3]
	return (0, x[0], x[1], x[2])
"""

def boxstat(group, family='all') :
    import charm
    """Print statistics for a group as in the tipsy boxstat command
    Arguments are group and family
    
    Return physical parameters of particles:
        number, mass, center of box, size of box, center of mass, 
        center of mass's velocity, angular momentum vector.

    Format output to say if gas, dark, star, baryon, or all.
    
    Check error cases: Bad input, Not a proper data type, Box not loaded."""
    
    # check if simulation loaded
    if charm.getGroups() == None :
        raise StandardError('Simulation not loaded')

    # There is a potential issue with the capitalization of "all" being mixed
    # in different implementations. Take either.
    if family == 'All':
        family = 'all'
    if group == 'all':
        group = 'All'    

    # Prepare values based on selected family
    if family == 'all':
        nPartGas = charm.getNumParticles(group, 'gas')
        nPartDark = charm.getNumParticles(group, 'dark')
        nPartStar = charm.getNumParticles(group, 'star')
        mass = charm.getAttributeSum(group, 'gas', 'mass')
        mass += charm.getAttributeSum(group, 'dark', 'mass')
        mass += charm.getAttributeSum(group, 'star', 'mass')
        bBox = list(charm.getAttributeRangeGroup(group, 'gas', 'position'))
        bBoxTmp = charm.getAttributeRangeGroup(group, 'dark', 'position')
        bBox[0] = map(lambda x, y : min(x,y), bBox[0], bBoxTmp[0])
        bBox[1] = map(lambda x, y : max(x,y), bBox[1], bBoxTmp[1])
        bBoxTmp = charm.getAttributeRangeGroup(group, 'star', 'position')
        bBox[0] = map(lambda x, y : min(x,y), bBox[0], bBoxTmp[0])
        bBox[1] = map(lambda x, y : max(x,y), bBox[1], bBoxTmp[1])
        groupfamname = group
    elif family == 'baryon':
        nPartGas = charm.getNumParticles(group, 'gas')
        nPartStar = charm.getNumParticles(group, 'star')
        mass = charm.getAttributeSum(group, 'gas', 'mass')
        mass += charm.getAttributeSum(group, 'star', 'mass')
        bBox = list(charm.getAttributeRangeGroup(group, 'gas', 'position'))
        bBoxTmp = charm.getAttributeRangeGroup(group, 'star', 'position')
        bBox[0] = map(lambda x, y : min(x,y), bBox[0], bBoxTmp[0])
        bBox[1] = map(lambda x, y : max(x,y), bBox[1], bBoxTmp[1])
        groupfamname = group
    else :
        nPart = charm.getNumParticles(group, family)
        mass = charm.getAttributeSum(group, family, 'mass')
        bBox = charm.getAttributeRangeGroup(group, family, 'position')
        groupfamname = group + 'FAM' + family
        charm.createGroup_Family(groupfamname, group, family)

    # Derive box properties
    size = map(lambda min, max : max - min, bBox[0], bBox[1])
    center = map(lambda min, max : 0.5*(max + min), bBox[1], bBox[0])
    mmoment = charm.reduceParticle(groupfamname, centmassmap, centmassreduce, None)
    cm = [mmoment[0][1]/mass, mmoment[0][2]/mass, mmoment[0][3]/mass]
    vmoment = charm.reduceParticle(groupfamname, centmassVelmap, centmassreduce, None)
    vcm = [vmoment[0][1]/mass, vmoment[0][2]/mass, vmoment[0][3]/mass]
    angmom1 = charm.reduceParticle(groupfamname, angmomMap, centmassreduce, None)

    # Apply parallel axis theorem and make specific
    angmom = [0,0,0]
    angmom[0] = (angmom1[0][1] - mass*(cm[1]*vcm[2] - cm[2]*vcm[1]))/mass
    angmom[1] = (angmom1[0][2] - mass*(cm[2]*vcm[0] - cm[0]*vcm[2]))/mass
    angmom[2] = (angmom1[0][3] - mass*(cm[0]*vcm[1] - cm[1]*vcm[0]))/mass

    # Write output header according to type of particles
    if family == 'all' :
        print 'number of dark, gas and star particles =', nPartDark, nPartGas, nPartStar
    elif family == 'baryon' :
        print 'number of baryon particles =', (nPartGas + nPartStar)
    else:
        print 'number of', family, 'particles =', nPart
    # Write physical parameters
    print 'mass =', mass
    print 'center coordinates =', center
    print 'size =', size
    print 'center of mass coordinates =', cm
    print 'center of mass velocity =', vcm
    print 'angular momentum vector =', angmom

def setsphere(xcenter, ycenter, zcenter, radius, group, sourcegroup='All') :
    """Select particles within radius from coordinate."""
    import charm
    # check if simulation loaded
    if charm.getGroups() == None :
        raise StandardError('Simulation not loaded')

    charm.createGroupAttributeSphere(group, sourcegroup, 'position', xcenter, ycenter, zcenter, radius)

def setbox(xcenter, ycenter, zcenter, xradius, yradius, zradius,  group, sourcegroup='All') :
    """Select particles within a box centered on coordinate which extends
    radius in either direction along each axis."""
    # check if simulation loaded
    if charm.getGroups() == None :
        raise StandardError('Simulation not loaded')

    edge = [xradius, yradius, zradius]
    corner = map(lambda center, radius : center - radius, [xcenter, ycenter, zcenter], edge)
    edge = map(lambda radius : radius * 2, edge)

    charm.createGroupAttributeBox(group, sourcegroup, 'position', corner[0], corner[1], corner[2], edge[0], 0, 0, 0, edge[1], 0, 0, 0, edge[2])

def markgal(group, maxTemp, minRho) :
    """Mark gas particles that are likely to be in galaxies based on selected
    temperature and density thresholds. Marked particles are stored in a group
    called "mark" which will be replaced every time a marking command is run."""
    # check if simulation loaded
    if charm.getGroups() == None :
        raise StandardError('Simulation not loaded')
    
    # take only gas particles
    charm.createGroup_Family('mark', group, 'gas')
    # get min temp, get max density
    minTemp = charm.getAttributeRangeGroup('mark', 'gas', 'temperature')[0]
    maxRho = charm.getAttributeRangeGroup('mark', 'gas', 'density')[1]
    # reduce members according to criteria
    charm.createGroup_AttributeRange('mark', 'mark', 'temperature', minTemp, maxTemp)
    charm.createGroup_AttributeRange('mark', 'mark', 'density', minRho, maxRho)

def unmarkall() :
    "unmark all particles"
    charm.unmarkParticlesGroup('All')
    
def markbox(group) :
    """Mark the contents of group. Marked particles are indicated by a "mark"
    attribute being set to non-zero."""

    # check if simulation loaded
    if charm.getGroups() == None :
        raise StandardError('Simulation not loaded')
    
    charm.markParticlesGroup(group)

def markgal(group, max_temp, min_rho) :
    """Mark all the gas particles in group that have temperatures less
    than or equal to max_temp and densities greater than or equal to
    min_rho. This command can be used to mark those gas particles that
    are likely to be in galaxies."""

    # check if simulation loaded
    if charm.getGroups() == None :
        raise StandardError('Simulation not loaded')
    
    # get opposing boundaries from simulation values
    max_rho = charm.getAttributeRange('gas', 'density')[1]
    min_temp = charm.getAttributeRange('gas', 'temperature')[0]
    
    # play the createGroup shuffle
    charm.createGroup_Family('tmp_group_fam', group, 'gas')
    charm.createGroup_AttributeRange('tmp_group1', 'tmp_group_fam', 'density',
                                     min_rho, max_rho)
    charm.createGroup_AttributeRange('tmp_group2', 'tmp_group1',
                                     'temperature', min_temp, max_temp)
    charm.markParticlesGroup('tmp_group2')
    
    # remove the temporary groups to avoid clutter
    charm.deleteGroup('tmp_group_fam')
    charm.deleteGroup('tmp_group1')
    charm.deleteGroup('tmp_group2')

def writemark(group, filename) :
    """Write a file which contains the index values for all
    particles in a group. This file is typically used to
    export and import particle markings.
    
    The file has a header row with ntotal ngas nstar."""

    import charm
    
    # check if simulation loaded
    groups = charm.getGroups()
    if groups == None :
        raise StandardError('Simulation not loaded')
    # check if group exists
    if group not in groups :
        raise StandardError('Group ' + group + ' does not exist')
    
    ngas = charm.getNumParticles(group, 'gas')
    nstar = charm.getNumParticles(group, 'star')
    ntotal = ngas + nstar + charm.getNumParticles(group, 'dark')
    
    # record header row to file
    f = open(filename, 'w')
    f.write('%d %d %d\n' % (ntotal, ngas, nstar))
    f.close()
    
    # record index values
    charm.writeIndexes(group, 'gas', filename)
    charm.writeIndexes(group, 'dark', filename)
    charm.writeIndexes(group, 'star', filename)

