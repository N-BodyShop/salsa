/** @file ResolutionServer.cpp
 */
 
#include <inttypes.h>

#include <iostream>
#include <fstream>
#include <set>
#include <iterator>

#ifdef HAVE_LIBPOPT
#include <popt.h>
#endif

#include "config.h"

#include "ResolutionServer.h"

#include "SiXFormat.h"
#include "TipsyFormat.h"


using namespace std;
using namespace SimulationHandling;

int verbosity;

Main::Main(CkArgMsg* m) {
	verbosity = 0;
	
#ifdef HAVE_LIBPOPT
	poptOption optionsTable[] = {
		{"verbose", 'v', POPT_ARG_NONE | POPT_ARGFLAG_ONEDASH | POPT_ARGFLAG_SHOW_DEFAULT, 0, 1, "be verbose about what's going on", "verbosity"},
		POPT_AUTOHELP
		POPT_TABLEEND
	};
	
	poptContext context = poptGetContext("ResolutionServer", m->argc, const_cast<const char **>(m->argv), optionsTable, 0);
	
	poptSetOtherOptionHelp(context, " [OPTION ...] simulation_file");
	
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
	
	poptFreeContext(context);
	delete m;
#else
	const char *optstring = "v";
	int c;
	while((c=getopt(m->argc,m->argv,optstring))>0){
		if(c == -1){
			break;
		}
		switch(c){
			case 'v':
				verbosity++;
				break;
		};
	}
	const char *fname;
	if(optind  < m->argc){
		fname = m->argv[optind];
	}else{
		fname = NULL;
	}
#endif
	
	if(verbosity)
		cerr << "Verbosity level " << verbosity << endl;
	
	if(fname != NULL) {
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
			if(verbosity > 2)
				cerr << "Read list of " << simulationList.size() << " simulations from " << fname << endl;
		} else
			simulationList[fname] = fname;
	    } else
		simulationList[fname] = fname;
	    sim->release();
	    delete sim;
	    }

	metaProxy = CProxy_MetaInformationHandler::ckNew();
    workers = CProxy_Worker::ckNew(metaProxy, CkNumPes());
	if(verbosity)
		cout << "Created workers and meta handler" << endl;
	
	CcsRegisterHandler("ListSimulations", CkCallback(CkIndex_Main::listSimulations(0), thishandle));
	CcsRegisterHandler("ChooseSimulation", CkCallback(CkIndex_Main::chooseSimulation(0), thishandle));
	CcsRegisterHandler("CreateColoring", CkCallback(CkIndex_Main::makeColoring(0), thishandle));
	CcsRegisterHandler("ShutdownServer", CkCallback(CkIndex_Main::shutdownServer(0), thishandle));
	CcsRegisterHandler("ClearBoxes", CkCallback(CkIndex_MetaInformationHandler::clearBoxes(0), metaProxy));
	CcsRegisterHandler("ClearSpheres", CkCallback(CkIndex_MetaInformationHandler::clearSpheres(0), metaProxy));
	CcsRegisterHandler("Activate", CkCallback(CkIndex_Main::activate(0), thishandle));
	CcsRegisterHandler("Statistics", CkCallback(CkIndex_Main::collectStats(0), thishandle));
	CcsRegisterHandler("Center", CkCallback(CkIndex_Main::calculateDepth(0), thishandle));
	CcsRegisterHandler("CreateGroup", CkCallback(CkIndex_Main::makeGroup(0), thishandle));
	CcsRegisterHandler("ActivateGroup", CkCallback(CkIndex_Main::activateGroup(0), thishandle));
	CcsRegisterHandler("DrawVectors", CkCallback(CkIndex_Main::drawVectors(0), thishandle));
	CcsRegisterHandler("GetAttributeInformation", CkCallback(CkIndex_Worker::getAttributeInformation(0), CkArrayIndex1D(0), workers));
	CcsRegisterHandler("GetColoringInformation", CkCallback(CkIndex_Worker::getColoringInformation(0), CkArrayIndex1D(0), workers));

	/* this is the old way   CcsRegisterHandler("ExecutePythonCode", CkCallback(CkIndex_Main::pyRequest(0),thishandle)); */
	/* this is the new way to register python */
	((CProxy_Main)thishandle).registerPython("ExecutePythonCode");
	CcsRegisterHandler("LocalParticleCode",
			   CkCallback(CkIndex_Main::localParticleCode(0), thishandle));
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
    FILE *fp;
	if(verbosity)
		cout << "You chose: \"" << string(m->data, m->length) << "\"" << endl;
	simListType::iterator chosen = simulationList.find(string(m->data, m->length));
	if(chosen != simulationList.end()) {
		workers.loadSimulation(chosen->second, CkCallback(CkIndex_Main::startVisualization(0), thishandle));
		delayedReply = m->reply;
		//return a list of available attributes
	}
	else if((fp = fopen(string(m->data, m->length).c_str(), "r")) != NULL) {
	    fclose(fp);
	    workers.loadSimulation(string(m->data, m->length),
				   CkCallback(CkIndex_Main::startVisualization(0), thishandle));
	    delayedReply = m->reply;
	}
	else {
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
	//cout << "Initializing liveViz" << endl;
    liveVizInit(cfg, workers, CkCallback(CkIndex_Worker::generateImage(0), workers));
	
	unsigned char success = 1;
	CcsSendDelayedReply(delayedReply, 1, &success);
	if(verbosity)
		cerr << "Ready for visualization" << endl;
}

