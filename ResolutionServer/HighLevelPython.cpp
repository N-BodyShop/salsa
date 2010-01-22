#include <inttypes.h>
#include "config.h"

#include <iostream>
#include <string>

#include "ResolutionServer.h"
using namespace std;
using namespace SimulationHandling;

void Main::loadSimulation(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *fileName;
    FILE *fp;
    if(PyArg_ParseTuple(arg, "s", &fileName) == false) {
	pythonReturn(handle, NULL);
	return;
	}
	
    simListType::iterator chosen = simulationList.find(fileName);
    if(chosen != simulationList.end()) {
	workers.loadSimulation(chosen->second, CkCallbackResumeThread());
	}
    else if((fp = fopen(fileName, "r")) != NULL) { // Check if file exists
	fclose(fp);
	workers.loadSimulation(fileName, CkCallbackResumeThread());
	}
    else {
	PyErr_SetString(PyExc_NameError, "No such file");
	}
    pythonReturn(handle);
    }

// usage: charm.readTipsyArray('filename', 'attribute'), where
// attribute is the attribute to which the array values will be assigned

void Main::readTipsyArray(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    Worker* w = this->workers[0].ckLocal();

    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle);
	return;
	}
    
    char *fileName;
    char *attributeName;
    FILE *fp;

    if(PyArg_ParseTuple(arg, "ss", &fileName, &attributeName) == false) {
	PyErr_SetString(PyExc_TypeError, "Usage: readTipsyArray(file, attribute)");
	pythonReturn(handle, NULL);
	return;
	}
    if((fp = fopen(fileName, "r")) != NULL) { // Check if file exists
	fclose(fp);
	workers[0].readTipsyArray(fileName, attributeName, 0, 0,
				  CkCallbackResumeThread());
	}
    else {
	PyErr_SetString(PyExc_NameError, "No such file");
	pythonReturn(handle, NULL);
	return;
	}
    pythonReturn(handle);
    }

void Main::getTime(int handle) {
    Worker* w = this->workers[0].ckLocal();

    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    pythonReturn(handle,Py_BuildValue("d", w->sim->time));
    }

void Main::getFamilies(int handle) {
    Worker* w = this->workers[0].ckLocal();
    PyObject *lFamily = PyList_New(0);

    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    for(Simulation::iterator iter = w->sim->begin(); iter != w->sim->end(); ++iter)
	PyList_Append(lFamily, Py_BuildValue("s", iter->first.c_str()));
    pythonReturn(handle, lFamily);
    }

// usage: charm.getAttributes('family')
// returns list of attributes of the family
 
void Main::getAttributes(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *familyName;
    Worker* w = this->workers[0].ckLocal();
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    PyObject *lAttributes = PyList_New(0);
    if(PyArg_ParseTuple(arg, "s", &familyName) == false) {
	pythonReturn(handle, NULL);
	return;
	}
	
    Simulation::iterator simIter = w->sim->find(familyName);

    if(simIter != w->sim->end()) {
	for(AttributeMap::iterator attrIter = simIter->second.attributes.begin(); attrIter != simIter->second.attributes.end(); ++attrIter)
	    PyList_Append(lAttributes,
			  Py_BuildValue("s", attrIter->first.c_str()));
	}
    pythonReturn(handle, lAttributes);
    }

void Main::getGroups(int handle) {
    Worker* w = this->workers[0].ckLocal();
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}

    PyObject *lGroup = PyList_New(0);
    
    for(Worker::GroupMap::iterator iter = w->groups.begin();
	iter != w->groups.end(); ++iter)
	PyList_Append(lGroup, Py_BuildValue("s", iter->first.c_str()));
    pythonReturn(handle, lGroup);
    }

