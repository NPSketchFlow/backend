# udp_listener_1.py
# Usage: python udp_listener_1.py --local-port 9877 --server-host 127.0.0.1 --server-port 9876 --listen-seconds 60
# This script binds a UDP socket, sends a HEARTBEAT from the same socket (so server records IP:port),
# then listens for incoming JSON notifications and prints them.

import socket
import json
import time
import argparse

DEFAULT_LOCAL_PORT = 9877
DEFAULT_SERVER_HOST = '127.0.0.1'
DEFAULT_SERVER_PORT = 9876
DEFAULT_LISTEN_SECONDS = 60

parser = argparse.ArgumentParser(description='UDP listener that binds, sends heartbeat, then listens')
parser.add_argument('--local-port', type=int, default=DEFAULT_LOCAL_PORT)
parser.add_argument('--server-host', default=DEFAULT_SERVER_HOST)
parser.add_argument('--server-port', type=int, default=DEFAULT_SERVER_PORT)
parser.add_argument('--listen-seconds', type=int, default=DEFAULT_LISTEN_SECONDS)
args = parser.parse_args()

LISTEN_HOST = '127.0.0.1'
LISTEN_PORT = args.local_port
SERVER_HOST = args.server_host
SERVER_PORT = args.server_port
LISTEN_SECONDS = args.listen_seconds

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

# Try to read an immediate ACK with short timeout
s.settimeout(2.0)
try:
    data, addr = s.recvfrom(4096)
    try:
        text = data.decode('utf-8')
    except Exception:
        text = repr(data)
    print(f"Immediate reply from {addr}: {text}")
except socket.timeout:
    print("No immediate ACK reply (timeout)")
except Exception as e:
    print(f"Error waiting for immediate reply: {e}")

# Now listen for incoming notifications for a limited time
print(f"Listening for incoming messages on {LISTEN_HOST}:{LISTEN_PORT} for {LISTEN_SECONDS} seconds...")
end_time = time.time() + LISTEN_SECONDS
s.settimeout(1.0)
try:
    while time.time() < end_time:
        try:
            data, addr = s.recvfrom(65536)
            try:
                msg = json.loads(data.decode('utf-8'))
                print(f"[Client1] From {addr} Payload JSON: {msg}")
            except Exception:
                print(f"[Client1] From {addr} Raw: {data!r}")
        except socket.timeout:
            continue
except KeyboardInterrupt:
    print("Interrupted by user")
finally:
    try:
        s.close()
    except Exception:
        pass
    print("Socket closed")
