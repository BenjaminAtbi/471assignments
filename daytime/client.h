#ifndef __CLIENT_H
#define __CLIENT_H
#include "common.h"

int parseArgs(char* argument, char* port, struct sockaddr_in* address, char* hostname);

void constructServaddr(struct sockaddr_in* servaddr,  char* address, int port);

void readMessage(message* msg, char* recvbuff);
#endif // __CLIENT_H