#ifndef __SERVER_H
#define __SERVER_H
#include "common.h"

void initializeMessage(message* msg, char* addr, char* currtime, char* payload);

int writeMessage(int fd, message* msg);

#endif // __SERVER_H