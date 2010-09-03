import math

def normalizeVector(vector):
    norm = math.sqrt(vector[0]**2+vector[1]**2+vector[2]**2)
    return(vector[0]/norm,vector[1]/norm,vector[2]/norm)
def divideVectorScalar(scalar,vector):
    return(vector[0]/scalar,vector[1]/scalar,vector[2]/scalar)
def multVectorScalar(scalar,vector):
    return (vector[0]*scalar,vector[1]*scalar,vector[2]*scalar)
def vectorLength(v1):
    return math.sqrt(v1[0]**2+v1[1]**2+v1[2]**2)
def dotProd(v1,v2):
    return (v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2])
def addVectors(v1,v2):
    return (v1[0]+v2[0],v1[1]+v2[1],v1[2]+v2[2])
def subVectors(v1,v2):
    return (v1[0]-v2[0],v1[1]-v2[1],v1[2]-v2[2])
def crossVectors(vector1, vector2):
    j=[0]*3
    #take cross-product of relative position and velocity
    j[0] = vector1[1]*vector2[2] - vector1[2]*vector2[1]
    j[1] = vector1[2]*vector2[0] - vector1[0]*vector2[2]
    j[2] = vector1[0]*vector2[1] - vector1[1]*vector2[0]
    return tuple(j)