/* usage: saveSimulation('savepath') */
void Main::saveSimulation(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *path;
    int result;
    CkReductionMsg* mesg;
    SiXFormatWriter simwriter;
    
    // If you don't get a path argument, then save 
    if(PyArg_ParseTuple(arg, "s", &path)== false) {
      path = (char *) malloc(256*sizeof(char));
      path[0] = '\0';
      //      strcpy(path, w->sim->name.c_str());
    }
    // The writing is done as a domino scheme.  Call the head node
    // and it calls all the other workers in sequence.
    this->workers[0].saveSimulation(path,
			       createCallbackResumeThread(mesg, result));

    pythonReturn(handle, Py_BuildValue("i",result));
    delete mesg;
    }

/* usage: writeGroupArray('Group', 'attribute', 'savepath') */
void Main::writeGroupArray(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    Worker* w = this->workers[0].ckLocal();
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}

    char *fileName;
    char *attributeName;
    char *groupName;
    
    if(PyArg_ParseTuple(arg, "sss", &groupName, &attributeName, &fileName)
       == false) {
	pythonReturn(handle, NULL);
	return;
	}

    // The writing is done as a domino scheme.  Call the head node
    // and it calls all the other workers in sequence.
    this->workers[0].writeGroupArray(groupName, attributeName, fileName,
				     CkCallbackResumeThread());

    pythonReturn(handle);
    }

/* usage: getNumParticles('family')  XXX deprecated
   or getNumParticles('group', 'family') */
void Main::getNumParticles(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *familyName;
    char *groupName = NULL;
    Worker* w = this->workers[0].ckLocal();
    if(PyArg_ParseTuple(arg, "ss", &groupName, &familyName) == false) {
	PyErr_Clear();
	if(PyArg_ParseTuple(arg, "s", &familyName) == false) {
	    pythonReturn(handle, NULL);
	    return;
	    }
	}
    
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}

    Simulation::iterator iter = w->sim->find(familyName);
    
    if(iter == w->sim->end()) {
	PyErr_SetString(PyExc_NameError, "No such family");
	pythonReturn(handle, NULL);
	return;
    }
    if(groupName == NULL) {	// Single argument version
	pythonReturn(handle,Py_BuildValue("l",
				      iter->second.count.totalNumParticles));
	}
    else 
	{
	    Worker::GroupMap::iterator giter = w->groups.find(groupName);
	    if(giter == w->groups.end()) {
		PyErr_SetString(PyExc_NameError, "No such group");
		pythonReturn(handle, NULL);
		return;
		}
	    CkReductionMsg* mesg;
	    int64_t result;
	    
	    workers.getNumParticlesGroup(groupName, familyName,
					 createCallbackResumeThread(mesg, result));
	    pythonReturn(handle, Py_BuildValue("l", result));
	    delete mesg;
	    }
}

void Main::getAttributeRange(int handle) {
    char *familyName, *attributeName;
    PyObject *arg = PythonObject::pythonGetArg(handle);
    if(PyArg_ParseTuple(arg, "ss", &familyName, &attributeName) == false) {
	PyErr_SetString(PyExc_TypeError, "Usage: getAttributeRange(family, attribute)");
	pythonReturn(handle, NULL);
	return;
	}
    Worker* w = this->workers[0].ckLocal();
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}

    Simulation::iterator simIter = w->sim->find(familyName);

    if(simIter == w->sim->end()) {
	PyErr_SetString(PyExc_NameError, "No such family");
	pythonReturn(handle, NULL);
	return;
	}
    AttributeMap::iterator attrIter = simIter->second.attributes.find(attributeName);
    if(attrIter == simIter->second.attributes.end()) {
	PyErr_SetString(PyExc_NameError, "No such attribute");
	pythonReturn(handle, NULL);
	return;
	}
    pythonReturn(handle,Py_BuildValue("(dd)", getScalarMin(attrIter->second),
				      getScalarMax(attrIter->second)));
}


