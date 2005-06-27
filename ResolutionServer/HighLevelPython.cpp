#include <stdint.h>
#include "config.h"
#include "ResolutionServer.h"

#include <iostream>
#include <string>

using namespace std;
using namespace SimulationHandling;

void Main::executePythonCode(CkCcsRequestMsg* m) {
	string s(m->data, m->length);
	cout << "Got code to execute: \"" << s << "\"" << endl;
	CcsDelayedReply d = m->reply;
	m->data[m->length-1] = 0;  // guarantee null termination.
	Main::pyRequest(m);
	
	unsigned char success = 1;
	CcsSendDelayedReply(d, 3, "ok\n");
}

void Main::getFamilies(int handle) {
    Worker* w = this->workers[0].ckLocal();
    PyObject *lFamily = PyList_New(0);
    for(Simulation::iterator iter = w->sim->begin(); iter != w->sim->end(); ++iter)
	PyList_Append(lFamily, Py_BuildValue("s", iter->first.c_str()));
    pythonReturn(handle, lFamily);
    }

void Main::getAttributes(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *familyName;
    Worker* w = this->workers[0].ckLocal();
    PyObject *lAttributes = PyList_New(0);
    PyArg_ParseTuple(arg, "s", &familyName);
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
    PyObject *lGroup = PyList_New(0);
    
    for(Worker::GroupMap::iterator iter = w->groups.begin();
	iter != w->groups.end(); ++iter)
	PyList_Append(lGroup, Py_BuildValue("s", iter->first.c_str()));
    pythonReturn(handle, lGroup);
    }

/* usage: getNumParticles('family') */
void Main::getNumParticles(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *familyName;
    Worker* w = this->workers[0].ckLocal();
    PyArg_ParseTuple(arg, "s", &familyName);
    Simulation::iterator iter = w->sim->find(familyName);
    
    if(iter == w->sim->end()) {
	    cerr << "No such family!" << endl;
	    pythonReturn(handle);
	    return;
    }
    pythonReturn(handle,Py_BuildValue("i",
				      iter->second.count.totalNumParticles));
}

void Main::getAttributeRange(int handle) {
    char *familyName, *attributeName;
    PyObject *arg = PythonObject::pythonGetArg(handle);
    PyArg_ParseTuple(arg, "ss", &familyName, &attributeName);
    Worker* w = this->workers[0].ckLocal();
    Simulation::iterator simIter = w->sim->find(familyName);

    if(simIter == w->sim->end()) {
	pythonReturn(handle);
	return;
	}
    AttributeMap::iterator attrIter = simIter->second.attributes.find(attributeName);
    if(attrIter == simIter->second.attributes.end()) {
	pythonReturn(handle);
	return;
	}
    pythonReturn(handle,Py_BuildValue("(dd)", getScalarMin(attrIter->second),
				      getScalarMax(attrIter->second)));
    }

void Main::getAttributeSum(int handle)
{
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *groupName, *attributeName;
    PyArg_ParseTuple(arg, "ss", &groupName, &attributeName);
    CkReductionMsg* mesg;
    double sum;

    pythonSleep(handle);
    workers.getAttributeSum(groupName, attributeName, createCallbackResumeThread(mesg, sum));
    delete mesg;
    pythonAwake(handle);
    pythonReturn(handle,Py_BuildValue("d", sum));
    }

void Main::findAttributeMin(int handle)
{
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *groupName, *attributeName;
    PyArg_ParseTuple(arg, "ss", &groupName, &attributeName);
    CkReductionMsg* mesg;
    pair<double, Vector3D<double> > compair;

    pythonSleep(handle);
    workers.findAttributeMin(groupName, attributeName,
			     createCallbackResumeThread(mesg, compair));
    delete mesg;
    Vector3D<double> retval = compair.second;
    pythonAwake(handle);
    pythonReturn(handle,Py_BuildValue("(ddd)", retval.x, retval.y, retval.z));
    }

void Main::getDimensions(int handle)
{
    char *familyName, *attributeName;
    Worker* w = this->workers[0].ckLocal();
    PyObject *arg = PythonObject::pythonGetArg(handle);
    int iDim;
    
    PyArg_ParseTuple(arg, "ss", &familyName, &attributeName);
    Simulation::iterator simIter = w->sim->find(familyName);
    if(simIter == w->sim->end())
	iDim = 0;
    AttributeMap::iterator attrIter = simIter->second.attributes.find(attributeName);
    if(attrIter == simIter->second.attributes.end())
	iDim = 0;
    else
	iDim =  attrIter->second.dimensions;
    pythonReturn(handle,Py_BuildValue("i", iDim));
    }

