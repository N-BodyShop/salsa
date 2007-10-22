#ifndef PYOBJECTMARSHAL_INCLUDED
#define PYOBJECTMARSHAL_INCLUDED
#include "PythonCCS.h"

#include "marshal.h"

// Class just to handle marshalling of Python Objects
class PyObjectMarshal {
 public:
    PyObject *obj;
    PyObjectMarshal(PyObject *_obj) {obj = _obj;}
    PyObjectMarshal() {}
    };

inline void operator|(PUP::er& p, PyObjectMarshal& objM) {
    char *buf;
    int nBytes;
    PyObject *pyStr;
    
    if(!p.isUnpacking()) {
#if PY_MINOR_VERSION > 3
	pyStr = PyMarshal_WriteObjectToString(objM.obj, Py_MARSHAL_VERSION);
#else
	pyStr = PyMarshal_WriteObjectToString(objM.obj);
#endif
	if(pyStr != NULL) {
	    buf = PyString_AsString(pyStr);
	    nBytes = PyString_Size(pyStr);
	    }
	else {
	    PyErr_Print();
	    nBytes = 0;
	    }
	}
    p|nBytes;
    if(p.isUnpacking())
	buf = new char[nBytes];
    p(buf, nBytes);
    
    if(p.isUnpacking()) {
	objM.obj = PyMarshal_ReadObjectFromString(buf, nBytes);
	delete [] buf;
	}
    else {
	Py_DECREF(pyStr);
	}
    }

#endif
