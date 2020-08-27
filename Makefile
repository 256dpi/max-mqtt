all: compile install bundle

# Download JAR from:
# https://repo.eclipse.org/content/repositories/paho-releases/org/eclipse/paho/org.eclipse.paho.client.mqttv3

compile:
	/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/bin/javac -d ./out -cp ".:./lib/mqtt.jar:/Applications/Max.app/Contents/Resources/C74/packages/max-mxj/java-classes/lib/max.jar" src/mqtt.java

install: compile
	mkdir -p ~/Documents/Max\ 8/Packages/mqtt/help
	mkdir -p ~/Documents/Max\ 8/Packages/mqtt/java-classes/lib
	mkdir -p ~/Documents/Max\ 8/Packages/mqtt/java-classes/classes
	cp help/mqtt.maxhelp ~/Documents/Max\ 8/Packages/mqtt/help
	cp lib/mqtt.jar ~/Documents/Max\ 8/Packages/mqtt/java-classes/lib
	cp out/mqtt.class ~/Documents/Max\ 8/Packages/mqtt/java-classes/classes

bundle:
	rm mqtt.zip
	rm -rf ./mqtt
	mkdir -p ./mqtt/help
	mkdir -p ./mqtt/java-classes/lib
	mkdir -p ./mqtt/java-classes/classes
	cp ./lib/mqtt.jar ./mqtt/java-classes/lib
	cp ./out/mqtt.class ./mqtt/java-classes/classes
	cp ./help/mqtt.maxhelp ./mqtt/help
	cp README.md ./mqtt
	zip mqtt.zip ./mqtt/*
	rm -rf ./mqtt
