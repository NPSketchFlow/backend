# udp_client_listener.py
"""
Bind a UDP socket to a local port, send a HEARTBEAT to server from that socket, then listen for incoming packets (ACK or broadcasts).
This avoids the problem of two separate processes trying to bind the same local port.

Usage:
    python udp_client_listener.py --local-port 60000 --server-host 127.0.0.1 --server-port 9876

Behavior:
 - Binds one socket to local port
 - Sends a heartbeat JSON from that socket to server
 - Waits for ACK and then continues to receive messages for `listen_seconds`
"""

import socket
import json
import time
import argparse

DEFAULT_LOCAL_PORT = 60000
DEFAULT_SERVER_HOST = '127.0.0.1'
DEFAULT_SERVER_PORT = 4096
DEFAULT_TIMEOUT = 5.0
DEFAULT_LISTEN_SECONDS = 30


def run(local_port=DEFAULT_LOCAL_PORT, server_host=DEFAULT_SERVER_HOST, server_port=DEFAULT_SERVER_PORT,
        timeout=DEFAULT_TIMEOUT, listen_seconds=DEFAULT_LISTEN_SECONDS):
    server = (server_host, server_port)

    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.bind(('0.0.0.0', local_port))
    except OSError as e:
        print(f"Failed to bind local port {local_port}: {e}")
        s.close()
        return

    s.settimeout(timeout)

    heartbeat = json.dumps({
        "type": "HEARTBEAT",
        "userId": "demo-user-1",
        "timestamp": int(time.time() * 1000)
    }).encode('utf-8')

    print(f"Sending heartbeat from local port {local_port} to server {server_host}:{server_port}")
    s.sendto(heartbeat, server)

    # Try to read an ACK (short timeout)
    try:
        msg, addr = s.recvfrom(4096)
        print("Immediate reply from", addr, ":", msg)
    except socket.timeout:
        print(f"No immediate reply within {timeout}s (may still have registered as online)")
    except OSError as e:
        print(f"Socket error while waiting for ACK: {e}")

    # Now keep listening for broadcasts/messages for a while
    print(f"Listening for incoming messages on port {local_port} for {listen_seconds} seconds...")
    end_time = time.time() + listen_seconds
    s.settimeout(1.0)
    try:
        while time.time() < end_time:
            try:
                data, addr = s.recvfrom(8192)
                print(f"RECV from {addr}: {data.decode('utf-8', errors='replace')}")
            except socket.timeout:
                # no packet in this second; loop
                continue
    finally:
        s.close()
        print("Socket closed")


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--local-port', type=int, default=DEFAULT_LOCAL_PORT)
    parser.add_argument('--server-host', default=DEFAULT_SERVER_HOST)
    parser.add_argument('--server-port', type=int, default=DEFAULT_SERVER_PORT)
    parser.add_argument('--timeout', type=float, default=DEFAULT_TIMEOUT)
    parser.add_argument('--listen-seconds', type=int, default=DEFAULT_LISTEN_SECONDS)
    args = parser.parse_args()
    print(f"Binding local port {args.local_port}, server {args.server_host}:{args.server_port}")
    run(local_port=args.local_port, server_host=args.server_host, server_port=args.server_port,
        timeout=args.timeout, listen_seconds=args.listen_seconds)

