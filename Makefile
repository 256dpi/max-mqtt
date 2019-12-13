compile:
	/Library/Java/JavaVirtualMachines/jdk1.8.0_191.jdk/Contents/Home/bin/javac -d ./out -cp ".:./lib/mqtt.jar:/Applications/Max.app/Contents/Resources/C74/packages/max-mxj/java-classes/lib/max.jar" src/mqtt.java

install:
	cp lib/mqtt.jar /Applications/Max.app/Contents/Resources/C74/packages/max-mxj/java-classes/lib
	cp out/mqtt.class /Applications/Max.app/Contents/Resources/C74/packages/max-mxj/java-classes/classes
	cp help/mqtt.maxhelp /Applications/Max.app/Contents/Resources/C74/packages/max-mxj/java-classes/help

bundle:
	rm mqtt.zip
	rm -rf ./build
	mkdir -p ./build
	cp ./lib/mqtt.jar ./build/
	cp ./out/mqtt.class ./build/
	cp ./help/mqtt.maxhelp ./build/
	cp README.md ./build/
	zip mqtt.zip ./build/*
