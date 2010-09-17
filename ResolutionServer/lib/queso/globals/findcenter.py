import charm
def findCenter(group2='All', method = 'pot' ):
    """Finds the center of a given group based on method as 'com' or 'pot'"""
    if (method=='pot'): 
        if (group2=='All'):  
            charm.createGroup_AttributeRange('highres', 'All', 'mass', -1, 7e-12)
            cntHr =  charm.getCenterOfMass('highres')
            charm.createGroupAttributeSphere('centerhr', 'All', 'position', cntHr[0], cntHr[1], cntHr[2], 1000./136986.30137)
            cnt = charm.findAttributeMin('centerhr', 'potential')
        else:
            cnt = charm.findAttributeMin(group2, 'potential')
    elif (method=='com'):
        cnt = charm.getCenterOfMass(group2)
    return cnt

