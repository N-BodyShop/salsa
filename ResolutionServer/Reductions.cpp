/** @file Reductions.cpp
 */
 
#include <utility>

#include "ParticleStatistics.h"
#include "OrientedBox.h"
#include "Reductions.h"
#include "PyObjectMarshal.h"

CkReduction::reducerType mergeStatistics;

CkReduction::reducerType growOrientedBox_float;
CkReduction::reducerType growOrientedBox_double;

CkReduction::reducerType sum_int64;

CkReduction::reducerType minmax_int;
CkReduction::reducerType minmax_float;
CkReduction::reducerType minmax_double;

CkReduction::reducerType pairByteDoubleMin;
CkReduction::reducerType pairByteDoubleMax;
CkReduction::reducerType pairDoubleDoubleMin;
CkReduction::reducerType pairDoubleDoubleMax;

CkReduction::reducerType pairFloatFloatSum;
CkReduction::reducerType pairDoubleDoubleSum;
CkReduction::reducerType pairDoubleVector3DSum;
CkReduction::reducerType pairDoubleVector3DMin;

CkReduction::reducerType pythonReduction;

using namespace std;

typedef unsigned char byte;

/// Combine statistics about a collection of particles
CkReductionMsg* mergeParticleStats(int nMsg, CkReductionMsg** msgs) {
	//what if one box is uninitialized?
	GroupStatistics* stats = static_cast<GroupStatistics *>(msgs[0]->getData());
	GroupStatistics* otherStats;
	for(int i = 1; i < nMsg; ++i) {
		otherStats = static_cast<GroupStatistics *>(msgs[i]->getData());
		stats->numParticles += otherStats->numParticles;
		if(otherStats->boundingBox.initialized()) {
			stats->boundingBox.grow(otherStats->boundingBox.lesser_corner);
			stats->boundingBox.grow(otherStats->boundingBox.greater_corner);
		}
	}
	return CkReductionMsg::buildNew(sizeof(GroupStatistics), stats);
}

/// Combine reduction messages to grow a box
template <typename T>
CkReductionMsg* boxGrowth(int nMsg, CkReductionMsg** msgs) {
	OrientedBox<T>* pbox = static_cast<OrientedBox<T> *>(msgs[0]->getData());
	OrientedBox<T>* msgpbox;
	for(int i = 1; i < nMsg; i++) {
		msgpbox = static_cast<OrientedBox<T> *>(msgs[i]->getData());
		if(msgpbox->initialized()) {
			pbox->grow(msgpbox->lesser_corner);
			pbox->grow(msgpbox->greater_corner);
		}
	}
	
	return CkReductionMsg::buildNew(sizeof(OrientedBox<T>), pbox);
}

CkReductionMsg *sum_int64_reducer(int nMsg,CkReductionMsg **msg)
{
  int i;
  int64_t ret=0;
  for (i=0;i<nMsg;i++)
    ret+=*(int64_t *)(msg[i]->getData());
  return CkReductionMsg::buildNew(sizeof(int64_t),(void *)&ret);
}

/// Combine reduction messages to get min/max pair
template <typename T>
CkReductionMsg* minmax(int nMsg, CkReductionMsg** msgs) {
	T* pminmax = static_cast<T *>(msgs[0]->getData());
	T* msgpminmax;
	for(int i = 1; i < nMsg; i++) {
		msgpminmax = static_cast<T *>(msgs[i]->getData());
		if(msgpminmax[0] < pminmax[0])
			pminmax[0] = msgpminmax[0];
		if(msgpminmax[1] > pminmax[1])
			pminmax[1] = msgpminmax[1];
	}
	
	return CkReductionMsg::buildNew(2 * sizeof(T), pminmax);
}

