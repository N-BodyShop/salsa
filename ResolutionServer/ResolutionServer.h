/** @file ResolutionServer.h
 */
 
#ifndef RESOLUTIONSERVER_H
#define RESOLUTIONSERVER_H

#include <vector>
#include <string>
#include <map>
#include <set>

#include <boost/shared_ptr.hpp>

#include "pup_stl.h"
#include "liveViz.h"
#include "PythonCCS.h"
#include "ckcallback-ccs.h"

#include "TipsyParticles.h"
#include "OrientedBox.h"
#include "Sphere.h"
#include "Box.h"
#include "Simulation.h"

template <typename MessageType, typename ResultType>
class CkCallbackResumeThreadResult : public CkCallback {
	MessageType*& m;
	ResultType& result;
public:
		
	CkCallbackResumeThreadResult(MessageType*& m_, ResultType& result_) : CkCallback(resumeThread), m(m_), result(result_) { }
	~CkCallbackResumeThreadResult() {
		m = reinterpret_cast<MessageType *>(thread_delay());
		result = *reinterpret_cast<ResultType *>(m->getData());
	}
};

template <typename MessageType, typename ResultType>
CkCallbackResumeThreadResult<MessageType, ResultType> createCallbackResumeThread(MessageType*& m, ResultType& r) {
	return CkCallbackResumeThreadResult<MessageType, ResultType>(m, r);
}

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
	int radius;
	int width;
	int height;
	Vector3D<double> x;
	Vector3D<double> y;
	Vector3D<double> z;
	Vector3D<double> o;
	int centerFindingMethod;
	//int activeGroup;
	std::string activeGroup;
	double minMass;
	double maxMass;
	int doSplatter;
	
	MyVizRequest() : width(0), height(0) { }

	friend std::ostream& operator<< (std::ostream& os, const MyVizRequest& r) {
		return os << "coloring: " << r.coloring
				<< "\nradius: " << r.radius
				<< "\nwidth: " << r.width
				<< "\nheight: " << r.height
				<< "\nx axis: " << r.x
				<< "\ny axis: " << r.y
				<< "\nz axis: " << r.z
				<< "\norigin: " << r.o
				<< "\ncenter finding: " << r.centerFindingMethod 
				<< "\nactive group: " << r.activeGroup;
	}
};

inline void operator|(PUP::er& p, MyVizRequest& req) {
	p | req.coloring;
	p | req.radius;
	p | req.width;
	p | req.height;
	p | req.x;
	p | req.y;
	p | req.z;
	p | req.o;
	p | req.centerFindingMethod;
	p | req.activeGroup;
	p | req.minMass;
	p | req.maxMass;
	p | req.doSplatter;
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

class PythonTopMain;

extern "C" void initResolutionServer();

class Main : public CBase_Main {
	friend class PythonTopMain;
	
	CProxy_MetaInformationHandler metaProxy;
	CProxy_Worker workers;
	typedef std::map<std::string, std::string> simListType;
	//typedef std::map<std::string, std::pair<std::string, std::string> > simListType;
	simListType simulationList;
	CcsDelayedReply delayedReply;
	std::string regionString;

public:
	
	Main(const Main&);
	
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
	
	void initializePython();
	void executePythonCode(CkCcsRequestMsg* m);
	void localParticleCode(CkCcsRequestMsg * m);
	void findAttributeMin(int handle);
	void getFamilies(int handle);
	void getAttributes(int handle);
	void getGroups(int handle);
	void getNumParticles(int);
	void getAttributeRange(int handle);
	void getAttributeSum(int handle);
	void getDimensions(int handle);
	void getDataType(int handle);
	void getCenterOfMass(int handle);
	void createScalarAttribute(int handle);
	void createGroup_Family(int handle);
	void createGroup_AttributeRange(int handle);
	void createGroupAttributeSphere(int handle);
	void createGroupAttributeBox(int handle);
	void runLocalParticleCode(int handle);
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

//forward declare Group class
namespace SimulationHandling {
class Group;
}

#include "Group.h"

class Worker : public CBase_Worker {
	friend class Main;
	friend class PythonTopMain;
	
	CProxy_MetaInformationHandler metaProxy;
	CkCallback callback;
	SimulationHandling::Simulation* sim;
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
	
	float minMass, maxMass;
	
	typedef std::map<std::string, boost::shared_ptr<SimulationHandling::Group> > GroupMap;
	GroupMap groups;
	// Filippo's Python stuff
	boost::shared_ptr<SimulationHandling::Group> localPartG;
	SimulationHandling::Group::GroupFamilies::iterator localPartFamIter;
	SimulationHandling::GroupIterator localPartIter;
	SimulationHandling::GroupIterator localPartEnd;
	
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
	
	void collectStats(const std::string& id, const CkCallback& cb);
	
	void makeColoring(const std::string& specification, const CkCallback& cb);
	void calculateDepth(MyVizRequest req, const CkCallback& cb);
	void makeGroup(const std::string& s, const CkCallback& cb);
	void setActiveGroup(const std::string& s, const CkCallback& cb);
	void setDrawVectors(const std::string& s, const CkCallback& cb);

	void findAttributeMin(const std::string& groupName,
			      const std::string& attributeName,
			      const CkCallback& cb);
	void getAttributeInformation(CkCcsRequestMsg* m);
	void getColoringInformation(CkCcsRequestMsg* m);
	
	void getAttributeSum(const std::string& groupName,
			     const std::string& familyName,
			     const std::string& attributeName,
			     const CkCallback& cb);
	void getCenterOfMass(const std::string& groupName, const CkCallback& cb);
	void createScalarAttribute(std::string const& familyName,
				   std::string const& attributeName,
				   CkCallback const& cb);
	void createGroup_Family(std::string const& groupName, std::string const& parentGroupName, std::string const& familyName, CkCallback const& cb);
	void createGroup_AttributeRange(std::string const& groupName, std::string const& parentGroupName, std::string const& attributeName, double minValue, double maxValue, CkCallback const& cb);
	void createGroup_AttributeSphere(std::string const& groupName,
					 std::string const& parentName,
					 std::string const& attributeName,
					 Vector3D<double> center, double size,
					 CkCallback const& cb);
	void createGroup_AttributeBox(std::string const& groupName,
				      std::string const& parentGroupName,
				      std::string const& attributeName,
				      Vector3D<double> corner,
				      Vector3D<double> edge1,
				      Vector3D<double> edge2,
				      Vector3D<double> edge3,
				      CkCallback const& cb);
	
	void localParticleCode(std::string s, const CkCallback &cb);
	int buildIterator(PyObject*, void*); // for localParticle
	int nextIteratorUpdate(PyObject*, PyObject*, void*); // for localParticle
};

#endif //RESOLUTIONSERVER_H
