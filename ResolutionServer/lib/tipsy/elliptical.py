import traceback, math, config, charm

def transpose(ell_matrix_inv,ell_matrix) :
    for i in range(3) :
        for j in range(3) :
            ell_matrix[i][j] = ell_matrix_inv[j][i]

def cross_product(cross, a, b) :
    cross[0] = a[1]*b[2] - a[2]*b[1]
    cross[1] = a[2]*b[0] - a[0]*b[2]
    cross[2] = a[0]*b[1] - a[1]*b[0]


def setvec(a, b) :
    for i in range(3) :
        a[i] = b[i]

def matrix_vector_mult(mat,a,b) :
    # matrix-vector mulitipliciation
    # b = mat * a
    for i in range(3) :
        b[i] = 0.
        for j in range(3) :
            b[i] += mat[i][j] * a[j]

def ell_distance(x1, ell_matrix, center_ell, ba, ca) :
    dx = [0.]*3
    dx_rot = [0.]*3
    for i in range(3) :
        dx[i] = x1[i] - center_ell[i]
    matrix_vector_mult(ell_matrix,dx,dx_rot)
    if (ca != 0.0) :
        invca2 = 1.0/(ca*ca)
    else :
        invca2 = config.HUGE
    if (ba != 0.0) :
        invba2 = 1.0/(ba*ba)
    else :
        invba2 = config.HUGE
    seperation = dx_rot[0] * dx_rot[0] + dx_rot[1] * dx_rot[1] * invba2 + dx_rot[2] * dx_rot[2] * invca2
    return math.sqrt(seperation)

def rotate (a,i,j,k,l,s,tau) :
    g = a[i][j]
    h = a[k][l]
    a[i][j] = g-s*(h+g*tau)
    a[k][l] = h+s*(g-h*tau)

def jacobi (a,n,d,v) :
    b = [0.] * 4
    z = [0.] * 4
    
    for ip in range(1,n+1) :
        for iq in range(1,n+1) :
            v[ip][iq] = 0.0
        v[ip][ip] = 1.0
    for ip in range(1,n+1) :
        b[ip] = d[ip] = a[ip][ip]
        z[ip] = 0.0
    
    nrot = 0
    
    for i in range(1,101) :
        sm = 0.0
        for ip in range(1,n) :
            for iq in range(ip+1,n+1) :
                sm += abs(a[ip][iq])
        if (sm <= 1.0e-12) :
            return
        if (i < 4) :
            tresh = 0.2*sm/(n*n)
        else :
            tresh = 0.0
        for ip in range(1,n) :
            for iq in range(ip+1,n+1) :
                g = 100.0*abs(a[ip][iq])
                if (i > 4 and abs(d[ip]) + g == abs(d[ip]) and abs(d[iq]) + g == abs(d[iq])) :
                    a[ip][iq] = 0.0
                elif (abs(a[ip][iq]) > tresh) :
                    h = d[iq] - d[ip]
                    if (abs(h) + g == abs(h)) :
                        t = (a[ip][iq])/h
                    else :
                        theta = 0.5*h/(a[ip][iq])
                        t = 1.0/(abs(theta) + math.sqrt(1.0+theta*theta))
                        if (theta < 0.0) :
                            t = -t
                    c = 1.0/math.sqrt(1+t*t)
                    s = t*c
                    tau = s/(1.0+c)
                    h = t*a[ip][iq]
                    z[ip] -= h
                    z[iq] += h
                    d[ip] -= h
                    d[iq] += h
                    a[ip][iq] = 0.0
                    for j in range(1,ip) :
                        rotate(a,j,ip,j,iq,s,tau)
                    for j in range(ip+1,iq) :
                        rotate(a,ip,j,j,iq,s,tau)
                    for j in range(iq+1,n+1) :
                        rotate(a,ip,j,iq,j,s,tau)
                    for j in range(1,n+1) :
                        rotate(v,j,ip,j,iq,s,tau)
                    nrot += 1
        for ip in range(1,n+1) :
            b[ip] += z[ip]
            d[ip] = b[ip]
            z[ip] = 0.0
    raise StandardError('Error in Jacobi.')

