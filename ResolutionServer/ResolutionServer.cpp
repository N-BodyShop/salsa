/** @file ResolutionServer.cpp
 */
 
#include <iostream>
#include <fstream>
#include <set>
#include <popt.h>

#include "ResolutionServer.h"

using namespace std;

int verbosity;

Main::Main(CkArgMsg* m) {
	cout << "Started main!" << endl;
	verbosity = 0;
	
	poptOption optionsTable[] = {
		{"verbose", 'v', POPT_ARG_NONE | POPT_ARGFLAG_ONEDASH | POPT_ARGFLAG_SHOW_DEFAULT, 0, 1, "be verbose about what's going on", "verbosity"},
		POPT_AUTOHELP
		POPT_TABLEEND
	};
	
	poptContext context = poptGetContext("ResolutionServer", m->argc, const_cast<const char **>(m->argv), optionsTable, 0);
	
	poptSetOtherOptionHelp(context, " [OPTION ...] simulation_list_file");
	
	int rc;
	while((rc = poptGetNextOpt(context)) >= 0) {
		switch(rc) {
			case 1: //increase verbosity
				verbosity++;
				break;
		}
	}
	
	if(rc < -1) {
		cerr << "Argument error: " << poptBadOption(context, POPT_BADOPTION_NOALIAS) << " : " << poptStrerror(rc) << endl;
		poptPrintUsage(context, stderr, 0);
		CkExit();
		return;
	}
	
	//std::string posfilename;
	//std::string valuefilename;
	const char* fname = poptGetArg(context);
	
	if(fname == 0) {
		cerr << "You must provide a simulation list file" << endl;
		poptPrintUsage(context, stderr, 0);
		CkExit();
		return;
	} else
		simulationListFilename = fname;

	poptFreeContext(context);
	delete m;
	
	if(verbosity)
		cerr << "Verbosity level " << verbosity << endl;
	
	metaProxy = CProxy_MetaInformationHandler::ckNew();
    workers = CProxy_Worker::ckNew(metaProxy, CkNumPes());
	cout << "Created workers and meta handler" << endl;
	
	authenticated = false;
	CcsRegisterHandler("AuthenticateNChilada", CkCallback(CkIndex_Main::authenticate(0), thishandle));
	CcsRegisterHandler("ListSimulations", CkCallback(CkIndex_Main::listSimulations(0), thishandle));
	CcsRegisterHandler("ChooseSimulation", CkCallback(CkIndex_Main::chooseSimulation(0), thishandle));
	CcsRegisterHandler("ChooseColorValue", CkCallback(CkIndex_Main::chooseColorValue(0), thishandle));
	CcsRegisterHandler("ShutdownServer", CkCallback(CkIndex_Main::shutdownServer(0), thishandle));
	CcsRegisterHandler("SpecifyBox", CkCallback(CkIndex_MetaInformationHandler::specifyBox(0), metaProxy));
	CcsRegisterHandler("ClearBoxes", CkCallback(CkIndex_MetaInformationHandler::clearBoxes(0), metaProxy));
	CcsRegisterHandler("SpecifySphere", CkCallback(CkIndex_MetaInformationHandler::specifySphere(0), metaProxy));
	CcsRegisterHandler("ClearSpheres", CkCallback(CkIndex_MetaInformationHandler::clearSpheres(0), metaProxy));
	CcsRegisterHandler("ValueRange", CkCallback(CkIndex_Worker::valueRange(0), CkArrayIndex1D(0), workers));
	CcsRegisterHandler("Activate", CkCallback(CkIndex_Main::activate(0), thishandle));
	CcsRegisterHandler("Statistics", CkCallback(CkIndex_Main::collectStats(0), thishandle));
	
	cerr << "Waiting for ccs authentication" << endl;
}

void Main::authenticate(CkCcsRequestMsg* m) {
	unsigned char reply = 0;
	string message(m->data, m->data + m->length);
	string::size_type index = message.find(':');
	if(index != string::npos) {
		string username = message.substr(0, index);
		string password = message.substr(index + 1);
		cout << "Asked to authenticate \"" << username << "\" with password \"" << password << "\"" << endl;
		if(rand() % 3) {
			reply = 1;
			simulationList.clear();
			ifstream infile(simulationListFilename.c_str());
			string line, description, directoryname;
			while(infile) {
				getline(infile, line);
				//split line, make entry into map
				string::size_type index = line.find(',');
				if(index != string::npos) {
					description = line.substr(0, index);
					directoryname = line.substr(index + 1);
					simulationList[description] = directoryname;
				}
			}
			authenticated = true;
		}
	}
	CcsSendDelayedReply(m->reply, 1, &reply);
	delete m;
}

