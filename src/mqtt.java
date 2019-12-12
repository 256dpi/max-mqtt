import com.cycling74.max.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class mqtt extends MaxObject implements MqttCallback {
  public String uri = "tcp://localhost";
  public String id = UUID.randomUUID().toString();

  private MqttClient client;

  private static final String[] INLET_ASSIST = new String[]{
    "send commands to the mqtt client"
  };

  private static final String[] OUTLET_ASSIST = new String[]{
    "a number representing the connection state",
    "incomming messages with topic and payload"
  };

  public mqtt(Atom[] args) {
    configure(args);

    declareInlets(new int[]{DataTypes.ALL});
    declareOutlets(new int[]{DataTypes.INT, DataTypes.MESSAGE});

    setInletAssist(INLET_ASSIST);
    setOutletAssist(OUTLET_ASSIST);
  }

  public void configure(Atom[] args) {
    if(args.length > 0) {
      this.uri = args[0].getString();
    }

    if(args.length > 1) {
      this.id = args[1].getString();
    }
  }

  public void connect() {
    URI uri = null;
    try {
      uri = new URI(this.uri);
    } catch(URISyntaxException e) {
      post("failed to parse URI: " + e.getMessage());
    }

    try {
      MqttConnectOptions options = new MqttConnectOptions();

      if (uri.getUserInfo() != null) {
        String[] auth = uri.getUserInfo().split(":");
        if(auth.length > 0) {
          String user = auth[0];
          String pass = auth[1];
          options.setUserName(user);
          options.setPassword(pass.toCharArray());
        }
      }

      if (uri.getPort()!=-1){
        client = new MqttClient("tcp://" + uri.getHost() + ":" + uri.getPort(), this.id, new MemoryPersistence());
      } else {
        client = new MqttClient("tcp://" + uri.getHost(), this.id, new MemoryPersistence());
      }

      client.setCallback(this);
      client.connect(options);
      outlet(0, 1);
    } catch (Exception e) {
      System.out.println("failed to connect: " + e.getMessage());
    }
  }

  public void subscribe(Atom args[]) {
    if(args.length == 1) {
      try {
        client.subscribe(args[0].toString(), 0);
      } catch (Exception e) {
        post("failed to subscribe: " + e.getMessage());
      }
    }
  }

  public void publish(Atom args[]) {
    if(args.length == 2) {
      try {
        client.publish(args[0].toString(), args[1].toString().getBytes("UTF-8"), 0, false);
      } catch (Exception e) {
        post("failed to publish: " + e.getMessage());
      }
    }
  }

  public void unsubscribe(Atom args[]) {
    if(args.length == 1) {
      try {
        client.unsubscribe(args[0].toString());
      } catch (MqttException e) {
        post("failed to unsubscribe: " + e.getMessage());
      }
    }
  }

  public void disconnect() {
    try {
      client.disconnect();
      outlet(0, 0);
    } catch (MqttException e) {
      post("failed to disconnect: " + e.getMessage());
    }
  }

  public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {}

  public void messageArrived(String topic, MqttMessage mqttMessage) {
    String payload = new String(mqttMessage.getPayload());

    try {
      outlet(1, new Atom[]{Atom.newAtom(topic), Atom.newAtom(Long.parseLong(payload))});
      return;
    } catch (NumberFormatException e) {
      // do nothing
    }

    try {
      outlet(1, new Atom[]{Atom.newAtom(topic), Atom.newAtom(Double.parseDouble(payload))});
      return;
    } catch(NumberFormatException e) {
      // do nothing
    }

    outlet(1, new Atom[]{Atom.newAtom(topic), Atom.newAtom(payload)});
  }

  public void connectionLost(Throwable throwable) {
    outlet(0, 0);
  }
}
