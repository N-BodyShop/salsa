import charm ,virialgroup, findcenter, math
from queso import quesoConfig

def getHalfMassRadius(group, center='pot') :
    """Returns the current stellar half mass radius."""
    #get the current galactic stellar mass
    rgal = 0.1*virialgroup.getVirialGroup()
    center = findcenter.findCenter(group2=group,method=center)
    halfMass = getFamilyAttributeSum(center,rgal,'star','mass')/2.0
    if (halfMass == 0 ): 
        print "Halfmass is zero inside rgal.  Are there stars inside rgal yet?." 
        return (0,0)
    #Bisection Method to locate half mass
    epsilon = .01
    count = 0
    maxIterations = 50
    a = charm.getTime()
    leftr = .1/(a*quesoConfig.kpcunit)
    rightr = 50/(a*quesoConfig.kpcunit)
    midpoint = (leftr + rightr)/2.0
    leftMass  = getFamilyAttributeSum(center,leftr   ,'star','mass')-halfMass  #Initialize values
    rightMass = getFamilyAttributeSum(center,rightr  ,'star','mass')-halfMass #offset to find root  
    midMass   = getFamilyAttributeSum(center,midpoint,'star','mass')-halfMass #offset so we can find root
    #Bisection to find halfmass radius using initialMass and mass
    #Run until values converge within epsilon or reach max count
    while (count<maxIterations) and (math.fabs((leftMass-rightMass)/halfMass) > epsilon*2.0):
        midpoint = (leftr + rightr)/2.0
        
        #if (math.fabs(leftMass-rightMass)/halfMass < 4*epsilon):break
        leftMass = getFamilyAttributeSum(center,leftr   ,'star','mass')-halfMass  #Initialize values
        rightMass= getFamilyAttributeSum(center,rightr  ,'star','mass')-halfMass #offset to find root  
        midMass  = getFamilyAttributeSum(center,midpoint,'star','mass')-halfMass #offset so we can find root
        #print str((leftMass,midMass,rightMass)) + "    " + str(math.fabs(leftMass-rightMass)/halfMass) +str((leftr,midpoint,rightr))
        if (leftMass*midMass<0)   : rightr = midpoint
        elif (rightMass*midMass<0): leftr  = midpoint
        elif (midMass==0)         : break
        elif (leftMass*midMass>0) and (rightMass*midMass>0):
            print "\nStellar half mass radius will not converge. Try larger bounds."
            return
        count += 1
    #Don't get stuck in a loop
    if (count==maxIterations): 
        print "\nDid not converge in " + str(maxIterations) + " iterations.  Try smaller bounds or raise epsilon in source."
        return
    resultVec=midpoint #already in units of kpc taking into account the expansion factor
    #print resultVec[i]
    return resultVec

def getFamilyAttributeSum(center2,r,family,attribute):
    """Integrates attributes of a given family over a given sphere."""
    charm.createGroupAttributeSphere('group', 'All', 'position', center2[0], center2[1], center2[2], r)
    charm.createGroup_Family('group','group',family)
    sum = charm.getAttributeSum('group', family, attribute)
    charm.deleteGroup('group')
    return sum