//CC 9/03
//This divides the data into equal size pieces and sends them out to the different workers 
void Main::divvyScalar (std::string const& familyName, std::string const& attributeName, int length, double c_data[]) {
  int numWorkers = 2;
  int i = 0, i1;
  int section = length/numWorkers; 
  int remainder = length%numWorkers;
  CkReductionMsg* mesg; // Used when sending stuff to Workers
  int result;
  double section0[section+1];
  double section1[section];
  int i2 = 0;
  for (i=0; i < numWorkers; i++){
    cout<<"Calling Workers"<<endl;
    if (i < remainder) {
      for (i1 = 0; i1 < section+1; i1++) {
	cout<<"First For"<<endl;
	section0[i1] = c_data[i1 + i2];
      }  
      i2 = i2 + section + 1;
      workers[i].importScalarData(familyName, attributeName, section+1, section0, createCallbackResumeThread(mesg, result));
    }
    else {
      for (i1 = 0; i1 < section; i1++) {
	cout<<"Second For"<<endl;
	section1[i1] = c_data[i1 + i2];
      }  
      i2 = i2 + section;
      workers[i].importScalarData(familyName, attributeName, section, section1, createCallbackResumeThread(mesg, result));
    }
  }
  delete mesg;
}

//CC 9/03
//This divides the data into equal size pieces and sends them out to the different workers
void Main::divvyVector(std::string const& familyName, std::string const&attributeName, int length, Vector3D<float> c_data[]) {
  int numWorkers = 2;
  //  workers[0].importVectorData(familyName, attributeName, length, c_data, createCallbackResumeThread(mesg, result));
  int i, i1;
  int section = length/numWorkers; 
  int remainder = length%numWorkers;      	
  CkReductionMsg* mesg; // Used when sending stuff to Workers
  int result;
  Vector3D<float> *section0;
  section0 = new Vector3D<float>[section+1];
  Vector3D<float> *section1;
  section1 = new Vector3D<float>[section];
  int i2 = 0;
  for (i=0; i < numWorkers; i++){
    cout<<"Calling Worker"<<i<< " (Vector)"<<endl;
    if (i < remainder) {
      cout<<i<<" (i) < "<<remainder<<" (remainder)"<<endl;
      for (i1 = 0; i1 < section+1; i1++) {
	section0[i1].x = c_data[i1 + i2].x;
	section0[i1].y = c_data[i1 + i2].y;
	section0[i1].z = c_data[i1 + i2].z;	
	cout<<"Section0, i"<<i<<", i1: "<<i1<<", i2: "<<i2<<endl;
      }  
      i2 = i2 + section + 1;
      workers[i].importVectorData(familyName, attributeName, section+1, section0, createCallbackResumeThread(mesg, result));
    }
    else {
      cout<<i<<" (i) >= "<<remainder<<" (remainder)"<<endl;
      for (i1 = 0; i1 < section; i1++) {
	section1[i1].x = c_data[i1 + i2].x;
	section1[i1].y = c_data[i1 + i2].y;
	section1[i1].z = c_data[i1 + i2].z;
	cout<<"Section1, i"<<i<<", i1: "<<i1<<", i2: "<<i2<<endl;
      }  
      i2 = i2 + section;
      workers[i].importVectorData(familyName, attributeName, section, section1, createCallbackResumeThread(mesg, result));
    }
  }
  delete mesg;
} 


