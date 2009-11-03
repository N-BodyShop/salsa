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
    char *buf = NULL;
    int nBytes;
    PyObject *pyStr = NULL;
    
    CkAssert(Py_IsInitialized());

    PyGILState_STATE state = PyGILState_Ensure();
    
    PyObject *pickle = PyImport_ImportModule("cPickle");
    PyObject *proto = PyObject_GetAttrString(pickle, "HIGHEST_PROTOCOL");
    
    if(!p.isUnpacking()) {
	PyObject *dumps = PyObject_GetAttrString(pickle, "dumps");
	pyStr = PyObject_CallFunctionObjArgs(dumps, objM.obj, proto, NULL);
#if 0
#if PY_MINOR_VERSION > 3
	pyStr = PyMarshal_WriteObjectToString(objM.obj, Py_MARSHAL_VERSION);
#else
	pyStr = PyMarshal_WriteObjectToString(objM.obj);
#endif
#endif
	if(pyStr != NULL) {
	    buf = PyString_AsString(pyStr);
	    nBytes = PyString_Size(pyStr);
	    }
	else {
	    PyErr_Print();
	    nBytes = 0;
	    }
	Py_DECREF(dumps);
	}
    p|nBytes;
    if(p.isUnpacking())
	buf = new char[nBytes];
    p(buf, nBytes);
    
    if(p.isUnpacking()) {
	PyObject *loads = PyObject_GetAttrString(pickle, "loads");
	pyStr = PyString_FromStringAndSize(buf, nBytes);
	objM.obj = PyObject_CallFunctionObjArgs(loads, pyStr, NULL);
	if(PyErr_Occurred() != NULL) {
	    PyErr_Print();
	    objM.obj = Py_None;
	    }
	// objM.obj = PyMarshal_ReadObjectFromString(buf, nBytes);
	delete [] buf;
	Py_DECREF(loads);
	}
    else {
	if(pyStr != NULL) {
	    Py_DECREF(pyStr);
	    }
	else {
	    PyErr_Print();
	    nBytes = 0;
	    }
	}
    Py_DECREF(proto);
    Py_DECREF(pickle);
    PyErr_Clear();
    PyGILState_Release(state);
    }

#endif