def find_vel (famgroups,center,center_vel,center_angular_mom, ell_matrix, center_ell, ba, ca, fit_radius) :

    mass_r = 0.
    center = [0.]*3
    center_vel = [0.]*3
    center_angular_mom = [0.]*3
    
    vel_params = (ell_matrix, center_ell, ba, ca, fit_radius)
    
    for eachgroup in famgroups :
        result = charm.reduceParticle(eachgroup, find_vel_map, find_vel_reduce, vel_params)
        mass_r += result[0][1]
        for i in range(3) :
            center[i] += result[0][2][i]
            center_vel[i] += result[0][3][i]
            center_angular_mom[i] += result[0][4][i]
    
    for i in range(3) :
        center[i] /= mass_r
        center_vel[i] /= mass_r
    ang_mom = [0.]*3
    cross_product(ang_mom, center, center_vel)
    for i in range(3) :
        center_angular_mom[i] = (center_angular_mom[i] - mass_r * ang_mom[i]) / mass_r

find_vel_map = """def localparticle(p):
    def matrix_vector_mult(mat,a,b) :
        for i in range(3) :
            b[i] = 0.
            for j in range(3) :
                b[i] += mat[i][j] * a[j]
    def ell_distance(x1, ell_matrix, center_ell, ba, ca) :
        import math
        dx = [0.]*3
        dx_rot = [0.]*3
        for i in range(3) :
            dx[i] = x1[i] - center_ell[i]
        matrix_vector_mult(ell_matrix,dx,dx_rot)
        if (ca != 0.0) :
            invca2 = 1.0/(ca*ca)
        else :
            invca2 = config.HUGE
        if (ba != 0.0) :
            invba2 = 1.0/(ba*ba)
        else :
            invba2 = config.HUGE
        seperation = dx_rot[0] * dx_rot[0] + dx_rot[1] * dx_rot[1] * invba2 + dx_rot[2] * dx_rot[2] * invca2
        return math.sqrt(seperation)
    def cross_product(cross, a, b) :
        cross[0] = a[1]*b[2] - a[2]*b[1]
        cross[1] = a[2]*b[0] - a[0]*b[2]
        cross[2] = a[0]*b[1] - a[1]*b[0]
    import math
    ell_matrix, center_ell, ba, ca, fit_radius = p._param
    radius = ell_distance(p.position, ell_matrix, center_ell, ba, ca)
    if radius > fit_radius :
        return (0, 0., [0.]*3, [0.]*3, [0.]*3)
    center = [p.mass*p.position[0], p.mass*p.position[1], p.mass*p.position[2]]
    center_vel = [p.mass*p.velocity[0], p.mass*p.velocity[1], p.mass*p.velocity[2]]
    ang_mom = [0.]*3
    cross_product(ang_mom, p.position, p.velocity)
    center_angular_mom = [p.mass*ang_mom[0], p.mass*ang_mom[1], p.mass*ang_mom[2]]
    return (0, p.mass, center, center_vel, center_angular_mom)
"""

find_vel_reduce = """def localparticle(p):
    mass_r = 0.
    center = [0.] * 3
    center_vel = [0.] * 3
    center_angular_mom = [0.] * 3
    for i in range(len(p.list)) :
        mass_r += p.list[i][1]
        for j in range(3) :
            center[j] += p.list[i][2][j]
            center_vel[j] += p.list[i][3][j]
            center_angular_mom[j] += p.list[i][4][j]
    return (0, mass_r, center, center_vel, center_angular_mom)
"""

