STRUCTURES_PATH = ../../utility/structures

CXXFLAGS += -g -Wall -I$(STRUCTURES_PATH) -I$(XERCESCROOT)/include -I../../utility/SimulationHandler $(BOOST_FLAGS) -I$(PYTHON_INCLUDE_DIR) -I.
LDFLAGS +=  -Wl,-rpath,$(XERCESCROOT)/lib -L$(STRUCTURES_PATH) -language charm++ -L$(XERCESCROOT)/lib #-static
LDLIBS += -lTipsy -module liveViz -module PythonCCS -lxerces-c -lutil $(PYTHON_LIB) $(LIBS)

PYTHON_INCLUDE_DIR = /usr/include/python2.4
PYTHON_LIB = -L/usr/lib/python2.4/config -lpython2.4

BOOST_PATH = /usr/include
BOOST_FLAGS = -ftemplate-depth-100 -DNDEBUG -finline-functions -Wno-inline -I$(BOOST_PATH)
XERCESCROOT = /astro/users/trq/nchil/src/xerces-c-src_2_6_0

LIBS = -lrt -lpthread -lpopt 
CHARMC = ../../charm/bin/charmc
CXX = $(CHARMC)
CC = $(CXX)

TARGET = ResolutionServer
VERSION = 0.3
all: $(TARGET)

$(TARGET).o: $(TARGET).decl.h Reductions.decl.h

Reductions.o: Reductions.decl.h ParticleStatistics.h

$(TARGET): ResolutionServer.o Worker.o MetaInformationHandler.o Reductions.o HighLevelPython.o ../../utility/SimulationHandler/SiXFormat.o ../../utility/SimulationHandler/TipsyFormat.o $(STRUCTURES_PATH)/libTipsy.a
	$(CXX) -o $(TARGET) $(LDFLAGS) $+ $(LDLIBS)

$(STRUCTURES_PATH)/libTipsy.a:
	cd $(STRUCTURES_PATH); make

ResolutionServer.o: PythonInstance.h ../../utility/SimulationHandler/Group.h \
	PyObjectMarshal.h
Worker.o ResolutionServer.o MetaInformationHandler.o: ResolutionServer.h ../../utility/SimulationHandler/Simulation.h
Worker.o: PixelDrawing.cpp ../../utility/SimulationHandler/Group.h \
	../../utility/structures/TypeHandling.h ../../utility/structures/TypedArray.h Reductions.h \
	PyObjectMarshal.h
Reductions.o: PyObjectMarshal.h
HighLevelPython.o: HighLevelPython.cpp config.h ResolutionServer.h \
  ../../utility/structures/TipsyParticles.h ../../utility/structures/Vector3D.h \
  ../../utility/structures/TypeSelection.h ../../utility/structures/OrientedBox.h \
  ../../utility/structures/Shape.h ../../utility/structures/Sphere.h ../../utility/structures/Box.h \
  ../../utility/SimulationHandler/Simulation.h ../../utility/structures/tree_xdr.h \
  ../../utility/structures/OrientedBox.h ../../utility/structures/xdr_template.h \
  ../../utility/structures/config.h ../../utility/structures/TypedArray.h \
  ../../utility/structures/TypeHandling.h ../../utility/SimulationHandler/SiXFormat.h \
  ../../utility/SimulationHandler/Simulation.h \
  ParticleStatistics.h ResolutionServer.decl.h PyObjectMarshal.h \
  Reductions.decl.h ../../utility/SimulationHandler/Group.h
../../utility/SimulationHandler/SiXFormat.o: ../../utility/SimulationHandler/Simulation.h
../../utility/SimulationHandler/TipsyFormat.o: ../../utility/SimulationHandler/Simulation.h

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
	
