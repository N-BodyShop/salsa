/** @file Worker.cpp
 */

#include <stdint.h>

#include <utility>
#include <cstdlib>
#include <vector>
#include <set>
#include <assert.h>
#include <numeric>

#include <boost/bind.hpp>
#include <boost/iterator/filter_iterator.hpp>
#include <boost/iterator/counting_iterator.hpp>

#include "tree_xdr.h"
#include "SiXFormat.h"
#include "TipsyFormat.h"
#include "SPH_Kernel.h"
#include "Interpolate.h"
#include "Group.h"

#include "pup_network.h"
#include "ResolutionServer.h"
#include "Reductions.h"
#include "Space.h"

std::string trim(const std::string& s) {
	std::string trimmed(s);
	trimmed.erase(0, trimmed.find_first_not_of(" \t\r\n"));
	trimmed.erase(trimmed.find_last_not_of(" \t\r\n") + 1);
	return trimmed;
}

template <typename T>
bool extract(const std::string& s, T& value) {
	std::istringstream iss(s);
	iss >> value;
	return iss;
}

std::list<std::string> splitString(const std::string& s, const char c = ',') {
	std::list<std::string> l;
	std::string::size_type past, pastOld = 0;
	while(pastOld < s.size()) {
		past = s.find(c, pastOld);
		if(past == std::string::npos) {
			l.push_back(trim(s.substr(pastOld)));
			break;
		} else
			++past;
		l.push_back(trim(s.substr(pastOld, past - pastOld - 1)));
		pastOld = past;
	}
	return l;
}

using namespace std;
using namespace SimulationHandling;
using namespace boost;
using namespace SPH;

const string Worker::coloringPrefix = "__internal_coloring";

void Worker::loadSimulation(const std::string& simulationName, const CkCallback& cb) {
	if(MetaInformationHandler* meta = metaProxy.ckLocalBranch()) {
		for(vector<Box<double> *>::iterator iter = meta->boxes.begin(); iter != meta->boxes.end(); ++iter)
			delete *iter;
		for(vector<Sphere<double> *>::iterator iter = meta->spheres.begin(); iter != meta->spheres.end(); ++iter)
			delete *iter;
		meta->boxes.clear();
		meta->spheres.clear();
		meta->activeRegion = 0;
		meta->regionMap.clear();
	}
	
	if(sim)
		sim->release();
	delete sim;
	
	sim = new SiXFormatReader(simulationName);
	if(sim->size() == 0) {
		//try plain tipsy format
		sim->release();
		delete sim;
		sim = new TipsyFormatReader(simulationName);
		if(sim->size() == 0)
			cerr << "Couldn't load simulation file (tried new format and plain tipsy)" << endl;
	}
	
	boundingBox = OrientedBox<float>();
	
	//load appropriate positions
	for(Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter) {
		//cout << "Family name: " << iter->first << endl;
		u_int64_t totalNumParticles = iter->second.count.totalNumParticles;
		u_int64_t numParticles = totalNumParticles / CkNumPes();
		u_int64_t leftover = totalNumParticles % CkNumPes();
		u_int64_t startParticle = CkMyPe() * numParticles;
		if(CkMyPe() < (int) leftover) {
			numParticles++;
			startParticle += CkMyPe();
		} else
			startParticle += leftover;
		
		sim->loadAttribute(iter->first, "position", numParticles, startParticle);
		TypedArray& arr = iter->second.attributes["position"];
		//grow the bounding box with this family's bounding box
		boundingBox.grow(arr.getMinValue(Type2Type<Vector3D<float> >()));
		boundingBox.grow(arr.getMaxValue(Type2Type<Vector3D<float> >()));
	}
	
	
	//create color attribute for all families
	byte familyColor = 2;
	Coloring c;
	c.name = "Family Colors";
	for(Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter, ++familyColor) {
		c.activeFamilies.insert(iter->first);
		byte* colors = new byte[iter->second.count.numParticles];
		memset(colors, familyColor, iter->second.count.numParticles);
		iter->second.addAttribute(coloringPrefix + c.name, colors);
	}
	colorings.clear();
	colorings.push_back(c);
	startColor = familyColor;
	
	//groupNames.push_back("All");
	//activeGroupName = "All";
	groups.clear();
	groups["All"] = boost::shared_ptr<SimulationHandling::Group>(new AllGroup(*sim));
	
	drawVectors = false;
	vectorScale = 0.01;

	minMass = HUGE_VAL;
	maxMass = -HUGE_VAL;
	for(Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter, ++familyColor) {
		TypedArray& massArr = iter->second.attributes["mass"];
		
		if(massArr.data == NULL)
		    break;
		
		float val = massArr.getMinValue(Type2Type<float>());
		if(val < minMass)
			minMass = val;
		val = massArr.getMaxValue(Type2Type<float>());
		if(maxMass < val)
			maxMass = val;
	}
	//handle case where all particles have same mass
	// or case with no masses  XXX do we need these? (should
	// masses be special --trq
	if(minMass >= maxMass)
		minMass = 0;
		
	contribute(sizeof(OrientedBox<float>), &boundingBox, growOrientedBox_float, cb);
}

const byte lineColor = 1;

#include "PixelDrawing.cpp"

class SimplePredicate {
public:
	//the SimplePredicate defines the "All" group, always returning true
	virtual bool operator()(const u_int64_t i) {
		return true;
	}	
};

class NonePredicate : public SimplePredicate {
public:
	//the NonePredicate defines the "None" group, always returning false
	virtual bool operator()(const u_int64_t i) {
		return false;
	}	
};

class IndexedPredicate : public SimplePredicate {
	vector<u_int64_t>::const_iterator nextIndex;
	vector<u_int64_t>::const_iterator endIndex;
public:
		
	IndexedPredicate(vector<u_int64_t>::const_iterator begin, vector<u_int64_t>::const_iterator end) : nextIndex(begin), endIndex(end) { }

	virtual bool operator()(const u_int64_t i) {
		/// XXX Sketchy here! (have to go forward, indices must be ordered, can't skip any indices)
		if(nextIndex != endIndex && i == *nextIndex) {
			++nextIndex;
			return true;
		} else
			return false;
	}	
};

template <typename T>
class InsideSpherePredicate : public SimplePredicate {
	Sphere<T> sphere;
	Vector3D<T> positions;
public:
		
	InsideSpherePredicate(const Sphere<T>& s, const Vector3D<T> pos) : sphere(s), positions(pos) { }

	virtual bool operator()(const u_int64_t i) {
		return sphere.contains(positions[i]);
	}	
};

/* This class acts as a predicate to filter the particles drawn/selected/whatever.
 It is applied to the particle indices by the Boost filter_iterator.
 It forwards the predicate decision on to a subclassed pointer to
 facilitate different predicates. */
class FilterPredicate {
	SimplePredicate* predicate;
public:
	
	FilterPredicate(SimplePredicate* pred = 0) : predicate(pred) { }
	
	bool operator()(const u_int64_t i) {
		return predicate->operator()(i);
	}
	
	void setPredicate(SimplePredicate* pred) {
		predicate = pred;
	}
};

typedef filter_iterator<FilterPredicate, counting_iterator<u_int64_t> > FilterIteratorType;

template <typename T>
struct DecimalLogarithmOp {
	T operator()(T value) {
		return log10(value);
	}
};

template <typename T>
struct DecimalLogarithmOp<Vector3D<T> > {
	Vector3D<T> operator()(Vector3D<T> value) {
		T length = value.length();
		if(length == 0)
			return 0;
		return value / length * log10(length + 1);
	}
};

