import traceback, config, tipsyf, math, spline, elliptical, star_lum

def safeprofile() :
    try :
        profile()
        print '\nMethod \'profile()\' completed with no errors.'
    except :
        print traceback.format_exc()

def profile(group='All', center='pot', family='all', projection='sph', bin_type='log', nbins=30, filename='profile.DAT', min_radius=0., fit_radius=0.) :
  """Perform the Tipsy profile() function.
    
    profile is a command that produces a file of  name,  file-
    name,  that  contains various physical profiles as a func-
    tion of radius for particles of type particle in group.

    The  center  of the radial distribution is taken to be the
    center of mass of the particles of the  type  particle  in
    group center. If  the  string  "pot"  is given  instead of
    a  group  name for  the  argument center, the  particle in
    group  with  the  lowest  potential  energy is used as the
    center of the radial bins.

    The projection argument refers to the shape  of  the  bins
    and  the bin_type argument refers to how the particles are
    binned, either linearly or log in radius.  There are nbins
    bins  distributed  equally  in either radius or log radius
    between min_radius and the size of group.

    The  output file has the following columns: radius, number
    in bin, density, cumulative mass (M(<r)), circular  veloc-
    ity (sqrt(M(<r)/r)), radial velocity, radial velocity dis-
    persion, tangential velocity, tangential velocity  disper-
    sion,  specific  angular momentum (j),j_theta, j_phi.  The
    j_theta and j_phi angles specify the direction of the spe-
    cific  angular  momentum vector (in degrees). If family is
    is either gas,  baryon, or all,  four  additional  columns
    containing the mean mass weighted gas density, mass weigh-
    ted gas temperature, mass weighted gas pressure, and  mass
    weighted  gas  entropy  are  added.  If particle is either
    star, baryon, or  all  an additional column containing the
    stellar  luminosity  in the Johnson V band is added.  This
    is  calculated  using  the  ages  of  the  star  particles
    as in Katz (Ap.J. 391: 502, 1992).
    
    Note: some rarer configuration options are not implemented
    e.g. non-uniform UV for star luminosity calculation."""
    
  try:
    import charm
    # check initialization state of meanmwt attribute
    if family in ['all', 'gas', 'baryon'] and not 'meanmwt' in charm.getAttributes('gas') :
        print 'Attribute meanmwt not initialized. Initializing...'
        import load_meanmwt
        load_meanmwt.do_meanmwt()
    print '\n'
    # these should be moved to tipsyf
    # fn to take the dot product of two vectors in R3
    def dotprod(a, b) :
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2]
    # fn to take the cross product of two vectors in R3
    def crossprod(a, b) :
        return [a[1]*b[2] - a[2]*b[1], a[2]*b[0] - a[0]*b[2], a[0]*b[1] - a[1]*b[0]]
    # fn to multiply a vector v by a scalar c
    def scalevec(c, v) :
        return [c * v[0], c * v[1], c * v[2]]
    # fn to add two vectors
    def addvec(a, b) :
        return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]
    # fn to subtract vector b from vector a
    def subvec(a, b) :
        return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]
    # fn to find the length of a vector
    def vlength(a) :
        import math
        return math.sqrt(pow(a[0], 2) + pow(a[1], 2) + pow(a[2], 2))
    # fn to combine two vectors, weighting by their corresponding mass scalars
    def weightvecs(m1, x1, m2, x2) :
        return [(x1[0]*m1 + x2[0]*m2)/(m1+m2), (x1[1]*m1 + x2[1]*m2)/(m1+m2), (x1[2]*m1 + x2[2]*m2)/(m1+m2)]
    
    # scrub input
    # check if simulation loaded, store group list
    groups = charm.getGroups()
    if groups == None :
        raise StandardError('Simulation not loaded')
    # case is inconsistent between groups and families, work for either
    if group == 'all' :
        group = 'All'
    if family == 'All' :
        family = 'all'
    # deal with group or center group does not exist
    if group not in groups :
        raise StandardError('Group does not exist!')
    if (center not in groups) and (center != 'pot') :
        raise StandardError('Center group does not exist!')
    # set up families iterator and throw family related exceptions
    families = charm.getFamilies()
    if families == None :
        raise StandardError('Simulation loaded but no families present.')
    isbaryon = 0
    if family == 'baryon' :
        isbaryon = 1
        if 'dark' in families:
            families.remove('dark')
        if len(families) == 0 :
            raise StandardError('No baryon particles present in simulation.')
    if (family != 'all') and (family != 'baryon') :
        if family not in families :
            raise StandardError('Family not present in simulation.')
        else :
            families = [family]
    if len(families) == 0 :
        raise StandardError('List of families to process has zero length.')
    # create groups containing one family each, store names in famgroups list
    famgroups = []
    for each in families :
        famgroup = group + 'FAM' + each
        charm.createGroup_Family(famgroup, group, each)
	if charm.getNumParticles(famgroup, each) != 0 :
            famgroups += [famgroup]
    if len(famgroups) == 0 :
        raise StandardError('No particles in group of selected type.')
    # validate other inputs
    if bin_type == 'linear' :
        bin_type = 'lin'
    elif bin_type == 'logarithmic' :
        bin_type = 'log'
    elif bin_type not in ['lin', 'log'] :
        raise StandardError('Value of bin_type must be lin or log.')
    if (int(nbins) != nbins) or (nbins < 1) :
        raise StandardError('Value of nbins must be a positive integer.')
    if not min_radius >= 0 :
        raise StandardError('Value of min_radius cannot be negative.')
    if min_radius == 0 and bin_type == 'log' :
        min_radius = config.LOG_MIN
        print 'Parameter min_radius set to ' + str(min_radius) + ' to accomodate logarithmic binning.'
    if not fit_radius >= 0 :
        raise StandardError('Value of fit_radius cannot be negative.')

    # begin calculating parameters

    # find center point
    if center == 'pot' :
        center = charm.findAttributeMin(group, 'potential')
    else:
        center = charm.getCenterOfMass(center)
    
    # get center_vel and center_angular_mom
    # families must be parsed individually then combined
    vmdata = charm.reduceParticle(famgroups[0], vmmap, vmreduce, None)
    if vmdata == None :
        raise StandardError('MapReduce returned NoneType object.')
    center_vel = vmdata[0][2]
    center_angular_mom = vmdata[0][3]
    # if there are additional families, parse each then combine results weighting by mass
    if len(famgroups) > 1 :
        old_mass = vmdata[0][1]
        for i in range(len(famgroups)-1) :
            vmdata = charm.reduceParticle(famgroups[i+1], vmmap, vmreduce, None)
            new_mass = vmdata[0][1]
            center_vel = weightvecs(old_mass, center_vel, new_mass, vmdata[0][2])
            center_angular_mom = weightvecs(old_mass, center_angular_mom, new_mass, vmdata[0][3])
            old_mass += new_mass
    
    # for an elliptical projection, use Katz to get shape & then find vel, mom
    # when restoring this, find out what happened to center_ell?
    if projection == 'ell' :
        if fit_radius == 0. :
            print 'fit_radius = 0 in an elliptical projection.\nusing all particles in batch to find shape of ellipses.'
        ell_matrix = [[0., 0., 0.], [0., 0., 0.], [0., 0., 0.]]
        center_ell = [0., 0., 0.]
        ba = ca = phi = theta = psi = 0.
        center = list(center)
        shape_data = elliptical.find_shape(famgroups, center, fit_radius, ell_matrix, center_ell)
        center = tuple(center)
        if type(shape_data) == type(None) :
            print 'find_shape() did not succeed'
            return
            # raise StandardError('Problem in elliptical.find_shape(): no fit found.')
        else :
            ba, ca, phi, theta, psi = shape_data
            print 'b/a = %g, c/a = %g' % (ba, ca)
        center = [0.]*config.MAXDIM
        center_vel = [0.]*config.MAXDIM
        center_angular_mom = [0.]*config.MAXDIM
        vel_data = elliptical.find_vel(famgroups,center,center_vel,center_angular_mom, ell_matrix, center_ell, ba, ca, fit_radius)
    else :
        ba, ca, phi, theta, psi, center_ell, ell_matrix = (0., 0., 0., 0., 0., [0.]*config.MAXDIM, [[0.]*config.MAXDIM]*config.MAXDIM)

    # find max_radius
    # maxradmap iterates over all particles and compares each radius to the previous max found
    p_param = (center)
    max_radius = 0.
    if family == 'all' :
        max_radius = charm.reduceParticle(group, maxradmap, maxradreduce, p_param)[0][1]
    else :
        for i in range(len(famgroups)) :
            fam_radius = charm.reduceParticle(famgroups[i], maxradmap, maxradreduce, p_param)[0][1]
            max_radius = max(max_radius, fam_radius)
    
    # TIPSY uses an approximation for max_radius which can't be adequately emulated here.
    # for the run99 data file, the value of max_radius is 8.18829; set it manually.
    # max_radius = 8.18829
    
    # determine bin_size
    # if min_radius > 0, then bin 0 should have the specified radius.
    # other than this exception, all bins have constant size in either lin or log space as specified
    if min_radius > max_radius and nbins > 1 :
        raise StandardException('Value for min_radius encompasses all particles; cannot bin data.')
    # min_radius > 0 case creates a scenario where nbins = 1 would create a /0 error, must handle here.
    if nbins == 1 :
        bin_size = max_radius
        # bin_type and min_radius are superfluous to nbins = 1 case
        bin_type = 'lin'
        min_radius = 0.
    # find size in lin space
    elif bin_type == 'lin' :
        if min_radius > 0. :
            bin_size = (max_radius - min_radius) / float(nbins - 1)
        else :
            bin_size = (max_radius - min_radius) / float(nbins)
    # find size in log space
    else :
        min_radius = math.log10(min_radius)
        bin_size = (math.log10(max_radius) - min_radius) / float(nbins - 1)
    
    # calculate bin boundary radii
    bounds = [0.] * (nbins + 1)
    # the first boundary radius is affected by min_radius, handle
    if bin_type == 'lin' and min_radius == 0.:
        bounds[1] = bin_size
    else :
        bounds[1] = min_radius
    # each following bound should be bound[previous] + bin_size
    for i in range(2, nbins + 1) :
        bounds[i] = bounds[i - 1] + bin_size
    # if logarithmic binning, convert values into linear space
    if bin_type == 'log' :
        for i in range(1, nbins + 1) :
            bounds[i] = pow(10., bounds[i])

    starlf = star_lum.star_lum()
    
    sim_time = charm.getTime()
    
    # do calculations which must see individual particles.
    # the set of attributes on a particle varies by family, so each family must be processed seperately
    params = [None, isbaryon, bounds, projection, nbins, bin_type, bin_size, min_radius, max_radius, center, center_vel, ell_matrix, center_ell, ba, ca, config.msolunit, config.gasconst, sim_time, config.time_unit, starlf.age, starlf.lum, starlf.lumv_fit, starlf.vv_dat, starlf.vv_fit, starlf.bv_dat, starlf.bv_fit, center_angular_mom]
    fam_data = [None] * len(famgroups)
    for i in range(len(famgroups)) :
        params[0] = families[i]
        fam_data[i] = charm.reduceParticle(famgroups[i], basemap, basereduce, tuple(params))
        
    # shows per-family result of MapReduce, sometimes useful for debugging
    #    if debug_flag == 1 :
    #        print '\nThe current family is: ' + families[i]
    #        for line in fam_data[i] :
    #            print line
    if len(fam_data) < 1 or fam_data == None :
        raise StandardException('MapReduce for fam_data has length zero or is NoneType.')
    
    # sum results into one list.
    # Note that if a bin has no particles, it will be missing from the reduce result. data[] should have no missing rows.
    # values are: bin, number, mass, vel_radial, vel_radial_sigma, vel_tang_sigma, angular_mom[x,y,z], lum, density, temp, pressure, entropy, gas_mass
    data = [None] * nbins
    for i in range(nbins) :
        data[i] = [i, 0, 0., 0., 0., 0., [0., 0., 0.], 0., 0., 0., 0., 0., 0.]    
    for fam in fam_data :
        for row in fam :
            bin = row[0]
            # merge fields 0 thru 5
            for i in range(1, 6) :
                data[bin][i] += row[i]
            # merge field 6 (angular momentum vector)
            for i in range(3) :
                data[bin][6][i] += row[6][i]
            # merge fields 7 thru 12
            for i in range(7, 13) :
                data[bin][i] += row[i]
    if not len(data) == nbins :
        raise StandardError('nbins != length of result after basemap')

    # calculate remaining "base" values that did not need to be handled in the map
    mass_r = 0.
    for i in range(nbins) :
        # get max radius, min radius, and mean radius
        r_max = bounds[i + 1]
        r_min = bounds[i]
        r_mean = (r_max + r_min) / 2.
        # number of particles in bin
        number = data[i][1]
        # if the bin is empty then some calculations e.g. 1/mass will explode; catch these.
        if number != 0 :
            # find volume by projection
            pi = 3.141592653589793
            if projection == 'ell' :
                volume = 4. * ba * ca / 3. * pi * (pow((r_max),3) - pow((r_min),3))
            elif projection == 'cyl' :
                volume = pi * (pow((r_max),2) - pow((r_min),2))
            else :
                volume = (4. / 3.) * pi * (pow(r_max,3) - pow(r_min,3))
            # unpack values. deal with non-sum calculations, largely n / mass
            mass = data[i][2]
            mass_r += mass
            vel_radial = data[i][3] / mass
            vel_radial_sigma = data[i][4] / mass
            if vel_radial_sigma > vel_radial**2 :
                vel_radial_sigma = math.sqrt(vel_radial_sigma - vel_radial**2)
            else :
                vel_radial_sigma = 0.
            vel_tang_sigma = data[i][5] / mass
            ang_mom = scalevec(1./mass, data[i][6])
            ang = vlength(ang_mom)
            if ang > 0.0 :
                ang_theta = 180.0 * math.acos(ang_mom[2] / ang) / pi
            else :
                ang_theta = 0.0
            ang_phi = 180.0 * math.atan2(ang_mom[1], ang_mom[0]) / pi
            rho = mass / volume
            vel_circ = ang / r_mean
            c_vel = math.sqrt(mass_r / r_max)
            if vel_tang_sigma > vel_circ**2 :
                vel_tang_sigma = math.sqrt(vel_tang_sigma - vel_circ**2)
            else :
                vel_tang_sigma = 0.
            # star stuff
            lum_den = data[i][7] / volume
            # gas stuff
            if 'gas' in families and data[i][12] > 0 :
                density  = data[i][8]  / data[i][12]
                temp     = data[i][9]  / data[i][12]
                pressure = data[i][10] / data[i][12]
                entropy  = data[i][11] / data[i][12]
            else :
                density  = 0.
                temp     = 0.
                pressure = 0.
                entropy  = -config.HUGE
            data[i] = [r_max, number, rho, mass_r, c_vel, vel_radial, vel_radial_sigma, vel_circ, vel_tang_sigma, ang, ang_theta, ang_phi, density, temp, pressure, entropy, lum_den]
        else :
            c_vel = math.sqrt(mass_r / r_max)
            data[i] = [r_max, 0, 0., mass_r, c_vel, 0., 0., 0., 0., 0., 0.,
                       0., 0., 0., 0., 0., 0.]
    
    # build header row
    # may need to update the exact text used for clarity or for consistency with TIPSY
    headers = ('radius', 'number', 'rho', 'mass_r', 'c_vel', 'vel_radial', 'vel_radial_sigma', 'vel_circ', 'vel_tang_sigma', 'ang', 'ang_theta', 'ang_phi')
    if family in ['gas', 'baryon', 'all'] :
        headers += ('mmwg_density', 'mwg_temp', 'mwg_pres', 'mwg_entr')
    if family in ['star', 'baryon', 'all'] :
        headers += ('lum_V',)
    
    # write output to file
    f = open(filename, 'w')
    # write headers according to families present
    f.write('#')
    for i in range(len(headers)) :
        f.write(' ' + headers[i])
    f.write('\n')
    # write data according to families present
    for i in range(nbins) :
        f.write('%g %d %g %g %g %g %g %g %g %g %g %g' % tuple(data[i][0:12]))
        if 'gas' in families :
            f.write(' %g %g %g %g' % tuple(data[i][12:16]))
        if 'star' in families :
            f.write(' %g' % tuple(data[i][16:17]))
        f.write('\n')
    f.close()

    # there is no current mechanism to delete groups.
    # if one is added, delete temporary working groups here.
  except :
    print traceback.format_exc()

