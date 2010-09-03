import charm
def getGroupRadius(group, center):
    maxRad = charm.reduceParticle(group, mapRadius, reduceRadius, center)[0][1]
    return maxRad

mapRadius                = """import math
def localparticle(p):
    _center = p._param
    relPosition = [p.position[0]-_center[0],p.position[1]-_center[1],p.position[2]-_center[2]]
    rad = math.sqrt(relPosition[0]**2+relPosition[1]**2+relPosition[2]**2)
    return (0,rad)
"""
reduceRadius                = """def localparticle(p):
    rad = 0
    for i in range(len(p.list)):
        if p.list[i][1] > rad: rad = p.list[i][1]
    return (0,rad)
"""