def find_shape (famgroups, center, fit_radius, ell_matrix, center_ell) :
    # famgroups is a list of the single-family groups created from the current working group.
    # center is the center coordinate for the parent operation. this may be a user-specified
    #       coordinate, but is often taken as the particle with least potential energy.
    # fit_radius is the maximum distance at which to include a particle. it should have a
    #       value > 0 which is meaningful for the simulation data.
    # ell_matrix should be a 3x3 list. the elliptical matrix result will be stored here.
    #       find_shape changes the state of this object to represent its result.
    # center_ell should be a 1x3 list. the center of the ellipse will be stored here.
    #       find_shape changes the state of this object to represent its result.
    
    mass_r = 0.
    ba = 0.
    ca = 0.
    cm_r = [0., 0., 0.]
    inertia_r = [[0., 0., 0.], [0., 0., 0.], [0., 0., 0.]]
    inertia_cm = [[0., 0., 0., 0.], [0., 0., 0., 0.], [0., 0., 0., 0.], [0., 0., 0., 0.]]
    evalues = [0.] * (3 + 1)
    evectors = [[0., 0., 0., 0.], [0., 0., 0., 0.], [0., 0., 0., 0.], [0., 0., 0., 0.]]
    VecProd = [0.] * (3 + 1)
    
    for j in range(3) :
        center_ell[j] = 0.
        for k in range(3) :
            ell_matrix[j][k] = 0.
    mode = 'lin'
    shape_params = mode, center, fit_radius, ell_matrix, center_ell, ba, ca
    for eachgroup in famgroups :
        result = charm.reduceParticle(eachgroup, shape_map, shape_reduce, shape_params)
        mass_r += result[0][1]
        for j in range(3) :
            cm_r[j] += result[0][2][j]
            for k in range(3) :
                if k <= j :
                    inertia_r[j][k] += result[0][3][j][k]
    if mass_r == 0.0 :
        raise StandardError('System mass is zero in find_shape().')
    
    for j in range(3) :
        for k in range(3) :
            if (k <= j) :
                inertia_cm[j+1][k+1]  = ((inertia_r[j][k] - cm_r[j] * cm_r[k] / mass_r) / mass_r)
            else :
                inertia_cm[j+1][k+1]  = ((inertia_r[k][j] - cm_r[k] * cm_r[j] / mass_r) / mass_r)
        evalues[j+1] = inertia_cm[j+1][j+1]
    
    jacobi(inertia_cm,3,evalues,evectors)
    
    if(evalues[1] < 0.0) :
        print 'Warning, negative eigenvalues in find_shape().'
        evalues[1] = 0.0
    
    if(evalues[2] < 0.0) :
        print 'Warning, negative eigenvalues in find_shape().'
        evalues[2] = 0.0
    
    if(evalues[3] < 0.0) :
        print 'Warning, negative eigenvalues in find_shape().'
        evalues[3] = 0.0
    
    if(evalues[1] == 0.0 and evalues[2] == 0.0 and evalues[3] == 0.0) :
        raise StandardError('Warning, negative eigenvalues in find_shape().\nGiving up in find_shape().')
        return
    
    if(evalues[1] >= evalues[2] and evalues[1] >= evalues[3]) :
        ia = 1
        if(evalues[2] >= evalues[3]) :
            ib = 2
            ic = 3
        else :
            ib = 3
            ic = 2
    elif (evalues[2] > evalues[1] and evalues[2] >= evalues[3]) :
        ia = 2
        if(evalues[1] >= evalues[3]) :
            ib = 1
            ic = 3
        else :
            ib = 3
            ic = 1
    else :
        ia = 3
        if(evalues[1] >= evalues[2]) :
            ib = 1
            ic = 2
        else :
            ib = 2
            ic = 1

    # Check if Eigenvectors are righthanded in 3D :
    #   ev[ib] x ev[ic] = ev[ia]

    VecProd[1] =  evectors[2][ib]*evectors[3][ic] - evectors[3][ib]*evectors[2][ic]
    VecProd[2] = -evectors[1][ib]*evectors[3][ic] + evectors[3][ib]*evectors[1][ic]
    VecProd[3] =  evectors[1][ib]*evectors[2][ic] - evectors[2][ib]*evectors[1][ic]
    ScalProd   =  evectors[1][ia]*VecProd[1] + evectors[2][ia]*VecProd[2] + evectors[3][ia]*VecProd[3]
    if (ScalProd < 0.0) :
        for i in range(3) :
            evectors[i+1][ia] = -evectors[i+1][ia]

    ba = math.sqrt(evalues[ib]/evalues[ia])
    ca = math.sqrt(evalues[ic]/evalues[ia])
    
    # euler angles for a zyz rotation
    theta = 180. / math.pi * math.acos(evectors[3][ic])
    if(evectors[1][ic]*evectors[1][ic] + evectors[2][ic]*evectors[2][ic] > 0.0) :
        phi = 180. / math.pi * math.acos(evectors[1][ic]/math.sqrt(evectors[1][ic]*evectors[1][ic] + evectors[2][ic]*evectors[2][ic]))
    else :
        phi = 0.0
    if(evectors[1][ic]*evectors[1][ic] + evectors[2][ic]*evectors[2][ic] > 0.0) :
        psi = (  180. / math.pi 
            * math.acos((-evectors[2][ic]*evectors[1][ib]
                + evectors[1][ic]*evectors[2][ib])
               /math.sqrt(evectors[1][ic]*evectors[1][ic]
                 + evectors[2][ic]*evectors[2][ic]))  )
    else :
        psi = 0.0

    # inverse acos is only defined between 0 and pi therefore we must deal with pi to 2*pi
    if(evectors[2][ic] < 0.0) :
        phi = 360. - phi # phi always positive
    if(evectors[3][ib] < 0.0) :
        psi = 360. - psi # psi always positive

    for i in range(3) :
        center[i] = cm_r[i] / mass_r
    
    ell_matrix_inv = [[0., 0., 0.], [0., 0., 0.], [0., 0., 0.]]
    for i in range(3) :
        ell_matrix_inv[i][0] = evectors[i+1][ia]
        ell_matrix_inv[i][1] = evectors[i+1][ib]
        ell_matrix_inv[i][2] = evectors[i+1][ic]
    
    transpose(ell_matrix_inv,ell_matrix)
    setvec(center_ell,center)
    
    if(ca == 0.0 or ba == 0.0) : # give up
        print 'Gave up in find_shape() because ba or ca was zero.'
        return
    ba_old = ba
    ca_old = ca
    phi_old = phi
    theta_old = theta
    psi_old = psi
    niter = 0
    
    while True :
        mass_r = 0.
        for j in range(3) :
            cm_r[j]  = 0.
            for k in range(3) :
                inertia_r[j][k]  = 0.

        mode = 'ell'
        shape_params = mode, center, fit_radius, ell_matrix, center_ell, ba, ca
        for eachgroup in famgroups :
            result = charm.reduceParticle(eachgroup, shape_map, shape_reduce, shape_params)
            mass_r += result[0][1]
            for j in range(3) :
                cm_r[j] += result[0][2][j]
                for k in range(3) :
                    if k <= j :
                        inertia_r[j][k] += result[0][3][j][k]
        
        if mass_r == 0.0 :
            return
        for j in range(3) :
            for k in range(3) :
                if (k <= j) :
                    inertia_cm[j+1][k+1]  = (inertia_r[j][k] - cm_r[j] * cm_r[k] / mass_r) / mass_r
                else :
                    inertia_cm[j+1][k+1]  = (inertia_r[k][j] - cm_r[k] * cm_r[j] / mass_r) / mass_r
            evalues[j+1] = inertia_cm[j+1][j+1]
        
        jacobi(inertia_cm,3,evalues,evectors)
        
        if(evalues[1] < 0.0) :
            print 'Warning, negative eigenvalues in find_shape().'
            evalues[1] = 0.0
        
        if(evalues[2] < 0.0) :
            print 'Warning, negative eigenvalues in find_shape().'
            evalues[2] = 0.0
        
        if(evalues[3] < 0.0) :
            print 'Warning, negative eigenvalues in find_shape().'
            evalues[3] = 0.0
        
        if(evalues[1] == 0.0 and evalues[2] == 0.0 and evalues[3] == 0.0) :
            raise StandardError('Warning, negative eigenvalues in find_shape().\nGiving up in find_shape().')
            return

        if (evalues[1] >= evalues[2] and evalues[1] >= evalues[3]) :
            ia = 1
            if (evalues[2] >= evalues[3]) :
                ib = 2
                ic = 3
            else :
                ib = 3
                ic = 2
        elif (evalues[2] > evalues[1] and evalues[2] >= evalues[3]) :
            ia = 2
            if (evalues[1] >= evalues[3]) :
                ib = 1
                ic = 3
            else :
                ib = 3
                ic = 1
        else :
            ia = 3 
            if (evalues[1] >= evalues[2]) :
                ib = 1
                ic = 2
            else :
                ib = 2
                ic = 1
        
        # Check if Eigenvectors are righthanded in 3D : ev[ib] x ev[ic] = ev[ia]
        VecProd[1] =  evectors[2][ib]*evectors[3][ic] - evectors[3][ib]*evectors[2][ic]
        VecProd[2] = -evectors[1][ib]*evectors[3][ic] + evectors[3][ib]*evectors[1][ic]
        VecProd[3] =  evectors[1][ib]*evectors[2][ic] - evectors[2][ib]*evectors[1][ic]
        ScalProd   =  evectors[1][ia]*VecProd[1] + evectors[2][ia]*VecProd[2] + evectors[3][ia]*VecProd[3]
        if (ScalProd < 0.0) :
            for i in range(3) :
                evectors[i+1][ia] = -evectors[i+1][ia]
        
        ba = math.sqrt(evalues[ib]/evalues[ia])
        ca = math.sqrt(evalues[ic]/evalues[ia])
        
        # euler angles for a zyz rotation
        theta = 180. / math.pi * math.acos(evectors[3][ic])
        if (evectors[1][ic]*evectors[1][ic] + evectors[2][ic]*evectors[2][ic] > 0.0) :
            phi =   180. / math.pi * math.acos(evectors[1][ic]/math.sqrt(evectors[1][ic]*evectors[1][ic] + evectors[2][ic]*evectors[2][ic]))
        else :
            phi = 0.0
        if (evectors[1][ic]*evectors[1][ic] + evectors[2][ic]*evectors[2][ic] > 0.0) :
            psi =  ( 180. / math.pi
            * math.acos((-evectors[2][ic]*evectors[1][ib]
                + evectors[1][ic]*evectors[2][ib])/
                   math.sqrt(evectors[1][ic]*evectors[1][ic]
                    + evectors[2][ic]*evectors[2][ic])) )
        else :
            psi = 0.0

        # inverse acos is only defined between 0 and pi therefore we must deal with pi to 2*pi
        if (evectors[2][ic] < 0.0) :
            phi = 360. - phi # phi always positive
        if (evectors[3][ib] < 0.0) :
            psi = 360. - psi # psi always positive
        for i in range(3) :
               center[i] = cm_r[i] / mass_r
        for i in range(3) :
            ell_matrix_inv[i][0] = evectors[i+1][ia]
            ell_matrix_inv[i][1] = evectors[i+1][ib]
            ell_matrix_inv[i][2] = evectors[i+1][ic]
        
        transpose(ell_matrix_inv,ell_matrix)
        setvec(center_ell,center)
        
        if abs(ba-ba_old)/ba_old <= 1.0e-4 and abs(ca-ca_old)/ca_old <= 1.0e-4 :
            break
        if niter > 20 :
            ba = (ba + ba_old) / 2.
            ca = (ca + ca_old) / 2.
            phi = (phi + phi_old) / 2.
            theta = (theta + theta_old) / 2.
            psi = (psi + psi_old) / 2.
            break
        
        ba_old = ba
        ca_old = ca
        phi_old = phi
        theta_old = theta
        psi_old = psi
        niter += 1
        if(ca == 0.0 or ba == 0.0) : # give up
            return
    return (ba, ca, phi, theta, psi)

