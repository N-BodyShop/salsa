#include <stdlib.h>
#include "ccs-client.h"
#include "ccs-client.c"
#include "PythonCCS-client.h"
#include <string.h>
#include <string>
#include <iostream>
#include <readline/readline.h>
#include <readline/history.h>
#include <assert.h>

int main (int argc, char** argv) {

  if (argc<3) {
      std::cerr << "Usage: taco host port [simulation]" << std::endl;
      return 1;
      }

  CcsServer server;
  char *host=argv[1];
  int port=atoi(argv[2]);

  int ret = CcsConnect (&server, host, port, NULL);
  assert(ret != -1);
  if(argc > 3) {
      CcsSendRequest (&server, "ChooseSimulation", 0, strlen(argv[3]), argv[3]);
      CcsRecvResponse (&server, 4, &ret, 1000);
      }

  int interpreterHandle = 0;
  read_history(".taco_history");
  
  while(1) {
      char *prompt = "Yes, master?";
      
      /* Get a line from the user. */
      char *my_gets_line = readline (prompt);

      /* If the line has any text in it, save it on the history. */
      if (my_gets_line && *my_gets_line)
	add_history (my_gets_line);
      else
	  continue;

      if(!strcmp(my_gets_line, "quit")) {
	  CcsSendRequest (&server, "ShutdownServer", 0, 0, 0);
	  break;
	  }
	  
      PythonExecute wrapper(my_gets_line, true, true, htonl(interpreterHandle));
      wrapper.setHighLevel(true);
      wrapper.setKeepPrint(true);
      wrapper.setPersistent(true);
      wrapper.setWait(true);
      CcsSendRequest (&server, "ExecutePythonCode", 0, wrapper.size(), wrapper.pack());
      char data[4];
            
      CcsRecvResponse (&server, 4, data, 1000);
      if(interpreterHandle == 0)
	  interpreterHandle = ntohl(*((int *) data));
      
      printf("handler: %d\n",interpreterHandle);
      // Possible bug in pack() method.
      // PythonPrint request(interpreterHandle);
      PythonPrint request(htonl(interpreterHandle));
      request.setWait(false);
      CcsSendRequest (&server, "ExecutePythonCode", 0, request.size(), request.pack());
      char *buffer;
      int msg_size;
      
      int len = CcsRecvResponseMsg(&server, &msg_size, (void **) &buffer, 1000);
      if(len != -1) {
	  buffer[len] = 0;
	  printf("response: %s\n",buffer);
	  free(buffer);
	  }
      else {
	  printf("bad response?\n");
	  }
	  
      free(my_gets_line);
      }
  stifle_history(50);
  write_history(".taco_history");
  return 0;
}
