#include "ResolutionServer.h"

#include <iostream>
#include <string>

using namespace std;

void Main::executePythonCode(CkCcsRequestMsg* m) {
	string s(m->data, m->length);
	cout << "Got code to execute: \"" << s << "\"" << endl;
	CcsDelayedReply d = m->reply;
	m->data[m->length-1] = 0;  // guarantee null termination.
	Main::execute(m);
	
	unsigned char success = 1;
	CcsSendDelayedReply(d, 1, &success);
}
