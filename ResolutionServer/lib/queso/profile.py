from queso.profiles import metalprof,sfrprofile

def metalProfile(group,nbins=20,cent='pot', fam='bar'):
    """
    Returns a 2 x nbins tuple (radius, metal fraction) for a given
    group.  cent can be 'pot' or 'com'. fam can be 'bar' for baryons,
    'star', or 'gas'.  Binning is linear and goes to the group radius
    """
    return metalprof.metalProfile(group=group,nbins=nbins,center=cent, family=fam)
def sfrSurfaceProfile(minRadius=.1, numBins=50,tstep='00512'):
    """
    Computes the cylindrical star formation rate surface profile (in
    galactic coordinates).Returns a 2 x nbin array of logarithmically
    spaced bins from min radius to r200*quesoConfig.rgalFraction.
    This requires some special configuration parameters in
    quesoConfig to work.  Units are msol/(yr*kpc^2)
    """
    return sfrprofile.SFRSurfaceProfile(minRadius=minRadius, numBins=numBins,tstep=tstep)
def getSFH(nbins=500):
    """
    Computes the star formation history in msol/yr.  Return is a 2 x
    nbins array of (stellar age,formation rate)  This assumes that
    the last output is loaded (as defined in quesoConfig.nTimesteps).
    The star formation section of quesoConfig needs to be configured
    for this to work.
    """
    return sfrprofile.getSFRTimeProfile(nbins=500)
