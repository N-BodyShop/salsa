#include <stdint.h>
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
	Main::execute(m);
	
	unsigned char success = 1;
	CcsSendDelayedReply(d, 1, &success);
}

void Main::getNumParticles(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *familyName;
    Worker* w = this->workers[0].ckLocal();
    PyArg_ParseTuple(arg, "s", &familyName);
    Simulation::iterator iter = w->sim->find(familyName);
    
    if(iter == w->sim->end()) {
	    cerr << "No such family!" << endl;
	    return;
    }
    pythonPrepareReturn(handle);
    pythonReturn(handle,Py_BuildValue("i",
				      iter->second.count.totalNumParticles));
}

void Main::runLocalParticleCode(int handle) {
    PyObject *arg = PythonObject::pythonGetArg(handle);
    char *achCode, *achEntry;
    PyArg_ParseTuple(arg, "ss", &achCode, &achEntry);

    string s = string(achCode) + string(1, '\0') + string(achEntry);
    
    workers.localParticleCode(s, CkCallbackResumeThread());
}
