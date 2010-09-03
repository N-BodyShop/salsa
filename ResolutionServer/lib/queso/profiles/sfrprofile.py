import math, charm
from queso.globals import galcoord, virialgroup , findcenter
from queso import quesoConfig
def SFRSurfaceProfile(minRadius=.1, numBins=50,tstep='00512',nbins=500):
    """Returns the list of tuples (radius, SFR, diskSFR, bulgeSFR) in solar masses/year! (for the 0.5Gyr preceding each time)"""
    #read in initial masses from file
    filename = quesoConfig.dataDir + quesoConfig.preTimestepFilename + tstep + ".massform"
    charm.readTipsyArray(filename,'initialMass')
    
    virialRadius = virialgroup.getVirialGroup()
    rgal = quesoConfig.rgalFraction*virialRadius
    center = findcenter.findCenter()       
    radialStep  = ((math.log10(rgal)-math.log10(minRadius))/(float(numBins)-1))
    simTime = getSimTime(timestep=tstep)/quesoConfig.timeunit #time in simulation units
    sfr = [[]]*numBins
    #initialize relevant attributes
    galcoord.calcGalacticCoordinates()
    
    #create sphere of stars
    charm.createGroupAttributeSphere('galStar', 'All', 'position', center[0], center[1], center[2], rgal) 
    charm.createGroup_Family('galStar', 'galStar', 'star')
    formHigh = simTime
    formLow  = formHigh-quesoConfig.sfrLookback/quesoConfig.timeunit
    param = (formLow, formHigh, radialStep, quesoConfig.kpcunit*charm.getTime(), minRadius)
    reduceResult = charm.reduceParticle('galStar' , mapSFRProfile, reduceSFRProfile, param)
    print reduceResult
    print (formLow,formHigh)
    maxbin=0
    
    for i in range(0,len(reduceResult)):
        if (reduceResult[i][0]>maxbin):maxbin=reduceResult[i][0]
    for i in range(0,len(reduceResult)):
        radiusOut = math.pow(10, radialStep*reduceResult[i][0])
        radiusInside = math.pow(10, radialStep*(reduceResult[i][0]-1))
        radius = (radiusOut+radiusInside)/2
        area  = math.pi*(radiusOut**2-radiusInside**2)
        formationRate      = reduceResult[i][1]*quesoConfig.msolunit/quesoConfig.sfrLookback/area #msolar/yr/kpc^2
        bin = reduceResult[i][0]+numBins-maxbin-1
        sfr[bin]=(radius, formationRate)
        print sfr[bin]
    for i in range(0,numBins):
        if (sfr[i] == []):
            radiusOut = math.pow(10, radialStep*(i+1))
            radiusInside = math.pow(10, radialStep*i)
            radius = (radiusOut+radiusInside)/2
            sfr[i]=(radius,0,0,0) 
    sfrData = [[],[]]        
    for i in range(len(sfr)):
        sfrData[0].append(sfr[i][0])
        sfrData[1].append(sfr[i][1])
    return sfrData
def getSFRTimeProfile(nbins=500):
    """Returns the list of tuples (time, SFR) in solar masses/year! (for the 0.5Gyr preceding each time)"""
    #read in initial masses from file
    filename = quesoConfig.dataDir + quesoConfig.preTimestepFilename + quesoConfig.nTimesteps + ".massform"
    charm.readTipsyArray(filename,'initialMass')
    
    simTime = getSimTime()/quesoConfig.timeunit #time in simulation units
    profileTStep = simTime/nbins
    sfr = [(0,0)]*nbins
    center = findcenter.findCenter()
    rgal = virialgroup.getVirialGroup()*quesoConfig.rgalFraction
    charm.createGroupAttributeSphere('galStar', 'star', 'position', center[0], center[1], center[2], rgal) #Particles within galactic radius
    charm.createGroup_Family('galStar', 'galStar', 'star')
    param = (profileTStep, nbins)
    reduceResult = charm.reduceParticle('galStar' , mapSFR, reduceSFR, param) 
    #sum = 0.0
    for i in range(0,len(reduceResult)):
        #try:
        formationRate      = reduceResult[i][1]*quesoConfig.msolunit/(profileTStep*quesoConfig.timeunit) #msolar/yr
        age = (simTime -reduceResult[i][0]*profileTStep)*quesoConfig.timeunit/1e9 #in gyr
        sfr[reduceResult[i][0]-1] =(age, formationRate)
    return sfr

def getSimTime(timestep='00512'):
    '''Returns the time of the the simulation in years.  By default it returns the time of the simulations'''
    fileHandle = open ( quesoConfig.dataDir + quesoConfig.preTimestepFilename + 'log',"r" )
    lineList = fileHandle.readlines()
    fileHandle.close()
    for n in range(0,len(lineList)):
        if (lineList[n][0] != '#'):
            for i in range(0,len(lineList[0])):
                if (lineList[n][i]==' '):
                    initTime = float(lineList[n][0:i])*quesoConfig.timeunit
                    break
            break
    for i in range(0,len(lineList[-1])):
        if (lineList[-1][i]==' '): 
            endTime = float(lineList[-1][0:i])*quesoConfig.timeunit
            break
    print 'sim time' + str((initTime,endTime,int(quesoConfig.nTimesteps)))
    simTime = initTime + (endTime-initTime)/int(quesoConfig.nTimesteps)*float(timestep)
    return simTime


mapSFRProfile             = """import math
def localparticle(p):
    tLow, tHigh, radialStep, kpcConvert, minRad = p._param    
    #find radius in cylindrical coords
    radius = math.sqrt(p.galPosition[0]**2+p.galPosition[1]**2)*kpcConvert
    #find bin number in logarithmically spaced bins
    if (radius > minRad):
        bin = int(math.ceil(math.log10(radius)/radialStep))
    else: 
        bin = 0
    formMass = 0.0
    formTime = p.formationtime
    if (tLow < formTime) & (formTime < tHigh):
        formMass += p.initialMass
    return (bin, formMass)
"""
reduceSFRProfile          = """def localparticle(p):
    bin = p.list[0][0]
    formMass = 0.0
    for i in range(0,len(p.list)):
        formMass += p.list[i][1]
    return (bin, formMass)
"""

mapSFR                    = """import math
def localparticle(p):
    profileTStep, numBins = p._param    
    formMass = 0.0
    formTime = p.formationtime
    bin = int(math.ceil(formTime/profileTStep))
    if (bin >= numBins): bin = numBins 
    formMass += p.initialMass
    return (bin, formMass)
"""
reduceSFR                 = """def localparticle(p):
    bin = p.list[0][0]
    formMass = 0.0
    for i in range(0,len(p.list)):
        formMass += p.list[i][1]
    return (bin, formMass)
"""

