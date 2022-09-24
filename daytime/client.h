#ifndef __CLIENT_H
#define __CLIENT_H
#include "common.h"

int parseArgs(char* argument, char* port, struct sockaddr_in* address, char* hostname);

void printResult(message* msg, struct sockaddr_in* servaddr, char* hostname);

void printResultTunnel(message* msg, struct sockaddr_in* servaddr, char* servname, struct sockaddr_in* tunneladdr, char* tunnelname, char* tunnelport);

#endif // __CLIENT_H