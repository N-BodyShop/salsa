/** @file Reductions.cpp
 */
 
#include "Reductions.h"

#include "ParticleStatistics.h"
#include "OrientedBox.h"

CkReduction::reducerType mergeStatistics;

CkReduction::reducerType growOrientedBox_float;
CkReduction::reducerType growOrientedBox_double;

CkReduction::reducerType minmax_int;
CkReduction::reducerType minmax_float;
CkReduction::reducerType minmax_double;

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

void registerReductions() {
	mergeStatistics = CkReduction::addReducer(mergeParticleStats);
	
	growOrientedBox_float = CkReduction::addReducer(boxGrowth<float>);
	growOrientedBox_double = CkReduction::addReducer(boxGrowth<double>);
	
	minmax_int = CkReduction::addReducer(minmax<int>);
	minmax_float = CkReduction::addReducer(minmax<float>);
	minmax_double = CkReduction::addReducer(minmax<double>);
}

#include "Reductions.def.h"
