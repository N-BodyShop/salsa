/** @file ResolutionServer.cpp
 */
 
#include <iostream>
#include <fstream>
#include <set>
#include <iterator>

#include <popt.h>

#include "pup_network.h"

#include "ResolutionServer.h"

#include "SiXFormat.h"
#include "TipsyFormat.h"

using namespace std;
using namespace SimulationHandling;

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
	}

	poptFreeContext(context);
	delete m;
	
	if(verbosity)
		cerr << "Verbosity level " << verbosity << endl;
	
	Simulation* sim = new SiXFormatReader(fname);
	if(sim->size() == 0) {
		//try plain tipsy format
		sim->release();
		delete sim;
		sim = new TipsyFormatReader(fname);
		if(sim->size() == 0) {
			//it's a list of simulations, parse it
			ifstream infile(fname);
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
		} else
			simulationList[fname] = fname;
	} else
		simulationList[fname] = fname;
	sim->release();
	delete sim;

	metaProxy = CProxy_MetaInformationHandler::ckNew();
    workers = CProxy_Worker::ckNew(metaProxy, CkNumPes());
	if(verbosity)
		cout << "Created workers and meta handler" << endl;
	
	CcsRegisterHandler("ListSimulations", CkCallback(CkIndex_Main::listSimulations(0), thishandle));
	CcsRegisterHandler("ChooseSimulation", CkCallback(CkIndex_Main::chooseSimulation(0), thishandle));
	CcsRegisterHandler("SetDefaultColors", CkCallback(CkIndex_Main::defaultColor(0), thishandle));
	CcsRegisterHandler("ChooseColorValue", CkCallback(CkIndex_Main::chooseColorValue(0), thishandle));
	CcsRegisterHandler("ShutdownServer", CkCallback(CkIndex_Main::shutdownServer(0), thishandle));
	CcsRegisterHandler("SpecifyBox", CkCallback(CkIndex_MetaInformationHandler::specifyBox(0), metaProxy));
	CcsRegisterHandler("ClearBoxes", CkCallback(CkIndex_MetaInformationHandler::clearBoxes(0), metaProxy));
	CcsRegisterHandler("SpecifySphere", CkCallback(CkIndex_MetaInformationHandler::specifySphere(0), metaProxy));
	CcsRegisterHandler("ClearSpheres", CkCallback(CkIndex_MetaInformationHandler::clearSpheres(0), metaProxy));
	CcsRegisterHandler("ValueRange", CkCallback(CkIndex_Worker::valueRange(0), CkArrayIndex1D(0), workers));
	CcsRegisterHandler("Activate", CkCallback(CkIndex_Main::activate(0), thishandle));
	CcsRegisterHandler("Statistics", CkCallback(CkIndex_Main::collectStats(0), thishandle));
	CcsRegisterHandler("Center", CkCallback(CkIndex_Main::calculateDepth(0), thishandle));
	CcsRegisterHandler("CreateGroup", CkCallback(CkIndex_Main::createGroup(0), thishandle));
	CcsRegisterHandler("ActivateGroup", CkCallback(CkIndex_Main::activateGroup(0), thishandle));
	CcsRegisterHandler("DrawVectors", CkCallback(CkIndex_Main::drawVectors(0), thishandle));
	
	cerr << "Waiting for ccs authentication" << endl;
}

void Main::listSimulations(CkCcsRequestMsg* m) {
	string reply;
	for(simListType::iterator iter = simulationList.begin(); iter != simulationList.end(); ++iter) {
		reply += iter->first + ',';
	}
	CcsSendDelayedReply(m->reply, reply.length(), reply.c_str());
	delete m;
}

void Main::chooseSimulation(CkCcsRequestMsg* m) {
	if(verbosity)
		cout << "You chose: \"" << string(m->data, m->length) << "\"" << endl;
	simListType::iterator chosen = simulationList.find(string(m->data, m->length));
	if(chosen != simulationList.end()) {
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
		for(SimulationHandling::Simulation::iterator iter = w->sim->begin(); iter != w->sim->end(); ++iter) {
			oss << iter->first << "," << iter->second.attributes.size() << ",";
			for(SimulationHandling::AttributeMap::iterator attrIter = iter->second.attributes.begin(); attrIter != iter->second.attributes.end(); ++attrIter) {
				if(attrIter->first == "color")
					oss << "family,";
				else
					oss << attrIter->first << ",";
			}
		}
		const string& reply = oss.str();
		CcsSendDelayedReply(delayedReply, reply.length(), reply.c_str());

		cerr << "Ready for visualization" << endl;
	} else
		cerr << "How the hell did this happen?!" << endl;
}

void Main::defaultColor(CkCcsRequestMsg* m) {
	//workers.defaultColor(CkCallback(m->reply));
	workers.defaultColor(CkCallbackResumeThread());
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

void Main::chooseColorValue(CkCcsRequestMsg* m) {
	//workers.chooseColorValue(string(m->data, m->length), CkCallback(m->reply));
	cout << "Color spec: \"" << string(m->data, m->length) << "\"" << endl;
	workers.chooseColorValue(string(m->data, m->length), CkCallbackResumeThread());
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
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

void Main::calculateDepth(CkCcsRequestMsg* m) {
	delayedReply = m->reply;
	MyVizRequest req = *reinterpret_cast<MyVizRequest *>(m->data + 4);
	//get correct endianness
	PUP::fromNetwork p;
	p | req;
	workers.calculateDepth(req, CkCallback(CkIndex_Main::depthCalculated(0), thishandle));
	delete m;
}

void Main::depthCalculated(CkReductionMsg* m) {
	double* z = 0;
	pair<byte, double>* mostPair;
	pair<double, double>* potPair;
	switch(m->getSize()) {
		case 3 * sizeof(double):
			z = static_cast<double *>(m->getData());
			*z /= *(z + 1);
			//cout << "Depth calculated by average, z = " << *z << " with " << *(z + 1) << " particles in the frame" << endl;
			break;
		case sizeof(pair<byte, double>):
			mostPair = static_cast<pair<byte, double> *>(m->getData());
			//cout << "Depth calculated by mostest, z = " << mostPair->second << " with value " << int(mostPair->first) << endl;
			z = &(mostPair->second);
			break;
		case sizeof(pair<double, double>):
			potPair = static_cast<pair<double, double> *>(m->getData());
			//cout << "Depth calculated by potential, z = " << potPair->second << " with potential " << potPair->first << endl;
			z = &(potPair->second);
			break;
	}
	PUP::toNetwork p;
	p | *z;
	CcsSendDelayedReply(delayedReply, sizeof(double), z);
	delete m;
}

void Main::createGroup(CkCcsRequestMsg* m) {
	string s(m->data, m->length);
	workers.createGroup(s, CkCallbackResumeThread());
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

void Main::activateGroup(CkCcsRequestMsg* m) {
	string s(m->data, m->length);
	workers.setActiveGroup(s, CkCallbackResumeThread());
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

void Main::drawVectors(CkCcsRequestMsg* m) {
	string s(m->data, m->length);
	workers.setDrawVectors(s, CkCallbackResumeThread());
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

#include "ResolutionServer.def.h"
