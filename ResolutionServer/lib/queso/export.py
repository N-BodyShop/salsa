from queso.exports import writeascii,writegalascii

def writeAscii(group, fname):
    """
    Writes out a tipsy-ascii file for the group to ascii file fname.
    """
    writeascii.writeGroupAscii(group=group, filename=fname)
def writeGalAscii(boxRadius, fname, angMomGroup=None, centerMethod ='pot'):
    """
    Writes out an tipsy-ascii (fname) representation for a box
    aligned with the galactic coordinate system spanning +-boxRadius
    (given in kpc) in the global.galcoord() directions. center can be
    'pot' or 'com'.  Default angMomGroup is cold galactic gas.
    """
    writegalascii.writeBoxAscii(boxRadius=boxRadius, filename=fname, angMomGroup=None, centerMethod ='pot')