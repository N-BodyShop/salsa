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
#include "Box.h"

#include "ParticleStatistics.h"
#include "ResolutionServer.decl.h"

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
	CProxy_MetaInformationHandler metaProxy;
	CProxy_Worker workers;
	std::string simulationListFilename;
	typedef std::map<std::string, std::pair<std::string, std::string> > simListType;
	simListType simulationList;
	bool authenticated;
	CcsDelayedReply delayedReply;
	std::string id;
public:
		
	Main(CkArgMsg* m);
	
	void authenticate(CkCcsRequestMsg* m);
	void listSimulations(CkCcsRequestMsg* m);
	void chooseSimulation(CkCcsRequestMsg* m);
	void startVisualization(CkReductionMsg* m);
	void shutdownServer(CkCcsRequestMsg* m);
	void activate(CkCcsRequestMsg* m);
	void collectStats(CkCcsRequestMsg* m);
	void statsCollected(CkReductionMsg* m);
	
};

class MetaInformationHandler : public Group {
	
	std::vector<Box<double> > boxes;
	std::vector<Sphere<double> > spheres;
	typedef std::map<std::string, Shape<double>* > RegionMap;
	RegionMap regionMap;
	Shape<double>* activeRegion;
public:
	
	MetaInformationHandler() : activeRegion(0) { }

	void specifyBox(CkCcsRequestMsg* m);
	void clearBoxes(CkCcsRequestMsg* m);
	void specifySphere(CkCcsRequestMsg* m);
	void clearSpheres(CkCcsRequestMsg* m);
	void activate(const std::string& id, const CkCallback& cb);
	
	friend class Worker;
};

class Worker : public ArrayElement1D {
	CProxy_MetaInformationHandler metaProxy;
	std::vector<colored_particle> myParticles;
	u_int64_t numParticles;
	CkCallback callback;
	float minValue, maxValue;
	bool beLogarithmic;
	byte* image;
	unsigned int imageSize;
	OrientedBox<float> boundingBox;
	const static byte numColors = 254;
public:
	
	Worker(const CkGroupID& metaID) : metaProxy(metaID), imageSize(0) { }
	Worker(CkMigrateMessage* m) { }
	~Worker() {
		delete[] image;
	}
	
	void readParticles(const std::string& posfilename, const std::string& valuefilename, const CkCallback& cb);
	
	void generateImage(liveVizRequestMsg* m);
	
	void valueRange(CkCcsRequestMsg* m);
	void recolor(CkCcsRequestMsg* m);
	
	void collectStats(const std::string& id, const CkCallback& cb);
};

template <typename T1>
Vector3D<T1> switchVector(const CkVector3d& v) {
	return Vector3D<T1>(static_cast<T1>(v.x), static_cast<T1>(v.y), static_cast<T1>(v.z));
}

template <typename T2>
CkVector3d switchVector(const Vector3D<T2>& v) {
	return CkVector3d(static_cast<double>(v.x), static_cast<double>(v.y), static_cast<double>(v.z));
}

template <typename T>
inline T swapEndianness(T val) {
	static const unsigned int size = sizeof(T);
	T swapped;
	unsigned char* source = reinterpret_cast<unsigned char *>(&val);
	unsigned char* dest = reinterpret_cast<unsigned char *>(&swapped);
	for(unsigned int i = 0; i < size; ++i)
		dest[i] = source[size - i - 1];
	return swapped;
}

#endif //RESOLUTIONSERVER_H