void Main::getDataType(int handle)
{
    char *familyName, *attributeName;
    int retcode;
    Worker* w = this->workers[0].ckLocal();
    PyObject *arg = PythonObject::pythonGetArg(handle);

    PyArg_ParseTuple(arg, "ss", &familyName, &attributeName);
    Simulation::iterator simIter = w->sim->find(familyName);
    if(simIter == w->sim->end())
	retcode = 0;
    
    AttributeMap::iterator attrIter = simIter->second.attributes.find(attributeName);
    if(attrIter == simIter->second.attributes.end())
	retcode = 0;
    else
	retcode =  attrIter->second.code;
    pythonReturn(handle,Py_BuildValue("i", retcode));
    }

void Main::getCenterOfMass(int handle)
{
    char *groupName;
    CkReductionMsg* mesg;
    PyObject *arg = PythonObject::pythonGetArg(handle);
    PyArg_ParseTuple(arg, "s", &groupName);
    pair<double, Vector3D<double> > compair;

    pythonSleep(handle);
    workers.getCenterOfMass(groupName, createCallbackResumeThread(mesg, compair));
    delete mesg;
    Vector3D<double> retval = compair.second / compair.first;
    pythonAwake(handle);
    pythonReturn(handle,Py_BuildValue("(ddd)", retval.x, retval.y, retval.z));
    }
	
void Main::createGroup_Family(int handle)
{
    char *groupName, *parentName, *familyName;
    CkReductionMsg* mesg;
    int result;
    PyObject *arg = PythonObject::pythonGetArg(handle);

    PyArg_ParseTuple(arg, "sss", &groupName, &parentName, &familyName);
    pythonSleep(handle);
    workers.createGroup_Family(groupName, parentName, familyName,
			       createCallbackResumeThread(mesg, result));
    delete mesg;
    pythonAwake(handle);
    pythonReturn(handle);
    }
	
void Main::createGroup_AttributeRange(int handle)
{
    char *groupName, *parentName, *attributeName;
    double minValue, maxValue;
    int result;
    CkReductionMsg* mesg;
    PyObject *arg = PythonObject::pythonGetArg(handle);

    PyArg_ParseTuple(arg, "sssdd", &groupName, &parentName, &attributeName,
		     &minValue, &maxValue);
    pythonSleep(handle);
    workers.createGroup_AttributeRange(groupName, parentName, attributeName,
				       minValue, maxValue,
				       createCallbackResumeThread(mesg, result));
    delete mesg;
    pythonAwake(handle);
    pythonReturn(handle);
    }

void Main::createGroupAttributeSphere(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achGroupName, *parentName, *attributeName;
    double xCenter, yCenter, zCenter, dSize;
    
    PyArg_ParseTuple(arg, "sssdddd", &achGroupName, &parentName,
		     &attributeName, &xCenter, &yCenter, &zCenter, &dSize);
    Vector3D<double> v3dCenter(xCenter, yCenter, zCenter);
    string sGroupName(achGroupName);
    string sParentName(parentName);
    string sAttributeName(attributeName);
    
    pythonSleep(handle);
    workers.createGroup_AttributeSphere(sGroupName, sParentName,
					sAttributeName, v3dCenter, dSize,
					CkCallbackResumeThread());
    pythonAwake(handle);
    pythonReturn(handle);
}

void Main::createGroupAttributeBox(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achGroupName, *parentName, *attributeName;
    double xCorner, yCorner, zCorner;
    double xEdge1, yEdge1, zEdge1;
    double xEdge2, yEdge2, zEdge2;
    double xEdge3, yEdge3, zEdge3;
    
    PyArg_ParseTuple(arg, "sssdddddddddddd", &achGroupName, &parentName,
		     &attributeName,
		     &xCorner, &yCorner, &zCorner,
		     &xEdge1, &yEdge1, &zEdge1,
		     &xEdge2, &yEdge2, &zEdge2,
		     &xEdge3, &yEdge3, &zEdge3);
    Vector3D<double> v3dCorner(xCorner, yCorner, zCorner);
    Vector3D<double> v3dEdge1(xEdge1, yEdge1, zEdge1);
    Vector3D<double> v3dEdge2(xEdge2, yEdge2, zEdge2);
    Vector3D<double> v3dEdge3(xEdge3, yEdge3, zEdge3);
    string sGroupName(achGroupName);
    string sParentName(parentName);
    string sAttributeName(attributeName);
    
    pythonSleep(handle);
    workers.createGroup_AttributeBox(sGroupName, sParentName,
					sAttributeName, v3dCorner,
					v3dEdge1, v3dEdge2,v3dEdge3,
					CkCallbackResumeThread());
    pythonAwake(handle);
    pythonReturn(handle);
}

void Main::runLocalParticleCode(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achCode;
    PyArg_ParseTuple(arg, "s", &achCode);

    string s = string(achCode);
    
    workers.localParticleCode(s, CkCallbackResumeThread());
    pythonReturn(handle);
}
