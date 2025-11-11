# udp_listener.py
import socket

LISTEN_PORT = 60000
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.bind(('0.0.0.0', LISTEN_PORT))
print("UDP listener bound on port", LISTEN_PORT)
while True:
    data, addr = s.recvfrom(4096)
    print("RECV from", addr, ":", data.decode('utf-8', errors='replace'))