template <typename T>
inline int sign(T x) {
	if(x < 0)
		return -1;
	else
		return 1;
}

template <typename T>
inline T uniform(T val) {
	if(val < 0)
		return 0;
	else if(val > 1)
		return 1;
	else
		return val;
}
/*
void Worker::generateImage(liveVizRequestMsg* m) {
	//double start = CkWallTimer();
	
	MyVizRequest req;
	liveVizRequestUnpack(m, req);
	
	if(verbosity > 2 && thisIndex == 0)
		cout << "Worker " << thisIndex << ": Image request: " << req << endl;
		
	if(imageSize < req.width * req.height) {
		delete[] image;
		imageSize = req.width * req.height;
		image = new byte[imageSize];
	}
	memset(image, 0, req.width * req.height);

	MetaInformationHandler* meta = metaProxy.ckLocalBranch();
	if(!meta) {
		cerr << "Well this sucks!  Couldn't get local pointer to meta handler" << endl;
		return;
	}
	
	float delta = 2 * req.x.length() / req.width;
	if(verbosity > 3 && thisIndex == 0)
		cout << "Pixel size: " << delta << " x " << (2 * req.y.length() / req.height) << endl;
	req.x /= req.x.lengthSquared();
	req.y /= req.y.lengthSquared();
	float x, y;
	unsigned int pixel;
	
	SimplePredicate* pred = new SimplePredicate;
	
	Coloring& c = colorings[req.coloring];
	activeGroupName = groupNames[req.activeGroup];
	
	bool drawSplatter = req.doSplatter;
		
	for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
		//if this piece doesn't have particles in this family, skip it
		if(simIter->second.count.numParticles == 0)
			continue;
		//don't try to draw inactive families
		if(c.activeFamilies.find(simIter->first) == c.activeFamilies.end())
			continue;
		
		Vector3D<float>* positions = simIter->second.getAttribute("position", Type2Type<Vector3D<float> >());
		byte* colors = simIter->second.getAttribute(coloringPrefix + c.name, Type2Type<byte>());
		//if the color doesn't exist, use the family color
		if(colors == 0)
			colors = simIter->second.getAttribute(coloringPrefix + "familyColor", Type2Type<byte>());
		if(activeGroupName != "All") {
			ParticleGroup& activeGroup = simIter->second.groups[activeGroupName];
			if(verbosity > 2 && thisIndex == 0)
				cerr << "Drawing using group " << activeGroupName << " which has " << activeGroup.size() << " particles" << endl;
			//pred.setIndexed(activeGroup.begin(), activeGroup.end());
			delete pred;
			pred = new IndexedPredicate(activeGroup.begin(), activeGroup.end());
		}
		counting_iterator<u_int64_t> beginIndex(0);
		counting_iterator<u_int64_t> endIndex(simIter->second.count.numParticles);
		FilterIteratorType iter(pred, beginIndex, endIndex);
		FilterIteratorType end(pred, endIndex, endIndex);
		
		bool drawVectorsThisFamily = drawVectors;
		AttributeMap::iterator vectorIter;
		if(drawVectorsThisFamily) {
			vectorIter = simIter->second.attributes.find(drawVectorAttributeName);
			if(vectorIter == simIter->second.attributes.end())
				drawVectorsThisFamily = false;
			else
				sim->loadAttribute(simIter->first, drawVectorAttributeName, simIter->second.count.numParticles, simIter->second.count.startParticle);
		}
		
		if(drawVectorsThisFamily) {
			//get vectors
			CoerciveExtractor<Vector3D<float> > vectorGetter(vectorIter->second);
			Vector3D<float> point;
			float x_end, y_end, t;
			int x0, y0, x1, y1, b;
			for(; iter != end; ++iter) {
				point = positions[*iter] - req.o;
				x = dot(req.x, point);
				if(x > -1 && x < 1) {
					y = dot(req.y, point);
					if(y > -1 && y < 1) {
						point += vectorScale * vectorGetter[*iter];
						x_end = dot(req.x, point);
						y_end = dot(req.y, point);
						if(x_end <= -1 || x_end >= 1) {
							b = sign(x_end);
							t = (b - x) / (x_end - x);
							x_end = b;
							y_end = y + (y_end - y) * t;
						} else if(y_end <= -1 || y_end >= 1) {
							b = sign(y_end);
							t = (b - y) / (y_end - y);
							x_end = x + (x_end - x) * t;
							y_end = b;
						}
						x0 = static_cast<unsigned int>(req.width * (x + 1) / 2);
						x1 = static_cast<unsigned int>(req.width * (x_end + 1) / 2);
						y0 = static_cast<unsigned int>(req.height * (1 - y) / 2);
						y1 = static_cast<unsigned int>(req.height * (1 - y_end) / 2);
						drawLine(image, req.width, req.height, x0, y0, x1, y1, colors[*iter]);
					}
				}
			}
		} else if(drawSplatter) { //draw splattered visual
			//make sure these attributes are loaded, and assign softening to smoothing for point-particles
			AttributeMap::iterator attrIter = simIter->second.attributes.find("mass");
			if(attrIter == simIter->second.attributes.end())
				cerr << "No Masses!" << endl;
			if(attrIter->second.length == 0)
				sim->loadAttribute(simIter->first, "mass", simIter->second.count.numParticles, simIter->second.count.startParticle);
			float* masses = simIter->second.getAttribute("mass", Type2Type<float>());
			if(masses == 0)
				cerr << "Masses pointer null!" << endl;
			string smoothingAttributeName = "softening";
			if(simIter->first == "gas") {
				attrIter = simIter->second.attributes.find("smoothingLength");
				if(attrIter != simIter->second.attributes.end())
					smoothingAttributeName = "smoothingLength";
			}
			attrIter = simIter->second.attributes.find(smoothingAttributeName);
			if(attrIter == simIter->second.attributes.end())
				cerr << "No smoothing or softening!" << endl;
			if(attrIter->second.length == 0)
				sim->loadAttribute(simIter->first, smoothingAttributeName, simIter->second.count.numParticles, simIter->second.count.startParticle);
			float* smoothingLengths = simIter->second.getAttribute(smoothingAttributeName, Type2Type<float>());
			if(smoothingLengths == 0)
				cerr << "D'oh!  smoothingLengths null!" << endl;
			
			if(!projectedKernel.isReady())
				initializeProjectedKernel(100);
			
			float massRange = req.maxMass - req.minMass;
			if(thisIndex == 0) {
				cout << "Mass range is from " << minMass << " to " << maxMass << endl;
				cout << "Splatter range is from " << req.minMass << " to " << req.maxMass << endl;
			}
			for(; iter != end; ++iter) {
				x = dot(req.x, positions[*iter] - req.o);
				float hpix = smoothingLengths[*iter] / delta;
				float xbound = 1 + 2 * 2 * hpix / req.width;
				if(x > -xbound && x < xbound) {
					y = dot(req.y, positions[*iter] - req.o);
					float ybound = 1 + 2 * 2 * hpix / req.height;
					if(y > -ybound && y < ybound) {
						//rescale masses (log masses?) to (256-startColor)
						//float m = (256 - startColor) * uniform((masses[*iter] - req.minMass) / massRange);
						//splatParticle(image, req.width, req.height, x, y, m, smoothingLengths[*iter], delta, startColor);
						double minAmount = req.minMass;
						double maxAmount = req.maxMass;
						splatParticle(image, req.width, req.height, x, y, masses[*iter], smoothingLengths[*iter], delta, startColor, minAmount, maxAmount);
					}
				}
			}
		} else { //draw points only
			for(; iter != end; ++iter) {
				x = dot(req.x, positions[*iter] - req.o);
				if(x > -1 && x < 1) {
					y = dot(req.y, positions[*iter] - req.o);
					if(y > -1 && y < 1) {
						if(req.radius == 0) {
							pixel = static_cast<unsigned int>(req.width * (x + 1) / 2) + req.width * static_cast<unsigned int>(req.height * (1 - y) / 2);
							if(pixel >= imageSize)
								cerr << "Worker " << thisIndex << ": How is my pixel so big? " << pixel << endl;
							if(image[pixel] < colors[*iter])
								image[pixel] = colors[*iter];
						} else
							drawDisk(image, req.width, req.height, static_cast<unsigned int>(req.width * (x + 1) / 2), static_cast<unsigned int>(req.height * (1 - y) / 2), req.radius, colors[*iter]);
					}
				}
			}
		}
	}
	delete pred;
	//XXX removed active region drawing from here
	
	if(thisIndex == 0) {
		if(meta->boxes.size() > 0) {
			//draw boxes onto canvas
			pair<int, int> vertices[8];
			Vector3D<double> vertex;
			for(vector<Box<double>* >::iterator iter = meta->boxes.begin(); iter != meta->boxes.end(); ++iter) {
				for(int i = 0; i < 8; ++i) {
					vertex = (*iter)->vertex(i);
					vertices[i].first = static_cast<int>(floor(req.width * (dot(req.x, vertex - req.o) + 1) / 2));
					vertices[i].second = static_cast<int>(floor(req.height * (1 - dot(req.y, vertex - req.o)) / 2));
				}

				for(int i = 0; i < 4; ++i) {
					drawLine(image, req.width, req.height, vertices[i].first, vertices[i].second, vertices[(i + 1) % 4].first, vertices[(i + 1) % 4].second);
					drawLine(image, req.width, req.height, vertices[i + 4].first, vertices[i + 4].second, vertices[(i + 1) % 4 + 4].first, vertices[(i + 1) % 4 + 4].second);
					drawLine(image, req.width, req.height, vertices[i].first, vertices[i].second, vertices[i + 4].first, vertices[i + 4].second);
				}
			}
		}
		if(meta->spheres.size() > 0) {
			//draw spheres onto canvas
			int x0, y0;
			int radius;
			for(vector<Sphere<double>* >::iterator iter = meta->spheres.begin(); iter != meta->spheres.end(); ++iter) {
				x0 = static_cast<int>(floor(req.width * (dot(req.x, (*iter)->origin - req.o) + 1) / 2));
				y0 = static_cast<int>(floor(req.height * (1 - dot(req.y, (*iter)->origin - req.o)) / 2));
				radius = static_cast<int>((*iter)->radius / delta);
				drawCircle(image, req.width, req.height, x0, y0, radius);
			}
		}
	}
	
	//double stop = CkWallTimer();
	liveVizDeposit(m, 0, 0, req.width, req.height, image, this, (drawSplatter ? sum_image_data : max_image_data));
	//cout << "Image generation took " << (CkWallTimer() - start) << " seconds" << endl;
	//cout << "my part: " << (stop - start) << " seconds" << endl;
}
*/
void Worker::generateImage(liveVizRequestMsg* m) {
	//double start = CkWallTimer();
	
	MyVizRequest req;
	liveVizRequestUnpack(m, req);
	
	if(verbosity > 2 && thisIndex == 0)
		cout << "Worker " << thisIndex << ": Image request: " << req << endl;
		
	if(imageSize < req.width * req.height) {
		delete[] image;
		imageSize = req.width * req.height;
		image = new byte[imageSize];
	}
	memset(image, 0, req.width * req.height);

	MetaInformationHandler* meta = metaProxy.ckLocalBranch();
	if(!meta) {
		cerr << "Well this sucks!  Couldn't get local pointer to meta handler" << endl;
		return;
	}
	
	float delta = 2 * req.x.length() / req.width;
	if(verbosity > 3 && thisIndex == 0)
		cout << "Pixel size: " << delta << " x " << (2 * req.y.length() / req.height) << endl;
	req.x /= req.x.lengthSquared();
	req.y /= req.y.lengthSquared();
	float x, y;
	unsigned int pixel;
	
	if(thisIndex == 0) {
		cout << "Worker " << thisIndex << ": Number of colorings: " << colorings.size() << endl;
		for(vector<Coloring>::iterator colorIter = colorings.begin(); colorIter != colorings.end(); ++colorIter)
			cout << "Coloring " << (colorIter - colorings.begin()) << " is called " << colorIter->name << endl;
	}
	Coloring& c = colorings[req.coloring];
	boost::shared_ptr<SimulationHandling::Group> g(groups[req.activeGroup]);
	
	bool drawSplatter = req.doSplatter;
		
	for(SimulationHandling::Group::GroupFamilies::iterator famIter = g->families.begin(); famIter != g->families.end(); ++famIter) {
		GroupIterator iter = g->make_begin_iterator(*famIter);
		GroupIterator end = g->make_end_iterator(*famIter);
		if(iter == end)
			continue;
		//don't try to draw inactive families
		if(c.activeFamilies.find(*famIter) == c.activeFamilies.end())
			continue;
		
		ParticleFamily& family = (*sim)[*famIter];
		Vector3D<float>* positions = family.getAttribute("position", Type2Type<Vector3D<float> >());
		byte* colors = family.getAttribute(coloringPrefix + c.name, Type2Type<byte>());
		//if the color doesn't exist, use the family color
		if(colors == 0)
			colors = family.getAttribute(coloringPrefix + "familyColor", Type2Type<byte>());
		
		bool drawVectorsThisFamily = drawVectors;
		AttributeMap::iterator vectorIter;
		if(drawVectorsThisFamily) {
			vectorIter = family.attributes.find(drawVectorAttributeName);
			if(vectorIter == family.attributes.end())
				drawVectorsThisFamily = false;
			else
				sim->loadAttribute(*famIter, drawVectorAttributeName, family.count.numParticles, family.count.startParticle);
		}
		
		if(drawVectorsThisFamily) {
			//get vectors
			CoerciveExtractor<Vector3D<float> > vectorGetter(vectorIter->second);
			Vector3D<float> point;
			float x_end, y_end, t;
			int x0, y0, x1, y1, b;
			for(; iter != end; ++iter) {
				point = positions[*iter] - req.o;
				x = dot(req.x, point);
				if(x > -1 && x < 1) {
					y = dot(req.y, point);
					if(y > -1 && y < 1) {
						point += vectorScale * vectorGetter[*iter];
						x_end = dot(req.x, point);
						y_end = dot(req.y, point);
						if(x_end <= -1 || x_end >= 1) {
							b = sign(x_end);
							t = (b - x) / (x_end - x);
							x_end = b;
							y_end = y + (y_end - y) * t;
						} else if(y_end <= -1 || y_end >= 1) {
							b = sign(y_end);
							t = (b - y) / (y_end - y);
							x_end = x + (x_end - x) * t;
							y_end = b;
						}
						x0 = static_cast<unsigned int>(req.width * (x + 1) / 2);
						x1 = static_cast<unsigned int>(req.width * (x_end + 1) / 2);
						y0 = static_cast<unsigned int>(req.height * (1 - y) / 2);
						y1 = static_cast<unsigned int>(req.height * (1 - y_end) / 2);
						drawLine(image, req.width, req.height, x0, y0, x1, y1, colors[*iter]);
					}
				}
			}
		} else if(drawSplatter) { //draw splattered visual
			//make sure these attributes are loaded, and assign softening to smoothing for point-particles
			AttributeMap::iterator attrIter = family.attributes.find("mass");
			if(attrIter == family.attributes.end())
				cerr << "No Masses!" << endl;
			if(attrIter->second.length == 0)
				sim->loadAttribute(*famIter, "mass", family.count.numParticles, family.count.startParticle);
			float* masses = family.getAttribute("mass", Type2Type<float>());
			//CoerciveExtractor<float> masses(attrIter->second);
			if(masses == 0)
				cerr << "Masses pointer null!" << endl;
			string smoothingAttributeName = "softening";
			if(*famIter == "gas") {
				attrIter = family.attributes.find("smoothingLength");
				if(attrIter != family.attributes.end())
					smoothingAttributeName = "smoothingLength";
			}
			attrIter = family.attributes.find(smoothingAttributeName);
			if(attrIter == family.attributes.end())
				cerr << "No smoothing or softening!" << endl;
			if(attrIter->second.length == 0)
				sim->loadAttribute(*famIter, smoothingAttributeName, family.count.numParticles,family.count.startParticle);
			float* smoothingLengths = family.getAttribute(smoothingAttributeName, Type2Type<float>());
			if(smoothingLengths == 0) {
			    cerr << "D'oh!  smoothingLengths null!" << endl;
			    continue; // To next family
			    }
			//CoerciveExtractor<float> smoothingLengths(attrIter->second);
			
			if(!projectedKernel.isReady())
				initializeProjectedKernel(100);
			
			if(thisIndex == 0) {
				cout << "Mass range is from " << minMass << " to " << maxMass << endl;
				cout << "Splatter range is from " << req.minMass << " to " << req.maxMass << endl;
			}
			for(; iter != end; ++iter) {
				x = dot(req.x, positions[*iter] - req.o);
				float hpix = smoothingLengths[*iter] / delta;
				float xbound = 1 + 2 * 2 * hpix / req.width;
				if(x > -xbound && x < xbound) {
					y = dot(req.y, positions[*iter] - req.o);
					float ybound = 1 + 2 * 2 * hpix / req.height;
					if(y > -ybound && y < ybound) {
						//rescale masses (log masses?) to (256-startColor)
						//float m = (256 - startColor) * uniform((masses[*iter] - req.minMass) / massRange);
						//splatParticle(image, req.width, req.height, x, y, m, smoothingLengths[*iter], delta, startColor);
						double minAmount = req.minMass;
						double maxAmount = req.maxMass;
						splatParticle(image, req.width, req.height, x, y, masses[*iter], smoothingLengths[*iter], delta, startColor, minAmount, maxAmount);
					}
				}
			}
		} else { //draw points only
			for(; iter != end; ++iter) {
				x = dot(req.x, positions[*iter] - req.o);
				if(x > -1 && x < 1) {
					y = dot(req.y, positions[*iter] - req.o);
					if(y > -1 && y < 1) {
						if(req.radius == 0) {
							pixel = static_cast<unsigned int>(req.width * (x + 1) / 2) + req.width * static_cast<unsigned int>(req.height * (1 - y) / 2);
							if(pixel >= imageSize)
								cerr << "Worker " << thisIndex << ": How is my pixel so big? " << pixel << endl;
							if(image[pixel] < colors[*iter])
								image[pixel] = colors[*iter];
						} else
							drawDisk(image, req.width, req.height, static_cast<unsigned int>(req.width * (x + 1) / 2), static_cast<unsigned int>(req.height * (1 - y) / 2), req.radius, colors[*iter]);
					}
				}
			}
		}
	}
	//double stop = CkWallTimer();
	liveVizDeposit(m, 0, 0, req.width, req.height, image, this, (drawSplatter ? sum_image_data : max_image_data));
	//cout << "Image generation took " << (CkWallTimer() - start) << " seconds" << endl;
	//cout << "my part: " << (stop - start) << " seconds" << endl;
}

