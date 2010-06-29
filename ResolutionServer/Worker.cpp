/** @file Worker.cpp
 */

#include <inttypes.h>

#include <utility>
#include <cstdlib>
#include <vector>
#include <set>
#include <assert.h>
#include <numeric>

#include <boost/bind.hpp>
#include <boost/iterator/filter_iterator.hpp>
#include <boost/iterator/counting_iterator.hpp>

#include "config.h"
#include "tree_xdr.h"
#include "SiXFormat.h"
#include "TipsyFormat.h"
#include "SPH_Kernel.h"
#include "Interpolate.h"
#include "Group.h"

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
	
	try {
	    sim = new SiXFormatReader(simulationName);
	    }
	catch (FileError &e) {
	    cerr << "File Problem:" << e.getText() << endl;
	    }
	    
	if(sim == NULL || sim->size() == 0) {
		//try plain tipsy format
		if(sim) {
		    sim->release();
		    delete sim;
		    }
		sim = new TipsyFormatReader(simulationName);
		if(!sim || sim->size() == 0)
			cerr << "Couldn't load simulation file (tried new format and plain tipsy)" << endl;
	}
	
	boundingBox = OrientedBox<float>();
	
	//load appropriate positions
	for(Simulation::iterator iter = sim->begin(); iter != sim->end(); ++iter) {
		u_int64_t totalNumParticles = iter->second.count.totalNumParticles;
		u_int64_t numParticles = totalNumParticles / CkNumPes();
		u_int64_t leftover = totalNumParticles % CkNumPes();
		u_int64_t startParticle = CkMyPe() * numParticles;
		if(CkMyPe() < (int) leftover) {
			numParticles++;
			startParticle += CkMyPe();
		} else
			startParticle += leftover;
		
		cerr << "loading " << totalNumParticles << endl;
		
		try {
		    sim->loadAttribute(iter->first, "position", numParticles,
				       startParticle);
		    }
		catch(NameError &e) {
		    cerr << CkMyPe() << ": Loading positions failed for family: "
			 << iter->second.familyName << ":" << e.getText() << endl;
		    iter->second.count.numParticles = 0;
		    continue;
		    }
		catch(FileError &e) {
		    cerr << CkMyPe() << ": Loading positions failed for family:"
			 << iter->second.familyName << e.getText() << endl;
		    iter->second.count.numParticles = 0;
		    continue;
		    }
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

static int readFamilyArray(FILE *fp,
			    const std::string & attributeName,
			    ParticleFamily &second) 
{
	AttributeMap::iterator attrIter = second.attributes.find(attributeName);
	if(attrIter == second.attributes.end()) {
	    // add attribute to family
	    float *array = new float[second.count.numParticles];
	    second.addAttribute(attributeName, array);
	    attrIter = second.attributes.find(attributeName);
	    }
	
	float* array = attrIter->second.getArray(Type2Type<float >());
	for(unsigned int i = 0; i < second.count.numParticles; i++) {
	    if(fscanf(fp, "%f", &array[i]) == EOF) {
		ckerr << "<Sorry, file format is wrong>\n" ;
		return 0;
		}
	    }
	attrIter->second.calculateMinMax();
	return 1;
    }

void Worker::readTipsyArray(const std::string& fileName,
			    const std::string& attributeName,
			    long off,
			    int iType, // 0 -> gas, 1 -> dark, 2-> star
			    const CkCallback& cb) {
    FILE *fp = fopen(fileName.c_str(), "r");
    StatusMsg *msg;
    unsigned int nTotal;
    if((thisIndex == 0) && (iType == 0)) {
	int count = fscanf(fp, "%d%*[, \t]%*d%*[, \t]%*d",&nTotal) ;
	if ( (count == EOF) || (count==0) ){
	    ckerr << "<Sorry, file format is wrong>\n" ;
	    fclose(fp);
	    msg = new StatusMsg(0);
	    cb.send(msg);
	    return;
	    }
	if(nTotal != sim->totalNumParticles()) {
	    ckerr << "Wrong number of particles" << endl;
	    ckerr << "Expected: " << sim->totalNumParticles() << ", got: "
		  << nTotal << endl;
	    msg = new StatusMsg(0);
	    cb.send(msg);
	    return;
	    }
	}
    else {
	fseek(fp, off, SEEK_SET);
	}
    
    ckout << "[" << thisIndex << "]: reading Array ... ";

    Simulation::iterator family;
    switch(iType) {
    case 0:
	family = sim->find("gas");
	break;
    case 1:
	family = sim->find("dark");
	break;
    case 2:
	family = sim->find("star");
	break;
    default:
	CkAssert("bad type");
	}
    
    if(family != sim->end()) {
	if(readFamilyArray(fp, attributeName, family->second) == 0) {
	    msg = new StatusMsg(0);
	    cb.send(msg);
	    fclose(fp);
	    return;
	    }
	}
    
    ckout << "Done\n";
    if(thisIndex+1 < CkNumPes()) {
	long offset = ftell(fp);
	
	CProxy_Worker workers(thisArrayID);
	workers[thisIndex+1].readTipsyArray(fileName, attributeName, offset,
					    iType, cb);
	}
    else {
	if(iType < 2) { // Do next type
	    long offset = ftell(fp);
	    CProxy_Worker workers(thisArrayID);
	    workers[0].readTipsyArray(fileName, attributeName, offset,
				      iType+1, cb);
	    
	    }
	else{
	    msg = new StatusMsg(1); // Success
	    cb.send(msg);
	    }
	}
    fclose(fp);
    }

void Worker::writeIndexes(const std::string& groupName,
                 const std::string& familyName,
			     const std::string& fileName,
			     const CkCallback& cb)
{
    GroupMap::iterator gIter = groups.find(groupName);
    
    FILE *fp;
    fp = fopen(fileName.c_str(), "a");
    
    StatusMsg *msg;
    
    if (gIter != groups.end()) {
        shared_ptr<SimulationHandling::Group>& g = gIter->second;
	    // write out index values
        ParticleFamily& family = (*sim)[familyName];
		AttributeMap::iterator attrIter = family.attributes.find("index");
		if(attrIter == family.attributes.end())
			cerr << "No Indexes!" << endl;
		if(attrIter->second.length == 0)
			sim->loadAttribute(familyName, "index", family.count.numParticles, family.count.startParticle);
		int64_t* indexes = family.getAttribute("index", Type2Type<int64_t>());
		if(indexes == 0)
			cerr << "Indexes pointer null!" << endl;
		GroupIterator iter = g->make_begin_iterator(familyName);
		GroupIterator end = g->make_end_iterator(familyName);
		for(; *iter != *end; ++iter) {
			fprintf(fp, "%ld\n", indexes[*iter]);
		}
	}
	fclose(fp);
	
    if (thisIndex+1 < CkNumPes()) {
        CProxy_Worker workers(thisArrayID);
        workers[thisIndex+1].writeIndexes(groupName, familyName, fileName, cb);
    } else {
        msg = new StatusMsg(1); // Success
    	cb.send(msg);
    }
}

void Worker::writeGroupArray(const std::string& groupName,
			     const std::string& attributeName,
			     const std::string& fileName,
			     const CkCallback& cb)
{
    FILE *fp;
    StatusMsg *msg;
    
    if(thisIndex == 0) {
	fp = fopen(fileName.c_str(), "w");
	fprintf(fp, "%ld\n", sim->totalNumParticles());
	}
    else {
	fp = fopen(fileName.c_str(), "a");
	}
    GroupMap::iterator gIter = groups.find(groupName);
    if (gIter != groups.end()) {
        shared_ptr<SimulationHandling::Group>& g = gIter->second;
    
	for(SimulationHandling::Group::GroupFamilies::iterator famIter = g->families.begin(); famIter != g->families.end(); ++famIter) {
	    GroupIterator iter = g->make_begin_iterator(*famIter);
	    GroupIterator end = g->make_end_iterator(*famIter);
	    ParticleFamily& family = (*sim)[*famIter];
	    float* array = family.getAttribute(attributeName,
					       Type2Type<float >());
	    if(array == NULL) {
		for(unsigned int i = 0; i < family.count.numParticles; i++) {
		fprintf(fp, "0\n");  // zero fill if no values
		}
	    }
	    else {
	    // XXX take care of type handling
		for(; *iter != *end; ++iter) {
		    fprintf(fp, "%g\n", array[*iter]);
		    }
	    }
	    }
	}
    fclose(fp);
    if (thisIndex+1 < CkNumPes()) {
        CProxy_Worker workers(thisArrayID);
        workers[thisIndex+1].writeGroupArray(groupName, attributeName, fileName, cb);
    } else {
        msg = new StatusMsg(1); // Success
    	cb.send(msg);
    }
}


const byte lineColor = 1;

#include "PixelDrawing.cpp"

#if 0
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

#endif

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

// Draw the image using this chare's particles

void Worker::generateImage(liveVizRequestMsg* m) {
	
	MyVizRequest req;
	liveVizRequestUnpack(m, req);
	bool doVolumeRender=false; /* if false, 2D image; if true, 3D volume */
	
	if(verbosity > 2 && thisIndex == 0)
		cout << "Worker " << thisIndex << ": Image request: " << req << endl;
		
	if(imageSize < (unsigned int) (req.width * req.height)) {
		delete[] image;
		imageSize = req.width * req.height;
		image = new byte[imageSize];
	}
	memset(image, 0, req.width * req.height);
	
	if (req.height==req.width*req.width) doVolumeRender=true;

	MetaInformationHandler* meta = metaProxy.ckLocalBranch();
	if(!meta) {
		cerr << "ResolutionServer Worker.cpp error: Couldn't get local pointer to meta handler" << endl;
		return;
	}
	
	double delta = 2 * req.x.length() / req.width;
	if(verbosity > 3 && thisIndex == 0)
		cout << "Pixel size: " << delta << " x " << (2 * req.y.length() / req.height) << endl;
	req.x /= req.x.lengthSquared();
	req.y /= req.y.lengthSquared();
	req.z /= req.z.lengthSquared();
	double x, y;
	unsigned int pixel;
	
	if(verbosity > 2 && thisIndex == 0) {
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
		if(positions == NULL) {
		    CkError(family.familyName.c_str());
		    CkError(":Family has no positions\n");
		    continue;
		    }
		
		byte* colors = family.getAttribute(coloringPrefix + c.name, Type2Type<byte>());
		//if the color doesn't exist, use the family color
		if(colors == 0)
			colors = family.getAttribute(coloringPrefix + "Family Colors", Type2Type<byte>());
		assert(colors != 0);
		
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
			for(; *iter != *end; ++iter) {
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
			for(; *iter != *end; ++iter) {
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
		  if (doVolumeRender) 
		  { /* 3D volume render */
		  	int SX=req.width, SY=req.width, SZ=req.width; /* size, pixels, along X, Y, Z */
			std::cout<<"Volume render dimensions: "<<SX<<"x"<<SY<<"x"<<SZ<<"\n";
			float hX=SX*0.5, hY=SY*0.5, hZ=SZ*0.5;
			Vector3D<float> xAxis=req.x*hX, yAxis=req.y*hY, zAxis=req.z*hZ; /* pixel axes */
			float xo=(dot(req.x,req.o)-1)*hX, yo=(dot(req.y,req.o)-1)*hY, zo=(dot(req.z,req.o)-1)*hZ; /* pixel origin */
			
			for(; *iter != *end; ++iter) {
				x=dot(xAxis,positions[*iter])-xo;
				if (x<0 || x>=SX) continue; /* point is offscreen: skip */
				y=dot(yAxis,positions[*iter])-yo;
				if (y<0 || y>=SY) continue;
				float z=dot(zAxis,positions[*iter])-zo;
				if (z<0 || z>=SZ) continue;
				pixel = (unsigned int) ((int)x + SX *((int)y + SY * (int)z));
				if(image[pixel] < colors[*iter])
				        image[pixel] = colors[*iter];
			}
		  } else 
		  { /* 2D image render only */
			float w=req.width/2, h=req.height/2;
			Vector3D<float> xAxis=req.x*w, yAxis=req.y*h; /* pixel axes */
			float xo=(dot(req.x,req.o)-1)*w, yo=(dot(req.y,req.o)-1)*h; /* pixel origin */
			// Only needed for clipping planes
			// float zo=dot(req.z,req.o)-1;
			for(; *iter != *end; ++iter) {
				x=dot(xAxis,positions[*iter])-xo;
				if (x<0 || x>=req.width) continue; /* point is offscreen: skip */
				y=dot(yAxis,positions[*iter])-yo;
				if (y<0 || y>=req.height) continue;

				// No z clipping planes for now
				// float z=dot(req.z,positions[*iter])-zo;
				// if (z<0 || z>=2.0) continue;
				
				if(req.radius == 0) {
				    pixel = (unsigned int) ((int)x + req.width * (int)(y));
					if(image[pixel] < colors[*iter])
				        	image[pixel] = colors[*iter];
				} else
				    drawDisk(image, req.width, req.height, (int)x, (int)y, req.radius, colors[*iter]);
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
						case TypeHandling::int32:
							assignColors<int>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case TypeHandling::uint32:
							assignColors<unsigned int>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case TypeHandling::int64:
							assignColors<int64_t>(attrIter->second.dimensions, colors, values, iter->second.count.numParticles, c.minValue, c.maxValue, c.beLogarithmic, c.clip);
							break;
						case TypeHandling::uint64:
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
	boost::shared_ptr<SimulationHandling::Group> g(groups[req.activeGroup]);
	
	req.centerFindingMethod = 2;  // potential only for now
	Coloring& c = colorings[req.coloring];
	
	switch(req.centerFindingMethod) {
	case 0: { //average of all pixels in frame	
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
		for(; *iter != *end; ++iter) {
		    x = dot(req.x, positions[*iter] - req.o);
		    if(x > -1 && x < 1) {
			y = dot(req.y, positions[*iter] - req.o);
			if(y > -1 && y < 1) {
			    numParticlesInFrame++;
			    z += dot(req.z, positions[*iter] - req.o);
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
		for(; *iter != *end; ++iter) {
		    x = dot(req.x, positions[*iter] - req.o);
		    if(x > -1 && x < 1) {
			y = dot(req.y, positions[*iter] - req.o);
			if(y > -1 && y < 1) {
			    if(colors[*iter] > maxPixel) {
				maxPixel = colors[*iter];
				z = dot(req.z, positions[*iter] - req.o);
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
		if(positions == NULL) {
		    CkError(family.familyName.c_str());
		    CkError(":Family has no positions or a bad type\n");
		    continue;
		    }
		float* potentials = family.getAttribute("potential",
							Type2Type<float>());
		if(potentials == 0) {
		    try {
			sim->loadAttribute(*famIter, "potential", family.count.numParticles, family.count.startParticle);
			potentials = family.getAttribute("potential", Type2Type<float>());
			}
		    catch(NameError &) {
			cerr << "Warning no potentials" << endl;
			}
		    }
		for(; *iter != *end; ++iter) {
		    x = dot(req.x, positions[*iter] - req.o);
		    if(x > -1 && x < 1) {
			y = dot(req.y, positions[*iter] - req.o);
			if(y > -1 && y < 1) {
			    if(potentials && potentials[*iter] < minPotential) {
				minPotential = potentials[*iter];
				z = dot(req.z, positions[*iter] - req.o);
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

// count particles in a given group

void Worker::getNumParticlesGroup(const std::string &groupName,
			  const std::string &familyName,
				  const CkCallback &cb)
{
    GroupMap::iterator gIter = groups.find(groupName);
    int64_t i = 0;

    if(gIter != groups.end()) {
	shared_ptr<SimulationHandling::Group>& g = gIter->second;
	GroupIterator iter = g->make_begin_iterator(familyName);
	GroupIterator end = g->make_end_iterator(familyName);
	for(i = 0; *iter < *end; iter++)
	    i++;
	}
    contribute(sizeof(int64_t), &i, sum_int64, cb);
    }
		

template <typename T, typename IteratorType>
double minAttribute(TypedArray const& arr, IteratorType begin, IteratorType end,
		    u_int64_t &itMin) {
	double min = HUGE_VAL;
	itMin = *begin;
	
	T const* array = arr.getArray(Type2Type<T>());
	if(array == 0)
		return min;
	for(; *begin != *end; ++begin)
	    if(array[*begin] < min) {
		min = array[*begin];
		itMin = *begin;
		}
	return min;
}

// @brief Find the minimum of an attribute array and the index it
// cooresponds to.

void Worker::findAttributeMin(const string& groupName, const string& attributeName, const CkCallback& cb) {
    double aMin = HUGE_VAL;
    pair<double, Vector3D<double> > compair;
    
	GroupMap::iterator gIter = groups.find(groupName);
	if(gIter != groups.end()) {
		shared_ptr<SimulationHandling::Group>& g = gIter->second;
		for(SimulationHandling::Group::GroupFamilies::iterator famIter = g->families.begin(); famIter != g->families.end(); ++famIter) {
		    double min = HUGE_VAL;
		    u_int64_t itGMin;
		    
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
					min = minAttribute<Code2Type<int8>::type>(arr, iter, end, itGMin); break;
				case uint8:
					min = minAttribute<Code2Type<uint8>::type>(arr, iter, end, itGMin); break;
				case int16:
					min = minAttribute<Code2Type<int16>::type>(arr, iter, end, itGMin); break;
				case uint16:
					min = minAttribute<Code2Type<uint16>::type>(arr, iter, end, itGMin); break;
				case TypeHandling::int32:
					min = minAttribute<Code2Type<TypeHandling::int32>::type>(arr, iter, end, itGMin); break;
				case TypeHandling::uint32:
					min = minAttribute<Code2Type<TypeHandling::uint32>::type>(arr, iter, end, itGMin); break;
				case TypeHandling::int64:
					min = minAttribute<Code2Type<TypeHandling::int64>::type>(arr, iter, end, itGMin); break;
				case TypeHandling::uint64:
					min = minAttribute<Code2Type<TypeHandling::uint64>::type>(arr, iter, end, itGMin); break;
				case float32:
					min = minAttribute<Code2Type<float32>::type>(arr, iter, end, itGMin); break;
				case float64:
					min = minAttribute<Code2Type<float64>::type>(arr, iter, end, itGMin); break;
			}
			if(min < aMin) {
			    aMin = min;
			    ParticleFamily& family = (*sim)[*famIter];
			    Vector3D<float>* positions = family.getAttribute("position", Type2Type<Vector3D<float> >());
			    compair.first = min;
			    compair.second = positions[itGMin];
			    }
		}
		
	}
	contribute(sizeof(compair), &compair, pairDoubleVector3DMin, cb);
}

// XXX This should be replaced with python functionality
// @brief returns property list of family and attribute information

void Worker::getAttributeInformation(CkCcsRequestMsg* m) {	
	ostringstream oss;
	if(sim == NULL) {
		oss << "Error: no simulation loaded\n" ;
		string result(oss.str());
		CcsSendDelayedReply(m->reply, result.length(), result.c_str());
		delete m;
		return;
		}
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

template <typename T, typename IteratorType>
void minmaxAttribute(TypedArray const& arr, IteratorType begin,
		     IteratorType end, double* minmax) {
	T const* array = arr.getArray(Type2Type<T>());
	if(array == 0)
	    return;
	for(; *begin != *end; ++begin) {
	    if(array[*begin] < minmax[0])
		minmax[0] = array[*begin];
	    if(array[*begin] > minmax[1])
		minmax[1] = array[*begin];
	    }
	return;
}

void Worker::saveSimulation(const std::string& path, const CkCallback& cb)
{
  CProxy_Worker workers(thisArrayID);
  SiXFormatWriter simwriter;
  int result = simwriter.save(sim,path,thisIndex);
  if(thisIndex < CkNumPes()) workers[thisIndex+1].saveSimulation(path, cb);
  contribute(sizeof(result),&result,CkReduction::logical_and,cb);
}

/*
 * fancy double macro to do the switch amoung types.  It gives us a
 * constant to use for the template instantiation.
 * To use, first define a caseCode2Type macro whose first argument is
 * a variable of type TypeHandling::DataTypeCode, the second is the
 * actual type it maps to.
 */
#define casesCode2Types \
    caseCode2Type(int8, char)	\
    caseCode2Type(uint8, unsigned char)	\
    caseCode2Type(int16, short)	\
    caseCode2Type(uint16, unsigned short)	\
    caseCode2Type(int32, int)	\
    caseCode2Type(uint32, unsigned int)	\
    caseCode2Type(int64, int64_t)	\
    caseCode2Type(uint64, u_int64_t)	\
    caseCode2Type(float32, float)	\
    caseCode2Type(float64, double)	\
    default: assert(0);
    
    
void Worker::getAttributeRangeGroup(const std::string& groupName,
				    const std::string& familyName,
				    const std::string& attributeName,
				    const CkCallback& cb)
{
    double minmax[2] = {HUGE, -HUGE};
    GroupMap::iterator gIter = groups.find(groupName);
    if(gIter != groups.end()) {
	shared_ptr<SimulationHandling::Group>& g = gIter->second;
	Simulation::iterator famIter = sim->find(familyName);
	if(famIter != sim->end()) {
	    TypedArray& arr = famIter->second.attributes[attributeName];
	    //only makes sense for scalar values
	    if(arr.dimensions != 1) {
		cerr << "This isn't a scalar attribute" << endl;
		contribute(2*sizeof(double), &minmax[0], minmax_double, cb);
		return;
		}
	    if(arr.data == 0) //attribute not loaded
		sim->loadAttribute(familyName, attributeName,
				   famIter->second.count.numParticles,
				   famIter->second.count.startParticle);
	    GroupIterator iter = g->make_begin_iterator(familyName);
	    GroupIterator end = g->make_end_iterator(familyName);
	    switch(arr.code) {
#define caseCode2Type(enumName,typeName) \
	    case enumName: \
		minmaxAttribute<typeName>(arr, iter, end, &minmax[0]); \
	    break;
	    
	    casesCode2Types
		}
	    }
	}
	contribute(2*sizeof(double), &minmax[0], minmax_double, cb);
    }

// Useful template for below
template <typename T, typename IteratorType>
void boundboxAttribute(TypedArray const& arr, IteratorType begin,
		    IteratorType end, OrientedBox<double> *bound) {

    Vector3D<T> const* array = arr.getArray(Type2Type<Vector3D<T> >());
    if(array == 0)
	return;
    for(; *begin != *end; ++begin) {
	bound->grow(array[*begin]);
	}
    return;
}

// find bounding box of a vector attribute

void Worker::getVecAttributeRangeGroup(const std::string& groupName,
				    const std::string& familyName,
				    const std::string& attributeName,
				    const CkCallback& cb)
{
    OrientedBox<double> bounds = OrientedBox<double>();
    GroupMap::iterator gIter = groups.find(groupName);
    if(gIter != groups.end()) {
	shared_ptr<SimulationHandling::Group>& g = gIter->second;
	Simulation::iterator famIter = sim->find(familyName);
	if(famIter != sim->end()) {
	    TypedArray& arr = famIter->second.attributes[attributeName];
	    //only makes sense for scalar values
	    if(arr.dimensions != 3) {
		cerr << "This isn't a vector attribute" << endl;
		contribute(sizeof(bounds), &bounds, growOrientedBox_double, cb);
		return;
		}
	    if(arr.data == 0) //attribute not loaded
		sim->loadAttribute(familyName, attributeName,
				   famIter->second.count.numParticles,
				   famIter->second.count.startParticle);
	    GroupIterator iter = g->make_begin_iterator(familyName);
	    GroupIterator end = g->make_end_iterator(familyName);
	    switch(arr.code) {
#undef caseCode2Type
#define caseCode2Type(enumName,typeName) \
	    case enumName: \
		boundboxAttribute<typeName>(arr, iter, end, &bounds); \
	    break;
	    
	    casesCode2Types
		}
	    }
	}
	contribute(sizeof(bounds), &bounds, growOrientedBox_double, cb);
    }

template <typename T, typename IteratorType>
double sumAttribute(TypedArray const& arr, IteratorType begin, IteratorType end) {
	double sum = 0;
	T const* array = arr.getArray(Type2Type<T>());
	if(array == 0)
		return 0;
	for(; *begin != *end; ++begin)
		sum += array[*begin];
	return sum;
}

void Worker::getAttributeSum(const string& groupName,
			     const string& familyName,
			     const string& attributeName,
			     const CkCallback& cb) {
	double sum = 0;
	GroupMap::iterator gIter = groups.find(groupName);
	if(gIter != groups.end()) {
		shared_ptr<SimulationHandling::Group>& g = gIter->second;
		Simulation::iterator famIter = sim->find(familyName);
		if(famIter != sim->end()) {
		    TypedArray& arr = famIter->second.attributes[attributeName];
		    //only makes sense for scalar values
		    if(arr.dimensions != 1) {
			cerr << "This isn't a scalar attribute" << endl;
			contribute(sizeof(double), &sum, CkReduction::sum_double, cb);		return;
		    }
		    if(arr.data == 0) //attribute not loaded
			sim->loadAttribute(familyName, attributeName,
					   famIter->second.count.numParticles,
					   famIter->second.count.startParticle);
		    GroupIterator iter = g->make_begin_iterator(familyName);
		    GroupIterator end = g->make_end_iterator(familyName);
		    switch(arr.code) {
				case int8:
					sum += sumAttribute<Code2Type<int8>::type>(arr, iter, end); break;
				case uint8:
					sum += sumAttribute<Code2Type<uint8>::type>(arr, iter, end); break;
				case int16:
					sum += sumAttribute<Code2Type<int16>::type>(arr, iter, end); break;
				case uint16:
					sum += sumAttribute<Code2Type<uint16>::type>(arr, iter, end); break;
				case TypeHandling::int32:
					sum += sumAttribute<Code2Type<TypeHandling::int32>::type>(arr, iter, end); break;
				case TypeHandling::uint32:
					sum += sumAttribute<Code2Type<TypeHandling::uint32>::type>(arr, iter, end); break;
				case TypeHandling::int64:
					sum += sumAttribute<Code2Type<TypeHandling::int64>::type>(arr, iter, end); break;
				case TypeHandling::uint64:
					sum += sumAttribute<Code2Type<TypeHandling::uint64>::type>(arr, iter, end); break;
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
	pair<double, Vector3D<double> > compair;
	GroupMap::iterator gIter = groups.find(groupName);
	if(gIter != groups.end()) {
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
			for(; *iter != *end; ++iter) {
				compair.first += masses[*iter];
				compair.second += masses[*iter ] * positions[*iter];
			}
		}
	}

	contribute(sizeof(compair), &compair, pairDoubleVector3DSum, cb);
}

void Worker::createScalarAttribute(std::string const& familyName, std::string const& attributeName, CkCallback const& cb) 
{
    int result = 1;
    float* empty;
    
    Simulation::iterator simIter = sim->find(familyName);
    empty = new float[simIter->second.count.numParticles];
    
    simIter->second.addAttribute(attributeName, TypedArray(empty, simIter->second.count.numParticles));
    contribute(sizeof(result), &result, CkReduction::logical_and, cb);
}

//CC 4/1/07
void Worker::createVectorAttribute(std::string const& familyName, std::string const& attributeName, CkCallback const& cb) 
{
    int result = 1;
    Vector3D<double>* empty;
    
    Simulation::iterator simIter = sim->find(familyName);
    empty = new Vector3D<double>[simIter->second.count.numParticles]; //an empty float array with length the number of particles
    
    simIter->second.addAttribute(attributeName, TypedArray(empty, simIter->second.count.numParticles));
    contribute(sizeof(result), &result, CkReduction::logical_and, cb);
}

// CC 7/19/07
void Worker::importScalarData( std::string const& familyName, std::string const& attributeName, int length, double c_data[], CkCallback const&cb)
{
  int result = 1;
  
  Simulation::iterator simIter = sim->find(familyName); //Now find the family or create a new one
  if(simIter == sim->end()) {
    ParticleFamily family;
    family.familyName = familyName;
    family.count.numParticles = length;
    family.count.startParticle = 0;
    family.count.totalNumParticles = length;
    sim->insert(make_pair(familyName, family)); //Is this correct?
    simIter = sim->find(familyName);
  }
  cout<<"AttributeName: "<<attributeName<<" Length: "<<length<<"\n";
  //  TypedArray& arr = simIter->second.attributes[attributeName]; //Add on an attribute
  simIter->second.addAttribute(attributeName, TypedArray(c_data, length)); //I want to load the attribute with my data
  contribute(sizeof(result), &result, CkReduction::logical_and, cb);
}

// CC 7/19/07
void Worker::importVectorData( std::string const& familyName, std::string const&attributeName, int length, Vector3D<float> c_data[], CkCallback const&cb)
{
  int result = 1;
  cout<<"In importVectorData\n";
  Simulation::iterator simIter = sim->find(familyName); //Now find the family or create a new one
  if(simIter == sim->end()) {
    ParticleFamily family;
    family.familyName = familyName;
    family.count.numParticles = length;
    family.count.startParticle = 0;
    family.count.totalNumParticles = length;
    sim->insert(make_pair(familyName, family)); //Is this correct
    simIter = sim->find(familyName);
  }
  // TypedArray& arr = simIter->second.attributes[attributeName]; //Add on an attribute
  cout<<"Attribute: "<<attributeName<<" Length: "<<length<<"Number of particles: "<<simIter->second.count.numParticles<<"\n";
  simIter->second.addAttribute(attributeName, TypedArray(c_data, length)); //I want to load the attribute with my data. 
  contribute(sizeof(result), &result, CkReduction::logical_and, cb);
  cout<<"done\n";
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

void Worker::createGroup_AttributeBox(std::string const& groupName,
				      std::string const& parentGroupName,
				      std::string const& attributeName,
				      Vector3D<double> corner,
				      Vector3D<double> edge1,
				      Vector3D<double> edge2,
				      Vector3D<double> edge3,
				      CkCallback const& cb) {
	int result = 0;
	boost::shared_ptr<SimulationHandling::Group>& parentGroup = groups[groups.find(parentGroupName) != groups.end() ? parentGroupName: "All"];
	boost::shared_ptr<SimulationHandling::Group> g = make_BoxGroup(*sim, parentGroup, attributeName, corner, edge1, edge2, edge3);
	if(g) {
		groups[groupName] = g;
		result = 1;
	}
	contribute(sizeof(result), &result, CkReduction::logical_and, cb);
}

class PythonLocalParticle: public PythonObjectLocal
{
    // Filippo's Python stuff
    // Keep track of group, family, and place in particle loop
public:
    SimulationHandling::Simulation* sim;
    boost::shared_ptr<SimulationHandling::Group> localPartG;
    SimulationHandling::Group::GroupFamilies::iterator localPartFamIter;
    SimulationHandling::ParticleFamily family;
    SimulationHandling::GroupIterator localPartIter;
    SimulationHandling::GroupIterator localPartEnd;

    bool isReducing;
    PyObject *localPartPyGlob;	// Hold global parameters
    PyObject *localPartReduction;
    PyObject *localReduceList;
    int buildIterator(PyObject*&, void*); // for localParticle
    int nextIteratorUpdate(PyObject*&, PyObject*, void*); // for localParticle
    };

    
// Run python code over all particles in a group
// Deprecated in favor of the next function
//
void Worker::localParticleCode(std::string const &s, const CkCallback &cb) 
{
    
    GroupMap::iterator gIter = groups.find("All");

    if(gIter != groups.end()) {
	PythonLocalParticle pyLocalPart;
	
	pyLocalPart.sim = sim;
	pyLocalPart.localPartG = gIter->second;
	pyLocalPart.localPartPyGlob = NULL;
	pyLocalPart.isReducing = false;

	PythonIterator info;
	PythonExecute wrapper((char*)s.c_str(), "localparticle", &info);

	pyLocalPart.execute(&wrapper);
	}
    
    
    contribute(0, 0, CkReduction::concat, cb); // barrier
    }

void Worker::localParticleCodeGroup(std::string const &g, // Group
				    std::string const &s, // Code
				    PyObjectMarshal &global, // Globals
				    const CkCallback &cb)
{
    GroupMap::iterator gIter = groups.find(g);

    if(gIter != groups.end()) {
	PythonLocalParticle pyLocalPart;
	pyLocalPart.sim = sim;
	pyLocalPart.localPartG = gIter->second;
	PyGILState_STATE pyState = PyGILState_Ensure();
	pyLocalPart.localPartPyGlob = global.getObj();
	PyGILState_Release(pyState);
	pyLocalPart.isReducing = false;
	
	PythonIterator info;	// XXX should this be initialized?
	PythonExecute wrapper((char*)s.c_str(), "localparticle", &info);

	pyLocalPart.execute(&wrapper);

	pyState = PyGILState_Ensure();
	Py_DECREF(pyLocalPart.localPartPyGlob);
	PyGILState_Release(pyState);
	}
    contribute(0, 0, CkReduction::concat, cb); // barrier
    }

void Worker::reduceParticle(std::string const &g, // Group
			    std::string const &sParticleCode, // Map code
			    std::string const &sReduceCode, // Reduce code
			    PyObjectMarshal &global,
			    const CkCallback &cb)
{
    GroupMap::iterator gIter = groups.find(g);

    PythonLocalParticle pyLocalPart;
    PyGILState_STATE pyState = PyGILState_Ensure();
    pyLocalPart.localReduceList = PyList_New(0);
    pyLocalPart.localPartPyGlob = global.getObj();
    PyGILState_Release(pyState);

    if(gIter != groups.end()) {
	// First apply the "map" function.
	pyLocalPart.sim = sim;
	pyLocalPart.localPartG = gIter->second;
	pyLocalPart.isReducing = true;
	
	PythonIterator info;	// XXX should this be initialized?
	PythonExecute wrapper((char*)sParticleCode.c_str(), "localparticle",
			      &info);

	pyLocalPart.execute(&wrapper);

	// Reduce the local list
	PythonReducer reducer(pyLocalPart.localReduceList,
			      pyLocalPart.localPartPyGlob);
	PythonExecute wrapperreduce((char*)sReduceCode.c_str(), "localparticle",
			      &info);
	reducer.execute(&wrapperreduce);
	
	pyState = PyGILState_Ensure();
	Py_DECREF(pyLocalPart.localReduceList);
	if(reducer.listResult)
	    pyLocalPart.localReduceList = reducer.listResult;
	else
	    pyLocalPart.localReduceList = PyList_New(0);
	PyGILState_Release(pyState);
	}
    pyState = PyGILState_Ensure();
    // Send the local list to the contribute function
    
    PyObject *result = Py_BuildValue("(sOO)", sReduceCode.c_str(),
				     pyLocalPart.localPartPyGlob,
				     pyLocalPart.localReduceList);
    
    PyObjectMarshal resultMarshal(result);
    int nBuf = PUP::size(resultMarshal);
    char *buf = new char[nBuf];
    PUP::toMemBuf(resultMarshal, buf, nBuf);

    Py_DECREF(result);
    Py_DECREF(pyLocalPart.localPartPyGlob);
    Py_DECREF(pyLocalPart.localReduceList);
    PyGILState_Release(pyState);

    contribute(nBuf, buf, pythonReduction, cb);
    delete[] buf;
    }

int PythonLocalParticle::buildIterator(PyObject *&arg, void *iter) {

    if(localPartPyGlob != NULL) {
	PyObject_SetAttrString(arg, "_param", localPartPyGlob);
	}
    
    localPartFamIter = localPartG->families.begin();
    if(localPartFamIter == localPartG->families.end())
	return 0;
    localPartIter = localPartG->make_begin_iterator(*localPartFamIter);
    localPartEnd = localPartG->make_end_iterator(*localPartFamIter);
    if(*localPartIter == *localPartEnd)
	return 0;

    family = (*sim)[*localPartFamIter];
    
    for(AttributeMap::iterator attrIter = family.attributes.begin();
	attrIter != family.attributes.end(); attrIter++) {
	TypedArray& arr = attrIter->second;
	if(arr.data == 0) //attribute not loaded
	    sim->loadAttribute(*localPartFamIter, attrIter->first,
			       family.count.numParticles,
			       family.count.startParticle);
	if(arr.dimensions == 1) {
	    switch(arr.code) {
	    case TypeHandling::int32:
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
	if(arr.dimensions == 3) {
	    PyObject *pyVec = NULL;
	    Vector3D<int> vl;
	    Vector3D<float> vf;
	    Vector3D<double> vd;
	    switch(arr.code) {
	    case TypeHandling::int32:
		vl = arr.getArray(Type2Type<Vector3D<int> >())[*localPartIter];
		pyVec = Py_BuildValue("(iii)", vl.x, vl.y, vl.z);
		break;
	    case float32:
		vf = arr.getArray(Type2Type<Vector3D<float> >())[*localPartIter];
		pyVec = Py_BuildValue("(ddd)", vf.x, vf.y, vf.z);
		break;
	    case float64:
		vd = arr.getArray(Type2Type<Vector3D<double> >())[*localPartIter];
		pyVec = Py_BuildValue("(ddd)", vd.x, vd.y, vd.z);
		break;
	    default:
		assert(0);
		}
	    PyObject_SetAttrString(arg, (char *) attrIter->first.c_str(),
				   pyVec);
	    Py_DECREF(pyVec);
	    }
	}
    return 1;
    }

int PythonLocalParticle::nextIteratorUpdate(PyObject *&arg, PyObject *result, void *iter) {
    // Copy out from Python object
    u_int64_t index = *localPartIter; // Optimize dereference

    for(AttributeMap::iterator attrIter = family.attributes.begin();
	attrIter != family.attributes.end(); attrIter++) {
	TypedArray& arr = attrIter->second;
	if(arr.dimensions == 1) {
	    long lvalue; // Python only does longs
	    double dvalue;

	    switch(arr.code) {
	    case TypeHandling::int32:
		pythonGetInt(arg, (char *) attrIter->first.c_str(), &lvalue);
		arr.getArray(Type2Type<int>())[index] = lvalue;
		break;
	    case float32:
		pythonGetFloat(arg, (char *) attrIter->first.c_str(), &dvalue);
		arr.getArray(Type2Type<float>())[index] = dvalue;
		break;
	    case float64:
		pythonGetFloat(arg, (char *) attrIter->first.c_str(), &arr.getArray(Type2Type<double>())[index]);
		break;
	    default:
		assert(0);
		}
	    }
	if(arr.dimensions == 3) {
	    PyObject *tmp = PyObject_GetAttrString(arg,
					(char *) attrIter->first.c_str());
	    // CkAssert(PyTuple_Check(tmp));
	    PyObject *tmpItemX = PyTuple_GET_ITEM(tmp, 0);
	    PyObject *tmpItemY = PyTuple_GET_ITEM(tmp, 1);
	    PyObject *tmpItemZ = PyTuple_GET_ITEM(tmp, 2);
	    
	    Vector3D<int> lvalue;
	    Vector3D<float> fvalue;
	    Vector3D<double> dvalue;

	    switch(arr.code) {
	    case TypeHandling::int32:
		lvalue.x = PyInt_AsLong(tmpItemX);
		lvalue.y = PyInt_AsLong(tmpItemY);
		lvalue.z = PyInt_AsLong(tmpItemZ);
		arr.getArray(Type2Type<Vector3D<int> >())[index]
		    = lvalue;
		break;
	    case float32:
		fvalue.x = PyFloat_AsDouble(tmpItemX);
		fvalue.y = PyFloat_AsDouble(tmpItemY);
		fvalue.z = PyFloat_AsDouble(tmpItemZ);
		arr.getArray(Type2Type<Vector3D<float> >())[index]
		    = fvalue;
		break;
	    case float64:
		dvalue.x = PyFloat_AsDouble(tmpItemX);
		dvalue.y = PyFloat_AsDouble(tmpItemY);
		dvalue.z = PyFloat_AsDouble(tmpItemZ);
		arr.getArray(Type2Type<Vector3D<double> >())[index]
		    = dvalue;
		break;
	    default:
		assert(0);
		}
	    Py_DECREF(tmp);
	    }
	}
    if(isReducing && (result != Py_None)) {
	PyList_Append(localReduceList, result);
	}
    
    // Increment
    localPartIter++;
    if(*localPartIter == *localPartEnd) { // End of particles in family
	localPartFamIter++;
	if(localPartFamIter == localPartG->families.end()) {
	    return 0;		// Done!
	    }
	localPartIter = localPartG->make_begin_iterator(*localPartFamIter);
	localPartEnd = localPartG->make_end_iterator(*localPartFamIter);
	family = (*sim)[*localPartFamIter];
	}
    index = *localPartIter; // Optimize dereference

    for(AttributeMap::iterator attrIter = family.attributes.begin();
	attrIter != family.attributes.end(); attrIter++) {
	TypedArray& arr = attrIter->second;
	if(arr.dimensions == 1) {
	    switch(arr.code) {
	    case TypeHandling::int32:
		pythonSetInt(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<int>())[index]);
		break;
	    case float32:
		pythonSetFloat(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<float>())[index]);
		break;
	    case float64:
		pythonSetFloat(arg, (char *) attrIter->first.c_str(), arr.getArray(Type2Type<double>())[index]);
		break;
	    default:
		assert(0);
		}
	    }
	if(arr.dimensions == 3) {
	    PyObject *pyVec = NULL;
	    Vector3D<int> vl;
	    Vector3D<float> vf;
	    Vector3D<double> vd;

	    switch(arr.code) {
	    case TypeHandling::int32:
		vl = arr.getArray(Type2Type<Vector3D<int> >())[index];
		pyVec = Py_BuildValue("(iii)", vl.x, vl.y, vl.z);
		break;
	    case float32:
		vf = arr.getArray(Type2Type<Vector3D<float> >())[index];
		pyVec = Py_BuildValue("(ddd)", vf.x, vf.y, vf.z);
		break;
	    case float64:
		vd = arr.getArray(Type2Type<Vector3D<double> >())[index];
		pyVec = Py_BuildValue("(ddd)", vd.x, vd.y, vd.z);
		break;
	    default:
		assert(0);
		}
	    PyObject_SetAttrString(arg, (char *) attrIter->first.c_str(),
				   pyVec);
	    Py_DECREF(pyVec);
	    }
	}
    return 1;
    }
