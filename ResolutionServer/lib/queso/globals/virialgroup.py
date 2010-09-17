import charm, findcenter, math, queso
from queso import quesoConfig

def getVirialGroup(group='All',center2='pot', virialGroup='virialGroup'):
    global center
    """This method returns the virial radius of a given group, bases around a center that can be given by one of three options: Default 'pot' finds the potential minimum, 'com' uses the center of mass, or a tuple of length 3 can be given.  virialGroup specifies the group name of the resulting """
    if   (center2=='pot'):cnt = findcenter.findCenter(group2=group, method='pot')
    elif (center2=='com'):cnt = findcenter.findCenter(group2=group, method='com')
    elif (len(center2)==3): 
        cnt = center2
    center = cnt #use center variable at later times.
    #===========================================================================
    # Use center coordinates to find virial radius and return group
    # Returns virial radius in sim units via bisection method.  
    # If this doesn't converge, try larger bounds or adjust the
    # other parameters in source.
    #===========================================================================
    def getGalDensity(center2, r) :
        charm.createGroupAttributeSphere(virialGroup, 'All', 'position', center[0], center[1], center[2], r)
        totMass  = charm.getAttributeSum(virialGroup, 'dark', 'mass')
        totMass += charm.getAttributeSum(virialGroup, 'gas',  'mass')
        totMass += charm.getAttributeSum(virialGroup, 'star', 'mass')
        v =  (4*math.pi/3.0)*r**3
        density=totMass/v
        return density 
    #================================================================
    #Specify bounds of bisection.  Default is 0.1 to 400kpc (at redshift zero)
    epsilon = .001 #maximum error in density convergence
    count = 0
    maxIterations = 200
    leftr = 0.1/quesoConfig.kpcunit
    rightr = 400./quesoConfig.kpcunit
    #================================================================
    from queso.quesoConfig import virialRadiusDensity
    midpoint = (leftr + rightr)/2
    leftDensity=getGalDensity(center,leftr)-virialRadiusDensity   #Initialize values
    rightDensity=getGalDensity(center,rightr)-virialRadiusDensity #offset so we can find root
    midDensity=getGalDensity(center,midpoint)-virialRadiusDensity #offset so we can find root
    #Bisection to find virial radius as defined in quesoConfig
    #Run until values converge within epsilon or reach max count
    while (count<maxIterations) & (math.fabs(leftDensity-rightDensity) > epsilon): 
        midpoint = (leftr + rightr)/2
        leftDensity=getGalDensity(center,leftr)-virialRadiusDensity   #offset so we can find root
        rightDensity=getGalDensity(center,rightr)-virialRadiusDensity #offset so we can find root
        midDensity=getGalDensity(center,midpoint)-virialRadiusDensity #offset so we can find root
        if (leftDensity*midDensity<0)   : rightr=midpoint
        elif (rightDensity*midDensity<0): leftr=midpoint
        elif (midDensity==0)            : break
        elif (leftDensity*midDensity>0) & (rightDensity*midDensity>0):
            print "\nVirial radius calculation will not converge. Try larger bounds."
            return
        count += 1
    #Don't get stuck in a loop
    if (count==maxIterations): 
        print "\nDid not converge in " + str(maxIterations) + " iterations.  Try smaller bounds or raise epsilon in source."
        return
    charm.createGroupAttributeSphere(virialGroup, 'All', 'position', center[0], center[1], center[2], midpoint)
    return (midpoint) #in sim units



