import traceback, config, tipsyf, math, grav

def saferotcur() :
    try :
        rotationcurve()
        print '\nMethod \'rotationcurve()\' completed with no errors.'
    except :
        print traceback.format_exc()

# the aliasing here is not handled particularly well.
# any default values will need to be updated on both commands simultaneously.

def rotcur(group='All', center_group='All', center_type='com', center_family='all', bin_type='lin', number_bins=30, filename='rotcurv.DAT', min_radius=0., max_radius=10.) :
    """Alias for rotationcurve()."""
    rotationcurve(group, center_group, center_type, center_family, bin_type, number_bins, filename, min_radius, max_radius)

def rotationcurve(group='All', center_group='All', center_type='com', center_family='all', bin_type='lin', number_bins=30, filename='rotcurv.DAT', min_radius=0., max_radius=10.) :
    """Perform the Tipsy rotationcurve() function.
    
    rotationcurve is a command that produces a file of name
    filename that contains the circular velocity as a function
    of radius gravitationally induced by the particles in group.
    
    The added parameter center_type parallels the cgroup_type attribute
    which Tipsy reads from the center group. Here, it specifies whether
    to take the center of center_group by potential or center of mass.
    Valid options are com and pot."""
    
    # check if simulation loaded. check if groups are valid.
    groups_loaded = charm.getGroups()
    if groups_loaded == None :
        raise StandardError('Simulation not loaded')
    if group not in groups_loaded :
        raise StandardError('Selected group is not present in the simulation.')
    if center_group not in groups_loaded :
        raise StandardError('Selected center_group is not present in the simulation.')
    # check if center_type is valid
    if center_type not in ['com', 'pot'] :
        raise StandardError('Value of center_type must be \'com\' or \'pot\'')
    # check if center_family is valid
    if center_family not in ['dark', 'gas', 'star', 'baryon', 'all'] :
        raise StandardError('Selected family not valid.')
    # check if bin_type is valid
    if bin_type == 'linear' :
        bin_type = 'lin'
    elif bin_type == 'logarithmic' :
        bin_type = 'log'
    elif bin_type not in ['lin', 'log'] :
        raise StandardError('Value of bin_type must be lin or log.')
    
    # adjust for log binning if necessary, calculate bin_size
    if bin_type == 'log' :
        if min_radius <= 0. :
            min_radius = config.LOG_MIN
        min_radius = math.log10(min_radius)
        max_radius = math.log10(max_radius)
    bin_size = (max_radius - min_radius) / number_bins
    
    # make famgroups
    gasgroup = center_group + 'FAMgas'
    stargroup = center_group + 'FAMstar'
    darkgroup = center_group + 'FAMdark'
    charm.createGroup_Family(gasgroup, center_group, 'gas')
    charm.createGroup_Family(stargroup, center_group, 'star')
    charm.createGroup_Family(darkgroup, center_group, 'dark')
    
    # find center and center_angular_mom
    center = [0., 0., 0.]
    center_angular_mom = [0., 0., 0.]
    if center_type == 'pot' :
        center = charm.findAttributeMin(group, 'potential')
    if center_family in ['dark', 'star', 'gas'] :
        if center_type == 'com' :
            center = charm.getCenterOfMass(center_group + 'FAM' + center_family)
        center_angular_mom = charm.reduceParticle(center_group + 'FAM' + center_family, angmommap, angmomreduce, None)[2]
    elif center_family == 'baryon' :
        charm.createGroup_Family(stargroup, center_group, 'star')
        charm.createGroup_Family(gasgroup, center_group, 'gas')
        stardata = charm.reduceParticle(stargroup, angmommap, angmomreduce, None)
        gasdata = charm.reduceParticle(gasgroup, angmommap, angmomreduce, None)
        if center_type == 'com' :
            censtar = charm.getCenterOfMass(stargroup)
            cengas = charm.getCenterOfMass(gasgroup)
            tipsyf.mass_add_vec(center, censtar, stardata[1], cengas, gasdata[1])
        tipsyf.mass_add_vec(center_angular_mom, stardata[2], stardata[1], gasdata[2], gasdata[1])
    else :
        if center_type == 'com' :
            center = charm.getCenterOfMass(center_group)
        center_angular_mom = charm.reduceParticle(center_group, angmommap, angmomreduce, None)[0][2]
    
    # normalize the center angular momentum vector
    norm = math.sqrt(tipsyf.dot_product(center_angular_mom, center_angular_mom))
    for i in range(3) :
        center_angular_mom[i] /= norm
    
    # get frame-specific unit vectors
    xaxis = [1, 0, 0]
    yaxis = [0, 1, 0]
    unit2 = [0., 0., 0.]
    unit4 = [0., 0., 0.]
    if abs(tipsyf.dot_product(center_angular_mom, xaxis)) < 0.95 :
        unit1 = tipsyf.cross_product(center_angular_mom, xaxis)
    else :
        unit1 = tipsyf.cross_product(center_angular_mom, yaxis)
    norm = math.sqrt(tipsyf.dot_product(unit1, unit1))
    for i in range(3) :
        unit1[i] /= norm
        unit2[i] = -unit1[i]
    unit3 = tipsyf.cross_product(center_angular_mom, unit1)
    norm = math.sqrt(tipsyf.dot_product(unit3, unit3))
    for i in range(3) :
        unit3[i] /= norm
        unit4[i] = -unit3[i]
    
    # calculate output
    
    index = range(number_bins)
    
    acc_rad_bar = [0.] * number_bins
    acc_rad_dark = [0.] * number_bins
    acc_rad_tot = [0.] * number_bins
    rot_vel_bar = [0.] * number_bins
    rot_vel_dark = [0.] * number_bins
    rot_vel_tot = [0.] * number_bins
    test_particle = [None] * number_bins
    acc_dark = [None] * number_bins
    acc_star = [None] * number_bins
    acc_gas = [None] * number_bins
    acc_bar = [None] * number_bins
    acc_tot = [None] * number_bins
    radius = [None] * number_bins
        
    for i in index :
        radius[i] = (i+1) * bin_size + min_radius
        test_particle[i] = [0., 0., 0.]
        # acc_dark[i] = [0., 0., 0.]
        # acc_star[i] = [0., 0., 0.]
        # acc_gas[i] = [0., 0., 0.]
        acc_bar[i] = [0., 0., 0.]
        acc_tot[i] = [0., 0., 0.]

    if bin_type == 'log' :
        for i in index :
            radius[i] = pow(10.,radius[i])
        
    for i in index :
        tipsyf.vec_add_const_mult_vec(test_particle[i],center,radius[i],unit1)
        
    # first grav call
    grav.grav(test_particle,acc_gas,acc_star,acc_dark,gasgroup,stargroup,darkgroup)
    
    for i in index :
        tipsyf.add_vec(acc_bar[i],acc_gas[i],acc_star[i])
        tipsyf.add_vec(acc_tot[i],acc_bar[i],acc_dark[i])
        acc_rad_bar[i] = tipsyf.dot_product(acc_bar[i],unit1)
        acc_rad_dark[i] = tipsyf.dot_product(acc_dark[i],unit1)
        acc_rad_tot[i] = tipsyf.dot_product(acc_tot[i],unit1)
        rot_vel_bar[i] += math.sqrt(abs(radius[i]*acc_rad_bar[i]))
        rot_vel_dark[i] += math.sqrt(abs(radius[i]*acc_rad_dark[i]))
        rot_vel_tot[i] += math.sqrt(abs(radius[i]*acc_rad_tot[i]))
        tipsyf.vec_add_const_mult_vec(test_particle[i],center,radius[i],unit2)
    
    # second grav call
    grav.grav(test_particle,acc_gas,acc_star,acc_dark,gasgroup,stargroup,darkgroup)
    
    for i in index :
        tipsyf.add_vec(acc_bar[i],acc_gas[i],acc_star[i])
        tipsyf.add_vec(acc_tot[i],acc_bar[i],acc_dark[i])
        acc_rad_bar[i] = tipsyf.dot_product(acc_bar[i],unit2)
        acc_rad_dark[i] = tipsyf.dot_product(acc_dark[i],unit2)
        acc_rad_tot[i] = tipsyf.dot_product(acc_tot[i],unit2)
        rot_vel_bar[i] += math.sqrt(abs(radius[i]*acc_rad_bar[i]))
        rot_vel_dark[i] += math.sqrt(abs(radius[i]*acc_rad_dark[i]))
        rot_vel_tot[i] += math.sqrt(abs(radius[i]*acc_rad_tot[i]))
        tipsyf.vec_add_const_mult_vec(test_particle[i],center,radius[i],unit3)
    
    # third grav call
    grav.grav(test_particle,acc_gas,acc_star,acc_dark,gasgroup,stargroup,darkgroup)
    
    for i in index :
        tipsyf.add_vec(acc_bar[i],acc_gas[i],acc_star[i])
        tipsyf.add_vec(acc_tot[i],acc_bar[i],acc_dark[i])
        acc_rad_bar[i] = tipsyf.dot_product(acc_bar[i],unit3)
        acc_rad_dark[i] = tipsyf.dot_product(acc_dark[i],unit3)
        acc_rad_tot[i] = tipsyf.dot_product(acc_tot[i],unit3)
        rot_vel_bar[i] += math.sqrt(abs(radius[i]*acc_rad_bar[i]))
        rot_vel_dark[i] += math.sqrt(abs(radius[i]*acc_rad_dark[i]))
        rot_vel_tot[i] += math.sqrt(abs(radius[i]*acc_rad_tot[i]))
        tipsyf.vec_add_const_mult_vec(test_particle[i],center,radius[i],unit4)
    
    # fourth grav call
    grav.grav(test_particle,acc_gas,acc_star,acc_dark,gasgroup,stargroup,darkgroup)
    
    for i in index :
        tipsyf.add_vec(acc_bar[i],acc_gas[i],acc_star[i])
        tipsyf.add_vec(acc_tot[i],acc_bar[i],acc_dark[i])
        acc_rad_bar[i] = tipsyf.dot_product(acc_bar[i],unit4)
        acc_rad_dark[i] = tipsyf.dot_product(acc_dark[i],unit4)
        acc_rad_tot[i] = tipsyf.dot_product(acc_tot[i],unit4)
        rot_vel_bar[i] += math.sqrt(abs(radius[i]*acc_rad_bar[i]))
        rot_vel_dark[i] += math.sqrt(abs(radius[i]*acc_rad_dark[i]))
        rot_vel_tot[i] += math.sqrt(abs(radius[i]*acc_rad_tot[i]))
        rot_vel_bar[i] /= 4.
        rot_vel_dark[i] /= 4.
        rot_vel_tot[i] /= 4.
    
    # record output to file
    f = open(filename, 'w')
    for i in index :
        f.write('%g %g %g %g\n' % (radius[i],rot_vel_tot[i],rot_vel_dark[i],rot_vel_bar[i]))
    f.close()

angmommap = """def localparticle(p):
    # finds center_angular_mom
    # fn to multiply a vector v by a scalar c
    def scalevec(c, v) :
        return [c * v[0], c * v[1], c * v[2]]
    # fn to take the cross product of two vectors in R3
    def crossprod(a, b) :
        return [a[1]*b[2] - a[2]*b[1], a[2]*b[0] - a[0]*b[2], a[0]*b[1] - a[1]*b[0]]
    center_ang_mom = scalevec(p.mass, crossprod(p.position, p.velocity))
    return (0, p.mass, center_ang_mom)
"""

angmomreduce = """def localparticle(p):
    # finds center_angular_mom
    # fn to add two vectors
    def addvec(a, b) :
        return [a[0] + b[0], a[1] + b[1], a[2] + b[2]]
    mass = 0.
    center_ang_mom = [0., 0., 0.]
    for i in range(len(p.list)) :
        mass += p.list[i][1]
        center_ang_mom = addvec(center_ang_mom, p.list[i][2])
    return (0, mass, center_ang_mom)
"""

