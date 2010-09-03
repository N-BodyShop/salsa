from queso.globals import angmom,virialgroup, mominertia, galcoord,findcenter,halfmassradius, epsilondist, metals
#===========================================================================
# This contains wrapper functions to easily interface to the globals package.
# There is very little functionality beyond what is callable. 
#===========================================================================

def angMomVec(group,cent='pot'):
    """
    Returns tuple of length 2 with [0] containing specific angular momentum vector of the passed group in
    physical units. [1] contains the first two Euler angles  Center can be 'pot' or 'com'
    """
    angMomVec = angmom.getAngMomVector(group=group, center=cent)
    euler = angmom.getEulerAngles(angMomVec)
    return (angMomVec,euler)
def virialGroup(group='All',cent='pot',groupName='virialGroup'):
    """
    This method returns the virial radius of a given group, bases
    around a center that can be given by one of three options:
    Default 'pot' finds the potential minimum, 'com' uses the center
    of mass, or a tuple of length 3 can be given.  groupName
    specifies the group name of the resulting virial sphere.
    """
    return virialgroup.getVirialGroup(group=group,center2=cent, virialGroup=groupName)
def momentInertia(group,cent='pot'):
    """
    Return is the 3x3 moment of inertia tensor for given group.
    center can be 'pot' or 'com'.  This is in physical units. [[Ixx,
    Ixy, Ixz], [Iyx, Iyy, Iyz], [Izx, Izy, Izz]]
    """
    return mominertia.getMomentInertiaTensor(group2=group,center=cent)
def galCoord(angMomGrp=None,cent='pot'):
    """
    This assigns two vector attributes to all particles, galVelocity
    and galPosition, which is the position in the galactic coordinate
    system.  The galactic coordinate system is defined by the angular
    momentum of the passed angMomGrp.  If left as None, it defaults
    to the galactic cold gas.  The galactic x-axis is the new z-axis
    crossed with the original y-axis, and the new y-axis is the cross
    of galZ with galX. cent can be 'pot' or 'com'.
    """
    galcoord.calcGalacticCoordinates(angMomGroup=angMomGrp, center=cent)
    return
def findCenter(group='All', cent='pot'):
    """
    Finds the center of a given group based on cent= 'com' or 'pot',
    center of mass or potential minimum respectively.
    """
    return findcenter.findCenter(group2=group, method=cent)
def halfMassRadius(group,cent='pot'):
    """
    Returns the current stellar half mass radius of a group group.
    cent can be 'pot' or 'com'
    """
    return halfmassradius.getHalfMassRadius(group, center=cent)
def epsilonDist(group='gal', nBins=50, epsCut=2., cent='pot'):
    """
    Returns a histogram of dimension 2 x nbins of stellar epsilons
    (jz/jcirc) and mass-fraction in bin.  epsCut defines the
    histogram bound in both directions. cent can be either 'pot' or 'com'
    """
    epsilondist.getEpsilonDist(group=group, nBins=nBins, epsCut=epsCut, center=cent)
def metals(group,fam='bar'):
    """
    Returns the mean mass-weighted metal fraction of the given group
    and fam ='gas','star', or 'bar'.
    """
    metals.getMeanMetals(group)