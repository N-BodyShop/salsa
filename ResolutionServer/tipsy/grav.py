import traceback, math, config, tipsyf, charm

def grav(pos, acc_gas, acc_star, acc_dark, gasgroup, stargroup, darkgroup) :
    # follows grav.c from Tipsy
    # 
    # note that this deviates in the behavior of eps_grav and
    # epsgas_grav - instead of using a user-input value for all
    # particles, the individual particle's softening value is
    # used. in TIPSY, this corresponds to the "eps" field of
    # a dark or star particle and the "hsmooth" of a gas particle.
    # 
    # method assumes that famgroups for group exist
    
    for i in range(3) :
	    acc_gas[i] = 0.
	    acc_star[i] = 0.
	    acc_dark[i] = 0.
    delta_x = [0., 0., 0.]
    params = (pos[0], pos[1], pos[2])
    
    # iterate over gas particles
    result = charm.reduceParticle(gasgroup, gravmap, gravreduce, params)
    acc_gas[0] = result[0][1][0]
    acc_gas[1] = result[0][1][1]
    acc_gas[2] = result[0][1][2]
    
    # iterate over star particles
    result = charm.reduceParticle(stargroup, gravmap, gravreduce, params)
    acc_star[0] = result[0][1][0]
    acc_star[1] = result[0][1][1]
    acc_star[2] = result[0][1][2]
    
    # iterate over dark particles
    result = charm.reduceParticle(darkgroup, gravmap, gravreduce, params)
    acc_dark[0] = result[0][1][0]
    acc_dark[1] = result[0][1][1]
    acc_dark[2] = result[0][1][2]
    
gravmap = """def localparticle(p):
    import math, config, tipsyf
    pos_x, pos_y, pos_z = p._param
    delta_x = [pos_x - p.position[0], pos_y - p.position[1], pos_z - p.position[2]]
    dxdotdx = delta_x[0]*delta_x[0] + delta_x[1]*delta_x[1] + delta_x[2]*delta_x[2]
    if dxdotdx == 0. :
        return (0, [0., 0., 0.])
    sdxdotdx = math.sqrt(dxdotdx)
    r3inveff = 1. / sdxdotdx / dxdotdx
    if not p.softening == 0. :
        dxdeldxg = 0.5 * sdxdotdx / p.softening * config.NINTERP
        smindex = min(config.NINTERP, int(dxdeldxg))
        drsm = min(1., dxdeldxg - smindex)
        r3inveff = r3inveff * (1. - drsm) * tipsyf.acsmooth_val(smindex) + drsm*tipsyf.acsmooth_val(smindex+1)
    acci = p.mass * r3inveff
    acc = [-delta_x[0]*acci, -delta_x[1]*acci, -delta_x[2]*acci]
    return (0, acc)
"""

gravreduce = """def localparticle(p):
    acc_fam = [0., 0., 0.]
    for i in range(len(p.list)) :
        acc_fam[0] += p.list[i][1][0]
        acc_fam[1] += p.list[i][1][1]
        acc_fam[2] += p.list[i][1][2]
    return (0, acc_fam)
"""