vmmap = """def localparticle(p):
    # map code to find the per-particle contribution to the system's mass, velocity, and angular momentum
    # fn to multiply a vector v by a scalar c
    def scalevec(c, v) :
        return [c * v[0], c * v[1], c * v[2]]
    # fn to take the cross product of two vectors in R3
    def crossprod(a, b) :
        return [a[1]*b[2] - a[2]*b[1], a[2]*b[0] - a[0]*b[2], a[0]*b[1] - a[1]*b[0]]
    center_vel = scalevec(p.mass, p.velocity)
    center_ang_mom = scalevec(p.mass, crossprod(p.position, p.velocity))
    return (0, p.mass, center_vel, center_ang_mom)
"""

vmreduce = """def localparticle(p):
    # reduce code to sum the contribution by each particle to system mass, velocity, and angular momentum
    # fn to add two vectors
    def addvec(a, b) :
        return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]
    mass = 0.
    center_vel = [0., 0., 0.]
    center_ang_mom = [0., 0., 0.]
    for i in range(len(p.list)) :
        mass += p.list[i][1]
        center_vel = addvec(center_vel, p.list[i][2])
        center_ang_mom = addvec(center_ang_mom, p.list[i][3])
    return (0, mass, center_vel, center_ang_mom)
"""

maxradmap = """def localparticle(p):
    # map code to get the radius for each particle
    # fn to subtract vector b from vector a
    def subvec(a, b) :
        return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]
    # fn to find the length of a vector
    def vlength(a) :
        import math
        return math.sqrt(pow(a[0], 2) + pow(a[1], 2) + pow(a[2], 2))
    # ---> ell and cyl use a center axis instead of a center point, must implement when other projections enabled <---
    import math
    center = p._param
    radius = vlength(subvec(p.position, center))
    return(0, radius)
"""

