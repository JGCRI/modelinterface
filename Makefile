
# Java compiler options to target specific JVM versions to allow
# backwards compatibility.
JAVA_TARGET = -target 1.7 -source 1.7

ifeq ($(strip $(BUILD_DIR)),)
	BUILD_DIR = ./
endif

ifeq ($(strip $(MANIFEST_OFFSET)),)
	MANIFEST_OFFSET = ./
endif

all: CSVToXML.jar

CSVToXML.jar: ModelInterface/ModelGUI2/csvconv/*.class
	cd $(BUILD_DIR) && jar -cmf $(MANIFEST_OFFSET)/MANIFEST.MK $@ ModelInterface

ModelInterface/ModelGUI2/csvconv/%.class: ModelInterface/ModelGUI2/csvconv/%.java
	javac $(JAVA_TARGET) -d $(BUILD_DIR) $^

# NOTE: dependency tracking is handled internally by javac however
# is very flakey and we recommnded to always do a clean build.
ModelInterface.jar: clean_MI ModelInterface/InterfaceMain.class
	cd $(BUILD_DIR) && jar -cmf $(MANIFEST_OFFSET)/MANIFEST_MI.MK $@ ModelInterface

ModelInterface/InterfaceMain.class: ModelInterface/InterfaceMain.java
	# NOTE: The third party jars are assumed to be listed in the CLASSPATH
	# environment variable
	javac $(JAVA_TARGET) -d $(BUILD_DIR) $^

ModelInterface/ModelGUI2/xmldb/RunMIQuery.class: ModelInterface/ModelGUI2/xmldb/RunMIQuery.java
	# NOTE: The third party jars are assumed to be listed in the CLASSPATH
	# environment variable
	javac $(JAVA_TARGET) -d $(BUILD_DIR) $^

# minimal build of CSVToXML.jar for the gcamdata package
gcamdata:
	mkdir -p gcamdata-build
	$(MAKE) BUILD_DIR=gcamdata-build MANIFEST_OFFSET=../ CSVToXML.jar

# minimal build of ModelInterface.jar for the rgcam package
rgcam: clean_MI
	mkdir -p rgcam-build
	$(MAKE) BUILD_DIR=rgcam-build MANIFEST_OFFSET=../ ModelInterface/ModelGUI2/xmldb/RunMIQuery.class
	cd rgcam-build && jar -cf ModelInterface.jar ModelInterface

clean:
	rm -f CSVToXML.jar
	rm -f ModelInterface.jar
	find ModelInterface -name *.class -delete

clean_CSVToXML:
	rm -f CSVToXML.jar
	find ModelInterface -name *.class -delete

clean_MI:
	rm -f ModelInterface.jar
	find ModelInterface -name *.class -delete

clean_gcamdata:
	rm -r gcamdata-build

clean_rgcam:
	rm -r rgcam-build