void Worker::collectStats(const string& id, const CkCallback& cb) {
	MetaInformationHandler* meta = metaProxy.ckLocalBranch();
	if(!meta) {
		cerr << "Well this sucks!  Couldn't get local pointer to meta handler" << endl;
		return;
	}
		
	cout << "Finding region pointer for \"" << id << "\"" << endl;
	Shape<double>* activeRegion = 0;
	MetaInformationHandler::RegionMap::iterator selectedRegion = meta->regionMap.find(id);
	if(selectedRegion != meta->regionMap.end())
		activeRegion = selectedRegion->second;
	else
		cout << "Didn't find region pointer" << endl;
	GroupStatistics stats;
	
	if(Sphere<double>* activeSphere = dynamic_cast<Sphere<double> *>(activeRegion)) {
		cout << "It's a sphere" << endl;
		for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
			Vector3D<float>* positions = simIter->second.getAttribute("position", Type2Type<Vector3D<float> >());
			for(u_int64_t i = 0; i < simIter->second.count.numParticles; ++i) {
				if(Space::contains(*activeSphere, positions[i])) {
					stats.numParticles++;
					stats.boundingBox.grow(positions[i]);
				}
			}
		}
	} else if(Box<double>* activeBox = dynamic_cast<Box<double> *>(activeRegion)) {
		cout << "It's a box" << endl;
		for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
			Vector3D<float>* positions = simIter->second.getAttribute("position", Type2Type<Vector3D<float> >());
			for(u_int64_t i = 0; i < simIter->second.count.numParticles; ++i) {
				if(Space::contains(*activeBox, positions[i])) {
					stats.numParticles++;
					stats.boundingBox.grow(positions[i]);
				}
			}
		}
	} else {
		cout << "It's everything" << endl;
		for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
			Vector3D<float>* positions = simIter->second.getAttribute("position", Type2Type<Vector3D<float> >());
			for(u_int64_t i = 0; i < simIter->second.count.numParticles; ++i)
				stats.boundingBox.grow(positions[i]);
			stats.numParticles += simIter->second.count.numParticles;
		}
	}
	cout << "contributing stats" << endl;
	contribute(sizeof(GroupStatistics), &stats, mergeStatistics, cb);
}

