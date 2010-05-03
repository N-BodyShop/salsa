import traceback, math, config, tipsyf, charm

def grav(pos, acc_gas, acc_star, acc_dark, group) :
    # follows grav.c from Tipsy
    
    for i in range(3) :
	    acc_gas[i] = 0.
	    acc_star[i] = 0.
	    acc_dark[i] = 0.
    delta_x = [0., 0., 0.]
    
    if not config.acc_loaded :
	    tipsyf.acc_load()
    
    # make famgroups
    charm.createGroup_Family(group + 'FAMgas', group, 'gas')
    charm.createGroup_Family(group + 'FAMstar', group, 'star')
    charm.createGroup_Family(group + 'FAMdark', group, 'dark')
    
    # iterate over gas particles
    params = (pos, config.epsgas_grav)
    result = charm.reduceParticle(group + 'FAMgas', gravmap, gravreduce, params)
    acc_gas[0] = result[0][1][0]
    acc_gas[1] = result[0][1][1]
    acc_gas[2] = result[0][1][2]
    
    # iterate over star particles
    params = (pos, config.epsgas_grav)
    result = charm.reduceParticle(group + 'FAMstar', gravmap, gravreduce, params)
    acc_star[0] = result[0][1][0]
    acc_star[1] = result[0][1][1]
    acc_star[2] = result[0][1][2]
    
    # iterate over dark particles
    params = (pos, config.eps_grav)
    result = charm.reduceParticle(group + 'FAMdark', gravmap, gravreduce, params)
    acc_dark[0] = result[0][1][0]
    acc_dark[1] = result[0][1][1]
    acc_dark[2] = result[0][1][2]
    
gravmap = """def localparticle(p):
    import math
    pos, eps = p._param
    delta_x = [pos[0] - p.position[0], pos[1] - p.position[1], pos[2] - p.position[2]]
    dxdotdx = delta_x[0]*delta_x[0] + delta_x[1]*delta_x[1] + delta_x[2]*delta_x[2]
    if dxdotdx == 0. :
        return (0, [0., 0., 0.])
    sdxdotdx = math.sqrt(dxdotdx)
    r3inveff = 1./sdxdotdx/dxdotdx
    if not eps == 0. :
        dxdeldxg = sdxdotdx * config.NINTERP / eps / 2.
        smindex = min(config.NINTERP, int(dxdeldxg))
        drsm = min(1., dxdeldxg-smindex)
        accsm = (1. - drsm) * config.acsmooth[smindex]+drsm*acsmooth[1+smindex]
        r3inveff = accsm * r3inveff
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

