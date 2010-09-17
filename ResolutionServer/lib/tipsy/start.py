import charm, load_meanmwt

charm.loadSimulation('run99.std')
print 'Simulation run99.std was loaded.'
load_meanmwt.do_meanmwt()
print 'Particle mean molecular weight has been calculated.