void Worker::valueRange(CkCcsRequestMsg* m) {
	string attributeName(m->data, m->length);
	
	double minValue = HUGE_VAL;
	double maxValue = -HUGE_VAL;
	double newMinVal, newMaxVal;
	for(Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter) {
		AttributeMap::iterator attrIter = iter->second.attributes.find(attributeName);
		if(attrIter != iter->second.attributes.end()) {
			newMinVal = getScalarMin(attrIter->second);
			newMaxVal = getScalarMax(attrIter->second);
			if(newMinVal < minValue)
				minValue = newMinVal;
			if(newMaxVal > maxValue)
				maxValue = newMaxVal;
		} else if(attributeName == "family") {
			minValue = 0;
			maxValue = sim->size() - 1;
		}
	}
	
	double minMaxPair[2];
	minMaxPair[0] = minValue;
	minMaxPair[1] = maxValue;
	PUP::toNetwork p;
	p(minMaxPair, 2);
	CcsSendDelayedReply(m->reply, 2 * sizeof(double), minMaxPair);
	delete m;
}

template <typename T>
void Worker::assignColors(const unsigned int dimensions, byte* colors, void* values, const u_int64_t N, double minVal, double maxVal, bool beLogarithmic, clipping clip) {
	double invdelta = 1.0 / (maxVal - minVal);
	double value;
	for(u_int64_t i = 0; i < N; ++i) {
		if(dimensions == 3)
			value = reinterpret_cast<Vector3D<T> *>(values)[i].length();
		else
			value = reinterpret_cast<T *>(values)[i];
		if(beLogarithmic) {
			if(value > 0)
				value = log10(value);
			else {
				colors[i] = 0;
				continue;
			}
		}
		value = floor(startColor + (256 - startColor) * (value - minVal) * invdelta);
		if(value < startColor) {
			if(clip == low || clip == both)
				colors[i] = 0;
			else
				colors[i] = startColor;
		} else if(value > 255) {
			if(clip == high || clip == both)
				colors[i] = 0;
			else
				colors[i] = 255;
		} else
			colors[i] = static_cast<byte>(value);
	}
}