template <typename T, typename U>
CkReductionMsg* pairMin(int nMsg, CkReductionMsg** msgs) {
	pair<T, U>* ppair = static_cast<pair<T, U> *>(msgs[0]->getData());
	pair<T, U>* msgppair;
	for(int i = 1; i < nMsg; i++) {
		msgppair = static_cast<pair<T, U> *>(msgs[i]->getData());
		if(msgppair->first < ppair->first)
			*ppair = *msgppair;
	}
	return CkReductionMsg::buildNew(sizeof(pair<T, U>), ppair);
}

template <typename T, typename U>
CkReductionMsg* pairMax(int nMsg, CkReductionMsg** msgs) {
	pair<T, U>* ppair = static_cast<pair<T, U> *>(msgs[0]->getData());
	pair<T, U>* msgppair;
	for(int i = 1; i < nMsg; i++) {
		msgppair = static_cast<pair<T, U> *>(msgs[i]->getData());
		if(*ppair < *msgppair)
			*ppair = *msgppair;
	}
	return CkReductionMsg::buildNew(sizeof(pair<T, U>), ppair);
}

template <typename T, typename U>
CkReductionMsg* pairSum(int nMsg, CkReductionMsg** msgs) {
	pair<T, U>* ppair = static_cast<pair<T, U> *>(msgs[0]->getData());
	pair<T, U>* msgppair;
	for(int i = 1; i < nMsg; i++) {
		msgppair = static_cast<pair<T, U> *>(msgs[i]->getData());
		ppair->first += msgppair->first;
		ppair->second += msgppair->second;
	}
	return CkReductionMsg::buildNew(sizeof(pair<T, U>), ppair);
}

//
// Helper to look for non-unique keys.
// Also copies unique keys over to the result list.
//
PyObject *
PythonReducer::getSubList()
{
    PyObject *listSub = PyList_New(0);
    while(iCurrent < PyList_Size(listReduce)) {
	// Get new sub-list of equivalent keys
	PyList_Append(listSub, PySequence_GetItem(listReduce, iCurrent));
	PyObject *objKey = PyTuple_GetItem(PyList_GetItem(listReduce, iCurrent),
					  0);
	iCurrent++;
	while(iCurrent < PyList_Size(listReduce)
	      && PyObject_Compare(objKey, PyTuple_GetItem(PyList_GetItem(listReduce, iCurrent), 0)) == 0) {
	    PyList_Append(listSub, PySequence_GetItem(listReduce, iCurrent));
	    iCurrent++;
	    }
	// if this list is of length 1, append to result list, and
	// start again
	if(PyList_Size(listSub) == 1) {
	    PyList_Append(listResult, PySequence_GetItem(listSub, 0));
	    listSub = PyList_New(0);
	    }
	else {
	    break;
	    }
	}
    return listSub;
    }

int
PythonReducer::buildIterator(PyObject*& arg, void* iter) {
    // Sort list
    PyList_Sort(listReduce);
    iCurrent = 0;

    // initialize result list
    listResult = PyList_New(0);

    PyObject *listSub = getSubList();
    // return 0 if we've got to the end
    if(PyList_Size(listSub) == 0)
	return 0;
    // Build iterator argument with globals and list

    if(objGlobals != NULL) {
	PyObject_SetAttrString(arg, "_param", objGlobals);
	}
    PyObject_SetAttrString(arg, "list", listSub);
    return 1;
    }

int PythonReducer::nextIteratorUpdate(PyObject*& arg, PyObject* result,
				      void* iter) {
    // Append this result to result list
    PyList_Append(listResult, result);
    Py_INCREF(result);

    PyObject *listSub = getSubList();

    if(PyList_Size(listSub) == 0) // reached the end
	return 0;
    
    // Build new iterator argument with globals and list
    PyObject_SetAttrString(arg, "list", listSub);
    return 1;
    }

// Perform Particle Reduction.
// The expected messages are python tuples of the form
// (string, Global, List)
// where <string> is code, Global is a global parameter object, and
// List is a list of tuples of the form (key, value1, value2, ...)
// Tuples with common keys will be reduced using the given code.

