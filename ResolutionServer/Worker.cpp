/** @file Worker.cpp
 */

#include <utility>
#include <cstdlib>

#include "tree_xdr.h"

#include "ResolutionServer.h"
#include "Reductions.h"
#include "Space.h"

void Worker::readParticles(const std::string& posfilename, const std::string& valuefilename, const CkCallback& cb) {		
	if(MetaInformationHandler* meta = metaProxy.ckLocalBranch()) {
		meta->boxes.clear();
		meta->spheres.clear();
		meta->regionMap.clear();
	}
	
	FILE* infile = fopen(posfilename.c_str(), "rb");
	if(!infile) {
		cerr << "Worker " << thisIndex << ": Couldn't open position file \"" << posfilename << "\"" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	
	XDR xdrs;
	xdrstdio_create(&xdrs, infile, XDR_DECODE);
	FieldHeader fh;
	if(!xdr_template(&xdrs, &fh)) {
		cerr << "Worker " << thisIndex << ": Couldn't read position header" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	if(fh.magic != FieldHeader::MagicNumber) {
		cerr << "Worker " << thisIndex << ": Position file appears corrupt (magic number isn't right)" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	if(fh.dimensions != 3 || fh.code != float32) {
		cerr << "Worker " << thisIndex << ": Position file contains wrong type" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	
	if(!xdr_template(&xdrs, &boundingBox.lesser_corner) || !xdr_template(&xdrs, &boundingBox.greater_corner)) {
		cerr << "Worker " << thisIndex << ": Had problems reading bounding box values" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	if(boundingBox.lesser_corner == boundingBox.greater_corner) {
		cerr << "Worker " << thisIndex << ": Can't handle all the same position" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	
	u_int64_t totalNumParticles = fh.numParticles;
	numParticles = totalNumParticles / CkNumPes();
	u_int64_t leftover = totalNumParticles % CkNumPes();
	u_int64_t startParticle = CkMyPe() * numParticles;
	if(CkMyPe() < leftover) {
		numParticles++;
		startParticle += CkMyPe();
	} else
		startParticle += leftover;
	
	if(verbosity > 2)
		cout << "Worker " << thisIndex << ": Starting at particle " << startParticle << ", loading " << numParticles << " particles." << endl;
	
	if(!seekField(fh, &xdrs, startParticle)) {
		cerr << "Worker " << thisIndex << ": Couldn't seek to my part of the positions file" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	
	myParticles.resize(numParticles);
	for(u_int64_t i = 0; i < numParticles; ++i) {
		if(!xdr_template(&xdrs, &myParticles[i].position)) {
			cerr << "Worker " << thisIndex << ": Couldn't read in all of my positions" << endl;
			contribute(0, 0, CkReduction::concat, cb);
			return;
		}
	}
	xdr_destroy(&xdrs);
	fclose(infile);
	
	infile = fopen(valuefilename.c_str(), "rb");
	if(!infile) {
		cerr << "Worker " << thisIndex << ": Couldn't open color value file \"" << valuefilename << "\"" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	
	xdrstdio_create(&xdrs, infile, XDR_DECODE);
	FieldHeader valfh;
	if(!xdr_template(&xdrs, &valfh)) {
		cerr << "Worker " << thisIndex << ": Couldn't read color value header" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	if(valfh.magic != FieldHeader::MagicNumber) {
		cerr << "Worker " << thisIndex << ": Color value file appears corrupt (magic number isn't right)" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	if(valfh.dimensions != 1 || valfh.code != float32) {
		cerr << "Worker " << thisIndex << ": Color value file contains wrong type" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	if(valfh.numParticles != fh.numParticles) {
		cerr << "Worker " << thisIndex << ": Color value file and positions file have different numbers of particles" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	
	if(!xdr_template(&xdrs, &minValue) || !xdr_template(&xdrs, &maxValue)) {
		cerr << "Worker " << thisIndex << ": Had problems reading min/max color values" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	
	if(minValue == maxValue) {
		if(verbosity > 2)
			cout << "Worker " << thisIndex << ": All particles have the same color" << endl;
		for(u_int64_t i = 0; i < numParticles; ++i) {
			myParticles[i].value = minValue;
			myParticles[i].color = 255;
		}
	} else {
		if(!seekField(valfh, &xdrs, startParticle)) {
			cerr << "Worker " << thisIndex << ": Couldn't seek to my part of the color value file" << endl;
			contribute(0, 0, CkReduction::concat, cb);
			return;
		}
		for(u_int64_t i = 0; i < numParticles; ++i) {
			if(!xdr_template(&xdrs, &myParticles[i].value)) {
				cerr << "Worker " << thisIndex << ": Couldn't read in all of my positions" << endl;
				contribute(0, 0, CkReduction::concat, cb);
				return;
			}
			myParticles[i].color = 255;
		}
	}
	
	if(verbosity > 3)
		cout << "Worker " << thisIndex << ": Values read." << endl;
	
	/*
	if(reverseColors) {
		float temp = -minValue;
		minValue = -maxValue;
		maxValue = temp;
	}
	
	if(minValue == maxValue) {
		if(verbosity > 2)
			cout << "Worker " << thisIndex << ": All particles have the same color" << endl;
		for(u_int64_t i = 0; i < numParticles; ++i)
			myParticles[i].color = numColors;
	} else {
		if(!seekField(valfh, &xdrs, startParticle)) {
			cerr << "Worker " << thisIndex << ": Couldn't seek to my part of the color value file" << endl;
			contribute(0, 0, CkReduction::concat, cb);
			return;
		}
		float value, delta, lower;
		if(beLogarithmic) {			
			if(minValue <= 0)
				delta = log10(maxValue - minValue + 1.0);
			else {
				lower = log10(minValue);
				delta = log10(maxValue) - lower;
			}
		} else
			delta = maxValue - minValue;
		for(u_int64_t i = 0; i < numParticles; ++i) {
			if(!xdr_template(&xdrs, &value)) {
				cerr << "Worker " << thisIndex << ": Couldn't read in all of my positions" << endl;
				contribute(0, 0, CkReduction::concat, cb);
				return;
			}
			if(reverseColors)
				value *= -1;
			if(beLogarithmic) {
				if(minValue <= 0)
					myParticles[i].color = 1 + static_cast<byte>((numColors - 1) * log10(value - minValue + 1.0) / delta);
				else
					myParticles[i].color = 1 + static_cast<byte>((numColors - 1) * (log10(value) - lower) / delta);
			} else
				myParticles[i].color = 1 + static_cast<byte>((numColors - 1) * (value - minValue) / delta);
		}
	}
	xdr_destroy(&xdrs);
	fclose(infile);
	
	if(verbosity > 3)
		cout << "Worker " << thisIndex << ": Colors set." << endl;
	*/
	contribute(sizeof(OrientedBox<float>), &boundingBox, growOrientedBox_float, cb);
}

class MyVizRequest {
public:
	int code;
	int width;
	int height;
	Vector3D<double> x;
	Vector3D<double> y;
	Vector3D<double> z;
	Vector3D<double> o;
	double minZ;
	double maxZ;
	
	MyVizRequest(const liveVizRequest3d& req) {
		code = req.code;
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
		return os << "code: " << r.code
				<< "\nwidth: " << r.width
				<< "\nheight: " << r.height
				<< "\nx axis: " << r.x
				<< "\ny axis: " << r.y
				<< "\nz axis: " << r.z
				<< "\norigin: " << r.o
				<< "\nz range: " << r.minZ << " <=> " << r.maxZ;
	}
};

void Worker::valueRange(CkCcsRequestMsg* m) {
	double minMaxPair[2];
	minMaxPair[0] = minValue;
	minMaxPair[0] = swapEndianness(minMaxPair[0]);
	minMaxPair[1] = maxValue;
	minMaxPair[1] = swapEndianness(minMaxPair[1]);
	CcsSendDelayedReply(m->reply, 2 * sizeof(double), minMaxPair);
	delete m;
}

void Worker::recolor(CkCcsRequestMsg* m) {
	if(m->length != sizeof(int) + 2 * sizeof(double)) {
		cerr << "Re-color message is wrong size!" << endl;
		return;
	}
	beLogarithmic = swapEndianness(*reinterpret_cast<int *>(m->data));
	double minVal = swapEndianness(*reinterpret_cast<double *>(m->data + sizeof(int)));
	double maxVal = swapEndianness(*reinterpret_cast<double *>(m->data + sizeof(int) + sizeof(double)));
	if(verbosity > 2) {
		cout << "Re-coloring from " << minVal << " to " << maxVal;
		if(beLogarithmic)
			cout << ", logarithmically" << endl;
		else
			cout << ", linearly" << endl;
	}
	
	double invdelta = 1.0 / (maxVal - minVal);
	double value;
	for(u_int64_t i = 0; i < numParticles; ++i) {
		value = myParticles[i].value;
		if(beLogarithmic) {
			if(value > 0)
				value = log10(value);
			else
				value = -HUGE_VAL;
		}
		
		value = floor(numColors * (value - minVal) * invdelta);
		
		if(value < 0 || value >= numColors)
			myParticles[i].color = 0;
		else
			myParticles[i].color = 1 + static_cast<byte>(value);
	}
	
	if(thisIndex == 0) {
		unsigned char success = 1;
		CcsSendDelayedReply(m->reply, 1, &success);
	}
	delete m;
	if(verbosity > 3)
		cout << "Re-colored particles" << endl;
}

const byte lineColor = 255;

void drawLine(byte* image, const int width, const int height, int x0, int y0, int x1, int y1) {
	int dx = 1;
	int a = x1 - x0;
	if(a < 0) {
		dx = -1;
		a = -a;
	}
	
	int dy = 1;
	int b = y1 - y0;
	if(b < 0) {
		dy = -1;
		b = -b;
	}
	
	int two_a = 2 * a, two_b = 2 * b;
	int xcrit = two_a - b;
	int eps = 0;
	
	for(;;) {
		if(x0 > 0 && x0 < width && y0 > 0 && y0 < height)
			image[x0 + width * y0] = lineColor;
		if(x0 == x1 && y0 == y1)
			break;
		if(eps <= xcrit) {
			x0 += dx;
			eps += two_b;
		}
		if(eps >= a || a <= b) {
			y0 += dy;
			eps -= two_a;
		}
	}
}
		
void drawCirclePoints(byte* image, const int width, const int height, const int xCenter, const int yCenter, const int x, const int y) {
	int xpix, ypix;
	
	xpix = xCenter + x;
	ypix = yCenter + y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
		image[xpix + ypix * width] = lineColor;
	xpix = xCenter - x;
	ypix = yCenter + y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
		image[xpix + ypix * width] = lineColor;
	xpix = xCenter + x;
	ypix = yCenter - y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
		image[xpix + ypix * width] = lineColor;
	xpix = xCenter - x;
	ypix = yCenter - y;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
		image[xpix + ypix * width] = lineColor;
	xpix = xCenter + y;
	ypix = yCenter + x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
		image[xpix + ypix * width] = lineColor;
	xpix = xCenter - y;
	ypix = yCenter + x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
		image[xpix + ypix * width] = lineColor;
	xpix = xCenter + y;
	ypix = yCenter - x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
		image[xpix + ypix * width] = lineColor;
	xpix = xCenter - y;
	ypix = yCenter - x;
	if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
		image[xpix + ypix * width] = lineColor;
}

void drawCircle(byte* image, const int width, const int height, const int x0, const int y0, const int radius) {
	int x = 0;
	int y = radius;
	int p = 1 - radius;
	
	while(x < y) {
		++x;
		if(p < 0)
			p += 2 * x + 1;
		else {
			--y;
			p += 2 * (x - y) + 1;
		}
		drawCirclePoints(image, width, height, x0, y0, x, y);
	}
}

void Worker::generateImage(liveVizRequestMsg* m) {
	double start = CkWallTimer();
	
	MyVizRequest req(m->req);
	
	cout << "Worker " << thisIndex << ": Image request: " << req << endl;
		
	if(imageSize < req.width * req.height) {
		delete[] image;
		imageSize = req.width * req.height;
		image = new byte[imageSize];
	}
	memset(image, 0, imageSize);

	MetaInformationHandler* meta = metaProxy.ckLocalBranch();
	if(!meta) {
		cerr << "Well this sucks!  Couldn't get local pointer to meta handler" << endl;
		return;
	}
	
	float delta = 2 * req.x.length() / req.width;
	cout << "Pixel size: " << delta << " x " << (2 * req.y.length() / req.height) << endl;
	req.x /= req.x.lengthSquared();
	req.y /= req.y.lengthSquared();
	float x, y;
	unsigned int pixel;
	
	if(Sphere<double>* activeSphere = dynamic_cast<Sphere<double> *>(meta->activeRegion)) {
		for(u_int64_t i = 0; i < numParticles; ++i) {
			if(Space::contains(*activeSphere, myParticles[i].position)) {
				x = dot(req.x, myParticles[i].position - req.o);
				if(x > -1 && x < 1) {
					y = dot(req.y, myParticles[i].position - req.o);
					if(y > -1 && y < 1) {
						pixel = static_cast<unsigned int>(req.width * (x + 1) / 2) + req.width * static_cast<unsigned int>(req.height * (1 - y) / 2);
						if(pixel >= imageSize)
							cerr << "Worker " << thisIndex << ": How is my pixel so big? " << pixel << endl;
						if(image[pixel] < myParticles[i].color)
							image[pixel] = myParticles[i].color;
					}
				}
			}
		}
	} else if(Box<double>* activeBox = dynamic_cast<Box<double> *>(meta->activeRegion)) {
		for(u_int64_t i = 0; i < numParticles; ++i) {
			if(Space::contains(*activeBox, myParticles[i].position)) {
				x = dot(req.x, myParticles[i].position - req.o);
				if(x > -1 && x < 1) {
					y = dot(req.y, myParticles[i].position - req.o);
					if(y > -1 && y < 1) {
						pixel = static_cast<unsigned int>(req.width * (x + 1) / 2) + req.width * static_cast<unsigned int>(req.height * (1 - y) / 2);
						if(pixel >= imageSize)
							cerr << "Worker " << thisIndex << ": How is my pixel so big? " << pixel << endl;
						if(image[pixel] < myParticles[i].color)
							image[pixel] = myParticles[i].color;
					}
				}
			}
		}
	} else {
		for(u_int64_t i = 0; i < numParticles; ++i) {
			x = dot(req.x, myParticles[i].position - req.o);
			if(x > -1 && x < 1) {
				y = dot(req.y, myParticles[i].position - req.o);
				if(y > -1 && y < 1) {
					pixel = static_cast<unsigned int>(req.width * (x + 1) / 2) + req.width * static_cast<unsigned int>(req.height * (1 - y) / 2);
					if(pixel >= imageSize)
						cerr << "Worker " << thisIndex << ": How is my pixel so big? " << pixel << endl;
					if(image[pixel] < myParticles[i].color)
						image[pixel] = myParticles[i].color;
				}
			}
		}
	}
	if(thisIndex == 0) {		
		if(meta->boxes.size() > 0) {
			//draw boxes onto canvas
			pair<int, int> vertices[8];
			Vector3D<double> vertex;
			for(vector<Box<double> >::iterator iter = meta->boxes.begin(); iter != meta->boxes.end(); ++iter) {
				for(int i = 0; i < 8; ++i) {
					vertex = iter->vertex(i);
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
			for(vector<Sphere<double> >::iterator iter = meta->spheres.begin(); iter != meta->spheres.end(); ++iter) {
				x0 = static_cast<int>(floor(req.width * (dot(req.x, iter->origin - req.o) + 1) / 2));
				y0 = static_cast<int>(floor(req.height * (1 - dot(req.y, iter->origin - req.o)) / 2));
				radius = static_cast<int>(iter->radius / delta);
				drawCircle(image, req.width, req.height, x0, y0, radius);
			}
		}
	}

	double stop = CkWallTimer();
	liveVizDeposit(m, 0, 0, req.width, req.height, image, this);
	cout << "Image generation took " << (CkWallTimer() - start) << " seconds" << endl;
	cout << "my part: " << (stop - start) << " seconds" << endl;
}

void Worker::collectStats(const string& id, const CkCallback& cb) {
	MetaInformationHandler* meta = metaProxy.ckLocalBranch();
	if(!meta) {
		cerr << "Well this sucks!  Couldn't get local pointer to meta handler" << endl;
		return;
	}
	Shape<double>* activeRegion = 0;
	MetaInformationHandler::RegionMap::iterator selectedRegion = meta->regionMap.find(id);
	if(selectedRegion != meta->regionMap.end())
		activeRegion = selectedRegion->second;
	
	GroupStatistics stats;
	
	if(Sphere<double>* activeSphere = dynamic_cast<Sphere<double> *>(activeRegion)) {
		for(u_int64_t i = 0; i < numParticles; ++i) {
			if(Space::contains(*activeSphere, myParticles[i].position)) {
				stats.numParticles++;
				stats.boundingBox.grow(myParticles[i].position);
			}
		}
	} else if(Box<double>* activeBox = dynamic_cast<Box<double> *>(activeRegion)) {
		for(u_int64_t i = 0; i < numParticles; ++i) {
			if(Space::contains(*activeBox, myParticles[i].position)) {
				stats.numParticles++;
				stats.boundingBox.grow(myParticles[i].position);
			}
		}
	} else {
		for(u_int64_t i = 0; i < numParticles; ++i) {
			stats.boundingBox.grow(myParticles[i].position);
		}
		stats.numParticles += numParticles;
	}
	
	contribute(sizeof(GroupStatistics), &stats, mergeStatistics, cb);
}
