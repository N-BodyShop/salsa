//PythonInstance.h

#ifndef PYTHONINSTANCE_H__89u0j4t28h92t7yt421y7ht419
#define PYTHONINSTANCE_H__89u0j4t28h92t7yt421y7ht419

#include <sstream>
#include <string>

#include <boost/python/dict.hpp>

class PythonStdoutWrapper {
	std::ostringstream oss;
public:
		
	void write(const std::string& s) {
		oss << s;
	}
	
	std::string getOutput() {
		std::string output = oss.str();
		//clear the underlying buffer
		oss.str("");
		return output;
	}
};

class PythonInstance {
public:
		
	boost::python::dict main_namespace;
	//std::auto_ptr<PythonMain> pythonMain;
	PythonStdoutWrapper my_stdout;
};


#endif //PYTHONINSTANCE_H__89u0j4t28h92t7yt421y7ht419
