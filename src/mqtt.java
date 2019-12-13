import com.cycling74.max.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class mqtt extends MaxObject implements MqttCallbackExtended {
  private static final String[] INLET_ASSIST = new String[]{
      "send commands to the mqtt client"
  };
  private static final String[] OUTLET_ASSIST = new String[]{
      "a number representing the connection state",
      "incoming messages with topic and payload"
  };

  private MqttClient client;
  private String uri = "tcp://localhost";
  private String id = "";

  public mqtt(Atom[] args) {
    // configure object
    configure(args);

    // declare inlets and outlets
    declareInlets(new int[]{DataTypes.ALL});
    declareOutlets(new int[]{DataTypes.INT, DataTypes.MESSAGE});

    // declare inlet and outlet assists
    setInletAssist(INLET_ASSIST);
    setOutletAssist(OUTLET_ASSIST);
  }

  public void configure(Atom[] args) {
    // check if URI is given
    if (args.length > 0) {
      this.uri = args[0].getString();
    }

    // check if id is given
    if (args.length > 1) {
      this.id = args[1].getString();
    }
  }

  public void connect() {
    // parse URI
    URI uri;
    try {
      uri = new URI(this.uri);
    } catch (URISyntaxException e) {
      post("failed to parse URI: " + e.getMessage());
      return;
    }

    try {
      // prepare options
      MqttConnectOptions options = new MqttConnectOptions();

      // enable reconnection (initial timeout 1s then doubled until 2min)
      options.setAutomaticReconnect(true);

      // add username and password
      if (uri.getUserInfo() != null) {
        String[] auth = uri.getUserInfo().split(":");
        if (auth.length > 0) {
          String user = auth[0];
          String pass = auth[1];
          options.setUserName(user);
          options.setPassword(pass.toCharArray());
        }
      }

      // create client
      if (uri.getPort() != -1) {
        client = new MqttClient("tcp://" + uri.getHost() + ":" + uri.getPort(), this.id, new MemoryPersistence());
      } else {
        client = new MqttClient("tcp://" + uri.getHost(), this.id, new MemoryPersistence());
      }

      // set callback
      client.setCallback(this);

      // connect client
      client.connect(options);

      // signal connected
      outlet(0, 1);
    } catch (Exception e) {
      // post error
      post("failed to connect: " + e.getMessage());
    }
  }

  public void subscribe(Atom[] args) {
    // subscribe to topic
    if (args.length == 1) {
      try {
        client.subscribe(args[0].toString(), 0);
      } catch (Exception e) {
        // post error
        post("failed to subscribe: " + e.getMessage());
      }
    }
  }

  public void publish(Atom[] args) {
    // publish message
    if (args.length == 2) {
      try {
        client.publish(args[0].toString(), args[1].toString().getBytes(StandardCharsets.UTF_8), 0, false);
      } catch (Exception e) {
        // post error
        post("failed to publish: " + e.getMessage());
      }
    }
  }

  public void unsubscribe(Atom[] args) {
    // unsubscribe topic
    if (args.length == 1) {
      try {
        client.unsubscribe(args[0].toString());
      } catch (MqttException e) {
        // post error
        post("failed to unsubscribe: " + e.getMessage());
      }
    }
  }

  public void disconnect() {
    // disconnect client
    try {
      client.disconnect();
      outlet(0, 0);
    } catch (MqttException e) {
      // post error
      post("failed to disconnect: " + e.getMessage());
    }
  }

  public void connectComplete(boolean reconnect, String serverURI) {
    // signal connection
    outlet(0, 1);
  }

  public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    // ignore
  }

  public void messageArrived(String topic, MqttMessage mqttMessage) {
    // get payload
    String payload = new String(mqttMessage.getPayload());

    // try to signal long
    try {
      // signal message
      outlet(1, new Atom[]{Atom.newAtom(topic), Atom.newAtom(Long.parseLong(payload))});
      return;
    } catch (NumberFormatException e) {
      // do nothing
    }

    // try to signal double
    try {
      // signal message
      outlet(1, new Atom[]{Atom.newAtom(topic), Atom.newAtom(Double.parseDouble(payload))});
      return;
    } catch (NumberFormatException e) {
      // do nothing
    }

    // signal message
    outlet(1, new Atom[]{Atom.newAtom(topic), Atom.newAtom(payload)});
  }

  public void connectionLost(Throwable throwable) {
    // signal disconnection
    outlet(0, 0);
  }
}