//CC 6/28
void Main::importData(int handle) {
    char *familyName, *attributeName;
    int length;
    int errorCheck;
    int dummy = 0; //Tag for whether the list is 3D

  CkReductionMsg* mesg; // Used when sending stuff to Workers
  int result;

    PyObject *data, *testelement;
    PyObject *arg = PythonObject::pythonGetArg(handle);
    cout << "Importing Data\n";
    errorCheck = PyArg_ParseTuple(arg, "ssO", &familyName, &attributeName, &data); //Read in the groupName, familyName, attributeName and an object that contains the list of data elements.
    cout<< "Data is read into an object\n"<<errorCheck<<"\n";
    if(PyList_Check(data)) { //Check to see if the data sent is in a list          
      //    	CkReductionMsg* mesg; // Used when sending stuff to Workers
      //	int result;
	length = PyList_Size(data); //Find the length of the data array
	testelement = PyList_GetItem(data,0);
       	if(PyList_Check(testelement)){
	  testelement = PyList_GetItem(data,0);
	  cout<<"2D array\n";
	  cout<<PyList_Size(testelement)<<"\n";
	  if (3 == PyList_Size(testelement)) //List of 3D Vecotrs
	    { 
	    cout<<"Vector\n";  
	    PyObject *lowerArray;
	    lowerArray = PyList_GetItem(data,0);
	    testelement = PyList_GetItem(lowerArray,0);
	    if(PyInt_Check(testelement)){
	      Vector3D<float> *c_data;
	      c_data = new Vector3D<float>[length];
	      for(int ct = 0; ct < length; ct++){
		lowerArray = PyList_GetItem(data,ct);
		c_data[ct].x = PyInt_AsLong(PyList_GetItem(lowerArray,0)); //Type casting here from integer to double -- done for simplicity of code
		c_data[ct].y = PyInt_AsLong(PyList_GetItem(lowerArray,1));
		c_data[ct].z = PyInt_AsLong(PyList_GetItem(lowerArray,2));	
	      }
	        workers[0].importVectorData(familyName, attributeName, length, c_data, createCallbackResumeThread(mesg, result));
		//divvyVector(familyName, attributeName, length, c_data);
	    }
	    else if(PyLong_Check(testelement)){
	      Vector3D<float> *c_data;
	      c_data = new Vector3D<float>[length];
	      for(int ct = 0; ct < length; ct++){
		lowerArray = PyList_GetItem(data,ct);
		c_data[ct].x = PyLong_AsLong(PyList_GetItem(lowerArray,0)); //More type casting from long to double
		c_data[ct].y = PyLong_AsLong(PyList_GetItem(lowerArray,1));
		c_data[ct].z = PyLong_AsLong(PyList_GetItem(lowerArray,2));
	      }
	        workers[0].importVectorData(familyName, attributeName, length, c_data, createCallbackResumeThread(mesg, result));
		//divvyVector(familyName, attributeName, length, c_data);
	    }
	    else if(PyFloat_Check(testelement)){
	      cout<<"Float Data\n";
	      Vector3D<float> *c_data;
	      c_data = new Vector3D<float>[length];
	      for(int ct = 0; ct < length; ct++){
		lowerArray = PyList_GetItem(data,ct);
		c_data[ct].x = PyFloat_AsDouble(PyList_GetItem(lowerArray,0));
		c_data[ct].y = PyFloat_AsDouble(PyList_GetItem(lowerArray,1));
		c_data[ct].z = PyFloat_AsDouble(PyList_GetItem(lowerArray,2));
	      }
	      cout<<"c_data[0].x: "<<c_data[0].x<<" c_data[0].y: "<<c_data[0].y<<"c_data[0].z: "<<c_data[0].z<<"\n";
	      workers[0].importVectorData(familyName, attributeName, length, c_data, createCallbackResumeThread(mesg, result));
	      //divvyVector(familyName, attributeName, length, c_data);
	    }
	    else if(PyString_Check(testelement)) dummy = 1; //Exit gracefuly, eventualy, add in strings
	    else dummy = 1; //Exit gracefuly if it is a type we have not checked for  
	  }
	else dummy = 1; //Exit gracefuly -- Some from of non-3D-but-multidimensional array
      }
      else { //Scalar List
	if(PyInt_Check(testelement)){
	  double c_data[length];
	  for(int ct = 0; ct < length; ct++) c_data[ct] = PyInt_AsLong(PyList_GetItem(data,ct)); //Type casting here from integer to double -- done for simplicity of code
	    workers[0].importScalarData(familyName, attributeName, length, c_data, createCallbackResumeThread(mesg, result));
	    //divvyScalar(familyName, attributeName, length, c_data);
	}
	else if(PyLong_Check(testelement)){
	  double c_data[length];
	  for(int ct = 0; ct < length; ct++) c_data[ct] = PyLong_AsLong(PyList_GetItem(data,ct)); //More type casting from long to double
	  workers[0].importScalarData(familyName, attributeName, length, c_data, createCallbackResumeThread(mesg, result));
	  //divvyScalar(familyName, attributeName, length, c_data);
	}
	else if(PyFloat_Check(testelement)){
	  double c_data[length];
	  for(int ct = 0; ct < length; ct++) c_data[ct] = PyFloat_AsDouble(PyList_GetItem(data,ct));
	  workers[0].importScalarData(familyName, attributeName, length, c_data, createCallbackResumeThread(mesg, result));
	  //divvyScalar(familyName, attributeName, length, c_data);
	}
	else if(PyString_Check(testelement)) dummy = 1; //Exit gracefuly, eventualy, add in strings
	else dummy = 1; //Exit gracefuly if it is a type we have not checked for  
	}
	//    delete mesg;
      }
    pythonReturn(handle);
}

