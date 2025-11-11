import socket
import json
import time
import argparse

def main():
    parser = argparse.ArgumentParser(description='Simple UDP client that sends a heartbeat and listens for notifications')
    parser.add_argument('--server-ip', default='127.0.0.1')
    parser.add_argument('--server-port', type=int, default=9876)
    parser.add_argument('--local-port', type=int, default=60000)
    args = parser.parse_args()

    UDP_SERVER_IP = args.server_ip
    UDP_SERVER_PORT = args.server_port
    LOCAL_PORT = args.local_port

    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.bind(("0.0.0.0", LOCAL_PORT))
        actual_local = LOCAL_PORT
        print(f"Bound local port {LOCAL_PORT}")
    except OSError as e:
        print(f"Failed to bind local port {LOCAL_PORT}: {e}")
        print("Falling back to an ephemeral port so this client can still send and listen.")
        s.bind(("0.0.0.0", 0))
        actual_local = s.getsockname()[1]
        print(f"Bound to ephemeral local port {actual_local}")

    # Send heartbeat to server
    heartbeat = json.dumps({"type": "HEARTBEAT", "userId": "test1", "seq": 1, "timestamp": int(time.time() * 1000)}).encode()
    try:
        s.sendto(heartbeat, (UDP_SERVER_IP, UDP_SERVER_PORT))
        print(f"Heartbeat sent to {UDP_SERVER_IP}:{UDP_SERVER_PORT} from local port {actual_local}")
    except Exception as e:
        print(f"Failed to send heartbeat: {e}")
        s.close()
        return

    # Listen for notifications
    print("Listening for notifications (CTRL+C to stop)...")
    try:
        while True:
            data, addr = s.recvfrom(65536)
            try:
                msg = json.loads(data.decode())
            except Exception:
                # If not JSON, print raw
                print("Received non-JSON from", addr, data)
                continue
            print("Received from", addr, msg)
    except KeyboardInterrupt:
        print("Interrupted by user")
    finally:
        s.close()

if __name__ == '__main__':
    main()
