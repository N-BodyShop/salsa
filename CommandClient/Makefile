#
# CHARMDIR may need to be set to point at your charm installation
#
CHARMDIR=../charm
INCLC = -I$(CHARMDIR)/include
LIBS = -lreadline  -lcurses -ltermcap -lm

OBJS = server.o


all: taco

taco: taco.C $(CHARMDIR)/tmp/libs/ck-libs/pythonCCS/PythonCCS-client.C
	g++ -Wall -g $(INCLC) -o $@ $^ $(LIBS)

clean:
	rm -f *.decl.h *.def.h *.o server client charmrun *~