void Main::getAttributeRangeGroup(int handle) {
    char *familyName, *attributeName, *groupName;
    Worker* w = workers[0].ckLocal();
    PyObject *arg = PythonObject::pythonGetArg(handle);
    if(PyArg_ParseTuple(arg, "sss", &groupName, &familyName, &attributeName)
       == false) {
	pythonReturn(handle,NULL);
	return;
	}
    
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}

    Worker::GroupMap::iterator giter = w->groups.find(groupName);
    if(giter == w->groups.end()) {
	PyErr_SetString(PyExc_NameError, "No such group");
	pythonReturn(handle, NULL);
	return;
	}
    Simulation::iterator simIter = w->sim->find(familyName);
    if(simIter == w->sim->end()) {
	PyErr_SetString(PyExc_NameError, "No such family");
	pythonReturn(handle, NULL);
	return;
	}
    AttributeMap::iterator attrIter
	= simIter->second.attributes.find(attributeName);
    if(attrIter == simIter->second.attributes.end()) {
	PyErr_SetString(PyExc_NameError, "No such attribute");
	pythonReturn(handle, NULL);
	return;
	}

    int iDim =  attrIter->second.dimensions;
    CkReductionMsg* mesg;
    if(iDim == 1) {
	pair<double,double> minmax;

	// pythonSleep(handle);
	workers.getAttributeRangeGroup(groupName, familyName, attributeName,
				 createCallbackResumeThread(mesg, minmax));
	// pythonAwake(handle);
	pythonReturn(handle,Py_BuildValue("(dd)", minmax.first, minmax.second));
	delete mesg;
	}
    else if(iDim == 3) {
	OrientedBox<double> bounds;

	// pythonSleep(handle);
	workers.getVecAttributeRangeGroup(groupName, familyName, attributeName,
				 createCallbackResumeThread(mesg, bounds));
	// pythonAwake(handle);
	pythonReturn(handle,Py_BuildValue("((ddd)(ddd))", bounds.lesser_corner.x,
					  bounds.lesser_corner.y,
					  bounds.lesser_corner.z,
					  bounds.greater_corner.x,
					  bounds.greater_corner.y,
					  bounds.greater_corner.z));
	delete mesg;
	}
    else {
	PyErr_SetString(PyExc_ValueError, "Dimension is not 1 or 3");
	pythonReturn(handle, NULL);
	return;
	}
    
    }

void Main::createScalarAttribute(int handle) {
    char *familyName, *attributeName;
    PyObject *arg = PythonObject::pythonGetArg(handle);
    CkReductionMsg* mesg;
    int result;

    if(PyArg_ParseTuple(arg, "ss", &familyName, &attributeName) == false) {
	pythonReturn(handle,NULL);
	return;
	}

    workers.createScalarAttribute(familyName, attributeName,
			       createCallbackResumeThread(mesg, result));
    delete mesg;

    pythonReturn(handle);
    }

//CC 4/1/07
void Main::createVectorAttribute(int handle) {
    char *familyName, *attributeName;
    PyObject *arg = PythonObject::pythonGetArg(handle);
    if(PyArg_ParseTuple(arg, "ss", &familyName, &attributeName) == false) {
	pythonReturn(handle, NULL);
	return;
	}
    CkReductionMsg* mesg;
    int result;

    workers.createVectorAttribute(familyName, attributeName,
			       createCallbackResumeThread(mesg, result));
    delete mesg;

    pythonReturn(handle);
    }


