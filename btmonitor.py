# Simple tool for talking to a Bluetooth device via rfcomm
#
# Connect to a Bluetooth device with "rfcomm -r connect 0" or similar,
# then fire up this utility with "python /dev/rfcomm0".

import threading
import sys

class ReadThread(threading.Thread):
  def __init__(self, port):
    threading.Thread.__init__(self)
    self.inp = open(port, 'rb', 0)
    self.setDaemon(1)
  def run(self):
    while 1:
      c = self.inp.read(1)
      sys.stdout.write(c)

if __name__ == '__main__':
  if len(sys.argv) > 1:
    PORT = sys.argv[1]
  else:
    PORT = '/dev/rfcomm0'
  f = open(PORT, 'wb', 0)
  ReadThread(PORT).start()
  while 1:
    try:
      s = raw_input()
      f.write(s + '\r\n')
    except EOFError:
      break

