/** @file pup_network.h
 @author Graeme Lufkin (gwl@u.washington.edu)
 @date Created November 24, 2003
 @version 1.0
 This file implements a PUP::er that converts to/from network byte order
 for 2-, 4-, and 8-byte values.  For example, it can be used to swap 
 endianness when communicating with a Java program via CCS.
 */
 
#ifndef PUP_NETWORK_H__kijldsxkjidf49807364208regu89
#define PUP_NETWORK_H__kijldsxkjidf49807364208regu89

#ifdef MACOSX
#include <ppc/endian.h>
#else
#include <endian.h>
#endif /*MACOSX*/
#include <sys/types.h>

//Network byte order is big endian, so we only have to swap on little endian machines

#if BYTE_ORDER == LITTLE_ENDIAN

#define __u16 unsigned short
#define __u32 unsigned int
#define __u64 u_int64_t

//Macros to do the arithmetic of byte swapping

#define ___swab16(x) \
({ \
	__u16 __x = (x); \
	((__u16)( \
		(((__u16)(__x) & (__u16)0x00ffU) << 8) | \
		(((__u16)(__x) & (__u16)0xff00U) >> 8) )); \
})

#define ___swab32(x) \
({ \
	__u32 __x = (x); \
	((__u32)( \
		(((__u32)(__x) & (__u32)0x000000ffUL) << 24) | \
		(((__u32)(__x) & (__u32)0x0000ff00UL) <<  8) | \
		(((__u32)(__x) & (__u32)0x00ff0000UL) >>  8) | \
		(((__u32)(__x) & (__u32)0xff000000UL) >> 24) )); \
})

#define ___swab64(x) \
({ \
	__u64 __x = (x); \
	((__u64)( \
		(__u64)(((__u64)(__x) & (__u64)0x00000000000000ffULL) << 56) | \
		(__u64)(((__u64)(__x) & (__u64)0x000000000000ff00ULL) << 40) | \
		(__u64)(((__u64)(__x) & (__u64)0x0000000000ff0000ULL) << 24) | \
		(__u64)(((__u64)(__x) & (__u64)0x00000000ff000000ULL) <<  8) | \
	    (__u64)(((__u64)(__x) & (__u64)0x000000ff00000000ULL) >>  8) | \
		(__u64)(((__u64)(__x) & (__u64)0x0000ff0000000000ULL) >> 24) | \
		(__u64)(((__u64)(__x) & (__u64)0x00ff000000000000ULL) >> 40) | \
		(__u64)(((__u64)(__x) & (__u64)0xff00000000000000ULL) >> 56) )); \
})

#endif

#include "pup.h"

namespace PUP {

class toNetwork : public er {
#if BYTE_ORDER == LITTLE_ENDIAN
	void swap16(unsigned short* p, int n) {
		for(; n > 0; --n, ++p)
			*p = ___swab16(*p);
	}
	
	void swap32(unsigned int* p, int n) {
		for(; n > 0; --n, ++p)
			*p = ___swab32(*p);
	}
	
	void swap64(u_int64_t* p, int n) {
		for(; n > 0; --n, ++p)
			*p = ___swab64((*p));
	}
#endif
		
protected:
		
	virtual void bytes(void* p, int n, size_t itemSize, dataType t) {
#if BYTE_ORDER == LITTLE_ENDIAN
		switch(t) {
			case Tchar: //nothing to do
			case Tuchar:
				break;
			case Tshort:
			case Tushort:
				return swap16(reinterpret_cast<unsigned short *>(p), n);
			case Tint:
			case Tlong:
			case Tuint:
			case Tulong:
			case Tfloat:
				return swap32(reinterpret_cast<unsigned int *>(p), n);
			case Tlonglong:
			case Tulonglong:
			case Tdouble:
				return swap64(reinterpret_cast<u_int64_t *>(p), n);
			default:
				break;
				//throw UnsupportedTypeException;
		}
#endif
	}

public:
		
	toNetwork(): er(IS_PACKING | IS_UNPACKING) { }
};

//swapping operation is the same either way, so 'to' is identical to 'from'
typedef toNetwork fromNetwork;

} //close namespace PUP

#endif //PUP_NETWORK_H__kijldsxkjidf49807364208regu89
