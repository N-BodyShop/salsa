class data:
    element1 = ""
    element2 = ""
    element3 = ""
    element4 = ""
    ra = []
    dec = []
    z = []
    mag = []
    def __init__():
        ra = []
        dec = []
        z = []
        mag = []

def main(argv):
    "Parse command line and do it..."
    import os, getopt, string
    import urllib

    url='http://cas.sdss.org/astro/en/tools/search/x_sql.asp'
    fmt='csv'
    qry = "SELECT TOP 52 Galaxy.dec, Galaxy.ra, Galaxy.z, Galaxy.u FROM specobj, Galaxy WHERE specobj.bestobjid=Galaxy.objid and specobj.zconf>.95 and 300000*specobj.z/70 < 500 and Galaxy.modelmag_r < 17.77"
    writefirst = 1
    verbose = 0

    params = urllib.urlencode({'cmd': qry, 'format': fmt})
    file = urllib.urlopen(url+'?%s' % params)
    line = file.readline()
    clean = line.split('\n')[0]
    position = clean.split(',')    
    data.element1 = position[0]
    data.element2 = position[1]
    data.element3 = position[2]
    data.element4 = position[3]

    if line.startswith("ERROR"): # SQL Statement Error -> stderr
            ofp = sys.stderr

    while line:
        #ofp.write(string.rstrip(line)+os.linesep)
        line = file.readline()
        clean = line.split('\n')[0]
        position = clean.split(',')
#        print position
        if position[0] != '':
            if position[1] != '':
                data.dec.append(float(position[0]))
                data.ra.append(float(position[1]))
                data.z.append(float(position[2]))
                data.mag.append(float(position[3]))
#    print data.dec
#    print data.ra
#    print data.z
#    print data.mag
    return data
