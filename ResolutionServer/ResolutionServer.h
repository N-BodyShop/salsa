/** @file ResolutionServer.h
 */
 
#ifndef RESOLUTIONSERVER_H
#define RESOLUTIONSERVER_H

#include <vector>
#include <string>
#include <map>

#include "pup_stl.h"
#include "liveViz.h"
#include "ckcallback-ccs.h"

#include "TipsyParticles.h"
#include "OrientedBox.h"
#include "Sphere.h"

template <typename T>
class Box {
public:
	Vector3D<T> vertices[8];
};

#include "ResolutionServer.decl.h"

template <typename T>
inline void operator|(PUP::er& p, Box<T>& b) {
	for(int i = 0; i < 8; ++i)
		p | b.vertices[i];
}

typedef unsigned char byte;

extern int verbosity;

class colored_particle {
public:
	
	Vector3D<Tipsy::Real> position;
	float value;
	byte color;
	
	colored_particle() : value(0), color(0)  { }
	
	colored_particle(const Tipsy::simple_particle& p) : position(p.pos), value(0), color(0) { }
};

class Main : public Chare {
	CProxy_Worker workers;
	std::string simulationListFilename;
	typedef std::map<std::string, std::pair<std::string, std::string> > simListType;
	simListType simulationList;
	bool authenticated;
	CcsDelayedReply delayedReply;
public:
		
	Main(CkArgMsg* m);
	
	void authenticate(CkCcsRequestMsg* m);
	void listSimulations(CkCcsRequestMsg* m);
	void chooseSimulation(CkCcsRequestMsg* m);
	void startVisualization(CkReductionMsg* m);
	void shutdownServer(CkCcsRequestMsg* m);
	
};

class Worker : public ArrayElement1D {
	std::vector<colored_particle> myParticles;
	u_int64_t numParticles;
	CkCallback callback;
	float minValue, maxValue;
	bool beLogarithmic;
	byte* image;
	unsigned int imageSize;
	OrientedBox<float> boundingBox;
	vector<Box<float> > boxes;
	vector<Sphere<double> > spheres;
	const static byte numColors = 254;
public:
	
	Worker() : imageSize(0) { }
	Worker(CkMigrateMessage* m) { }
	~Worker() {
		delete[] image;
	}
	
	void readParticles(const std::string& posfilename, const std::string& valuefilename, const CkCallback& cb);
	
	void generateImage(liveVizRequestMsg* m);
	
	void specifyBox(CkCcsRequestMsg* m);
	void clearBoxes(CkCcsRequestMsg* m);
	void specifySphere(CkCcsRequestMsg* m);
	void clearSpheres(CkCcsRequestMsg* m);
	void valueRange(CkCcsRequestMsg* m);
	void recolor(CkCcsRequestMsg* m);
};

template <typename T1>
Vector3D<T1> switchVector(const CkVector3d& v) {
	return Vector3D<T1>(static_cast<T1>(v.x), static_cast<T1>(v.y), static_cast<T1>(v.z));
}

template <typename T2>
CkVector3d switchVector(const Vector3D<T2>& v) {
	return CkVector3d(static_cast<double>(v.x), static_cast<double>(v.y), static_cast<double>(v.z));
}

#endif //RESOLUTIONSERVER_H