/*
class ColoringVisitor : public static_visitor<> {
	double minValue, maxValue;
	double invdelta;
	bool beLogarithmic;
	Clipping clipping;
	byte minColor, maxColor;
public:
		
	ColoringVisitor(double minVal, double maxVal, bool beLog, Clipping clip, byte minC, byte maxC) : minValue(minVal), maxValue(maxVal), invdelta(1.0 / (maxVal - minVal)), beLogarithmic(beLog), clipping(clip), minColor(minC), maxColor(maxC) { }

	//needed, but unused
	template <typename T, typename U>
	void operator()(T&, U&) const { }
	
	//this visitor can only be applied when the second operand is an array of color values (bytes)
	template <typename T>
	void operator()(ArrayWithLimits<T>& attribute, ArrayWithLimits<byte>& colors) const {
		typename ArrayWithLimits<byte>::iterator colorIter = colors.begin();
		double value = 0;
		for(typename ArrayWithLimits<T>::iterator iter = arr.begin(); iter != arr.end(); ++iter, ++colorIter) {
			value = *iter;
			if(beLogarithmic) {
				if(value == 0) {
					*colorIter = 0;
					continue;
				}
				value = log10(value);
			}
			value = (value - minValue) * invdelta;
			if(value < 0) {
				if(clipping == both || clipping == low)
					*colorIter = 0;
				else
					*colorIter = minColor;
			} else if(value >= 1) {
				if(clipping == both || clipping == high)
					*colorIter = 0;
				else
					*colorIter = maxColor;
			} else
				*colorIter = minColor + static_cast<byte>((1 + maxColor - minColor) * value);
		}
	}

	//for vector quantities use the length of the vector
	template <typename T>
	void operator()(ArrayWithLimits<Vector3D<T> >& attribute, ArrayWithLimits<byte>& colors) const {
		typename ArrayWithLimits<byte>::iterator colorIter = colors.begin();
		double value = 0;
		for(typename ArrayWithLimits<Vector3D<T> >::iterator iter = arr.begin(); iter != arr.end(); ++iter, ++colorIter) {
			value = iter->length();
			if(beLogarithmic) {
				if(value == 0) {
					*colorIter = 0;
					continue;
				}
				value = log10(value);
			}
			value = (value - minValue) * invdelta;
			if(value < 0) {
				if(clipping == both || clipping == low)
					*colorIter = 0;
				else
					*colorIter = minColor;
			} else if(value >= 1) {
				if(clipping == both || clipping == high)
					*colorIter = 0;
				else
					*colorIter = maxColor;
			} else
				*colorIter = minColor + static_cast<byte>((1 + maxColor - minColor) * value);
		}
	}
};

void Worker::createNewColoring(const std::string& specification, const CkCallback& cb) {
	//decode specification, get colorName, attributeName, minValue, maxValue, beLogarithmic
	
	ColoringVisitor coloring(minValue, maxValue, beLogarithmic, startColor, 255)
	for(Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter) {
		AttributeMap::iterator attrIter = iter->second.attributes.find(attributeName);
		if(attrIter != iter->second.attributes.end()) {
			variant_attribute_type color_variant(ArrayWithLimits<byte>(new byte[iter->second.count.numParticles], iter->second.count.numParticles));
			apply_visitor(coloring, attrIter->second, color_variant);
			iter->second.addAttribute(colorName, color_variant);
		}
	}
}
*/

Coloring::Coloring(const string& s) {
	infoKnown = false;
	
	list<string> args(splitString(s));
	assert(args.size() >= 7);
	
	list<string>::iterator iter = args.begin();
	
	name = *iter;
	
	beLogarithmic = false;
	if(*++iter == "logarithmic")
		beLogarithmic = true;
	
	attributeName = *++iter;
	
	extract(*++iter, minValue);
	extract(*++iter, maxValue);
	
	++iter;
	clip = none;
	if(*iter == "cliplow")
		clip = low;
	else if(*iter == "cliphigh")
		clip = high;
	else if(*iter == "clipboth")
		clip = both;
	
	activeFamilies.insert(++iter, args.end());
	
	infoKnown = true;
}

class NamePredicate {
	string name;
public:
	NamePredicate(const string& s) : name(s) { }

	template <typename T>
	bool operator()(const T& val) const {
		return val.name == name;
	}
};

void Worker::makeColoring(const std::string& specification, const CkCallback& cb) {
	Coloring c(specification);
	
	int index = 0;
	vector<Coloring>::iterator coloringIter = find_if(colorings.begin(), colorings.end(), NamePredicate(c.name));
	if(coloringIter != colorings.end()) {
		*coloringIter = c;
		index = coloringIter - colorings.begin();
	} else {
		colorings.push_back(c);
		index = colorings.size() - 1;
	}
		
	byte familyColor = startColor - sim->size();
	for(Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter, ++familyColor) {
		if(c.activeFamilies.count(iter->first) != 0) { //it's active
			AttributeMap::iterator attrIter = iter->second.attributes.find(c.attributeName);
			if(attrIter != iter->second.attributes.end()) {	//this family has the desired attribute
				byte* colors = new byte[iter->second.count.numParticles];
				if(attrIter->second.length == 0)
					sim->loadAttribute(iter->first, c.attributeName, iter->second.count.numParticles, iter->second.count.startParticle);
				void* values = iter->second.attributes[c.attributeName].data;
				if(values != 0) {
					switch(attrIter->second.code) {
						case int8:
							assignColors<char>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case uint8:
							assignColors<unsigned char>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case int16:
							assignColors<short>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case uint16:
							assignColors<unsigned short>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case int32:
							assignColors<int>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case uint32:
							assignColors<unsigned int>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case int64:
							assignColors<int64_t>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case uint64:
							assignColors<u_int64_t>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case float32:
							assignColors<float>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case float64:
							assignColors<double>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						default:
							break;
					}
					iter->second.addAttribute(coloringPrefix + c.name, colors);
				}
			}
		}
	}
	
	contribute(sizeof(int), &index, CkReduction::max_int, cb);
	//contribute(0, 0, CkReduction::concat, cb);
}

