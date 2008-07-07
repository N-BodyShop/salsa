from math import *
centmassmap = """def localparticle(p):
	return (0, p.mass*p.position[0], p.mass*p.position[1],p.mass*p.position[2])
""" 
centmassVelmap = """def localparticle(p):
	return (0, p.mass*p.velocity[0], p.mass*p.velocity[1],p.mass*p.velocity[2])
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

def boxstat(group, family) :
    """Print statistics for a group as in the tipsy boxstat command
    Arguments are group and family"""
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
    else :
        nPart = charm.getNumParticles(group, family)
        mass = charm.getAttributeSum(group, family, 'mass')
        bBox = charm.getAttributeRangeGroup(group, family, 'position')
        groupfamname = group + 'FAM' + family
        charm.createGroup_Family(groupfamname, group, family)
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
    if family == 'all' :
        print 'number of dark, gas and star particles =', nPartDark, nPartGas, nPartStar
    else:
        print 'number of', family, 'particles =', nPart
    print 'mass =', mass
    print 'center coordinates =', center
    print 'size =', size
    print 'center of mass coordinates =', cm
    print 'center of mass velocity =', vcm
    print 'angular momentum vector =', angmom

profileMap = """from math import *
def localparticle(p) :
    cm, binType, minRadius, binSize = p._param
    radius = sqrt((p.position[0] - cm[0])**2 + (p.position[1] - cm[1])**2 + (p.position[2] - cm[2])**2)
    if binType == 'log' :
	if radius > 0.0 :
	    radius = log10(radius)
	else :
	    radius = minRadius
    bin = max(int(floor((radius - minRadius) / binSize)),0)
    return (bin, 1, p.mass)
"""
profileReduce = """def localparticle(p) :
    sum = [0, 0.0]
    for i in range(0, len(p.list)) :
        sum[0] += p.list[i][1]
        sum[1] += p.list[i][2]
    return (p.list[0][0], sum[0], sum[1])
"""
def profile(group, center, family, projection, binType, nbins, minRadius) :
    """Create a radial profile for a group as in the tipsy profile command
    Arguments are group, center group (or 'pot'), family,
    projection type ('sphere' or 'cylindrical'),
    binning type ('linear' or 'log'), number of bins, and minimum radius.
    Returns a list, with one item per radial bin, and each item contains
    radius, npart, rho, M(<r), v_circ, v_radial, sigma_vr, v_tang, sigma_vt,
    J, theta_J, phi_J"""
    cm = charm.getCenterOfMass(center)
    if family == 'all':
        bBox = list(charm.getAttributeRangeGroup(group, 'gas', 'position'))
        bBoxTmp = charm.getAttributeRangeGroup(group, 'dark', 'position')
        bBox[0] = map(lambda x, y : min(x,y), bBox[0], bBoxTmp[0])
        bBox[1] = map(lambda x, y : max(x,y), bBox[1], bBoxTmp[1])
        bBoxTmp = charm.getAttributeRangeGroup(group, 'star', 'position')
        bBox[0] = map(lambda x, y : min(x,y), bBox[0], bBoxTmp[0])
        bBox[1] = map(lambda x, y : max(x,y), bBox[1], bBoxTmp[1])
        groupfamname = group
    else :
        bBox = charm.getAttributeRangeGroup(group, family, 'position')
        groupfamname = group + 'FAM' + family
        charm.createGroup_Family(groupfamname, group, family)

    size = map(lambda min, max : max - min, bBox[0], bBox[1])
    radius = sqrt(0.5*(size[0]**2 + size[1]**2 + size[2]**2))
    if binType == 'lin' :
        binSize = (radius - minRadius)/nbins


    globals = (cm, binType, minRadius, binSize)
    bins = charm.reduceParticle(groupfamname, profileMap, profileReduce, globals)
    massR = 0.0
    outbins = []
    for bin in bins :
        i = bin[0]
        radiusOld = binSize*i;
        radius = binSize * (i+1)
        if binType == 'lin' :
            radius += minRadius
            radiusOld += minRadius
        else:
            radius = pow(10., radius + minRadius)
            radiusOld = pow(10., radiusOld + minRadius)
        radiusMean = 0.5*(radius + radiusOld)
        massR += bin[2]
        volume = (4./3.*pi*(radius**3 - radiusOld**3))
        rho = bin[2] / volume ;
        vel = sqrt(massR/radius)
        outbins.append([radius, bin[1], rho, massR, vel])
    return outbins