void Main::makeColoring(CkCcsRequestMsg* m) {
	workers.makeColoring(string(m->data, m->length), CkCallback(CkIndex_Main::coloringMade(0), thishandle));
	//doing this is dangerous, could reset delayedReply before it's used by the next handler in the chain
	delayedReply = m->reply;
	delete m;
}

void Main::coloringMade(CkReductionMsg* m) {
	assert(sizeof(int) == m->getSize());
	int value = *static_cast<int *>(m->getData());
	ostringstream oss;
	oss << value;
	string result = oss.str();
	CcsSendDelayedReply(delayedReply, result.length(), result.c_str());
	delete m;
}

void Main::shutdownServer(CkCcsRequestMsg* m) {
	cerr << "Quitting" << endl;
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
	char* buf = reinterpret_cast<char *>(m->data);
	liveVizRequest lvr;
	PUP_toNetwork_unpack up(buf);
	lvr.pupNetwork(up);
	buf += up.size();
	liveVizRequestMsg* msg = liveVizRequestMsg::buildNew(lvr, buf, m->length - up.size());
	MyVizRequest req;
	liveVizRequestUnpack(msg, req);
	//delete msg;
	//MyVizRequest* preq = reinterpret_cast<MyVizRequest *>(m->data + 4 * sizeof(int));
	//get correct endianness
	//PUP::fromNetwork p;
	//p | *preq;
	//cout << "Centering request got MyVizRequest: " << *preq << endl;
	//cout << "Centering request got MyVizRequest: " << req << endl;
	workers.calculateDepth(req, CkCallback(CkIndex_Main::depthCalculated(0), thishandle));
	delete m;
}

void Main::depthCalculated(CkReductionMsg* m) {
	double* z = 0;
	pair<double, double>* potPair;
			potPair = static_cast<pair<double, double> *>(m->getData());
			//cout << "Depth calculated by potential, z = " << potPair->second << " with potential " << potPair->first << endl;
			z = &(potPair->second);
	ostringstream oss;
	oss << *z;
	string result = oss.str();
	CcsSendDelayedReply(delayedReply, result.length(), result.c_str());
	delete m;
}

void Main::makeGroup(CkCcsRequestMsg* m) {
	//coloringMade does what we want, just use it
	workers.makeGroup(string(m->data, m->length), CkCallback(CkIndex_Main::coloringMade(0), thishandle));
	//doing this is dangerous, could reset delayedReply before it's used by the next handler in the chain
	delayedReply = m->reply;
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

void Main::localParticleCode(CkCcsRequestMsg * m) 
{
    workers.localParticleCode(string(m->data, m->length), CkCallbackResumeThread());
    unsigned char success = 1;
    CcsSendDelayedReply(m->reply, 1, &success);
    delete m;
}
  


#include "ResolutionServer.def.h"