shape_map = """def localparticle(p):
    def matrix_vector_mult(mat,a,b) :
        for i in range(3) :
            b[i] = 0.
            for j in range(3) :
                b[i] += mat[i][j] * a[j]
    def ell_distance(x1, ell_matrix, center_ell, ba, ca) :
        import math
        dx = [0.]*3
        dx_rot = [0.]*3
        for i in range(3) :
            dx[i] = x1[i] - center_ell[i]
        matrix_vector_mult(ell_matrix,dx,dx_rot)
        if (ca != 0.0) :
            invca2 = 1.0/(ca*ca)
        else :
            invca2 = config.HUGE
        if (ba != 0.0) :
            invba2 = 1.0/(ba*ba)
        else :
            invba2 = config.HUGE
        seperation = dx_rot[0] * dx_rot[0] + dx_rot[1] * dx_rot[1] * invba2 + dx_rot[2] * dx_rot[2] * invca2
        return math.sqrt(seperation)
    import math
    mode, center, fit_radius, ell_matrix, center_ell, ba, ca = p._param
    if mode == 'ell' :
        radius = ell_distance(p.position, ell_matrix, center_ell, ba, ca)
    else :
        radius = math.sqrt(pow(center[0] - p.position[0], 2) + pow(center[1] - p.position[1], 2) + pow(center[2] - p.position[2], 2))
    if fit_radius > 0. and radius >= fit_radius :
        return (0, 0., [0.] * 3, [[0., 0., 0.], [0., 0., 0.], [0., 0., 0.]])
    cm_r = [p.mass * p.position[0], p.mass * p.position[1], p.mass * p.position[2]]
    inertia_r = [[0., 0., 0.], [0., 0., 0.], [0., 0., 0.]]
    for j in range(3) :
        for k in range(3) :
            if k <= j :
                inertia_r[j][k] = p.mass * p.position[j] * p.position[k]
    return (0, p.mass, cm_r, inertia_r)
"""

shape_reduce = """def localparticle(p):
    mass_r = 0.
    cm_r = [0., 0., 0.]
    inertia_r = [[0., 0., 0.], [0., 0., 0.], [0., 0., 0.]]
    for i in range(len(p.list)) :
        mass_r += p.list[i][1]
        for j in range(3) :
            cm_r[j] += p.list[i][2][j]
            for k in range(3) :
                if k <= j :
                    inertia_r[j][k] += p.list[i][3][j][k]
    return (0, mass_r, cm_r, inertia_r)
"""

