# The main import module for the Python implementation of TIPSY.
#
# Performing "import tipsy" will place the TIPSY commands in the
# Python symbol table under "tipsy" so that they can be called
# as "tipsy.function()" for example "tipsy.profile(...)"

from tprofile import profile
from trot_cur import rotcur
from trot_cur import rotationcurve
from tshortfns import boxstat
from tshortfns import setsphere
from tshortfns import setbox
from tshortfns import markgal
from tshortfns import markbox

