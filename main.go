package main

import (
	"strconv"
	"sync/atomic"
	"time"

	"github.com/256dpi/gomqtt/client"
	"github.com/256dpi/gomqtt/packet"
	"github.com/256dpi/max-go"
)

type mqtt struct {
	cmd   *max.Inlet
	state *max.Outlet
	data  *max.Outlet
	svc   *client.Service
	conn  int64
	url   string
	id    string
}

func (m *mqtt) Init(obj *max.Object, args []max.Atom) bool {
	// declare inlets
	m.cmd = obj.Inlet(max.Any, "commands", true)

	// declare outlets
	m.state = obj.Outlet(max.Int, "connection state")
	m.data = obj.Outlet(max.List, "incoming messages ")

	// create service
	m.svc = client.NewService(100)

	// drop commands if not queued withing 1ms
	m.svc.QueueTimeout = time.Millisecond

	// TODO: Log error for dropped commands.

	// register online callback
	m.svc.OnlineCallback = func(resumed bool) {
		// store state
		atomic.StoreInt64(&m.conn, 1)

		// set state
		m.state.Int(1)
	}

	// register message callback
	m.svc.MessageCallback = func(message *packet.Message) error {
		// send as list
		m.data.List([]max.Atom{message.Topic, string(message.Payload), int64(message.QOS), bool2int64(message.Retain)})

		return nil
	}

	// register error callback
	m.svc.ErrorCallback = func(err error) {
		// log error
		max.Error("mqtt: %s", err.Error())
	}

	// register offline callback
	m.svc.OfflineCallback = func() {
		// store state
		atomic.StoreInt64(&m.conn, 0)

		// set state
		m.state.Int(0)
	}

	// configure
	m.configure(args)

	return true
}

func (m *mqtt) Handle(_ int, msg string, args []max.Atom) {
	// handle message
	switch msg {
	case "configure":
		m.configure(args)
	case "connect":
		m.connect()
	case "subscribe":
		m.subscribe(args)
	case "publish":
		m.publish(args)
	case "unsubscribe":
		m.unsubscribe(args)
	case "disconnect":
		m.disconnect()
	default:
		max.Error("unknown message %s", msg)
	}
}

func (m *mqtt) configure(args []max.Atom) {
	// get url
	if len(args) > 0 {
		m.url, _ = args[0].(string)
	}

	// get id
	if len(args) > 1 {
		m.id, _ = args[1].(string)
	}

	// reconnect if connected
	if atomic.LoadInt64(&m.conn) == 1 {
		m.disconnect()
		m.connect()
	}
}

func (m *mqtt) connect() {
	// start service
	m.svc.Start(client.NewConfigWithClientID(m.url, m.id))
}

func (m *mqtt) subscribe(args []max.Atom) {
	// get topic
	var topic string
	if len(args) > 0 {
		topic, _ = args[0].(string)
	}

	// get qos
	var qos int64
	if len(args) > 1 {
		qos, _ = args[1].(int64)
	}

	// check topic
	if topic == "" {
		max.Error("missing topic")
		return
	}

	// check qos
	if !packet.QOS(qos).Successful() {
		max.Error("invalid qos")
		return
	}

	// subscribe
	m.svc.Subscribe(topic, packet.QOS(qos))
}

func (m *mqtt) publish(args []max.Atom) {
	// get topic
	var topic string
	if len(args) > 0 {
		topic, _ = args[0].(string)
	}

	// get payload
	var payload []byte
	if len(args) > 1 {
		switch arg := args[1].(type) {
		case int64:
			payload = []byte(strconv.FormatInt(arg, 10))
		case float64:
			payload = []byte(strconv.FormatFloat(arg, 'f', -1, 64))
		case string:
			payload = []byte(arg)
		}
	}

	// get qos
	var qos int64
	if len(args) > 2 {
		qos, _ = args[2].(int64)
	}

	// get retain
	var retain bool
	if len(args) > 3 && args[3] == 1 {
		retain = true
	}

	// check topic
	if topic == "" {
		max.Error("missing topic")
		return
	}

	// check qos
	if !packet.QOS(qos).Successful() {
		max.Error("invalid qos")
		return
	}

	// publish
	m.svc.Publish(topic, payload, packet.QOS(qos), retain)
}

func (m *mqtt) unsubscribe(args []max.Atom) {
	// get topic
	var topic string
	if len(args) > 0 {
		topic, _ = args[0].(string)
	}

	// check topic
	if topic == "" {
		max.Error("missing topic")
		return
	}

	// subscribe
	m.svc.Unsubscribe(topic)
}

func (m *mqtt) disconnect() {
	// stop service
	m.svc.Stop(true)
}

func (m *mqtt) Free() {
	m.disconnect()
}

func main() {
	// initialize Max class
	max.Register("mqtt", &mqtt{})
}

func bool2int64(b bool) int64 {
	if b {
		return 1
	}
	return 0
}
