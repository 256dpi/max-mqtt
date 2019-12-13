import com.cycling74.max.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

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
  private HashSet<String> subscriptions = new HashSet<>();

  public mqtt(Atom[] args) {
    // declare inlets and outlets
    declareInlets(new int[]{DataTypes.ALL});
    declareOutlets(new int[]{DataTypes.INT, DataTypes.MESSAGE});

    // declare inlet and outlet assists
    setInletAssist(INLET_ASSIST);
    setOutletAssist(OUTLET_ASSIST);

    // configure object
    configure(args);
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

    // reconnect if connected
    if (this.client != null && this.client.isConnected()) {
      disconnect();
      connect();
    }
  }

  public void connect() {
    // check if connected
    if (this.client != null && client.isConnected()) {
      this.disconnect();
    }

    // parse URI
    URI uri;
    try {
      uri = new URI(this.uri);
    } catch (URISyntaxException e) {
      post("connect: " + e.getMessage());
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
          options.setUserName(user);
          if (auth.length > 1) {
            String pass = auth[1];
            options.setPassword(pass.toCharArray());
          }
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
    } catch (Exception e) {
      post("connect: " + e.getMessage());
      return;
    }

    // signal connected
    outlet(0, 1);
  }

  public void subscribe(Atom[] args) {
    // check if connected
    if (this.client == null || !client.isConnected()) {
      post("subscribe: not connected");
      return;
    }

    // check args
    if (args.length < 1) {
      post("subscribe: missing topic");
      return;
    }

    // get topic
    String topic = args[0].toString();

    // add to set
    this.subscriptions.add(topic);

    // subscribe to topic
    try {
      client.subscribe(topic, 0);
    } catch (Exception e) {
      post("subscribe: " + e.getMessage());
    }
  }

  public void publish(Atom[] args) {
    // check if connected
    if (this.client == null || !client.isConnected()) {
      post("publish: not connected");
      return;
    }

    // check args
    if (args.length < 1) {
      post("publish: missing topic");
      return;
    }

    // get topic
    String topic = args[0].toString();

    // get payload
    byte[] payload = new byte[]{};
    if (args.length > 1) {
      payload = args[1].toString().getBytes(StandardCharsets.UTF_8);
    }

    // publish message
    try {
      client.publish(topic, payload, 0, false);
    } catch (Exception e) {
      post("publish: " + e.getMessage());
    }
  }

  public void unsubscribe(Atom[] args) {
    // check if connected
    if (this.client == null || !client.isConnected()) {
      post("unsubscribe: not connected");
      return;
    }

    // check args
    if (args.length < 1) {
      post("unsubscribe: missing topic");
      return;
    }

    // get topic
    String topic = args[0].toString();

    // remove from set
    this.subscriptions.remove(topic);

    // unsubscribe topic
    try {
      client.unsubscribe(topic);
    } catch (MqttException e) {
      post("unsubscribe: " + e.getMessage());
    }
  }

  public void disconnect() {
    // check if connected
    if (this.client == null || !client.isConnected()) {
      post("disconnect: not connected");
      return;
    }

    // disconnect client
    try {
      client.disconnect();
    } catch (MqttException e) {
      post("disconnect: " + e.getMessage());
    }

    // signal disconnection
    outlet(0, 0);
  }

  public void connectComplete(boolean reconnect, String serverURI) {
    // signal connection
    outlet(0, 1);

    // resubscribe topics
    for (String topic : this.subscriptions) {
      try {
        client.subscribe(topic, 0);
      } catch (Exception e) {
        post("resubscribe: " + e.getMessage());
      }
    }
  }

  public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    // ignore
  }

  public void messageArrived(String topic, MqttMessage mqttMessage) {
    // get payload
    String payload = new String(mqttMessage.getPayload());

    // signal message
    outlet(1, new Atom[]{Atom.newAtom(topic), Atom.newAtom(payload)});
  }

  public void connectionLost(Throwable throwable) {
    // signal disconnection
    outlet(0, 0);
  }
}
