/** @file ResolutionServer.cpp
 */
 
#include <iostream>
#include <fstream>
#include <popt.h>

#include "ResolutionServer.h"

using namespace std;

int verbosity;

Main::Main(CkArgMsg* m) {
	
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
	CcsRegisterHandler("ShutdownServer", CkCallback(CkIndex_Main::shutdownServer(0), thishandle));
	CcsRegisterHandler("SpecifyBox", CkCallback(CkIndex_MetaInformationHandler::specifyBox(0), metaProxy));
	CcsRegisterHandler("ClearBoxes", CkCallback(CkIndex_MetaInformationHandler::clearBoxes(0), metaProxy));
	CcsRegisterHandler("SpecifySphere", CkCallback(CkIndex_MetaInformationHandler::specifySphere(0), metaProxy));
	CcsRegisterHandler("ClearSpheres", CkCallback(CkIndex_MetaInformationHandler::clearSpheres(0), metaProxy));
	CcsRegisterHandler("ValueRange", CkCallback(CkIndex_Worker::valueRange(0), CkArrayIndex1D(0), workers));
	CcsRegisterHandler("Recolor", CkCallback(CkIndex_Worker::recolor(0), workers));
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
			string line, description, posfilename, colorfilename;
			string::size_type index, nextindex;
			while(infile) {
				getline(infile, line);
				//split line, make entry into map
				index = line.find(',');
				if(index != string::npos) {
					description = line.substr(0, index);
					nextindex = line.find(',', index + 1);
					if(nextindex != string::npos) {
						posfilename = line.substr(index + 1, nextindex - index - 1);
						colorfilename = line.substr(nextindex + 1);
						cout << "Parsed line completely: \"" << description << "\"\n\"" << posfilename << "\"\n\"" << colorfilename << "\"" << endl;
						simulationList[description] = make_pair(posfilename, colorfilename);
					}
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
	cout << "You chose: \"" << string(m->data, m->data + m->length) << "\"" << endl;
	simListType::iterator chosen = simulationList.find(string(m->data, m->data + m->length));
	if(authenticated && chosen != simulationList.end()) {
		workers.readParticles(chosen->second.first, chosen->second.second, CkCallback(CkIndex_Main::startVisualization(0), thishandle));
		delayedReply = m->reply;
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
	
	unsigned char success = 1;
	CcsSendDelayedReply(delayedReply, 1, &success);
	
	cerr << "Ready for visualization" << endl;
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
	regionString = string(m->data, m->data + m->length);
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
