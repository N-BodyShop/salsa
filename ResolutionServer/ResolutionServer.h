/** @file ResolutionServer.h
 */
 
#ifndef RESOLUTIONSERVER_H
#define RESOLUTIONSERVER_H

#include <vector>
#include <string>

#include "pup_stl.h"
#include "liveViz.h"

#include "TipsyParticles.h"
#include "OrientedBox.h"

#include "ResolutionServer.decl.h"

typedef unsigned char byte;

extern int verbosity;

class colored_particle {
public:
	
	Vector3D<Tipsy::Real> position;
	byte color;
	
	colored_particle() : color(0)  { }
	
	colored_particle(const Tipsy::simple_particle& p) : position(p.pos), color(255) { }
};

class Main : public Chare {
	CProxy_Worker workers;
public:
		
	Main(CkArgMsg* m);
	void nextPart(CkReductionMsg* m);
};

class Worker : public ArrayElement1D {
	std::vector<colored_particle> myParticles;
	u_int64_t numParticles;
	CkCallback callback;
	float minValue, maxValue;
	float* values;
	bool beLogarithmic;
	byte* image;
	unsigned int imageSize;
	OrientedBox<float> boundingBox;
public:
	
	Worker() : imageSize(0) { }
	Worker(CkMigrateMessage* m) { }
	~Worker() {
		delete[] image;
	}
	
	void readParticles(const std::string& posfilename, const std::string& valuefilename, bool logarithmic, bool reversed, const CkCallback& cb);
	void calculateColors(CkReductionMsg* m);
	
	void generateImage(liveVizRequestMsg* m);

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