void Main::getAttributeSum(int handle)
{
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *groupName, *familyName, *attributeName;
    if(PyArg_ParseTuple(arg, "sss", &groupName, &familyName, &attributeName)
       == false) {
	PyErr_SetString(PyExc_TypeError,
			"Usage: getAttributeSum(group, family, attribute)");
	pythonReturn(handle, NULL);
	return;
	}
    CkReductionMsg* mesg;
    double sum;

    workers.getAttributeSum(groupName, familyName, attributeName, createCallbackResumeThread(mesg, sum));
    pythonReturn(handle,Py_BuildValue("d", sum));
    delete mesg;
    }

void Main::findAttributeMin(int handle)
{
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *groupName, *attributeName;
    CkReductionMsg* mesg;
    pair<double, Vector3D<double> > compair;
    Worker* w = workers[0].ckLocal();

    if(PyArg_ParseTuple(arg, "ss", &groupName, &attributeName) == false) {
	pythonReturn(handle, NULL);
	return;
	}
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    Worker::GroupMap::iterator giter = w->groups.find(groupName);
    if(giter == w->groups.end()) {
	PyErr_SetString(PyExc_NameError, "No such group");
	pythonReturn(handle, NULL);
	return;
	}
	
    // pythonSleep(handle);
    workers.findAttributeMin(groupName, attributeName,
			     createCallbackResumeThread(mesg, compair));
    Vector3D<double> retval = compair.second;
    // pythonAwake(handle);
    pythonReturn(handle,Py_BuildValue("(ddd)", retval.x, retval.y, retval.z));
    delete mesg;
    }

void Main::getDimensions(int handle)
{
    char *familyName, *attributeName;
    Worker* w = this->workers[0].ckLocal();
    PyObject *arg = PythonObject::pythonGetArg(handle);
    int iDim;
    
    if(PyArg_ParseTuple(arg, "ss", &familyName, &attributeName) == false) {
	pythonReturn(handle, NULL);
	return;
	}
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    Simulation::iterator simIter = w->sim->find(familyName);
    if(simIter == w->sim->end()) {
	PyErr_SetString(PyExc_NameError, "No such family");
	pythonReturn(handle, NULL);
	return;
	}
    AttributeMap::iterator attrIter
	= simIter->second.attributes.find(attributeName);
    if(attrIter == simIter->second.attributes.end()) {
	PyErr_SetString(PyExc_NameError, "No such attribute");
	pythonReturn(handle, NULL);
	return;
	}
    iDim =  attrIter->second.dimensions;
    pythonReturn(handle,Py_BuildValue("i", iDim));
    }

void Main::getDataType(int handle)
{
    char *familyName, *attributeName;
    int retcode;
    Worker* w = this->workers[0].ckLocal();
    PyObject *arg = PythonObject::pythonGetArg(handle);

    if(PyArg_ParseTuple(arg, "ss", &familyName, &attributeName) == false) {
	pythonReturn(handle, NULL);
	return;
	}
    
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    Simulation::iterator simIter = w->sim->find(familyName);
    if(simIter == w->sim->end()) {
	PyErr_SetString(PyExc_NameError, "No such family");
	pythonReturn(handle, NULL);
	return;
	}
    AttributeMap::iterator attrIter =
	simIter->second.attributes.find(attributeName);
    if(attrIter == simIter->second.attributes.end()) {
	PyErr_SetString(PyExc_NameError, "No such attribute");
	pythonReturn(handle, NULL);
	return;
	}
    retcode =  attrIter->second.code;
    pythonReturn(handle,Py_BuildValue("i", retcode));
    }

void Main::getCenterOfMass(int handle)
{
    char *groupName;
    CkReductionMsg* mesg;
    Worker* w = workers[0].ckLocal();

    PyObject *arg = PythonObject::pythonGetArg(handle);
    if(PyArg_ParseTuple(arg, "s", &groupName) == false) {
	pythonReturn(handle, NULL);
	return;
	}
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    Worker::GroupMap::iterator giter = w->groups.find(groupName);
    if(giter == w->groups.end()) {
	PyErr_SetString(PyExc_NameError, "No such group");
	pythonReturn(handle, NULL);
	return;
	}

    pair<double, Vector3D<double> > compair;
    // pythonSleep(handle);
    workers.getCenterOfMass(groupName, createCallbackResumeThread(mesg, compair));
    Vector3D<double> retval = compair.second / compair.first;
    delete mesg;
    // pythonAwake(handle);
    pythonReturn(handle,Py_BuildValue("ddd", retval.x, retval.y, retval.z));
    }
	
