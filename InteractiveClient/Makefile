CLASSES = CcsServer.class CcsThread.class JSliderOldValue.class Vector3D.class ColorMapDisplay.class Config.class InteractiveWindow.class ViewingPanel.class InteractiveClient.class CommandPane.class InputPanel.class ConfigPanel.class SubsetPanel.class QuantitativePanel.class ProfilePanel.class ManualPanel.class CommandPrompt.class Storage.class Point3D.class DuplicateView.class SelectionView.class

TARGET = InteractiveClient
VERSION = 0.1
all:: $(TARGET).jar

$(TARGET).jar: Manifest $(CLASSES)
	jar cmf Manifest $@ *.class

.java.class :
	javac $<

.SUFFIXES: .java .class

dist::
	mkdir $(TARGET)-$(VERSION)
	cp Makefile Manifest *.java $(TARGET)-$(VERSION)/
	tar zcf $(TARGET)-$(VERSION).tar.gz $(TARGET)-$(VERSION)
	rm -Rf $(TARGET)-$(VERSION)
	
clean::
	rm -f *.class *.jar
	
