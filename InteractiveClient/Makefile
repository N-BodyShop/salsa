CLASSES = CcsServer.class CcsThread.class Vector3D.class Config.class PngEncoder.class Simulation.class ConnectFrame.class ChooseSimulationFrame.class SimulationFrame.class ReColorFrame.class Simulation.class NameValue.class ColorBarPanel.class ToolBarPanel.class ViewPanel.class RightClickMenu.class RotateSlider.class RotatedViewFrame.class MovieMakerFrame.class SelectGroupFrame.class SelectGroupPanel.class Family.class Group.class GroupPiece.class ActivateGroup.class Salsa.class AuthenticationRequest.class ListSimulations.class ChooseSimulation.class

TARGET = Salsa
VERSION = 0.1
all:: $(TARGET).jar

$(TARGET).jar: Manifest $(CLASSES)
	jar cmf Manifest $@ *.class
	tar zcf salsa.tar.gz $@ salsa

.SUFFIXES: .java .class

.java.class :
	javac $<

#CcsServer.class : CcsServer.java
#	javac CcsServer.java
#CcsThread.class : CcsThread.java
#	javac CcsThread.java
#JSliderOldValue.class : JSliderOldValue.java
#	javac JSliderOldValue.java
#Vector3D.class : Vector3D.java
#	javac Vector3D.java
#ColorMapDisplay.class : ColorMapDisplay.java
#	javac ColorMapDisplay.java
#Config.class : Config.java
#	javac Config.java
#CommandPane.class : CommandPane.java
#	javac CommandPane.java
#InputPanel.class : InputPanel.java
#	javac InputPanel.java
#ConfigPanel.class : ConfigPanel.java
#	javac ConfigPanel.java
#SubsetPanel.class : SubsetPanel.java
#	javac SubsetPanel.java
#QuantitativePanel.class : QuantitativePanel.java
#	javac QuantitativePanel.java
#ProfilePanel.class : ProfilePanel.java
#	javac ProfilePanel.java
#ManualPanel.class : ManualPanel.java
#	javac ManualPanel.java
#CommandPrompt.class : CommandPrompt.java
#	javac CommandPrompt.java
#ParentPanel.class : ParentPanel.java
#	javac ParentPanel.java
#MainView.class : MainView.java
#	javac MainView.java
#CommandParser.class : CommandParser.java
#	javac CommandParser.java
#PngEncoder.class : PngEncoder.java
#	javac PngEncoder.java
#Connection.class : Connection.java
#	javac Connection.java
#Simulation.class : Simulation.java
#	javac Simulation.java
#ConnectFrame.class : ConnectFrame.java
#	javac ConnectFrame.java
#ChooseSimulationFrame.class : ChooseSimulationFrame.java
#	javac ChooseSimulationFrame.java
#SimulationFrame.class : SimulationFrame.java
#	javac SimulationFrame.java
#Simulation.class : Simulation.java
#	javac Simulation.java
#ReColorFrame.class : ReColorFrame.java
#	javac ReColorFrame.java


dist::
	mkdir $(TARGET)-$(VERSION)
	cp Makefile Manifest *.java $(TARGET)-$(VERSION)/
	tar zcf $(TARGET)-$(VERSION).tar.gz $(TARGET)-$(VERSION)
	rm -Rf $(TARGET)-$(VERSION)
	
clean::
	rm -f *.class *.jar
	