void Main::createGroup_Family(int handle)
{
    char *groupName, *parentName, *familyName;
    CkReductionMsg* mesg;
    int result;
    PyObject *arg = PythonObject::pythonGetArg(handle);

    if(PyArg_ParseTuple(arg, "sss", &groupName, &parentName, &familyName)
       == false) {
	pythonReturn(handle, NULL);
	return;
	}
    workers.createGroup_Family(groupName, parentName, familyName,
			       createCallbackResumeThread(mesg, result));
    delete mesg;
    pythonReturn(handle);
    }
	
void Main::createGroup_AttributeRange(int handle)
{
    char *groupName, *parentName, *attributeName;
    double minValue, maxValue;
    int result;
    CkReductionMsg* mesg;
    PyObject *arg = PythonObject::pythonGetArg(handle);

    if(PyArg_ParseTuple(arg, "sssdd", &groupName, &parentName, &attributeName,
			&minValue, &maxValue) == false) {
	PyErr_SetString(PyExc_TypeError, "Usage: createGroup_AttributeRange(group, parent_group, attribute, min, max)");
	pythonReturn(handle, NULL);
	return;
	}
    workers.createGroup_AttributeRange(groupName, parentName, attributeName,
				       minValue, maxValue,
				       createCallbackResumeThread(mesg, result));
    delete mesg;
    pythonReturn(handle);
    }

void Main::createGroupAttributeSphere(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achGroupName, *parentName, *attributeName;
    double xCenter, yCenter, zCenter, dSize;
    
    if(PyArg_ParseTuple(arg, "sssdddd", &achGroupName, &parentName,
			&attributeName, &xCenter, &yCenter, &zCenter,
			&dSize) == false) {
	
	PyErr_SetString(PyExc_TypeError, "Usage: createGroupAttributeSphere(group, parent_group, attribute, xcenter, ycenter, zcenter, radius)");
	pythonReturn(handle, NULL);
	return;
	}
    Vector3D<double> v3dCenter(xCenter, yCenter, zCenter);
    string sGroupName(achGroupName);
    string sParentName(parentName);
    string sAttributeName(attributeName);
    
    // pythonSleep(handle);
    workers.createGroup_AttributeSphere(sGroupName, sParentName,
					sAttributeName, v3dCenter, dSize,
					CkCallbackResumeThread());
    // pythonAwake(handle);
    pythonReturn(handle);
}

void Main::createGroupAttributeBox(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achGroupName, *parentName, *attributeName;
    double xCorner, yCorner, zCorner;
    double xEdge1, yEdge1, zEdge1;
    double xEdge2, yEdge2, zEdge2;
    double xEdge3, yEdge3, zEdge3;
    
    if(PyArg_ParseTuple(arg, "sssdddddddddddd", &achGroupName, &parentName,
		     &attributeName,
		     &xCorner, &yCorner, &zCorner,
		     &xEdge1, &yEdge1, &zEdge1,
		     &xEdge2, &yEdge2, &zEdge2,
			&xEdge3, &yEdge3, &zEdge3) == false) {
	pythonReturn(handle, NULL);
	return;
	}
	
    Vector3D<double> v3dCorner(xCorner, yCorner, zCorner);
    Vector3D<double> v3dEdge1(xEdge1, yEdge1, zEdge1);
    Vector3D<double> v3dEdge2(xEdge2, yEdge2, zEdge2);
    Vector3D<double> v3dEdge3(xEdge3, yEdge3, zEdge3);
    string sGroupName(achGroupName);
    string sParentName(parentName);
    string sAttributeName(attributeName);
    
    // pythonSleep(handle);
    workers.createGroup_AttributeBox(sGroupName, sParentName,
					sAttributeName, v3dCorner,
					v3dEdge1, v3dEdge2,v3dEdge3,
					CkCallbackResumeThread());
    // pythonAwake(handle);
    pythonReturn(handle);
}

