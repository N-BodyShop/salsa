import traceback, math, config, tipsyf

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
    
    # make an iterator for the bins
    nbins = len(pos)
    index = range(nbins)
    
    # zero the return fields
    for i in index :
        acc_gas[i] = [0., 0., 0.]
        acc_star[i] = [0., 0., 0.]
        acc_dark[i] = [0., 0., 0.]
    
    params = tuple([nbins] + pos)
    
    # iterate over gas particles
    result = charm.reduceParticle(gasgroup, gravmap, gravreduce, params)
    print result
    for i in index :
        acc_gas[i][0] = result[0][i+1][0]
        acc_gas[i][1] = result[0][i+1][1]
        acc_gas[i][2] = result[0][i+1][2]
    
    # iterate over star particles
    result = charm.reduceParticle(stargroup, gravmap, gravreduce, params)
    for i in index :
        acc_star[i][0] = result[0][i+1][0]
        acc_star[i][1] = result[0][i+1][1]
        acc_star[i][2] = result[0][i+1][2]
    
    # iterate over dark particles
    result = charm.reduceParticle(darkgroup, gravmap, gravreduce, params)
    for i in index :
        acc_dark[i][0] = result[0][i+1][0]
        acc_dark[i][1] = result[0][i+1][1]
        acc_dark[i][2] = result[0][i+1][2]
    
gravmap = """def localparticle(p):
    import math
    from tipsy import config, tipsyf
    nbins = p._param[0]
    pos = p._param[1:nbins+1]
    result = [None] * nbins
    for i in range(nbins) :
        delta_x = [pos[i][0] - p.position[0], pos[i][1] - p.position[1], pos[i][2] - p.position[2]]
        dxdotdx = delta_x[0]*delta_x[0] + delta_x[1]*delta_x[1] + delta_x[2]*delta_x[2]
        if dxdotdx == 0. :
            result[i] = [0., 0., 0.]
        else :
            sdxdotdx = math.sqrt(dxdotdx)
            r3inveff = 1. / sdxdotdx / dxdotdx
            if not p.softening == 0. :
                dxdeldxg = 0.5 * sdxdotdx / p.softening * config.NINTERP
                smindex = min(config.NINTERP, int(dxdeldxg))
                drsm = min(1., dxdeldxg - smindex)
                r3inveff = r3inveff * (1. - drsm) * tipsyf.acsmooth_val(smindex) + drsm*tipsyf.acsmooth_val(smindex+1)
            acci = p.mass * r3inveff
            result[i] = [-delta_x[0]*acci, -delta_x[1]*acci, -delta_x[2]*acci]
    return tuple([0] + result)
"""

gravreduce = """def localparticle(p):
    nbins = p._param[0]
    index = range(nbins)
    acc_fam = [None] * nbins
    for i in index :
        acc_fam[i] = [0., 0., 0.]
    for i in range(len(p.list)) :
        for j in index :
            acc_fam[j][0] += p.list[i][j+1][0]
            acc_fam[j][1] += p.list[i][j+1][1]
            acc_fam[j][2] += p.list[i][j+1][2]
    return tuple([0] + acc_fam)
"""

