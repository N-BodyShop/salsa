//ResolutionServerPython.cpp

#include <sstream>

#include <boost/python/class.hpp>
#include <boost/python/module.hpp>
#include <boost/python/def.hpp>
#include <boost/python/extract.hpp>

//Include the code we want to expose
#include "ResolutionServer.h"
#include "PythonInstance.h"

#include "export_Vector3D.h"

using namespace std;
using namespace boost::python;
using namespace SimulationHandling;

/* PythonTopMain wraps a Main object and provides functions exportable to Python
 */
class PythonTopMain {
	Main* m;
	Worker* w;
	
public:
	
	PythonTopMain(Main* m_) : m(m_) {
		w = m->workers[0].ckLocal();
	}
	
	boost::python::list getFamilies() {
		boost::python::list families;
		for(Simulation::iterator iter = w->sim->begin(); iter != w->sim->end(); ++iter)
			families.append(iter->first);
		return families;
	}
	
	boost::python::list getAttributes(const string& familyName) {
		boost::python::list attributes;
		Simulation::iterator simIter = w->sim->find(familyName);
		if(simIter != w->sim->end()) {
		for(AttributeMap::iterator attrIter = simIter->second.attributes.begin(); attrIter != simIter->second.attributes.end(); ++attrIter)
			attributes.append(attrIter->first);
		}
		return attributes;
	}
	
	boost::python::list getGroups() {
		boost::python::list groups;
		for(Worker::GroupMap::iterator iter = w->groups.begin(); iter != w->groups.end(); ++iter)
			groups.append(iter->first);
		return groups;
	}
	
	int getNumParticles(const string& familyName) {
		Simulation::iterator iter = w->sim->find(familyName);
		if(iter == w->sim->end()) {
			cerr << "No such family!" << endl;
			return 0;
		}
		return iter->second.count.totalNumParticles;
	}
	
	string getAttributeRange(const string& familyName, const string& attributeName) {
		Simulation::iterator simIter = w->sim->find(familyName);
		if(simIter == w->sim->end())
			return "";
		AttributeMap::iterator attrIter = simIter->second.attributes.find(attributeName);
		if(attrIter == simIter->second.attributes.end())
			return "";
		ostringstream oss;
		oss << "[" << getScalarMin(attrIter->second) << ", " << getScalarMax(attrIter->second) << "]";
		return oss.str();
	}
	
	double getAttributeSum(const string& groupName, const string& attributeName) {
		CkReductionMsg* mesg;
		double sum;
		m->workers.getAttributeSum(groupName, attributeName, createCallbackResumeThread(mesg, sum));
		delete mesg;
		return sum;
	}
	
	int getDimensions(const string& familyName, const string& attributeName) {
		Simulation::iterator simIter = w->sim->find(familyName);
		if(simIter == w->sim->end())
			return 0;
		AttributeMap::iterator attrIter = simIter->second.attributes.find(attributeName);
		if(attrIter == simIter->second.attributes.end())
			return 0;
		return attrIter->second.dimensions;
	}

	int getDataType(const string& familyName, const string& attributeName) {
		Simulation::iterator simIter = w->sim->find(familyName);
		if(simIter == w->sim->end())
			return 0;
		AttributeMap::iterator attrIter = simIter->second.attributes.find(attributeName);
		if(attrIter == simIter->second.attributes.end())
			return 0;
		return attrIter->second.code;
	}
	/*
	double getWeightedSum(const string& groupName, const string& weightName, string const& attributeName) {
		CkReductionMsg* mesg;
		m->workers.getWeightedSum(groupName, weightName, attributeName, createCallbackResumeThread(mesg));
		double sum = *reinterpret_cast<double *>(mesg->getData());
		delete mesg;
		return sum;
	}
	*/
	Vector3D<double> getCenterOfMass(const string& groupName) {
		CkReductionMsg* mesg;
		pair<double, Vector3D<double> > compair;
		m->workers.getCenterOfMass(groupName, createCallbackResumeThread(mesg, compair));
		//pair<double, Vector3D<double> > compair(*reinterpret_cast<pair<double, Vector3D<double> > *>(mesg->getData()));
		delete mesg;
		return compair.second / compair.first;
	}
	
	bool createGroup_Family(string const& familyName) {
		CkReductionMsg* mesg;
		int result;
		m->workers.createGroup_Family(familyName, createCallbackResumeThread(mesg, result));
		delete mesg;
		return result;
	}
	
