import spline
class star_lum:
    "store stellar luminosity function"
    age      = [  .01,   .02,   .05,   .1,   .2,   .5,   1.,   2.,   5.,  10.,  20.]
    lum      = [.0635, .0719, .0454, .153, .293, .436, .636, .898, 1.39, 2.54, 5.05]
    vv_dat   = [ -.01,  -.02,  -.01,   0., -.01, -.01, -.01,   0.,   0.,  .01,  .01]
    bv_dat   = [ -.02,  -.33,    0.,  .19,  .04,   .1,  .18,  .31,  .42,  .75,  .84]
    uv_dat   = [  .16,  -.12,   .84,  .97,  .83, 1.08, 1.24, 1.39, 1.51, 2.22, 2.50]
    uuv_dat  = [ -.32,  -.21,  1.68, 1.67, 1.75, 2.62, 3.50, 4.70, 6.30, 8.86, 9.97]
    n = 11
    lumv_fit = [None] * n
    vv_fit   = [None] * n
    bv_fit   = [None] * n
    uv_fit   = [None] * n
    uuv_fit  = [None] * n
    def __init__(self) :
        spline.splinit( self.age, self.lum,     self.lumv_fit, self.n, 0., 0. )
        spline.splinit( self.age, self.vv_dat,  self.vv_fit,   self.n, 0., 0. )
        spline.splinit( self.age, self.bv_dat,  self.bv_fit,   self.n, 0., 0. )
        spline.splinit( self.age, self.uv_dat,  self.uv_fit,   self.n, 0., 0. )
        spline.splinit( self.age, self.uuv_dat, self.uuv_fit,  self.n, 0., 0. )
    