void Worker::calculateDepth(MyVizRequest req, const CkCallback& cb) {
	//z component of the viewing frame
	double z = 0;
	
	if(verbosity > 2 && thisIndex == 0)
		cout << "Got request for centering: " << req << endl;
	
	req.z = cross(req.x, req.y);
	req.z.normalize();
	req.x /= req.x.lengthSquared();
	req.y /= req.y.lengthSquared();
	float x, y;
	u_int64_t numParticlesInFrame = 0;
	byte maxPixel = 0;
	double minPotential = HUGE_VAL;
	
	//should only use active particles to calculate this!!!!
	
	switch(req.centerFindingMethod) {
		case 0: { //average of all pixels in frame	
			for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
				Vector3D<float>* positions = simIter->second.getAttribute("position", Type2Type<Vector3D<float> >());
				for(u_int64_t i = 0; i < simIter->second.count.numParticles; ++i) {
					x = dot(req.x, positions[i] - req.o);
					if(x > -1 && x < 1) {
						y = dot(req.y, positions[i] - req.o);
						if(y > -1 && y < 1) {
							numParticlesInFrame++;
							z += dot(req.z, positions[i] - req.o);
						}
					}
				}
			}
			//send an extra unused number to differentiate the responses
			double numbers[3];
			numbers[0] = z;
			numbers[1] = numParticlesInFrame;
			numbers[2] = 0;
			contribute(3 * sizeof(double), numbers, CkReduction::sum_double, cb);
			break;
		}
		case 1: { //"mostest" particle in frame
			for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
				Vector3D<float>* positions = simIter->second.getAttribute("position", Type2Type<Vector3D<float> >());
				byte* colors = simIter->second.getAttribute("color", Type2Type<byte>());
				for(u_int64_t i = 0; i < simIter->second.count.numParticles; ++i) {
					x = dot(req.x, positions[i] - req.o);
					if(x > -1 && x < 1) {
						y = dot(req.y, positions[i] - req.o);
						if(y > -1 && y < 1) {
							if(colors[i] > maxPixel) {
								maxPixel = colors[i];
								z = dot(req.z, positions[i] - req.o);
							}
						}
					}
				}
			}
			pair<byte, double> mostest(maxPixel, z);
			contribute(sizeof(pair<byte, double>), &mostest, pairByteDoubleMax, cb);
			break;
		}
		case 2: { //particle with lowest potential in frame
			for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
				Vector3D<float>* positions = simIter->second.getAttribute("position", Type2Type<Vector3D<float> >());
				float* potentials = simIter->second.getAttribute("potential", Type2Type<float>());
				if(potentials == 0) {
					sim->loadAttribute(simIter->first, "potential", simIter->second.count.numParticles, simIter->second.count.startParticle);
					potentials = simIter->second.getAttribute("potential", Type2Type<float>());
				}
				//should only look at active particles here!
				for(u_int64_t i = 0; i < simIter->second.count.numParticles; ++i) {
					x = dot(req.x, positions[i] - req.o);
					if(x > -1 && x < 1) {
						y = dot(req.y, positions[i] - req.o);
						if(y > -1 && y < 1) {
							if(potentials && potentials[i] < minPotential) {
								minPotential = potentials[i];
								z = dot(req.z, positions[i] - req.o);
							}
						}
					}
				}
			}
			pair<double, double> lowest(minPotential, z);
			contribute(sizeof(pair<double, double>), &lowest, pairDoubleDoubleMin, cb);
			break;
		}
	}
}

void Worker::makeGroup(const string& s, const CkCallback& cb) {
	string groupName, attributeName;
	list<string> parts = splitString(s);
	float minValue, maxValue;
	int index = 0;
	if(parts.size() >= 4) {
		list<string>::iterator iter = parts.begin();
		groupName = *iter;
		++iter;
		attributeName = *iter;
		if(extract(*++iter, minValue) && extract(*++iter, maxValue)) {
			if(verbosity > 2 && thisIndex == 0)
				cerr << "Defining group " << groupName << " on attribute " << attributeName << " from " << minValue << " to " << maxValue << endl;
			for(Simulation::const_iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
				AttributeMap::const_iterator attrIter = simIter->second.attributes.find(attributeName);
				if(attrIter != simIter->second.attributes.end() && attrIter->second.data == 0)
					sim->loadAttribute(simIter->first, attributeName, simIter->second.count.numParticles, simIter->second.count.startParticle);
			}
			groups[groupName] = make_AttributeRangeGroup(*sim, groups["All"], attributeName, minValue, maxValue);
			/*
			vector<string>::iterator groupIter = find(groupNames.begin(), groupNames.end(), groupName);
			if(groupIter != groupNames.end())
				index = groupIter - groupNames.begin();
			else {
				groupNames.push_back(groupName);
				index = groupNames.size() - 1;
			}
			for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
				sim->loadAttribute(simIter->first, attributeName, simIter->second.count.numParticles, simIter->second.count.startParticle);
				ParticleGroup& g = simIter->second.createGroup(groupName, attributeName, static_cast<void *>(&minValue), static_cast<void *>(&maxValue));
				if(verbosity > 3)
					cout << "Piece " << thisIndex << ": My part of the group includes " << g.size() << " particles" << endl;
			}
			*/
		} else
			cerr << "Problem getting group range values, no group created" << endl;
	} else
		cerr << "Group definition string malformed, no group created" << endl;
	
	if(verbosity > 2 && thisIndex == 0)
		cout << "Created group " << groupName << endl;
	
	//contribute(0, 0, CkReduction::concat, cb);
	contribute(sizeof(int), &index, CkReduction::max_int, cb);
}

void Worker::setActiveGroup(const std::string& s, const CkCallback& cb) {
	activeGroupName = s;
	if(thisIndex == 0)
		cerr << "Activated group " << activeGroupName << endl;
	contribute(0, 0, CkReduction::concat, cb);
}

void Worker::setDrawVectors(const string& s, const CkCallback& cb) {
	list<string> parts = splitString(s);
	if(parts.size() >= 2) {
		list<string>::iterator iter = parts.begin();
		drawVectorAttributeName = *iter;
		if(drawVectorAttributeName == "")
			drawVectors = false;
		else if(extract(*++iter, vectorScale))
			drawVectors = true;
	}
		
	if(verbosity > 2 && thisIndex == 0)
		cout << "Changed drawing of vectors somehow " << s << endl;
	
	contribute(0, 0, CkReduction::concat, cb);
}

int makeColor(const string& s) {
	if(s == "dark")
		return ((190 << 16) | (200 << 8) | (255));
	else if(s == "gas")
		return ((255 << 16) | (63 << 8) | (63));
	else if(s == "star")
		return ((255 << 16) | (255 << 8) | (140));
	else
		return ((255 << 16) | (255 << 8) | (255));
}

template <typename T>
string javaFormat(T x) {
	ostringstream oss;
	if(!(x == x))
		oss << "NaN";
	else if(x > numeric_limits<T>::max())
		oss << "Infinity";
	else if(x < -numeric_limits<T>::max())
		oss << "-Infinity";
	else
		oss << x;
	return oss.str();
}

void Worker::getAttributeInformation(CkCcsRequestMsg* m) {	
	ostringstream oss;
	oss << "simulationName = " << sim->name << "\n";
	oss << "numFamilies = " << sim->size() << "\n";
	int familyNumber = 0;
	for(SimulationHandling::Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter, ++familyNumber) {
		ParticleFamily& family = iter->second;
		oss << "family-" << familyNumber << ".name = " << family.familyName << "\n";
		oss << "family-" << familyNumber << ".numParticles = " << family.count.totalNumParticles << "\n";
		oss << "family-" << familyNumber << ".defaultColor = " << makeColor(family.familyName) << "\n";
		int numAttributes = 0;
		for(SimulationHandling::AttributeMap::iterator attrIter = family.attributes.begin(); attrIter != family.attributes.end(); ++attrIter) {
			if(attrIter->first.find("__internal") != 0) {
				oss << "family-" << familyNumber << ".attribute-" << numAttributes << ".name = " << attrIter->first << "\n";
				TypedArray& arr = attrIter->second;
				oss << "family-" << familyNumber << ".attribute-" << numAttributes << ".dimensionality = ";
				if(arr.dimensions == 1)
					oss << "scalar\n";
				else
					oss << "vector\n";
				oss << "family-" << familyNumber << ".attribute-" << numAttributes << ".dataType = " << arr.code << "\n";
				oss << "family-" << familyNumber << ".attribute-" << numAttributes << ".definition = external\n";
				oss << "family-" << familyNumber << ".attribute-" << numAttributes << ".minScalarValue = " << javaFormat(getScalarMin(arr)) << "\n";
				oss << "family-" << familyNumber << ".attribute-" << numAttributes << ".maxScalarValue = " << javaFormat(getScalarMax(arr)) << "\n";
				++numAttributes;
			}
		}
		oss << "family-" << familyNumber << ".numAttributes = " << numAttributes << "\n";
	}		
	string result(oss.str());
	CcsSendDelayedReply(m->reply, result.length(), result.c_str());
	delete m;
}