	bool createGroup_AttributeRange(string const& groupName, string const& attributeName, double minValue, double maxValue) {
		CkReductionMsg* mesg;
		int result;
		m->workers.createGroup_AttributeRange(groupName, attributeName, minValue, maxValue, createCallbackResumeThread(mesg, result));
		delete mesg;
		return result;
	}
};

void Main::initializePython() {
	PyImport_AppendInittab("ResolutionServer", initResolutionServer);
	
	Py_Initialize();
	
	pythonInterpreter.reset(new PythonInstance);
	
	handle<> main_module(borrowed( PyImport_AddModule("__main__") ));
	pythonInterpreter->main_namespace = extract<dict>(PyModule_GetDict(main_module.get()));
	
	try {
		handle<> unused(PyRun_String("import ResolutionServer", Py_file_input, pythonInterpreter->main_namespace.ptr(), pythonInterpreter->main_namespace.ptr()));
	} catch(error_already_set) {
		cerr << "Exception caught!" << endl;
		PyErr_Print();
	}
	
	//create an instance of PythonTopMain, tie it to this object
	//pythonInterpreter->pythonMain.reset(new PythonTopMain(this));
	PythonTopMain* pythonMain = new PythonTopMain(this); //pythonMain will not get deleted, and is therefore a memory leak, damn it!
	//pythonInterpreter->pythonMain = PythonTopMain(this);
	//Make a converter for an existing PythonTopMain object
	to_python_indirect<PythonTopMain*, detail::make_reference_holder> tpi;
	//Use it, and insert the PythonMain into namespace of Python
	//pythonInterpreter->main_namespace["system"] = handle<>(borrowed(tpi(pythonInterpreter->pythonMain)));
	//pythonInterpreter->main_namespace["system"] = handle<>(borrowed(tpi(pythonInterpreter->pythonMain.get())));
	pythonInterpreter->main_namespace["system"] = handle<>(borrowed(tpi(pythonMain)));
	
	to_python_indirect<PythonStdoutWrapper&, detail::make_reference_holder> tpi_stdout;
	pythonInterpreter->main_namespace["my_stdout"] = handle<>(borrowed(tpi_stdout(pythonInterpreter->my_stdout)));
	try {
		handle<> unused(PyRun_String("import sys\nsys.stdout = my_stdout\nsys.stderr = my_stdout\n", Py_file_input, pythonInterpreter->main_namespace.ptr(), pythonInterpreter->main_namespace.ptr()));
	} catch(error_already_set) {
		cerr << "Exception caught redirecting stdout!" << endl;
		PyErr_Print();
	}
}

void Main::executePythonCode(CkCcsRequestMsg* m) {
	string s(m->data, m->length);
	cout << "Got code to execute: \"" << s << "\"" << endl;
	try {
		handle<> unused(PyRun_String(const_cast<char *>(s.c_str()), Py_file_input, pythonInterpreter->main_namespace.ptr(), pythonInterpreter->main_namespace.ptr()));
	} catch(error_already_set) {
		PyErr_Print();
	}
	string result = pythonInterpreter->my_stdout.getOutput();
	CcsSendDelayedReply(m->reply, result.length(), result.c_str());
	delete m;
}

//declare a module called 'ResolutionServer'
BOOST_PYTHON_MODULE(ResolutionServer) {
	
	//expose the class, give it the same name in Python as C++, don't let it be constructed
	class_<PythonTopMain, boost::noncopyable>("PythonTopMain", no_init)
		//expose functions
		.def("getNumParticles", &PythonTopMain::getNumParticles)
		.def("getFamilies", &PythonTopMain::getFamilies)
		.def("getAttributes", &PythonTopMain::getAttributes)
		.def("getGroups", &PythonTopMain::getGroups)
		.def("getAttributeRange", &PythonTopMain::getAttributeRange)
		.def("getAttributeSum", &PythonTopMain::getAttributeSum)
		.def("getDimensions", &PythonTopMain::getDimensions)
		.def("getDataType", &PythonTopMain::getDataType)
		.def("getCenterOfMass", &PythonTopMain::getCenterOfMass)
		.def("createGroup_Family", &PythonTopMain::createGroup_Family)
		.def("createGroup_AttributeRange", &PythonTopMain::createGroup_AttributeRange)
		;
	
	class_<PythonStdoutWrapper, boost::noncopyable>("PythonStdoutWrapper", no_init)
		.def("write", &PythonStdoutWrapper::write)
		;
	
	export_Vector3D<double>();
}
