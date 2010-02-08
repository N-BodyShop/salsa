import traceback, math

# simulation constants
msolunit  = 1.e12          # mass scale
kpcunit   = 1.             # distance scale
fhydrogen = 0.76           # fraction of gas as hydrogen 

# physical constants
KPCCM     = 3.085678e21    # kiloparsec in centimeters
GCGS      = 6.67e-8        # G in cgs
MSOLG     = 1.99e33        # solar mass in grams
GYRSEC    = 3.155693e16    # gigayear in seconds
MHYDR     = 1.67e-24       # mass of hydrogen atom in grams
KBOLTZ    = 1.381e-16      # bolzman constant in cgs
GAMMA     = (5.0/3.0)      # gamma of ideal gas
SIGMAES   = 6.665e-25      # Thomson cross section
ME        = 9.11e-28       # electron mass in grams
C         = 2.998e10       # speed of light in cm/sec

# system constants

HUGE      = 1e308          # a really big number

# derived constants
time_unit = math.sqrt(pow(kpcunit*KPCCM, 3.) / (GCGS*msolunit*MSOLG)) / GYRSEC
gasconst  = kpcunit*KPCCM*KBOLTZ/MHYDR/GCGS/msolunit/MSOLG

