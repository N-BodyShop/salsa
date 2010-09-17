import csv, math


#===============================================================================
# Constants
#===============================================================================
# These can be found in the output .param file
kpcunit             = 136986.30137
msolunit            = 3.80129293151e+17
timeunit            = 3.88e10 #in years
velocityunit        = 3454.94#km/s
fHubble0            = math.sqrt(8*math.pi/3)
omega0              = 0.25
#===============================================================================
# Definitions
#===============================================================================
virialRadiusDensity = 200 # Virial radius overdensity
rgalFraction        = 0.1 # fraction of the virial radius to use for galactic radius
coldGasTemp         = 2e5 # Temperature cutoff for cold gas to hot gas
#============================================================================== 
# Only needed for SFR Profile functions
#============================================================================== 
sfrLookback         = 0.5e9 #in yr
nTimesteps          = '00512'  # Number of last simulation output (used for sfr profile and assumes run till 13.59gyr)
dataDir             = '/astro/net/nbody1/ecarlso2/aq-c-5-sph/'
preTimestepFilename = 'aq-c-5-sph.' #including the period