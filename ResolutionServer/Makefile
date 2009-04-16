STRUCTURES_PATH = ../structures

#OPTS=-g
OPTS=-O3 -ffast-math
CXXFLAGS += $(OPTS) -Wall -I$(STRUCTURES_PATH) -I$(XERCESCROOT)/include -I../SimulationHandler $(BOOST_FLAGS) -I$(PYTHON_INCLUDE_DIR) -I.
LDFLAGS +=  $(OPTS) -Wl,-rpath,$(XERCESCROOT)/lib -L$(STRUCTURES_PATH) -language charm++ -L$(XERCESCROOT)/lib #-static
LDLIBS += -lTipsy -module liveViz -module PythonCCS -lxerces-c -lutil $(PYTHON_LIB) $(LIBS)

PYTHON_VER=python2.5
# PYTHON_VER=python2.3
PYTHON_INCLUDE_DIR = /usr/include/$(PYTHON_VER)
PYTHON_LIB = -L/usr/lib/$(PYTHON_VER)/config -l$(PYTHON_VER)

BOOST_PATH = /usr/include
BOOST_FLAGS = -ftemplate-depth-100 -DNDEBUG -finline-functions -Wno-inline -I$(BOOST_PATH)
XERCESCROOT = /local/tmp/trq/dist/ResolutionServer-0.3/xerces-c-src_2_7_0

LIBS = -lpthread -lpopt 
CHARMC = $(CHARM_DIR)/bin/charmc
CXX = $(CHARMC)
CC = $(CXX)

TARGET = ResolutionServer
VERSION = 0.3
all: $(TARGET)

$(TARGET).o: $(TARGET).decl.h Reductions.decl.h

Reductions.o: Reductions.decl.h ParticleStatistics.h

$(TARGET): ResolutionServer.o Worker.o MetaInformationHandler.o Reductions.o HighLevelPython.o ../SimulationHandler/SiXFormat.o ../SimulationHandler/TipsyFormat.o $(STRUCTURES_PATH)/libTipsy.a
	$(CXX) -o $(TARGET) $(LDFLAGS) $+ $(LDLIBS)

$(STRUCTURES_PATH)/libTipsy.a:
	cd $(STRUCTURES_PATH); make

ResolutionServer.o: PythonInstance.h ../SimulationHandler/Group.h
Worker.o ResolutionServer.o MetaInformationHandler.o: ResolutionServer.h ../SimulationHandler/Simulation.h
Worker.o: PixelDrawing.cpp ../SimulationHandler/Group.h

%.decl.h %.def.h : %.ci
	$(CHARMC) $<

.PHONY: all clean docs dist binarydist

docs:
	doxygen $(TARGET).doxygen

dist:
	make clean
	mkdir $(TARGET)-$(VERSION)
	mkdir $(TARGET)-$(VERSION)/SimulationHandler
	mkdir $(TARGET)-$(VERSION)/structures
	mkdir $(TARGET)-$(VERSION)/InteractiveClient
	mkdir $(TARGET)-$(VERSION)/ResolutionServer
	cp README Makefile configure install-sh config.h.in Makefile.in Run_Salsa.sh run_server.sh run99.std *.h *.cpp *.ci $(TARGET)-$(VERSION)/ResolutionServer
	cp ../SimulationHandler/Makefile ../SimulationHandler/*.h ../SimulationHandler/*.cpp $(TARGET)-$(VERSION)/SimulationHandler
	cp ../structures/Makefile ../structures/config.h.in ../structures/configure ../structures/*.h ../structures/*.cpp $(TARGET)-$(VERSION)/structures
	cp ../InteractiveClient/README ../InteractiveClient/Makefile ../InteractiveClient/Manifest ../InteractiveClient/*.java $(TARGET)-$(VERSION)/InteractiveClient
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
	
