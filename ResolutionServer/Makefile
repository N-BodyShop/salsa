STRUCTURES_PATH = ../structures
BOOST_FLAGS = -ftemplate-depth-100 -DNDEBUG -finline-functions -Wno-inline -I/usr/include/python2.2 -I/users/gwl/Projects/boost
CXXFLAGS += -g -Wall -I$(STRUCTURES_PATH) -I$(XERCESCROOT)/include -I../SimulationHandler $(BOOST_FLAGS) -I.
LDFLAGS += -L$(STRUCTURES_PATH) -language charm++ -L$(XERCESCROOT)/lib -memory paranoid #-static
LDLIBS += -lTipsy -lpopt -module liveViz -module PythonCCS -lxerces-c /usr/lib/python2.2/config/libpython2.2.a -lutil -lpthread

CHARMC = $(HOME)/peak/src/charm/bin/charmc
CXX = $(CHARMC)
CC = $(CXX)

TARGET = ResolutionServer
VERSION = 0.2-beta2
all: $(TARGET)

$(TARGET).o: $(TARGET).decl.h Reductions.decl.h

Reductions.o: Reductions.decl.h ParticleStatistics.h

$(TARGET): Worker.o MetaInformationHandler.o Reductions.o HighLevelPython.o ../SimulationHandler/SiXFormat.o ../SimulationHandler/TipsyFormat.o

ResolutionServer.o: PythonInstance.h
Worker.o ResolutionServer.o MetaInformationHandler.o: ResolutionServer.h ../SimulationHandler/Simulation.h
Worker.o: PixelDrawing.cpp ../SimulationHandler/Group.h

%.decl.h %.def.h : %.ci
	$(CHARMC) $<

.PHONY: all clean docs dist binarydist

docs:
	doxygen $(TARGET).doxygen

dist:
	mkdir $(TARGET)-$(VERSION)
	cp Makefile $(TARGET).doxygen *.h *.cpp *.ci $(TARGET)-$(VERSION)/
	tar zcf $(TARGET)-$(VERSION).tar.gz $(TARGET)-$(VERSION)
	rm -Rf $(TARGET)-$(VERSION)

clean:
	rm -f core* *.o *~ $(TARGET) *.decl.h *.def.h charmrun conv-host 

binarydist: all
	mkdir -p $(HOME)/public/$(TARGET)-$(VERSION)
	cp charmrun $(TARGET) simlist nodelist run_server.sh wrapper $(HOME)/public/$(TARGET)-$(VERSION)
	#mkdir $(TARGET)-$(VERSION)
	#cp charmrun $(TARGET) simlist nodelist run_server.sh $(TARGET)-$(VERSION)
	#tar zcf $(TARGET)-$(VERSION).tar.gz $(TARGET)-$(VERSION)
	#rm -Rf $(TARGET)-$(VERSION)
	
