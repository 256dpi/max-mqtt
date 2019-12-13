# max-mqtt

**mqtt java (mxj) object for max**

This mxj object capable java class wraps the [Java MQTT Client](https://eclipse.org/paho/clients/java/) of the eclipse paho project.

Use this object to allow your patches to communicate around the world using your own MQTT broker or an hosted service like [shiftr.io](https://shiftr.io).

![max-mqtt](http://joel-github-static.s3.amazonaws.com/max-mqtt/max-mqtt3.png)

The object currently supports only a basic set of features to get going. If you need any specific functionality please create an issue.

## Download

[Download the latest version of the object here.](https://github.com/256dpi/max-mqtt/releases)

## Installation

The zip file includes 3 files that have to be added to your Max.app directory. Open the applications folder by right-clicking and selecting 'Show package contents'.

Now place the files as the following matrix shows:

- `mqtt.jar` > `Contents/Resources/C74/java-classes/lib`
- `mqtt.class` > `Contents/Resources/C74/java-classes/classes`
- `mqtt.maxhelp` > `Contents/Resources/C74/java-classes/help`
