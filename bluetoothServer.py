#!/usr/bin/python

import bluetooth
import thread
import os

server_sock=bluetooth.BluetoothSocket( bluetooth.RFCOMM )

port = 0
server_sock.bind(("",port))
server_sock.listen(1)
print "listening on port %d" % port


uuid = "00001101-0000-1000-8000-00805F9B34FB"

def listen(input_client_sock):
	while(1):
		incomingData = "nothing"
		incomingData = input_client_sock.recv(1024)
		if incomingData != "nothing":		
			f = open('image', 'a+')
			f.write(incomingData)
			f.close()
			print("read the data")

def write(client_sock, data):
	client_sock.send(data)

def run(client_sock):
	while(1):
		inputData = input('')
		print "Sending: " + inputData
		write(client_sock, inputData)
		os.system('cls' if os.name == 'nt' else 'clear')

bluetooth.advertise_service( server_sock, "FooBar Service", uuid )

client_sock,address = server_sock.accept()
print "Accepted connection from ",address

print "Running a conversation"


try:
	thread.start_new_thread(listen, (client_sock, ))
	thread.start_new_thread(run, (client_sock, ))
except:
	print "Unable to start the listen thread"
while 1:
	pass

client_sock.close()
server_sock.close()


