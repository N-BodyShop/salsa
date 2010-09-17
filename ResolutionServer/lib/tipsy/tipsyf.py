# Contains common functions for the Python port of TIPSY.
# This file imports the modules containing each command or class of commands
# for Tipsy, and is also used to define widely useful, simple helper methods
# such as cross_product().
import math, config

def strlist(oldlist) :
    """Change the elements of a list into strings."""
    newlist = []
    for i in oldlist :
        newlist.append(str(i))
    return newlist

def cross_product(a, b) :
    """Calculate cross product of two vectors in R3."""
    if len(a) != 3 or len(b) != 3 :
        raise StandardError('Vector must have dimensionality 3.')
    return [a[1]*b[2] - a[2]*b[1], a[2]*b[0] - a[0]*b[2], a[0]*b[1] - a[1]*b[0]]

def dot_product(a, b) :
    """Calculate dot product of two vectors."""
    if len(a) != len(b) :
        raise StandardError('Vector dimensionality not equal.')
    prod = 0.0
    for i in range(len(a)) :
        prod += a[i]*b[i]
    return prod

def length(a) :
    """Calculate the length of a vector."""
    r_sq = 0.0
    for each in a :
        r_sq += each ** 2
    return math.sqrt(r_sq)

def distance(a, b) :
    """Calculate the linear distance between two points."""
    r_sq = 0.0
    if len(a) != len(b) :
        raise StandardError('Vector dimensionality not equal.')
    for i in range(len(a)) :
        r_sq += (a[i] - b[i]) ** 2
    return math.sqrt(r_sq)

def perp_distance(x1, x2, x3) :
    """Calculate the distance of a point from a line in R3."""
    radius = sub_vec(x3, x1)
    cross = cross_product(radius, x2)
    return math.sqrt(dot_product(cross, cross) / dot_product(x2, x2))

def ell_distance(x1, center_ell, ell_matrix, ba, ca) :
    """Calculate the elliptical distance."""
    dx = sub_vec(x1, center_ell)
    dx_rot = matrix_vector_mult(ell_matrix, dx)
    if ca != 0.0 :
        invca2 = 1.0/(ca**2)
    else :
        invca2 = 3.40282346638528860e+38
    if ba != 0.0 :
        invba2 = 1.0/(ba**2)
    else :
        invba2 = 3.40282346638528860e+38
    return math.sqrt(dx_rot[0] * dx_rot[0] + dx_rot[1] * dx_rot[1] *invba2 + dx_rot[2] * dx_rot[2] * invca2)

def sub_vec(a, b) :
    if len(a) != len(b) :
        raise StandardError('Vector dimensionality not equal.')
    x = [0.0] * len(a)
    for i in range(len(a)) :
        x[i] = a[i] - b[i]
    return x

def matrix_vector_mult(mat,a,b) :
    # matrix-vector mulitipliciation
    # b = mat * a
    for i in range(config.MAXDIM) :
        b[i] = 0.
        for j in range(config.MAXDIM) :
            b[i] += mat[i][j] * a[j]

def add_const_mult_vec(a, constant, b) :
    # scale vector b by a constant and add to vector a
    for i in range(len(a)) :
        a[i] += constant * b[i]
    return a

def mass_add_vec(a, b, mass_b, c, mass_c) :
    # mass-weighted vector addition
    for i in range(3) :
        a[i] = (mass_b * b[i] + mass_c * c[i]) / (mass_b + mass_c)

def add_vec(a, b, c) :
    for i in range(3) :
        a[i] = b[i] + c[i]

def vec_add_const_mult_vec(a, b, constant, c) :
    for i in range(3) :
        a[i] = b[i] + constant * c[i]

def acc_load() :
    # initialize acsmooth
    deldrg = 2./config.NINTERP 
    for i in range(config.NINTERP + 2) :
        xw = i * deldrg
        xw2 = xw * xw
        xw3 = xw2 * xw
        xw4 = xw2 * xw2
        if xw <= 1. :
            config.acsmooth[i] = xw3*(4./3.-6.*xw2/5.+0.5*xw3)
        elif xw > 1. and xw < 2. :
            config.acsmooth[i] = -1./15.+8.*xw3/3.-3.*xw4+6.*xw3*xw2/5.-xw4*xw2/6.
        else :
            config.acsmooth[i] = 1.
    config.acc_loaded = True

def acsmooth_val(i) :
    # returns the acsmooth value for a given index
    # replaces acsmooth array created by acc_load in TIPSY
    xw = i * 2. / config.NINTERP
    if xw <= 1. :
        return pow(xw,3) * (4./3. - 1.2*pow(xw,2) + 0.5*pow(xw,3))
    elif xw > 1. and xw < 2. :
        return -1./15. + 8./3.*pow(xw,3) - 3.*pow(xw,4) + 1.2*pow(xw,5) - pow(xw,6)/6.
    else :
        return 1.

