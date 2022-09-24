#ifndef __SERVER_H
#define __SERVER_H
#include "common.h"

int runWhoCmd(char* buf);

int printClient(struct sockaddr* clientaddr, char* port);

#endif // __SERVER_H