/** @file Worker.cpp
 */

#include <utility>
#include <cstdlib>

#include "tree_xdr.h"

#include "ResolutionServer.h"
#include "Reductions.h"

void Worker::readParticles(const std::string& posfilename, const std::string& valuefilename, byte numColors, bool logarithmic, bool reversed, const CkCallback& cb) {
	beLogarithmic = logarithmic;
	bool reverseColors = reversed;
		
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

void Worker::specifyBox(CkCcsRequestMsg* m) {
	if(m->length != 8 * 3 * sizeof(double))
		return;
	Vector3D<double>* vertices = reinterpret_cast<Vector3D<double> *>(m->data);
	cout << "Got a box definition" << endl;
	Box<float> box;
	for(int i = 0; i < 8; ++i) {
		vertices[i].x = swapEndianness(vertices[i].x);
		vertices[i].y = swapEndianness(vertices[i].y);
		vertices[i].z = swapEndianness(vertices[i].z);
		box.vertices[i] = vertices[i];
		cout << "Vertex " << (i + 1) << ": " << vertices[i] << endl;
	}
	
	//save box somehow
	boxes.push_back(box);
	
	//give it unique identifier
	
	//send identifier back to client
	
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

void Worker::clearBoxes(CkCcsRequestMsg* m) {
	boxes.clear();
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

void Worker::specifySphere(CkCcsRequestMsg* m) {
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
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

void Worker::clearSpheres(CkCcsRequestMsg* m) {
	spheres.clear();
	unsigned char success = 1;
	CcsSendDelayedReply(m->reply, 1, &success);
	delete m;
}

const byte lineColor = 255;

void drawLine(byte* image, const int width, const int height, const pair<double, double> p1, const pair<double, double> p2) {
	int min_pixel, max_pixel, xpix, ypix;
	double x, y;
	
	min_pixel = static_cast<int>(floor(width * (p1.first + 1) / 2));
	max_pixel = static_cast<int>(floor(width * (p2.first + 1) / 2));
	if(max_pixel < min_pixel)
		swap(min_pixel, max_pixel);
	if(max_pixel >= 0 && min_pixel < width) {
		for(xpix = min_pixel; xpix <= max_pixel; ++xpix) {
			x = 2 * (xpix + 0.5) / width - 1;
			y = p1.second + (p2.second - p1.second) * (x - p1.first) / (p2.first - p1.first);
			ypix = static_cast<int>(floor(height * (1 - y) / 2));
			if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
				image[xpix + ypix * width] = lineColor;
		}
	}
	
	min_pixel = static_cast<int>(floor(height * (1 - p1.second) / 2));
	max_pixel = static_cast<int>(floor(height * (1 - p2.second) / 2));
	if(max_pixel < min_pixel)
		swap(min_pixel, max_pixel);
	if(max_pixel >= 0 && min_pixel < height) {
		for(ypix = min_pixel; ypix <= max_pixel; ++ypix) {
			y = 1 - 2 * (ypix + 0.5) / height;
			x = p1.first + (p2.first - p1.first) * (y - p1.second) / (p2.second - p1.second);
			xpix = static_cast<int>(floor(width * (x + 1) / 2));
			if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
				image[xpix + ypix * width] = lineColor;
		}
	}
}

void drawCircle(byte* image, const int width, const int height, const pair<double, double> origin, const double radius) {
	int min_pixel, max_pixel, xpix, ypix;
	double x, y, rsq = radius * radius, temp;
	
	min_pixel = static_cast<int>(floor(width * (origin.first - radius + 1) / 2));
	max_pixel = static_cast<int>(floor(width * (origin.first + radius + 1) / 2));
	if(max_pixel < min_pixel)
		swap(min_pixel, max_pixel);
	for(xpix = min_pixel; xpix <= max_pixel; ++xpix) {
		x = 2 * (xpix + 0.5) / width - 1;
		temp = sqrt((x - origin.first) * (x - origin.first) - rsq);
		y = origin.second + temp;
		ypix = static_cast<int>(floor(height * (1 - y) / 2));
		if(xpix >= 0 && xpix < width && ypix >=0 && ypix < height)
			image[xpix + ypix * width] = lineColor;
		y = origin.second - temp;
		ypix = static_cast<int>(floor(height * (1 - y) / 2));
		if(xpix >= 0 && xpix < width && ypix >= 0 && ypix < height)
			image[xpix + ypix * width] = lineColor;
	}
	
	min_pixel = static_cast<int>(floor(height * (1 - origin.second + radius) / 2));
	max_pixel = static_cast<int>(floor(height * (1 - origin.second - radius) / 2));
	if(max_pixel < min_pixel)
		swap(min_pixel, max_pixel);
	for(ypix = min_pixel; ypix <= max_pixel; ++ypix) {
		y = 1 - 2 * (ypix + 0.5) / height;
		temp = sqrt((y - origin.second) * (y - origin.second) - rsq);
		x = origin.first + temp;
		xpix = static_cast<int>(floor(width * (x + 1) / 2));
		if(xpix >= 0 && xpix < width && ypix >=0 && ypix < height)
			image[xpix + ypix * width] = lineColor;
		x = origin.first - temp;
		xpix = static_cast<int>(floor(width * (x + 1) / 2));
		if(xpix >= 0 && xpix < width && ypix >=0 && ypix < height)
			image[xpix + ypix * width] = lineColor;
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

	req.x /= req.x.lengthSquared();
	req.y /= req.y.lengthSquared();
	float x, y;
	unsigned int pixel;
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
	if(boxes.size() > 0) {
		//draw boxes onto canvas
		pair<double, double> vertices[8];
		for(vector<Box<float> >::iterator iter = boxes.begin(); iter != boxes.end(); ++iter) {
			for(int i = 0; i < 8; ++i) {
				vertices[i].first = dot(req.x, iter->vertices[i] - req.o);
				vertices[i].second = dot(req.y, iter->vertices[i] - req.o);
			}
			
			for(int i = 0; i < 4; ++i) {
				drawLine(image, req.width, req.height, vertices[i], vertices[(i + 1) % 4]);
				drawLine(image, req.width, req.height, vertices[i + 4], vertices[(i + 1) % 4 + 4]);
				drawLine(image, req.width, req.height, vertices[i], vertices[i + 4]);
			}
		}
	}
	if(spheres.size() > 0) {
		//draw spheres onto canvas
		pair<double, double> origin;
		double radius;
		for(vector<Sphere<double> >::iterator iter = spheres.begin(); iter != spheres.end(); ++iter) {
			origin.first = dot(req.x, iter->origin - req.o);
			origin.second = dot(req.y, iter->origin - req.o);
			radius = 3.0 / req.width;
			drawCircle(image, req.width, req.height, origin, radius);
		}
	}

	double stop = CkWallTimer();
	liveVizDeposit(m, 0, 0, req.width, req.height, image, this);
	cout << "Image generation took " << (CkWallTimer() - start) << " seconds" << endl;
	cout << "my part: " << (stop - start) << " seconds" << endl;
}