void Main::listSimulations(CkCcsRequestMsg* m) {
	string reply;
	if(authenticated) {
		for(simListType::iterator iter = simulationList.begin(); iter != simulationList.end(); ++iter) {
			reply += iter->first + ',';
		}
	}
	CcsSendDelayedReply(m->reply, reply.length(), reply.c_str());
	delete m;
}

void Main::chooseSimulation(CkCcsRequestMsg* m) {
	cout << "You chose: \"" << string(m->data, m->length) << "\"" << endl;
	simListType::iterator chosen = simulationList.find(string(m->data, m->length));
	if(authenticated && chosen != simulationList.end()) {
		workers.loadSimulation(chosen->second, CkCallback(CkIndex_Main::startVisualization(0), thishandle));
		delayedReply = m->reply;
		//return a list of available attributes
	} else {
		unsigned char fail = 0;
		CcsSendDelayedReply(m->reply, 1, &fail);
	}
	delete m;
}

void Main::startVisualization(CkReductionMsg* m) {
	OrientedBox<float> boundingBox = *reinterpret_cast<OrientedBox<float> *>(m->getData());
	delete m;
	CkBbox3d box;
	box.min = switchVector(boundingBox.lesser_corner);
	box.max = switchVector(boundingBox.greater_corner);
    liveVizConfig cfg(false, false, box);
    //cfg.moreVerbose();
	//cfg.moreVerbose();
    liveVizInit(cfg, workers, CkCallback(CkIndex_Worker::generateImage(0), workers));
	
	Worker* w = workers[0].ckLocal();
	if(w) {
		ostringstream oss;
		oss << static_cast<int>(w->startColor) << "," << w->sim->size() << ",";
		set<string> attributeNames;
		for(SimulationHandling::Simulation::iterator iter = w->sim->begin(); iter != w->sim->end(); ++iter) {
			oss << iter->first << ",";
			for(SimulationHandling::AttributeMap::iterator attrIter = iter->second.attributes.begin(); attrIter != iter->second.attributes.end(); ++attrIter)
				attributeNames.insert(attrIter->first);
		}
		attributeNames.erase("color");
		attributeNames.insert("family");
		copy(attributeNames.begin(), attributeNames.end(), ostream_iterator<string>(oss, ","));
		const string& reply = oss.str();
		CcsSendDelayedReply(delayedReply, reply.length(), reply.c_str());

		cerr << "Ready for visualization" << endl;
	} else
		cerr << "How the hell did this happen?!" << endl;
}

void Main::chooseColorValue(CkCcsRequestMsg* m) {
	int beLogarithmic = swapEndianness(*reinterpret_cast<int *>(m->data));
	double minVal = swapEndianness(*reinterpret_cast<double *>(m->data + sizeof(int)));
	double maxVal = swapEndianness(*reinterpret_cast<double *>(m->data + sizeof(int) + sizeof(double)));
	string attributeName(m->data + sizeof(int) + 2 * sizeof(double), m->data + m->length);
	cout << "You're choosing attribute \"" << attributeName << "\" to represent the color of particles" << endl;
	unsigned char value = 0;
	if(authenticated) {
		workers.chooseColorValue(attributeName, beLogarithmic, minVal, maxVal, CkCallbackResumeThread());
		value = 1;
	}
	CcsSendDelayedReply(m->reply, 1, &value);
	delete m;
}

void Main::shutdownServer(CkCcsRequestMsg* m) {
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
	CkExit();
}

void Main::activate(CkCcsRequestMsg* m) {
	regionString.assign(m->data, m->length);
	metaProxy.activate(regionString, CkCallbackResumeThread());
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

void Main::collectStats(CkCcsRequestMsg* m) {
	//regionString.assign(m->data, m->length);
	regionString = string(m->data,m->length);
	delayedReply = m->reply;
	workers.collectStats(regionString, CkCallback(CkIndex_Main::statsCollected(0), thishandle));
	//delete m;
}

void Main::statsCollected(CkReductionMsg* m) {
	GroupStatistics* stats = static_cast<GroupStatistics *>(m->getData());
	ostringstream oss;
	oss << "Statistics for \"" << regionString << "\"\nNumber of particles: " << stats->numParticles << "\nBounding box: " << stats->boundingBox << "\n";
	string output = oss.str();
	CcsSendDelayedReply(delayedReply, output.length(), output.c_str());
	delete m;
}

#include "ResolutionServer.def.h"
