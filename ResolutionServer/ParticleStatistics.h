/** @file ParticleStatistics.h
 */

#ifndef PARTICLESTATISTICS_H
#define PARTICLESTATISTICS_H

#include "OrientedBox.h"

class GroupStatistics {
public:
		
	unsigned int numParticles;
	OrientedBox<double> boundingBox;
	//double totalMass;
	//Vector3D<double> centerOfMass;

	GroupStatistics() : numParticles(0) { }

};

#endif //PARTICLESTATISTICS_H
