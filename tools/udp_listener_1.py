import socket, zlib, json
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.bind(("127.0.0.1", 9877))
print("Client1 listening on 127.0.0.1:9877")
while True:
    data, addr = s.recvfrom(65536)
    chk = int.from_bytes(data[:4], 'big')
    payload = data[4:]
    try:
        msg = json.loads(payload.decode())
    except:
        msg = payload.decode(errors="ignore")
    print("[Client1] From", addr, "Checksum", chk, "Payload:", msg)
