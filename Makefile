all: check build bundle

check:
	go fmt .
	go vet .
	golint .

build:
	maxgo -name mqtt -cross -install mqtt
	cp mqtt.maxhelp /Users/256dpi/Documents/Max\ 8/Packages/mqtt/help/

bundle:
	rm -f mqtt.zip
	rm -rf ./mqtt
	mkdir -p ./mqtt/help
	mkdir -p ./mqtt/externals
	cp -r ./out/mqtt.mxo ./mqtt/externals/
	cp ./out/mqtt.mxe64 ./mqtt/externals/
	cp ./mqtt.maxhelp ./mqtt/help/
	cp README.md ./mqtt/
	zip -r mqtt.zip ./mqtt
