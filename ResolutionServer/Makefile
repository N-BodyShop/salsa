STRUCTURES_PATH = ../structures
BOOST_FLAGS = -ftemplate-depth-100 -DNDEBUG -DBOOST_PYTHON_DYNAMIC_LIB -finline-functions -Wno-inline -I/usr/include/python2.2 -I/users/gwl/Projects/boost
CXXFLAGS += -g -O3 -Wall -I$(STRUCTURES_PATH) -I$(XERCESCROOT)/include -I../SimulationHandler $(BOOST_FLAGS)
BOOST_LDFLAGS = -L/usr/lib/python2.2/config -L/users/gwl/Projects/boost_install/lib
LDFLAGS += -L$(STRUCTURES_PATH) -language charm++ -L$(XERCESCROOT)/lib $(BOOST_LDFLAGS) #-static
BOOST_LDLIBS = -lboost_python-gcc -lpython2.2 -ldl -lutil -lpthread  -Wl,-rpath-link,.
LDLIBS += -lTipsy -lpopt -module liveViz -lxerces-c $(BOOST_LDLIBS)

CHARMC = $(HOME)/Projects/charm/net-linux/bin/charmc
CXX = $(CHARMC)
CC = $(CXX)

TARGET = ResolutionServer
VERSION = 0.2-beta2
all: $(TARGET)

$(TARGET).o: $(TARGET).decl.h Reductions.decl.h

Reductions.o: Reductions.decl.h ParticleStatistics.h

$(TARGET): Worker.o MetaInformationHandler.o Reductions.o ResolutionServerPython.o ../SimulationHandler/SiXFormat.o ../SimulationHandler/TipsyFormat.o

ResolutionServer.o: PythonInstance.h
Worker.o ResolutionServer.o MetaInformationHandler.o: ResolutionServer.h ../SimulationHandler/Simulation.h
ResolutionServerPython.o: ResolutionServer.h PythonInstance.h export_Vector3D.h
Worker.o: PixelDrawing.cpp

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
	
