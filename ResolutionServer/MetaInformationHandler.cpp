/** @file MetaInformationHandler.cpp
 */

#include <inttypes.h>

#include <sstream>
#include <assert.h>

#include "config.h"
#include "ResolutionServer.h"

using namespace std;

void MetaInformationHandler::specifyBox(CkCcsRequestMsg* m) {
	if(m->length != 8 * 3 * sizeof(double))
		return;
	assert(0);
#if 0
	Vector3D<double>* vertices = reinterpret_cast<Vector3D<double> *>(m->data);
	//cout << "Got a box definition" << endl;
	// This is done in python now
	assert(0);
#if 0
	PUP::fromNetwork p;
	p(vertices, 8);
	
	Box<double>* box = new Box<double>(vertices);
	boxes.push_back(box);
	
	//give it identifier, send identifier back to client
	ostringstream oss;
	oss << "Box " << boxes.size();
	string stringID = oss.str();
	regionMap[stringID] = box;
	if(CkMyNode() == 0)
		CcsSendDelayedReply(m->reply, stringID.length(), stringID.c_str());
#endif
	delete m;
#endif
}

void MetaInformationHandler::clearBoxes(CkCcsRequestMsg* m) {
	activeRegion = 0;
	for(vector<Box<double> *>::iterator iter = boxes.begin(); iter != boxes.end(); ++iter)
		delete *iter;
	boxes.clear();
	if(CkMyNode() == 0) {
		unsigned char success = 1;
		CcsSendDelayedReply(m->reply, 1, &success);
	}
	delete m;
}

void MetaInformationHandler::clearSpheres(CkCcsRequestMsg* m) {
	activeRegion = 0;
	for(vector<Sphere<double> *>::iterator iter = spheres.begin(); iter != spheres.end(); ++iter)
		delete *iter;
	spheres.clear();
	if(CkMyNode() == 0) {
		unsigned char success = 1;
		CcsSendDelayedReply(m->reply, 1, &success);
	}
	delete m;
}

void MetaInformationHandler::activate(const string& id, const CkCallback& cb) {
	RegionMap::iterator selectedRegion = regionMap.find(id);
	if(selectedRegion != regionMap.end())
		activeRegion = selectedRegion->second;
	else
		activeRegion = 0;
	contribute(0, 0, CkReduction::concat, cb);
}
