import traceback, config, math, charm

def safecalc_meanmwt(temp, rho) :
    try :
        calc_meanmwt(temp, rho)
        print 'Fn calc_meanmwt() completed successfully.'
    except :
        print traceback.format_exc()

def calc_meanmwt(temperature, density) :
    params = [None]*5
    
    cosmof3 = 1.0
    gp0_H = 0
    gp0_He = 0
    gp0_Hep = 0
    
    n_h = (density/cosmof3)*config.MSOLG*config.msolunit*config.fhydrogen/((pow(config.kpcunit, 3))*config.MHYDR*pow(config.KPCCM, 3))
    y = 1.0 - config.fhydrogen
    r = y / 4.0 / (1.0 - y)
    g0 = gp0_H
    g1 = gp0_He
    g2 = gp0_Hep
    
    # xion(temperature, [x, x_1, x_2, x_3, n_e], n_h, g0, g1, g2)
    xion(temperature, params, n_h, g0, g1, g2, r)
    
    x   = params[0]
    x_1 = params[1]
    x_2 = params[2]
    x_3 = params[3]
    f_e = 1.0 - x + x_2*r + (x_3)*2.0*r
    
    return ((1. + 4.*r) / (1. + r + f_e))

def a_Hp(t) :
    if (t > 0.0) :
        return (8.40e-11/math.sqrt(t))*pow(t/1e3, -0.2)/(1.0 + pow(t/1e6, 0.7))
    return 0.0
    
def g_H(t) :
    if (t == 0.0) :
        return 0.0
    return 5.85e-11*math.sqrt(t)*math.exp(-157809.1 / t)/(1.0 + math.sqrt(t/1e5))
    
def a_Hep(t) :
    if (t > 0.0) :
        return 1.5e-10*pow(t, -0.6353)
    return 0.0
    
def a_p(t) :
    if (t == 0.0) :
        return 0.0
    return .0019*pow(t, -1.5)*math.exp(-4.7e5/t)*(math.exp(-9.4e4/t)*.3 + 1.0)
    
def g_He(t) :
    if (t == 0.0) :
        return 0.0
    return 2.38e-11*math.sqrt(t)*math.exp(-285335.4 / t)/(1.0 + math.sqrt(t/1e5))
    
def a_Hepp(t) :
    if (t > 0.0) :
        return (3.36e-10/math.sqrt(t))*pow(t/1e3, -0.2)/(1.0 + pow(t/1e6, 0.7))
    return 0.0
    
def g_Hep(t) :
    if (t == 0.0) :
        return 0.0
    return 5.68e-12*math.sqrt(t)*math.exp(-631515.0 / t)/(1.0 + math.sqrt(t/1e5))

def xion(t, params, n_h, g0, g1, g2, r) :
    a = i = zx = zx1 = zx2 = zx3 = za_Hp = za_Hep = za_Hepp = zg_H = zg_He = zg_Hep = n_e = old_n_e = None
    TOLERANCE = 1e-14
    # the params list holds values which are to be changed by xion() to get around Python's immutable types
    n_e = params[4]

    zx = .05
    zx1 = 1.0
    zx2 = 0.0
    n_e = (1.0 - zx)*n_h
    old_n_e = n_e

    za_Hp = a_Hp(t)
    zg_H = g_H(t)
    za_Hep = a_Hep(t) + a_p(t)
    zg_He = g_He(t)
    za_Hepp = a_Hepp(t)
    zg_Hep = g_Hep(t)
    
    for i in range(1, 51) : 
        if(i == 50) :
            raise StandardError('Too many iterations in xion.\nt: %g, n_h: %g, n_e: %g' % (t, n_h, n_e))
        # solve for neutral H fraction given constant n_e
        if (g0 + (zg_H + za_Hp)*n_e != 0.0) :
            zx = za_Hp*n_e/(g0 + (zg_H + za_Hp)*n_e)
        else :
            zx = 1.0
        if (g1 + zg_He*n_e > 1e-50) :
            a = za_Hep*n_e/(g1 + zg_He*n_e)
        else :
            zx1 = 1.0
            zx2 = 0.0
            zx3 = 0.0
            n_e = (1.0 - zx)*n_h
            if(old_n_e == 0.0 or abs(n_e - old_n_e)/n_h < TOLERANCE) :
                break
            n_e = .5*(n_e + old_n_e)
            old_n_e = n_e
            continue
        if (g2 + (zg_Hep + (a + 1.0)*za_Hepp)*n_e != 0.0) :
            zx2 = za_Hepp*n_e/(g2 + (zg_Hep + (a + 1.0)*za_Hepp)*n_e)
            zx1 = zx2 * a
            zx3 = zx2*(g2 + zg_Hep*n_e)/za_Hepp/n_e
        else :
            zx3 = 0.0
            zx2 = 0.0
            zx1 = 1.0
        n_e = (1.0 - zx + zx2*r + zx3*2.0*r)*n_h
        if(abs(n_e - old_n_e)/n_h < TOLERANCE) :
            break
        n_e = .5*(n_e + old_n_e)
        old_n_e = n_e
    
    zx = min(1.,zx)
    # x = zx
    # x_1 = zx1
    # x_2 = zx2
    # x_3 = zx3
    # p_ne = n_e
    params[0] = zx
    params[1] = zx1
    params[2] = zx2
    params[3] = zx3
    params[4] = n_e

