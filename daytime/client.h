#ifndef __CLIENT_H
#define __CLIENT_H
#include "common.h"

int parseArgs(char* argument, char* port, struct sockaddr_in* address, char* hostname);



void readMessage(message* msg, char* recvbuff);

void printResult(message* msg, struct sockaddr_in* servaddr, char* hostname);
#endif // __CLIENT_H