void Worker::getColoringInformation(CkCcsRequestMsg* m) {	
	ostringstream oss;
	oss << "numColorings = " << colorings.size() << "\n";
	for(int i = 0; i < (int) colorings.size(); ++i) {
		oss << "coloring-" << i << ".name = " << colorings[i].name << "\n";
		oss << "coloring-" << i << ".id = " << i << "\n";
		oss << "coloring-" << i << ".infoKnown = " << (colorings[i].infoKnown ? "true\n" : "false\n");
		if(colorings[i].infoKnown) {
			oss << "coloring-" << i << ".activeFamilies = ";
			copy(colorings[i].activeFamilies.begin(), colorings[i].activeFamilies.end(), ostream_iterator<string>(oss, ","));
			oss << "\n";
			oss << "coloring-" << i << ".logarithmic = " << (colorings[i].beLogarithmic ? "true\n" : "false\n");
			oss << "coloring-" << i << ".clipping = ";
			switch(colorings[i].clip) {
				case low: oss << "cliplow\n"; break;
				case high: oss << "cliphigh\n"; break;
				case both: oss << "clipboth\n"; break;
				case none: oss << "clipnone\n"; break;
			}
			oss << "coloring-" << i << ".minValue = " << colorings[i].minValue << "\n";
			oss << "coloring-" << i << ".maxValue = " << colorings[i].maxValue << "\n";
		}
	}
	string result(oss.str());
	CcsSendDelayedReply(m->reply, result.length(), result.c_str());
	delete m;
}
/*
void Worker::getGroupInformation(CkCcsRequestMsg* m) {	

	CcsSendDelayedReply(m->reply, 2 * sizeof(double), minMaxPair);
	delete m;
}
*/

template <typename T, typename IteratorType>
double sumAttribute(TypedArray const& arr, IteratorType begin, IteratorType end) {
	double sum = 0;
	T const* array = arr.getArray(Type2Type<T>());
	if(array == 0)
		return 0;
	for(; begin != end; ++begin)
		sum += array[*begin];
	return sum;
}
/*
void Worker::getAttributeSum(const string& groupName, const string& attributeName, const CkCallback& cb) {
	double sum = 0;
	SimplePredicate* pred = new SimplePredicate;
	for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
		AttributeMap::iterator attrIter = simIter->second.attributes.find(attributeName);
		if(attrIter == simIter->second.attributes.end())
			continue;
		
		if(groupName != "All") {
			ParticleGroup& activeGroup = simIter->second.groups[groupName];
			delete pred;
			pred = new IndexedPredicate(activeGroup.begin(), activeGroup.end());
		}
		counting_iterator<u_int64_t> beginIndex(0);
		counting_iterator<u_int64_t> endIndex(simIter->second.count.numParticles);
		FilterIteratorType iter(pred, beginIndex, endIndex);
		FilterIteratorType end(pred, endIndex, endIndex);
		
		//only makes sense for scalar values
		if(attrIter->second.dimensions != 1) {
			cerr << "This isn't a scalar attribute" << endl;
			continue;
		}
		if(attrIter->second.data == 0) //attribute not loaded
			sim->loadAttribute(simIter->first, attrIter->first, simIter->second.count.numParticles, simIter->second.count.startParticle);
		switch(attrIter->second.code) {
			case int8:
				sum += sumAttribute<Code2Type<int8>::type>(attrIter->second, iter, end); break;
			case uint8:
				sum += sumAttribute<Code2Type<uint8>::type>(attrIter->second, iter, end); break;
			case int16:
				sum += sumAttribute<Code2Type<int16>::type>(attrIter->second, iter, end); break;
			case uint16:
				sum += sumAttribute<Code2Type<uint16>::type>(attrIter->second, iter, end); break;
			case int32:
				sum += sumAttribute<Code2Type<int32>::type>(attrIter->second, iter, end); break;
			case uint32:
				sum += sumAttribute<Code2Type<uint32>::type>(attrIter->second, iter, end); break;
			case int64:
				sum += sumAttribute<Code2Type<int64>::type>(attrIter->second, iter, end); break;
			case uint64:
				sum += sumAttribute<Code2Type<uint64>::type>(attrIter->second, iter, end); break;
			case float32:
				sum += sumAttribute<Code2Type<float32>::type>(attrIter->second, iter, end); break;
			case float64:
				sum += sumAttribute<Code2Type<float64>::type>(attrIter->second, iter, end); break;
		}
	}
	delete pred;
	contribute(sizeof(double), &sum, CkReduction::sum_double, cb);
}
/*/
void Worker::getAttributeSum(const string& groupName, const string& attributeName, const CkCallback& cb) {
	double sum = 0;
	GroupMap::iterator gIter = groups.find(groupName);
	if(gIter != groups.end()) {
		shared_ptr<SimulationHandling::Group>& g = gIter->second;
		for(SimulationHandling::Group::GroupFamilies::iterator famIter = g->families.begin(); famIter != g->families.end(); ++famIter) {
			Simulation::iterator simIter = sim->find(*famIter);
			TypedArray& arr = simIter->second.attributes[attributeName];
			//only makes sense for scalar values
			if(arr.dimensions != 1) {
				cerr << "This isn't a scalar attribute" << endl;
				continue;
			}
			if(arr.data == 0) //attribute not loaded
				sim->loadAttribute(*famIter, attributeName, simIter->second.count.numParticles, simIter->second.count.startParticle);
			GroupIterator iter = g->make_begin_iterator(*famIter);
			GroupIterator end = g->make_end_iterator(*famIter);
			switch(arr.code) {
				case int8:
					sum += sumAttribute<Code2Type<int8>::type>(arr, iter, end); break;
				case uint8:
					sum += sumAttribute<Code2Type<uint8>::type>(arr, iter, end); break;
				case int16:
					sum += sumAttribute<Code2Type<int16>::type>(arr, iter, end); break;
				case uint16:
					sum += sumAttribute<Code2Type<uint16>::type>(arr, iter, end); break;
				case int32:
					sum += sumAttribute<Code2Type<int32>::type>(arr, iter, end); break;
				case uint32:
					sum += sumAttribute<Code2Type<uint32>::type>(arr, iter, end); break;
				case int64:
					sum += sumAttribute<Code2Type<int64>::type>(arr, iter, end); break;
				case uint64:
					sum += sumAttribute<Code2Type<uint64>::type>(arr, iter, end); break;
				case float32:
					sum += sumAttribute<Code2Type<float32>::type>(arr, iter, end); break;
				case float64:
					sum += sumAttribute<Code2Type<float64>::type>(arr, iter, end); break;
			}
		}
	}
	contribute(sizeof(double), &sum, CkReduction::sum_double, cb);
}

