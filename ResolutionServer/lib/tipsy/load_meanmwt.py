import traceback, charm, meanmwt

def safeload() :
    try :
        do_meanmwt()
        print 'Completed successfully.'
    except :
        print traceback.format_exc()

def do_meanmwt() :
    if 'AllFAMgas' not in charm.getGroups() :
        charm.createGroup_Family('AllFAMgas', 'All', 'gas')
    if 'meanmwt' not in charm.getAttributes('gas') :
        charm.createScalarAttribute('gas', 'meanmwt')
    charm.runLocalParticleCodeGroup('AllFAMgas', setvals, None)
    
setvals = """def localparticle(p):
    import meanmwt
    n = 0.
    n = meanmwt.calc_meanmwt(p.temperature, p.density)
    p.meanmwt = n
"""

