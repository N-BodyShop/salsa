STRUCTURES_PATH = $(HOME)/projects/structures
CXXFLAGS += -g -Wall -I$(STRUCTURES_PATH) -I$(XERCESCROOT)/include -I../SimulationHandler
LDFLAGS += -L$(STRUCTURES_PATH) -language charm++ -L$(XERCESCROOT)/lib
LDLIBS += -lTipsy -lpopt -module liveViz -lxerces-c

CHARMC = $(HOME)/projects/charm/net-linux/bin/charmc
CXX = $(CHARMC)
CC = $(CXX)

TARGET = ResolutionServer
VERSION = 0.1
all:: $(TARGET)

$(TARGET).o: $(TARGET).decl.h Reductions.decl.h

Reductions.o: Reductions.decl.h ParticleStatistics.h

$(TARGET): Worker.o MetaInformationHandler.o Reductions.o ../SimulationHandler/SiXFormat.o ../SimulationHandler/TipsyFormat.o

Worker.o ResolutionServer.o MetaInformationHandler.o: ResolutionServer.h

%.decl.h %.def.h : %.ci
	$(CHARMC) $<

docs::
	doxygen $(TARGET).doxygen

dist::
	mkdir $(TARGET)-$(VERSION)
	cp Makefile $(TARGET).doxygen *.h *.cpp *.ci $(TARGET)-$(VERSION)/
	tar zcf $(TARGET)-$(VERSION).tar.gz $(TARGET)-$(VERSION)
	rm -Rf $(TARGET)-$(VERSION)

clean::
	rm -f core* *.o *~ $(TARGET) *.decl.h *.def.h charmrun conv-host 
