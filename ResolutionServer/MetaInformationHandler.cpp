/** @file MetaInformationHandler.cpp
 */

#include "ResolutionServer.h"

void MetaInformationHandler::specifyBox(CkCcsRequestMsg* m) {
	if(m->length != 8 * 3 * sizeof(double))
		return;
	Vector3D<double>* vertices = reinterpret_cast<Vector3D<double> *>(m->data);
	//cout << "Got a box definition" << endl;
	for(int i = 0; i < 8; ++i) {
		vertices[i].x = swapEndianness(vertices[i].x);
		vertices[i].y = swapEndianness(vertices[i].y);
		vertices[i].z = swapEndianness(vertices[i].z);
		//cout << "Vertex " << (i + 1) << ": " << vertices[i] << endl;
	}
	
	Box<double> box(vertices);
	boxes.push_back(box);
	
	//give it identifier, send identifier back to client
	ostringstream oss;
	oss << "Box " << boxes.size();
	string stringID = oss.str();
	regionMap[stringID] = &boxes.back();
	if(CkMyNode() == 0)
		CcsSendDelayedReply(m->reply, stringID.length(), stringID.c_str());
	delete m;
}

void MetaInformationHandler::clearBoxes(CkCcsRequestMsg* m) {
	boxes.clear();
	if(CkMyNode() == 0) {
		unsigned char success = 1;
		CcsSendDelayedReply(m->reply, 1, &success);
	}
	delete m;
}


void MetaInformationHandler::specifySphere(CkCcsRequestMsg* m) {
	if(m->length != 4 * sizeof(double))
		return;
	Sphere<double> s;
	s.origin = *reinterpret_cast<Vector3D<double> *>(m->data);
	s.radius = *reinterpret_cast<double *>(m->data + 3 * sizeof(double));
	s.origin.x = swapEndianness(s.origin.x);
	s.origin.y = swapEndianness(s.origin.y);
	s.origin.z = swapEndianness(s.origin.z);
	s.radius = swapEndianness(s.radius);
	cout << "Got a sphere definition: " << s << endl;
	spheres.push_back(s);
	ostringstream oss;
	oss << "String " << spheres.size();
	string stringID = oss.str();
	regionMap[stringID] = &spheres.back();
	if(CkMyNode() == 0)
		CcsSendDelayedReply(m->reply, stringID.length(), stringID.c_str());
	delete m;
}

void MetaInformationHandler::clearSpheres(CkCcsRequestMsg* m) {
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
