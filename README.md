# TCP over UDP
This project's objective is to help implementing a TCP protocol based on UDP **for educational purposes**.<br />

## Background
TCP and UDP are both protocols,built on top of the Internet Protocol, used for sending bits of data known as packets.<br />
TCP, the most common protocol on the Internet, stands for Transmission Control Protocol. TCP guarantees in-order transmission of packets and makes sure the recipient recieves all the packets, in other words, TCP guarantees reliability, there is no lost or corrupted packets in TCP transmission.<br />
On the other hand, UDP is unreliable and fast. UDP stands for User Datagram Protocol, a datagram is the same as a data packet. UDP does not guarantees the Receipt of data by the reciever, this means that if a packet is lost, there is no way the reciever can claim it again, however, since all those reliability overheads are gone, it works considerably faster.<br />
## Introduction
This project will help implementing a TCP protocol using UDP transmission, students are not allowed to use tcp transmission and they are asked to implement the differences between these two protocols and transmit using udp. For example, tcp 3-way handshake or the send/recieve methods should be implemented by students themselves. In this regard, we have implemented a new version of Datagram Socket called Enhanced Datagram Socket that simplifize assessment and evaluation of the TCP protocol implemented by students. It is an obligation for students to transmit packets using Enhanced Datagram Socket.

### Enhanced Datagram Socket
Enhanced Datagram Socket extends JAVA DatagramSocket, therefore, it aquires all the properties and behaviours of its parent. Students must pay attention that they can use all the methods and fields of DatagramSocket that is inherited to Enhanced Datagram Socket, however, they can never use DatagramSocket itself for transmitting data.<br />
In EnhancedDatagramSocket.java the default value of payload limit, loss rate, delay and the sampling period of sent bytes are adjustable in order to simulate the characteristics of an actual link. Enhanced Datagram Socket sends packets of data based on the loss rate and the delay specified in this file. Simultanously, it plots the bytes of data sent per milliseconds, this way the actual transmission of data can be assessed.

### TCP Socket
Students are asked to complete this class in TCPSocketImpl.java, there are five methods in TCPSocketImpl that students have to complete: send, receive, close, getSSThreshold and getWindowSize.<br />
the ** send ** method, sends the file that its address is recieved in input to the destination, and ** recieve ** method, will recieve data and write it to the input address.<br />
send and receive should be implemented based on the automatic repeat-request (ARQ) protocol assigned to students by their teaching assistant, for example, if students are asked to implement Go-Back-N protocol, the send and recieve in both TCP Socket and TCP Server Socket should be based on Go-Back-N. For implementing these protocols you can use this link [Selective Repeat / Go Back N](http://www.ccs-labs.org/teaching/rn/animations/gbn_sr/). The reliability of the implemented TCP will be validated by increasing the loss rate during transmission.<br />


## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

This project is implemented in JAVA, therefore, in order to run the codes, you need to have JAVA installed on your computer.<br />
project-jdk-name="1.8" <br />
project-jdk-type="JavaSDK" <br />

### Running the tests

A step by step series of examples that tell you how to get a development env running

Say what the step will be

```
Give the example
```

And repeat

```
until finished
```

End with an example of getting some data out of the system or using it for a little demo


## Deployment

Add additional notes about how to deploy this on a live system

## Built With

* [Java8](https://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html/) - Development Platform
* [Maven](https://maven.apache.org/) - Dependency Management


## Authors

* **Alireza  Roshanzamir**
* **Nicky Bayat**

