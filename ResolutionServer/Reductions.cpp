/** @file Reductions.cpp
 */
 
#include <utility>

#include "Reductions.h"

#include "ParticleStatistics.h"
#include "OrientedBox.h"

CkReduction::reducerType mergeStatistics;

CkReduction::reducerType growOrientedBox_float;
CkReduction::reducerType growOrientedBox_double;

CkReduction::reducerType minmax_int;
CkReduction::reducerType minmax_float;
CkReduction::reducerType minmax_double;

CkReduction::reducerType pairByteDoubleMin;
CkReduction::reducerType pairByteDoubleMax;
CkReduction::reducerType pairDoubleDoubleMin;
CkReduction::reducerType pairDoubleDoubleMax;

CkReduction::reducerType pairFloatFloatSum;
CkReduction::reducerType pairDoubleDoubleSum;
CkReduction::reducerType pairDoubleVector3DSum;

using namespace std;

typedef unsigned char byte;

/// Combine statistics about a collection of particles
CkReductionMsg* mergeParticleStats(int nMsg, CkReductionMsg** msgs) {
	//what if one box is uninitialized?
	GroupStatistics* stats = static_cast<GroupStatistics *>(msgs[0]->getData());
	GroupStatistics* otherStats;
	for(int i = 1; i < nMsg; ++i) {
		otherStats = static_cast<GroupStatistics *>(msgs[i]->getData());
		stats->numParticles += otherStats->numParticles;
		if(otherStats->boundingBox.initialized()) {
			stats->boundingBox.grow(otherStats->boundingBox.lesser_corner);
			stats->boundingBox.grow(otherStats->boundingBox.greater_corner);
		}
	}
	return CkReductionMsg::buildNew(sizeof(GroupStatistics), stats);
}

/// Combine reduction messages to grow a box
template <typename T>
CkReductionMsg* boxGrowth(int nMsg, CkReductionMsg** msgs) {
	OrientedBox<T>* pbox = static_cast<OrientedBox<T> *>(msgs[0]->getData());
	OrientedBox<T>* msgpbox;
	for(int i = 1; i < nMsg; i++) {
		msgpbox = static_cast<OrientedBox<T> *>(msgs[i]->getData());
		if(msgpbox->initialized()) {
			pbox->grow(msgpbox->lesser_corner);
			pbox->grow(msgpbox->greater_corner);
		}
	}
	
	return CkReductionMsg::buildNew(sizeof(OrientedBox<T>), pbox);
}

/// Combine reduction messages to get min/max pair
template <typename T>
CkReductionMsg* minmax(int nMsg, CkReductionMsg** msgs) {
	T* pminmax = static_cast<T *>(msgs[0]->getData());
	T* msgpminmax;
	for(int i = 1; i < nMsg; i++) {
		msgpminmax = static_cast<T *>(msgs[i]->getData());
		if(msgpminmax[0] < pminmax[0])
			pminmax[0] = msgpminmax[0];
		if(msgpminmax[1] > pminmax[1])
			pminmax[1] = msgpminmax[1];
	}
	
	return CkReductionMsg::buildNew(2 * sizeof(T), pminmax);
}

template <typename T, typename U>
CkReductionMsg* pairMin(int nMsg, CkReductionMsg** msgs) {
	pair<T, U>* ppair = static_cast<pair<T, U> *>(msgs[0]->getData());
	pair<T, U>* msgppair;
	for(int i = 1; i < nMsg; i++) {
		msgppair = static_cast<pair<T, U> *>(msgs[i]->getData());
		if(*msgppair < *ppair)
			*ppair = *msgppair;
	}
	return CkReductionMsg::buildNew(sizeof(pair<T, U>), ppair);
}

template <typename T, typename U>
CkReductionMsg* pairMax(int nMsg, CkReductionMsg** msgs) {
	pair<T, U>* ppair = static_cast<pair<T, U> *>(msgs[0]->getData());
	pair<T, U>* msgppair;
	for(int i = 1; i < nMsg; i++) {
		msgppair = static_cast<pair<T, U> *>(msgs[i]->getData());
		if(*ppair < *msgppair)
			*ppair = *msgppair;
	}
	return CkReductionMsg::buildNew(sizeof(pair<T, U>), ppair);
}

template <typename T, typename U>
CkReductionMsg* pairSum(int nMsg, CkReductionMsg** msgs) {
	pair<T, U>* ppair = static_cast<pair<T, U> *>(msgs[0]->getData());
	pair<T, U>* msgppair;
	for(int i = 1; i < nMsg; i++) {
		msgppair = static_cast<pair<T, U> *>(msgs[i]->getData());
		ppair->first += msgppair->first;
		ppair->second += msgppair->second;
	}
	return CkReductionMsg::buildNew(sizeof(pair<T, U>), ppair);
}

void registerReductions() {
	mergeStatistics = CkReduction::addReducer(mergeParticleStats);
	
	growOrientedBox_float = CkReduction::addReducer(boxGrowth<float>);
	growOrientedBox_double = CkReduction::addReducer(boxGrowth<double>);
	
	minmax_int = CkReduction::addReducer(minmax<int>);
	minmax_float = CkReduction::addReducer(minmax<float>);
	minmax_double = CkReduction::addReducer(minmax<double>);
	
	pairByteDoubleMin = CkReduction::addReducer(pairMin<byte, double>);
	pairByteDoubleMax = CkReduction::addReducer(pairMax<byte, double>);
	pairDoubleDoubleMin = CkReduction::addReducer(pairMin<double, double>);
	pairDoubleDoubleMax = CkReduction::addReducer(pairMax<double, double>);
	
	pairFloatFloatSum = CkReduction::addReducer(pairSum<float, float>);
	pairDoubleDoubleSum = CkReduction::addReducer(pairSum<double, double>);
	pairDoubleVector3DSum = CkReduction::addReducer(pairSum<double, Vector3D<double> >);
}

#include "Reductions.def.h"
