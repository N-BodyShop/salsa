/** @file ResolutionServer.h
 */
 
#ifndef RESOLUTIONSERVER_H
#define RESOLUTIONSERVER_H

#include <vector>
#include <string>
#include <map>
#include <set>

#include "pup_stl.h"
#include "liveViz.h"
#include "ckcallback-ccs.h"

#include "TipsyParticles.h"
#include "OrientedBox.h"
#include "Sphere.h"
#include "Box.h"
#include "Simulation.h"

enum clipping { low, high, both, none };

struct Coloring {
	std::string name;
	bool infoKnown;
	std::set<std::string> activeFamilies;
	std::string attributeName;
	bool beLogarithmic;
	clipping clip;
	double minValue;
	double maxValue;
	
	Coloring() : infoKnown(false), beLogarithmic(false), clip(none), minValue(0), maxValue(0) { }
	Coloring(const std::string& s);
};

inline void operator|(PUP::er& p, liveVizRequest3d& req) {
	p | req.code;
	p | req.wid;
	p | req.ht;
	p | req.x;
	p | req.y;
	p | req.z;
	p | req.o;
	p | req.minZ;
	p | req.maxZ;
}

template <typename T1>
Vector3D<T1> switchVector(const CkVector3d& v) {
	return Vector3D<T1>(static_cast<T1>(v.x), static_cast<T1>(v.y), static_cast<T1>(v.z));
}

template <typename T2>
CkVector3d switchVector(const Vector3D<T2>& v) {
	return CkVector3d(static_cast<double>(v.x), static_cast<double>(v.y), static_cast<double>(v.z));
}

class MyVizRequest {
public:
	int coloring;
	int width;
	int height;
	Vector3D<double> x;
	Vector3D<double> y;
	Vector3D<double> z;
	Vector3D<double> o;
	double minZ;
	double maxZ;
	
	
	MyVizRequest() : width(0), height(0) { }
	
	MyVizRequest(const liveVizRequest3d& req) {
		coloring = req.code;
		width = req.wid;
		height = req.ht;
		x = switchVector<double>(req.x);
		y = switchVector<double>(req.y);
		z = switchVector<double>(req.z);
		o = switchVector<double>(req.o);
		minZ = req.minZ;
		maxZ = req.maxZ;
	}
	
	friend std::ostream& operator<< (std::ostream& os, const MyVizRequest& r) {
		return os << "coloring: " << r.coloring
				<< "\nwidth: " << r.width
				<< "\nheight: " << r.height
				<< "\nx axis: " << r.x
				<< "\ny axis: " << r.y
				<< "\nz axis: " << r.z
				<< "\norigin: " << r.o
				<< "\nz range: " << r.minZ << " <=> " << r.maxZ;
	}
};

inline void operator|(PUP::er& p, MyVizRequest& req) {
	p | req.coloring;
	p | req.width;
	p | req.height;
	p | req.x;
	p | req.y;
	p | req.z;
	p | req.o;
	p | req.minZ;
	p | req.maxZ;
}

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
	typedef std::map<std::string, std::string> simListType;
	//typedef std::map<std::string, std::pair<std::string, std::string> > simListType;
	simListType simulationList;
	CcsDelayedReply delayedReply;
	std::string regionString;
public:
	
	Main(CkArgMsg* m);
	
	void listSimulations(CkCcsRequestMsg* m);
	void chooseSimulation(CkCcsRequestMsg* m);
	void makeColoring(CkCcsRequestMsg* m);
	void coloringMade(CkReductionMsg* m);
	void startVisualization(CkReductionMsg* m);
	void shutdownServer(CkCcsRequestMsg* m);
	void activate(CkCcsRequestMsg* m);
	void collectStats(CkCcsRequestMsg* m);
	void statsCollected(CkReductionMsg* m);
	void calculateDepth(CkCcsRequestMsg* m);
	void depthCalculated(CkReductionMsg* m);
	void makeGroup(CkCcsRequestMsg* m);
	void activateGroup(CkCcsRequestMsg* m);
	void drawVectors(CkCcsRequestMsg* m);
	
};

class MetaInformationHandler : public Group {
	
	std::vector<Box<double> * > boxes;
	std::vector<Sphere<double> * > spheres;
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
	friend class Main;
	
	CProxy_MetaInformationHandler metaProxy;
	SimulationHandling::Simulation* sim;
	CkCallback callback;
	byte* image;
	unsigned int imageSize;
	OrientedBox<float> boundingBox;
	byte startColor;
	static const std::string coloringPrefix;
	std::vector<Coloring> colorings;
	std::string activeGroupName;
	std::vector<std::string> groupNames;
	
	std::string drawVectorAttributeName;
	bool drawVectors;
	float vectorScale;
	
	template <typename T>
	void assignColors(const unsigned int dimensions, byte* colors, void* values, const u_int64_t N, double minVal, double maxVal, bool beLogarithmic, clipping clip);
	
public:
	
	Worker(const CkGroupID& metaID) : metaProxy(metaID), sim(0), image(new byte[0]), imageSize(0), activeGroupName("All") { }
	Worker(CkMigrateMessage* m) { }
	~Worker() {
		if(sim)
			sim->release();
		delete sim;
		delete[] image;
	}
	
	void loadSimulation(const std::string& simulationName, const CkCallback& cb);
	
	void generateImage(liveVizRequestMsg* m);
	
	void valueRange(CkCcsRequestMsg* m);
	
	void collectStats(const std::string& id, const CkCallback& cb);
	
	void makeColoring(const std::string& specification, const CkCallback& cb);
	void calculateDepth(MyVizRequest req, const CkCallback& cb);
	void makeGroup(const std::string& s, const CkCallback& cb);
	void setActiveGroup(const std::string& s, const CkCallback& cb);
	void setDrawVectors(const std::string& s, const CkCallback& cb);

	void getAttributeInformation(CkCcsRequestMsg* m);
	void getColoringInformation(CkCcsRequestMsg* m);
};

#endif //RESOLUTIONSERVER_H
