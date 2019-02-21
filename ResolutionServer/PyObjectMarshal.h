#ifndef PYOBJECTMARSHAL_INCLUDED
#define PYOBJECTMARSHAL_INCLUDED
#include "PythonCCS.h"

#include "marshal.h"

// Class just to handle marshalling of Python Objects
class PyObjectMarshal {
 private:
    char *buf;
    int nBytes;
    // No copies of this class because of the character pointer
    PyObjectMarshal(const PyObjectMarshal &);
    PyObjectMarshal& operator=(const PyObjectMarshal &);
 public:
    // PyObject *obj;
    PyObjectMarshal() {
	nBytes = 0; buf = NULL;
	}
    PyObjectMarshal(PyObject *obj);
    ~PyObjectMarshal() { if(buf != NULL) delete [] buf; }
    PyObject *getObj() const;
    friend void operator|(PUP::er& p, PyObjectMarshal& objM);
    };

inline PyObjectMarshal::PyObjectMarshal(PyObject *obj) {
    CkAssert(Py_IsInitialized());

    PyObject *pickle = PyImport_ImportModule("cPickle");
    PyObject *proto = PyObject_GetAttrString(pickle, "HIGHEST_PROTOCOL");
    
    PyObject *dumps = PyObject_GetAttrString(pickle, "dumps");
    PyObject *pyStr = PyObject_CallFunctionObjArgs(dumps, obj, proto, NULL);
    if(pyStr != NULL) {
	nBytes = PyString_Size(pyStr);
	buf = new char[nBytes];
	memcpy(buf, PyString_AsString(pyStr), nBytes);
	Py_DECREF(pyStr);
	}
    else {
	PyErr_Print();
	nBytes = 0;
	buf = NULL;
	}
    Py_DECREF(dumps);
    Py_DECREF(proto);
    Py_DECREF(pickle);
    PyErr_Clear();
}

inline PyObject *PyObjectMarshal::getObj() const {
    PyObject *obj;
    
    CkAssert(Py_IsInitialized());

    PyObject *pickle = PyImport_ImportModule("cPickle");
    PyObject *proto = PyObject_GetAttrString(pickle, "HIGHEST_PROTOCOL");
    PyObject *loads = PyObject_GetAttrString(pickle, "loads");
    PyObject *pyStr = PyString_FromStringAndSize(buf, nBytes);
    obj = PyObject_CallFunctionObjArgs(loads, pyStr, NULL);
    if(pyStr != NULL) {
	Py_DECREF(pyStr);
	}
    else {
	PyErr_Print();
	}
    if(PyErr_Occurred() != NULL) {
	PyErr_Print();
	obj = Py_None;
	}
    Py_DECREF(loads);
    Py_DECREF(proto);
    Py_DECREF(pickle);
    PyErr_Clear();
    return obj;
}

inline void operator|(PUP::er& p, PyObjectMarshal& objM) {
    p|objM.nBytes;
    if(p.isUnpacking())
	objM.buf = new char[objM.nBytes];
    p(objM.buf, objM.nBytes);
    }

#endif
