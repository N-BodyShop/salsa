CLASSES = Vector3D.class \
	Config.class PngEncoder.class Simulation.class SimulationView.class \
	SimulationViewGL.class \
	ColorBarPanel.class ViewingPanel.class RightClickMenu.class \
	WindowManager.class Manager.class NotifyingHashtable.class \
	RenderingPanel.class ViewListener.class ViewEvent.class \
	AttributeManager.class RotateSlider.class ColoringManager.class \
	SideBar.class SalsaRequests.class ChooseSimulationFrame.class \
	Salsa.class DelimitedStringEnumeration.class GroupManager.class \
	CodePanel.class GroupQuery.class MenuBar.class LocalParticle.class \
	HighLevelPython.class PyPrintHandler.class 
	
CLASSPATH=-classpath .:jogl.jar:gluegen-rt.jar

TARGET = Salsa
VERSION = 0.1

all: $(TARGET).jar

$(TARGET).jar: jogl.jar Manifest $(CLASSES) charm
	jar cmf Manifest $@ *.class charm

####################
# JOGL implementation to use is different in 32-bit and 64-bit linux...
JOGL_ARCHIVE32=jogl-1.1.1-linux-i586
JOGL_ARCHIVE64=jogl-1.1.1-linux-amd64
ifeq ($(shell uname -m),x86_64)
JOGL_ARCHIVE=$(JOGL_ARCHIVE64)
else
JOGL_ARCHIVE=$(JOGL_ARCHIVE32)
endif

jogl.jar: $(JOGL_ARCHIVE).zip 
	unzip -u $<
	cp $(JOGL_ARCHIVE)/lib/*.jar .
	cp $(JOGL_ARCHIVE)/lib/*.so .
	touch jogl.jar

$(JOGL_ARCHIVE32).zip: 
	wget http://download.java.net/media/jogl/builds/archive/jsr-231-1.1.1/$@

$(JOGL_ARCHIVE64).zip: 
	wget http://download.java.net/media/jogl/builds/archive/jsr-231-1.1.1/$@


charm: charm/ccs/*.java
	javac charm/ccs/*.java
	touch charm

.SUFFIXES: .java .class

.java.class :
	javac $(CLASSPATH) $<

.PHONY: all clean dist binarydist

clean:
	rm -f *.class *.jar salsa.tar.gz
	
dist:
	mkdir $(TARGET)-$(VERSION)
	cp salsa Makefile Manifest *.java $(TARGET)-$(VERSION)/
	tar zcf $(TARGET)-$(VERSION).tar.gz $(TARGET)-$(VERSION)
	rm -Rf $(TARGET)-$(VERSION)

binarydist: $(TARGET).jar jogl.jar 
	tar zcvf salsa.tar.gz salsa *.jar *.so

BINDIR=../bin
install: $(TARGET).jar jogl.jar 
	mkdir -p $(BINDIR)
	cp -r salsa *.jar *.so  $(BINDIR)


#CcsThread.class: CcsServer.class
#Config.class: Vector3D.class
#Simulation.class: CcsThread.class Vector3D.class
#WindowManager.class: CcsThread.class Simulation.class AttributeManager.class Vector3D.class Manager.class ViewingWindow.class
#SimulationView.class: WindowManager.class Vector3D.class
#ViewingWindow.class: WindowManager.class SimulationView.class ColorBarPanel.class
#ChooseSimulation.class: CcsThread.class WindowManager.class
#AttributeManager.class: Manager.class WindowManager.class Simulation.class
#Salsa.class: CcsThread.class ChooseSimulation.class
