STRUCTURES_PATH = $(HOME)/projects/structures
CXXFLAGS += -O3 -Wall -I$(STRUCTURES_PATH)
LDFLAGS += -L$(STRUCTURES_PATH) -language charm++
LDLIBS += -lTipsy -lpopt -module liveViz -memory paranoid

CHARMC = $(HOME)/projects/charm/bin/charmc
CXX = $(CHARMC)
CC = $(CXX)

TARGET = ResolutionServer
VERSION = 0.1
all:: $(TARGET)

$(TARGET).o: $(TARGET).decl.h Reductions.decl.h

Reductions.o: Reductions.decl.h ParticleStatistics.h

$(TARGET): Worker.o MetaInformationHandler.o Reductions.o

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
