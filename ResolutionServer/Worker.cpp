/** @file Worker.cpp
 */
 
#include <cstdlib>

#include "tree_xdr.h"
#include "SwapEndianness.h"

#include "ResolutionServer.h"
#include "Reductions.h"

void Worker::readParticles(const std::string& posfilename, const std::string& valuefilename, bool logarithmic, bool reversed, const CkCallback& cb) {
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
		boundingBox.grow(myParticles[i].position);
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
	
	if(!seekField(valfh, &xdrs, startParticle)) {
		cerr << "Worker " << thisIndex << ": Couldn't seek to my part of the color value file" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	values = readField<float>(&xdrs, numParticles);
	if(!values) {
		cerr << "Worker " << thisIndex << ": Couldn't read in all of my color values" << endl;
		contribute(0, 0, CkReduction::concat, cb);
		return;
	}
	xdr_destroy(&xdrs);
	fclose(infile);
	
	float minMax[] = {HUGE_VAL, -HUGE_VAL};
	float x;
	for(u_int64_t i = 0; i < numParticles; ++i) {
		if(reverseColors)
			values[i] *= -1;
		x = values[i];
		if(x < minMax[0])
			minMax[0] = x;
		if(minMax[1] < x)
			minMax[1] = x;
	}
	callback = cb;
	contribute(2 * sizeof(float), minMax, minmax_float, CkCallback(CkIndex_Worker::calculateColors(0), thisArrayID));
}

void Worker::calculateColors(CkReductionMsg* m) {
	minValue = static_cast<float *>(m->getData())[0];
	maxValue = static_cast<float *>(m->getData())[1];
	delete m;
	
	if(beLogarithmic) {
		float delta;
		if(minValue <= 0) {
			delta = log10(maxValue - minValue + 1.0);
			for(u_int64_t i = 0; i < numParticles; ++i)
				myParticles[i].color = static_cast<byte>(255.0 * log10(values[i] - minValue + 1.0) / delta);
		} else {
			float lower = log10(minValue);
			delta = log10(maxValue) - lower;
			for(u_int64_t i = 0; i < numParticles; ++i)
				myParticles[i].color = static_cast<byte>(255.0 * (log10(values[i]) - lower) / delta);
		}		
	} else {
		for(u_int64_t i = 0; i < numParticles; ++i)
			myParticles[i].color = static_cast<byte>(255.0 * (values[i] - minValue) / (maxValue - minValue));
	}
	delete[] values;
	
	if(verbosity > 3)
		cout << "Worker " << thisIndex << ": Colors set." << endl;
		
	contribute(sizeof(OrientedBox<float>), &boundingBox, growOrientedBox_float, callback);
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

void Worker::generateImage(liveVizRequestMsg* m) {
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
				pixel = static_cast<unsigned int>(req.width * (x + 1) / 2) + req.width * static_cast<unsigned int>(req.height * (y + 1) / 2);
				if(pixel >= imageSize)
					cerr << "Worker " << thisIndex << ": How is my pixel so big? " << pixel << endl;
				if(image[pixel] < myParticles[i].color)
					image[pixel] = myParticles[i].color;
				/*if(image[pixel] + myParticles[i].color > 255)
					image[pixel] = 255;
				else
					image[pixel] += myParticles[i].color;
				*/
			}
		}
	}
	/*
	for(u_int64_t i = 0; i < numParticles; ++i) {
		x = dot(req.x, myParticles[i].position - req.o);
		if(x >= 0 && x < 1) {
			y = dot(req.y, myParticles[i].position - req.o);
			if(y >= 0 && y < 1) {
				pixel = static_cast<unsigned int>(req.width * x) + req.width * static_cast<unsigned int>(req.height * y);
				if(pixel >= imageSize)
					cerr << "Worker " << thisIndex << ": How is my pixel so big? " << pixel << endl;
				if(image[pixel] < myParticles[i].color)
					image[pixel] = myParticles[i].color;
				/*if(image[pixel] + myParticles[i].color > 255)
					image[pixel] = 255;
				else
					image[pixel] += myParticles[i].color;
				
			}
		}
	}
	*/
	liveVizDeposit(m, 0, 0, req.width, req.height, image, this);
}