CkReductionMsg* pythonReduce(int nMsg, CkReductionMsg** msgs) {
    PyObjectMarshal tupleFirst;
    PUP::fromMemBuf(tupleFirst, msgs[0]->getData(), msgs[0]->getSize());

    PyObject *listReduce = PySequence_GetItem(tupleFirst.obj, 2);
    
    for(int i = 1; i < nMsg; ++i) {
	PyObjectMarshal tupleNext;
	PUP::fromMemBuf(tupleNext, msgs[i]->getData(), msgs[i]->getSize());
	// Concatenate to first list
	listReduce = PySequence_InPlaceConcat(listReduce,
					      PySequence_GetItem(tupleNext.obj, 2));
	Py_DECREF(tupleNext.obj);
	}
    // Reduce the local list using code and globals
    // code is first item
    char *sReduceCode = PyString_AsString(PyTuple_GetItem(tupleFirst.obj, 0));
    // globals is second
    PyObject *global = PySequence_GetItem(tupleFirst.obj, 1);
    PythonReducer reducer(listReduce, global);
    PythonIterator info;
    PythonExecute wrapperreduce(sReduceCode, "localparticle", &info);

    int interp = reducer.execute(&wrapperreduce);

    if(reducer.listResult == NULL)
	reducer.listResult = PyList_New(0);
    // Send the resulting list onward
    PyObject *result = Py_BuildValue("(sOO)", sReduceCode, global,
				    reducer.listResult);
    
    PyObjectMarshal resultMarshal(result);
    int nBuf = PUP::size(resultMarshal);
    char *buf = new char[nBuf];
    PUP::toMemBuf(resultMarshal, buf, nBuf);

    Py_DECREF(tupleFirst.obj);

    CkReductionMsg *msgRed = CkReductionMsg::buildNew(nBuf, buf);
    delete [] buf;
    
    return msgRed;
    }

void registerReductions() {
	mergeStatistics = CkReduction::addReducer(mergeParticleStats);
	
	growOrientedBox_float = CkReduction::addReducer(boxGrowth<float>);
	growOrientedBox_double = CkReduction::addReducer(boxGrowth<double>);
	
	sum_int64 = CkReduction::addReducer(sum_int64_reducer);

	minmax_int = CkReduction::addReducer(minmax<int>);
	minmax_float = CkReduction::addReducer(minmax<float>);
	minmax_double = CkReduction::addReducer(minmax<double>);
	
	pairByteDoubleMin = CkReduction::addReducer(pairMin<byte, double>);
	pairByteDoubleMax = CkReduction::addReducer(pairMax<byte, double>);
	pairDoubleDoubleMin = CkReduction::addReducer(pairMin<double, double>);
	pairDoubleDoubleMax = CkReduction::addReducer(pairMax<double, double>);
	
	pairFloatFloatSum = CkReduction::addReducer(pairSum<float, float>);
	pairDoubleDoubleSum = CkReduction::addReducer(pairSum<double, double>);
	pairDoubleVector3DSum = CkReduction::addReducer(pairSum<double, Vector3D<double> >);
	pairDoubleVector3DMin = CkReduction::addReducer(pairMin<double, Vector3D<double> >);
	pythonReduction = CkReduction::addReducer(pythonReduce);
}

// Implementation of PythonObject::execute() that doesn't deal with
// CCS messages.  See the original implementation in pythonCCS.C for
// a comparision

// First a function that does nothing: we don't want a reply to be
// sent.

void replyNone(PythonObject *obj, CcsDelayedReply *reply, CmiUInt4 *value) {
    }

int PythonObjectLocal::execute (PythonExecute *pyMsg) {

  CmiUInt4 pyReference = prepareInterpreter(pyMsg);
  replyIntFn = &replyNone;	// set up a null reply

  // run the program
  executeThread(pyMsg);
  
  return pyReference;
}
#include "Reductions.def.h"
