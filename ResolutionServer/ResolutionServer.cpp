/** @file ResolutionServer.cpp
 */
 
#include <iostream>
#include <popt.h>

#include "ResolutionServer.h"

using namespace std;

int verbosity;

Main::Main(CkArgMsg* m) {
	
	verbosity = 0;
	int logarithmic = 0;
	int reversed = 0;
	
	poptOption optionsTable[] = {
		{"verbose", 'v', POPT_ARG_NONE | POPT_ARGFLAG_ONEDASH | POPT_ARGFLAG_SHOW_DEFAULT, 0, 1, "be verbose about what's going on", "verbosity"},
		{"logarithmic", 'l', POPT_ARG_NONE | POPT_ARGFLAG_ONEDASH | POPT_ARGFLAG_SHOW_DEFAULT, &logarithmic, 0, "color by the log of the value", "logarithmic"},
		{"reversed", 'r', POPT_ARG_NONE | POPT_ARGFLAG_ONEDASH | POPT_ARGFLAG_SHOW_DEFAULT, &reversed, 0, "reverse color value", "reverse-color"},
		POPT_AUTOHELP
		POPT_TABLEEND
	};
	
	poptContext context = poptGetContext("ResolutionServer", m->argc, const_cast<const char **>(m->argv), optionsTable, 0);
	
	poptSetOtherOptionHelp(context, " [OPTION ...] positionfile colorvaluefile");
	
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
	
	std::string posfilename;
	std::string valuefilename;
	const char* fname = poptGetArg(context);
	
	if(fname == 0) {
		cerr << "You must provide a position file to visualize" << endl;
		poptPrintUsage(context, stderr, 0);
		CkExit();
		return;
	} else
		posfilename = fname;
	
	fname = poptGetArg(context);
	if(fname == 0) {
		cerr << "You must provide a color value file to visualize" << endl;
		poptPrintUsage(context, stderr, 0);
		CkExit();
		return;
	} else
		valuefilename = fname;
		
	poptFreeContext(context);
	
	if(verbosity)
		cerr << "Verbosity level " << verbosity << endl;
	
    workers = CProxy_Worker::ckNew(CkNumPes());
	cout << "Created workers!" << endl;
	if(logarithmic)
		cout << "Color scale is logarithmic" << endl;
	if(reversed)
		cout << "Color scale is reversed" << endl;
	workers.readParticles(posfilename, valuefilename, logarithmic, reversed, CkCallback(CkIndex_Main::nextPart(0), thishandle));
	cout << "Workers reading!" << endl;
}

void Main::nextPart(CkReductionMsg* m) {
	
	OrientedBox<float> boundingBox = *reinterpret_cast<OrientedBox<float> *>(m->getData());
	delete m;
	CkBbox3d box;
	box.min = switchVector(boundingBox.lesser_corner);
	box.max = switchVector(boundingBox.greater_corner);
    liveVizConfig cfg(false, false, box);
    //cfg.moreVerbose();
	//cfg.moreVerbose();
    liveVizInit(cfg, workers, CkCallback(CkIndex_Worker::generateImage(0), workers));
	
	cerr << "Waiting for ccs" << endl;
	//CkExit();
}

#include "ResolutionServer.def.h"
