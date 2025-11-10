import socket
import json
import time
import argparse

DEFAULT_LOCAL_PORT = 9877
DEFAULT_SERVER_HOST = '127.0.0.1'
DEFAULT_SERVER_PORT = 9876

parser = argparse.ArgumentParser(description='UDP listener that binds, sends heartbeat, then listens')
parser.add_argument('--local-port', type=int, default=DEFAULT_LOCAL_PORT)
parser.add_argument('--server-host', default=DEFAULT_SERVER_HOST)
parser.add_argument('--server-port', type=int, default=DEFAULT_SERVER_PORT)
args = parser.parse_args()

LISTEN_HOST = '127.0.0.1'
LISTEN_PORT = args.local_port
SERVER_HOST = args.server_host
SERVER_PORT = args.server_port

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
try:
    s.bind((LISTEN_HOST, LISTEN_PORT))
except OSError as e:
    print(f"Failed to bind {LISTEN_HOST}:{LISTEN_PORT}: {e}")
    raise

print(f"Client1 listening on {LISTEN_HOST}:{LISTEN_PORT}")

# send a heartbeat from the same socket so the server records this client's address:port
heartbeat = json.dumps({
    "type": "HEARTBEAT",
    "userId": f"client1-{LISTEN_PORT}",
    "timestamp": int(time.time() * 1000)
}).encode('utf-8')
try:
    s.sendto(heartbeat, (SERVER_HOST, SERVER_PORT))
    print(f"Client1 sent heartbeat to {SERVER_HOST}:{SERVER_PORT} from {LISTEN_HOST}:{LISTEN_PORT}")
except Exception as e:
    print(f"Client1 failed to send heartbeat: {e}")

while True:
    data, addr = s.recvfrom(65536)
    try:
        msg = json.loads(data.decode('utf-8'))
        print(f"[Client1] From {addr} Payload JSON: {msg}")
    except Exception:
        print(f"[Client1] From {addr} Raw: {data!r}")