void Worker::getCenterOfMass(const string& groupName, const CkCallback& cb) {
	cerr << "Worker " << thisIndex << ": Trying to get com" << endl;
	pair<double, Vector3D<double> > compair;
	GroupMap::iterator gIter = groups.find(groupName);
	if(gIter != groups.end()) {
		cerr << "Worker " << thisIndex << ": Found group" << endl;
		shared_ptr<SimulationHandling::Group>& g = gIter->second;
		for(SimulationHandling::Group::GroupFamilies::iterator famIter = g->families.begin(); famIter != g->families.end(); ++famIter) {
			ParticleFamily& family = (*sim)[*famIter];
			Vector3D<float>* positions = family.getAttribute("position", Type2Type<Vector3D<float> >());
			if(positions == 0) {
				cerr << "Worker " << thisIndex << " Null positions!" << endl;
				return;
			}
			AttributeMap::iterator attrIter = family.attributes.find("mass");
			if(attrIter == family.attributes.end())
				cerr << "No Masses!" << endl;
			if(attrIter->second.length == 0)
				sim->loadAttribute(*famIter, "mass", family.count.numParticles, family.count.startParticle);
			float* masses = family.getAttribute("mass", Type2Type<float>());
			if(masses == 0)
				cerr << "Masses pointer null!" << endl;
			GroupIterator iter = g->make_begin_iterator(*famIter);
			GroupIterator end = g->make_end_iterator(*famIter);
			for(; iter != end; ++iter) {
				compair.first += masses[*iter];
				compair.second += masses[*iter ] * positions[*iter];
			}
		}
	}

	contribute(sizeof(compair), &compair, pairDoubleVector3DSum, cb);
}

void Worker::createGroup_Family(std::string const& groupName, std::string const& parentGroupName, std::string const& familyName, CkCallback const& cb) {
	int result = 0;
	//parent group idiom: look up parent group by name
	boost::shared_ptr<SimulationHandling::Group>& parentGroup = groups[groups.find(parentGroupName) != groups.end() ? parentGroupName: "All"];
	if(sim->find(familyName) != sim->end()) {
		groups[groupName] = boost::shared_ptr<SimulationHandling::Group>(new FamilyGroup(*sim, parentGroup, familyName));
		result = 1;
	}
	contribute(sizeof(result), &result, CkReduction::logical_and, cb);
}

void Worker::createGroup_AttributeRange(std::string const& groupName, std::string const& parentGroupName, std::string const& attributeName, double minValue, double maxValue, CkCallback const& cb) {
	int result = 0;
	//parent group idiom: look up parent group by name
	boost::shared_ptr<SimulationHandling::Group>& parentGroup = groups[groups.find(parentGroupName) != groups.end() ? parentGroupName: "All"];
	boost::shared_ptr<SimulationHandling::Group> g = make_AttributeRangeGroup(*sim, parentGroup, attributeName, minValue, maxValue);
	if(g) {
		groups[groupName] = g;
		result = 1;
	}
	contribute(sizeof(result), &result, CkReduction::logical_and, cb);
}

void Worker::createGroup_AttributeSphere(std::string const& groupName,
					 std::string const& parentGroupName,
					 std::string const& attributeName,
					 Vector3D<double> center, double size,
					 CkCallback const& cb) {
	int result = 0;
	boost::shared_ptr<SimulationHandling::Group>& parentGroup = groups[groups.find(parentGroupName) != groups.end() ? parentGroupName: "All"];
	boost::shared_ptr<SimulationHandling::Group> g = make_SphericalGroup(*sim, parentGroup, attributeName, center, size);
	if(g) {
		groups[groupName] = g;
		result = 1;
	}
	contribute(sizeof(result), &result, CkReduction::logical_and, cb);
}

// Below is a test of Filippo's python integrator

void Worker::localParticleCode(std::string s, const CkCallback &cb) 
{
    CkCcsRequestMsg *msg=new (s.length(),0) CkCcsRequestMsg;

    msg->length=s.length()+1; // make sure final NULL gets copied
    memcpy(msg->data, s.c_str(), s.length()+1);

    iterate(msg);
    contribute(0, 0, CkReduction::concat, cb); // barrier
    }

int Worker::buildIterator(PyObject *arg, void *iter) {
    GroupMap::iterator gIter = groups.find("All");

    if(gIter == groups.end())
	return 0;
    localPartG = gIter->second;
    localPartFamIter = localPartG->families.begin();
    if(localPartFamIter == localPartG->families.end())
	return 0;
    localPartIter = localPartG->make_begin_iterator(*localPartFamIter);
    localPartEnd = localPartG->make_end_iterator(*localPartFamIter);
    if(localPartIter == localPartEnd)
	return 0;

    ParticleFamily& family = (*sim)[*localPartFamIter];
    
    for(AttributeMap::iterator attrIter = family.attributes.begin();
	attrIter != family.attributes.end(); attrIter++) {
	TypedArray& arr = attrIter->second;
	if(arr.dimensions != 1)
	    continue;
	if(arr.data == 0) //attribute not loaded
	    sim->loadAttribute(*localPartFamIter, attrIter->first, family.count.numParticles, family.count.startParticle);
	switch(arr.code) {
	case int32:
	    pythonSetInt(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<int>())[*localPartIter]);
	    break;
	case float32:
	    pythonSetFloat(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<float>())[*localPartIter]);
	    break;
	case float64:
	    pythonSetFloat(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<double>())[*localPartIter]);
	    break;
	default:
	    assert(0);
	    }
	}
    return 1;
    }

int Worker::nextIteratorUpdate(PyObject *arg, PyObject *result, void *iter) {
    // Copy out from Python object

    ParticleFamily& family = (*sim)[*localPartFamIter];
    
    for(AttributeMap::iterator attrIter = family.attributes.begin();
	attrIter != family.attributes.end(); attrIter++) {
	TypedArray& arr = attrIter->second;
	if(arr.dimensions != 1)
	    continue;
	long lvalue; // Python only does longs
	double dvalue;
	
	switch(arr.code) {
	case int32:
	    pythonGetInt(arg, (char *) attrIter->first.c_str(), &lvalue);
	    arr.getArray(Type2Type<int>())[*localPartIter] = lvalue;
	    break;
	case float32:
	    pythonGetFloat(arg, (char *) attrIter->first.c_str(), &dvalue);
	    arr.getArray(Type2Type<float>())[*localPartIter] = dvalue;
	    break;
	case float64:
	    pythonGetFloat(arg, (char *) attrIter->first.c_str(), &arr.getArray(Type2Type<double>())[*localPartIter]);
	    break;
	default:
	    assert(0);
	    }
	}
    // Increment
    localPartIter++;
    if(localPartIter == localPartEnd) {
	localPartFamIter++;
	if(localPartFamIter == localPartG->families.end()) {
	    return 0;
	    }
	localPartIter = localPartG->make_begin_iterator(*localPartFamIter);
	localPartEnd = localPartG->make_end_iterator(*localPartFamIter);
	family = (*sim)[*localPartFamIter];
	}

    // Stuff new values in
    
    for(AttributeMap::iterator attrIter = family.attributes.begin();
	attrIter != family.attributes.end(); attrIter++) {
	TypedArray& arr = attrIter->second;
	if(arr.dimensions != 1)
	    continue;
	switch(arr.code) {
	case int32:
	    pythonSetInt(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<int>())[*localPartIter]);
	    break;
	case float32:
	    pythonSetFloat(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<float>())[*localPartIter]);
	    break;
	case float64:
	    pythonSetFloat(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<double>())[*localPartIter]);
	    break;
	default:
	    assert(0);
	    }
	}
    return 1;
    }
