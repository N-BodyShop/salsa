/** @file Worker.cpp
 */

#include <utility>
#include <cstdlib>
#include <vector>
#include <set>

#include "tree_xdr.h"
#include "SiXFormat.h"
#include "TipsyFormat.h"

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
	//cout << "Simulation " << sim->name << " contains " << sim->size() << " families" << endl;
	
	boundingBox = OrientedBox<float>();
	
	//load appropriate positions
	for(Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter) {
		//cout << "Family name: " << iter->first << endl;
		u_int64_t totalNumParticles = iter->second.count.totalNumParticles;
		u_int64_t numParticles = totalNumParticles / CkNumPes();
		u_int64_t leftover = totalNumParticles % CkNumPes();
		u_int64_t startParticle = CkMyPe() * numParticles;
		if(CkMyPe() < leftover) {
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
	colorings.push_back(c);
	startColor = familyColor;
	
	groupNames.push_back("All");
	activeGroupName = "All";
	
	drawVectors = false;
	vectorScale = 0.01;
	
	contribute(sizeof(OrientedBox<float>), &boundingBox, growOrientedBox_float, cb);
}

const byte lineColor = 1;

#include "PixelDrawing.cpp"

#include <boost/iterator/filter_iterator.hpp>
#include <boost/iterator/counting_iterator.hpp>
using namespace boost;

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
	
	for(Simulation::iterator simIter = sim->begin(); simIter != sim->end(); ++simIter) {
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
	/*
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
	*/
	//double stop = CkWallTimer();
	liveVizDeposit(m, 0, 0, req.width, req.height, image, this, max_image_data);
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
							if(potentials[i] < minPotential) {
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
	for(int i = 0; i < colorings.size(); ++i) {
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
