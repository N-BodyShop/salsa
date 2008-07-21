/** @file Reductions.h
 */

#include "Reductions.decl.h"
#include "PyObjectMarshal.h"

class PythonObjectLocal: public PythonObject
{
 public:
    int execute(PythonExecute *pyMsg);
    };

class PythonReducer: public PythonObjectLocal
{
 private:
    PyObject *listReduce;
    PyObject *objGlobals;
    int iCurrent;
    PyObject *getSubList();

 public:
    PyObject *listResult;
    PythonReducer(PyObject *list, PyObject *globals) {
	listReduce = list; objGlobals = globals;
	listResult = NULL;
	};
    
    int buildIterator(PyObject*, void*);
    int nextIteratorUpdate(PyObject*, PyObject*, void*);
    virtual ~PythonReducer() {} ;
    };

extern CkReduction::reducerType mergeStatistics;

extern CkReduction::reducerType growOrientedBox_float;
extern CkReduction::reducerType growOrientedBox_double;

extern CkReduction::reducerType sum_int64;

extern CkReduction::reducerType minmax_int;
extern CkReduction::reducerType minmax_float;
extern CkReduction::reducerType minmax_double;

extern CkReduction::reducerType pairByteDoubleMin;
extern CkReduction::reducerType pairByteDoubleMax;
extern CkReduction::reducerType pairDoubleDoubleMin;
extern CkReduction::reducerType pairDoubleDoubleMax;

extern CkReduction::reducerType pairFloatFloatSum;
extern CkReduction::reducerType pairDoubleDoubleSum;
extern CkReduction::reducerType pairDoubleVector3DSum;
extern CkReduction::reducerType pairDoubleVector3DMin;
extern CkReduction::reducerType pythonReduction;