maxradreduce = """def localparticle(p):
    # reduce code to find maximum radii among values calculated in maxradmap
    max_radius = 0.0
    for i in range(len(p.list)) :
        max_radius = max(max_radius, p.list[i][1])
    return(0, max_radius)
"""

basemap = """def localparticle(p):
    # map code to get per-particle values needed to calculate base fields
    import math
    # define some functions to improve readability of parent function
    # fn to take the dot product of two vectors in R3
    def dotprod(a, b) :
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2]
    # fn to take the cross product of two vectors in R3
    def crossprod(a, b) :
        return [a[1]*b[2] - a[2]*b[1], a[2]*b[0] - a[0]*b[2], a[0]*b[1] - a[1]*b[0]]
    # fn to multiply a vector v by a scalar c
    def scalevec(c, v) :
        return [c * v[0], c * v[1], c * v[2]]
    # fn to add two vectors
    def addvec(a, b) :
        return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]
    # fn to subtract vector b from vector a
    def subvec(a, b) :
        return [a[0] - b[0], a[1] - b[1], a[2] - b[2]]
    # fn to find the length of a vector
    def vlength(a) :
        import math
        return math.sqrt(pow(a[0], 2) + pow(a[1], 2) + pow(a[2], 2))
    # fn to find distance from a line.
    # sqrt(dot(cross(pos - cen, ang_mom), cross(pos - cen, ang_mom)) / dot(ang_mom, ang_mom))
    def perp_distance(cen, ang_mom, pos) :
        import math
        return math.sqrt( ( ((pos[1] - cen[1])*ang_mom[2] - (pos[2] - cen[2])*ang_mom[1])**2
                          + ((pos[2] - cen[2])*ang_mom[0] - (pos[0] - cen[0])*ang_mom[2])**2
                          + ((pos[0] - cen[0])*ang_mom[1] - (pos[1] - cen[1])*ang_mom[0])**2 )
                          / ( ang_mom[0]**2 + ang_mom[1]**2 + ang_mom[2]**2 ) )
    # multiply a matrix and a vector
    def matrix_vector_mult(mat,a,b) :
        for i in range(3) :
            b[i] = 0.
            for j in range(3) :
                b[i] += mat[i][j] * a[j]
    # find distance with respect to elliptical parameters
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
    # import necessary packages & unpack values passed on p._param
    import math
    import tipsy.spline as spline
    fam, isbaryon, bounds, projection, nbins, bin_type, bin_size, min_radius, max_radius, center, center_vel, ell_matrix, center_ell, ba, ca, msolunit, gasconst, sim_time, time_unit, age, lum, lumv_fit, vv_dat, vv_fit, bv_dat, bv_fit, center_angular_mom = p._param
    # find radius and bin number
    radius = 0.
    if projection == 'sph' :
        radius = vlength(subvec(p.position, center))
    elif projection == 'cyl' :
        radius = perp_distance(center, center_angular_mom, p.position)
    elif projection == 'ell' :
        radius = ell_distance(p.position, ell_matrix, center_ell, ba, ca)
    # deal with out-of-bounds conditions as a result of precision
    if radius > max_radius :
        return (nbins - 1, 0, 0., 0., 0., 0., [0., 0., 0.], 0., 0., 0., 0., 0., 0.)
    # find the bin number by comparison to a list of boundary radii
    bin = nbins - 1
    for i in range(1, len(bounds)) :
        if radius < bounds[i] :
            bin = i - 1
            break
    # calculate per-particle properties
    delta_x = subvec(p.position, center)
    delta_v = subvec(p.velocity, center_vel)
    ang_mom = scalevec(p.mass, crossprod(delta_x, delta_v))
    dx2 = dotprod(delta_x, delta_x)
    vel_tang_sigma = 0.
    if dx2 != 0. :
        vel_shell = crossprod(ang_mom, delta_x)
        c_vtp = -1. / dx2
        c_vt = c_vtp * dotprod(delta_x, delta_v)
        vel_tang = addvec(delta_v, scalevec(c_vt, delta_x))
        vel_tang_pec = addvec(vel_tang, scalevec(c_vtp, vel_shell))
        vel_tang_sigma = p.mass * dotprod(vel_tang_pec, vel_tang_pec)
        vel = dotprod(delta_x, delta_v) / math.sqrt(dx2)
    else :
        vel = vlength(delta_v)
    vel_radial = p.mass * vel
    vel_radial_sigma = p.mass * pow(vel, 2)
    # per-family calculations: zero result variables & pass always
    lum_star = 0.
    density = 0.
    temp = 0.
    pressure = 0.
    entropy = 0.
    gas_mass = 0.
    if fam == 'star' :
        mass = p.mass
        time = p.formationtime
        n = 11
        
        star_age = max((sim_time - time) * time_unit, .01)
        mass_to_light = spline.spft(star_age, age, lum, lumv_fit, n, 0)
        lum_star = mass * msolunit / mass_to_light
        if isbaryon == 1 :
            vv = spline.spft(star_age, age, vv_dat, vv_fit, n, 0)
            lum_star *= pow(10., -.4*vv)
        else :
            bv = spline.spft(star_age, age, bv_dat, bv_fit, n, 0)
            lum_star *= pow(10., -.4*bv)
    if fam == 'gas' :
        gas_mass = p.mass
        pd = p.density
        pt = p.temperature
        pm = p.meanmwt
        density = gas_mass * pd
        temp = gas_mass * pt
        pressure = density * pt * gasconst / pm
        entropy = gas_mass * math.log10(pow(pt, 1.5)/(pd))
    return (bin, 1, p.mass, vel_radial, vel_radial_sigma, vel_tang_sigma, ang_mom, lum_star, density, temp, pressure, entropy, gas_mass)
"""

basereduce = """def localparticle(p):
    # sum values used for calculation of base fields to get per-bin results
    bin = p.list[0][0]
    num = 0
    mass = 0.
    vel_radial = 0.
    vel_radial_sigma = 0.
    vel_tang_sigma = 0.
    ang_mom = [0., 0., 0.]
    radius = 0.
    lum = 0.
    density = 0.
    temp = 0.
    pressure = 0.
    entropy = 0.
    gas_mass = 0.
    for i in range(len(p.list)) :
        num += p.list[i][1]
        mass += p.list[i][2]
        vel_radial += p.list[i][3]
        vel_radial_sigma += p.list[i][4]
        vel_tang_sigma += p.list[i][5]
        ang_mom[0] += p.list[i][6][0]
        ang_mom[1] += p.list[i][6][1]
        ang_mom[2] += p.list[i][6][2]
        lum += p.list[i][7]
        density += p.list[i][8]
        temp += p.list[i][9]
        pressure += p.list[i][10]
        entropy += p.list[i][11]
        gas_mass += p.list[i][12]
    return (bin, num, mass, vel_radial, vel_radial_sigma, vel_tang_sigma, ang_mom, lum, density, temp, pressure, entropy, gas_mass)
"""


