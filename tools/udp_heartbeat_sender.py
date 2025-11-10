# udp_heartbeat_sender.py
"""
Sends a simple HEARTBEAT UDP packet to the server and waits for an ACK (if any).
This script handles the Windows-specific "[WinError 10054] An existing connection was forcibly closed by the remote host"
which often appears when the remote host sends an ICMP Port Unreachable (i.e. nothing is listening on the target UDP port).

Usage:
    python udp_heartbeat_sender.py --server-port 9876 --local-port 60000

Behavior:
 - Sends a single heartbeat from LOCAL_PORT so the server sees the client source address:port
 - Waits for up to `timeout_sec` seconds for a reply
 - Retries sending `send_attempts` times if desired
 - Specifically catches and explains WinError 10054 to avoid confusing tracebacks during viva/demo
"""

import socket
import json
import time
import argparse

# Defaults
DEFAULT_LOCAL_PORT = 60001
DEFAULT_SERVER_HOST = '127.0.0.1'
DEFAULT_SERVER_PORT = 9876  # update if your server uses a different UDP port
DEFAULT_TIMEOUT_SEC = 2.0
DEFAULT_SEND_ATTEMPTS = 1


def send_heartbeat(local_port=DEFAULT_LOCAL_PORT, server_host=DEFAULT_SERVER_HOST, server_port=DEFAULT_SERVER_PORT,
                   timeout=DEFAULT_TIMEOUT_SEC, attempts=DEFAULT_SEND_ATTEMPTS):
    server = (server_host, server_port)

    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Bind to a local port so server can reply to that exact port
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

    for attempt in range(1, attempts + 1):
        try:
            print(f"Sending heartbeat to {server_host}:{server_port} from local port {local_port} (attempt {attempt})")
            s.sendto(heartbeat, server)

            # Wait for a reply (ACK)
            msg, addr = s.recvfrom(2048)
            print("Received reply from", addr, ":", msg)
            break

        except socket.timeout:
            print(f"No reply within {timeout} seconds (attempt {attempt}).")
            # On timeout, we may retry (loop continues)
        except OSError as e:
            # Windows maps ICMP Port Unreachable to WSAECONNRESET which appears as WinError 10054
            win_err = getattr(e, 'winerror', None)
            err_no = getattr(e, 'errno', None)
            if win_err == 10054 or err_no == 10054:
                print(f"Received WinError 10054 (connection forcibly closed) when waiting for reply.\n"
                      "This typically means the remote host responded with ICMP Port Unreachable"
                      " (no UDP listener on the server port).\n"
                      "Quick checks: 1) Is the backend running? 2) Is the backend UDP port the same as this script's --server-port?\n"
                      "3) Is a firewall blocking UDP? 4) Did you bind your listener to the same local port?\n")
            else:
                print(f"OSError while waiting for reply: {e} (winerror={win_err}, errno={err_no})")
            # Do not spam; stop after this error
            break
        except Exception as e:
            print(f"Unexpected error: {e}")
            break
    else:
        # Reached if loop completes without break
        print("All attempts finished without receiving a reply.")

    s.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Send a UDP heartbeat to the server and wait for ACK')
    parser.add_argument('--local-port', type=int, default=DEFAULT_LOCAL_PORT, help='local source port to bind')
    parser.add_argument('--server-host', default=DEFAULT_SERVER_HOST, help='UDP server host')
    parser.add_argument('--server-port', type=int, default=DEFAULT_SERVER_PORT, help='UDP server port')
    parser.add_argument('--timeout', type=float, default=DEFAULT_TIMEOUT_SEC, help='recv timeout seconds')
    parser.add_argument('--attempts', type=int, default=DEFAULT_SEND_ATTEMPTS, help='number of send attempts')

    args = parser.parse_args()

    print(f"Using server {args.server_host}:{args.server_port}, local port {args.local_port}")
    send_heartbeat(local_port=args.local_port, server_host=args.server_host, server_port=args.server_port,
                   timeout=args.timeout, attempts=args.attempts)
