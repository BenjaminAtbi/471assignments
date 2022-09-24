#ifndef __SERVER_H
#define __SERVER_H
#include "common.h"

void initializeMessage(message* msg, char* addr, char* currtime, char* payload);

int writeMessage(int fd, message* msg);

int runWhoCmd(char* buf);

int printClient(struct sockaddr* clientaddr, char* port);

#endif // __SERVER_H