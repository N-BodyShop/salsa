Queso Analysis Package for Salsa
Original Author: Eric Carlson (ecarlso2@uoregon.edu)
Originally Written: Aug. 26, 2010

Introduction: Queso is modified from the needs of the Aquila code comparison project.  It has not been rigourously tested, but nonetheless should provide useful analysis tools and example code for those beginning to work with salsa.  In it's current incarnation, this toolset is not optimized for speed, but was rewritten for dominantly standalone usage.  If efficiency becomes a concern, it may be better to rewrite the routines in a way that is less conducive to repeated calculations.

Usage: Typing 'from queso import *' from the taco command line should import all the wrapper modules (which then interface with the packages.  These can then be used by simply using the package name and method (e.g. 'glob.virialGroup()').  Help is found by typing help(<pkg name>) which has docstrings for all of the methods.

Packages:  There are four packages in queso and a configuration file.  Further documentation on specific functions (and a complete list) should be found with python's built-in help function as it will contain the most up to date information.
   -quesoConfig:This is the configuration file, which should be checked any time a
                new simulation is being analyzed.
   -vectormath: Provides basic vector operations in salsa.
   -glob:       Provides functions related to global quantities (as opposed to
                profiles).  
                Currently implemented methods include:  
                  -Finding a group center via center of mass or potential min
                  -Determine the virialRadius and create a virial sphere group
                  -Get the specific angular momentum vector of a group and 
                   the first associated Euler angles.
                  -Transform position and velocity into a galactic coordinate
                   system aligned with an arbitrary groups angular momentum vector.
                  -Find the stellar half-mass-radius of a group.
                  -Find the mean mass weighted metallicity of a group
                  -Find the moment of inertia tensor of a group
                  -Calculate the distribution of jz/jcirc for a group
    -profile:  Provides some additional profiling capabilities that are 
               not included in the tipsy profile package.
               Currently implemented methods include:  
                 -Compute cylindrical SFR profile aligned with the galaxy.
                 -Compute star formation history (age vs formation rate)
                 -Compute spherical metallicity profile.
    -export:   Provides functionality to export salsa groups to tipsy-ascii files
               Currently implemented methods include:  
                 -Write group to tipsy ascii file
                 -Export a cube of specified radius that is aligned with
                  the galactic coordinate system (for face-on/edge-on projections
                  etc...).  Can be used in conjunction with tipsy's 'vista' 
                  command to generate density projections.  For tipsy 
                  automation from within salsa, see additional info section.

Additional information:  The Aquila project code is not nearly as general purpose, nor well documented outside of the docstrings, but it provides a very high level of analysis automation (basically one command for all the analysis of all the simulation outputs), as well as automatically integrating pylab plot generation and tipsy density and temperature projections (into .fits images)  There also exist idl scripts which generate a pdf that combines many of these quantities.  See README.txt in ~ecarlso2/Aquila/ for more information.