void Main::runLocalParticleCode(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achCode;
    if(PyArg_ParseTuple(arg, "s", &achCode) == false) {
	pythonReturn(handle, NULL);
	return;
	}

    string s = string(achCode);
    
    workers.localParticleCode(s, CkCallbackPython());
    pythonReturn(handle);
}

#include "marshal.h"

// Fancier group version of above.
void Main::runLocalParticleCodeGroup(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achGroup;
    char *achCode;
    PyObject *global;		// data available to all particles
    
    if(PyArg_ParseTuple(arg, "ssO", &achGroup, &achCode, &global) == false) {
	PyErr_SetString(PyExc_TypeError, "Usage: runLocalParticleCodeGroup(group, code string, parameters)");
	pythonReturn(handle, NULL);
	return;
	}

    string g(achGroup);
    string s(achCode);
    
    Worker* w = workers[0].ckLocal();
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    Worker::GroupMap::iterator giter = w->groups.find(achGroup);
    if(giter == w->groups.end()) {
	PyErr_SetString(PyExc_NameError, "No such group");
	pythonReturn(handle, NULL);
	return;
	}
    PyThreadState *_save = PyThreadState_Swap(NULL);
    PyEval_ReleaseLock();
    // PyGILState_STATE state = PyGILState_Ensure();
    workers.localParticleCodeGroup(g, s, PyObjectMarshal(global),
				   CkCallbackResumeThread());
    // PyGILState_Release(state);
    PyEval_AcquireLock();
    PyThreadState_Swap(_save);

    pythonReturn(handle);
}

// Perform a reduction of particle data
// Two pieces of code are required:
// 1) a particle function that returns a list of tuples, the first
// item of each tuple is an index.
// 2) a reducer that takes a list of tuples and combines common
// indices and returns a new list
// How to get reduction code to the reducer?
// One idea: workers produce tuples of (code, globals, list) to send
// to the reducer.  The reducer first concatenates the lists and then
// calls code with (globals, list) as an argument.

void Main::reduceParticle(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achGroup;
    char *achParticleCode;
    char *achReduceCode;
    PyObject *global;		// data available to all particles
    
    if(PyArg_ParseTuple(arg, "sssO", &achGroup, &achParticleCode,
			&achReduceCode, &global) == false) {
	PyErr_SetString(PyExc_TypeError, "Usage: reduceParticle(group, code string, reduce string, parameters)");
	pythonReturn(handle, NULL);
	return;
	}

    string g(achGroup);
    string sParticleCode(achParticleCode);
    string sReduceCode(achReduceCode);
    
    Worker* w = workers[0].ckLocal();
    if(w->sim == NULL) {
	PyErr_SetString(PyExc_StandardError, "Simulation not loaded");
	pythonReturn(handle, NULL);
	return;
	}
    Worker::GroupMap::iterator giter = w->groups.find(achGroup);
    if(giter == w->groups.end()) {
	PyErr_SetString(PyExc_NameError, "No such group");
	pythonReturn(handle, NULL);
	return;
	}
    CkReductionMsg* mesg;
    char *data;
    PyObjectMarshal result;
    PyThreadState *_save = PyThreadState_Swap(NULL);
    PyEval_ReleaseLock();
    PyGILState_STATE state = PyGILState_Ensure();
    workers.reduceParticle(g, sParticleCode, sReduceCode,
			   PyObjectMarshal(global),
			   createCallbackResumeThread(mesg, data));

    PUP::fromMemBuf(result, mesg->getData(), mesg->getSize());
    PyGILState_Release(state);
    PyEval_AcquireLock();
    PyThreadState_Swap(_save);
    pythonReturn(handle, PySequence_GetItem(result.obj, 2));
    delete mesg;
}
