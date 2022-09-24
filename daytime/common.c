#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <stdlib.h>
#include <strings.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include "common.h"

void dumpMessage(message* msg) {
    printf("message: %i | %i | %i | %s | %s| %s |\n",msg->addrlen, msg->timelen, msg->msglen, msg->addr, msg->currtime, msg->payload);
}

void constructSockAddr(struct sockaddr_in* sockaddr,  char* address, int port) {
    bzero(sockaddr, sizeof(*sockaddr));
    sockaddr->sin_family = AF_INET;
    sockaddr->sin_port = htons(port);  /* daytime server */
    if (inet_pton(AF_INET, address, &(sockaddr->sin_addr)) <= 0) {
        printf("inet_pton error for %s\n", address);
        exit(1);
    }
}

int nameFromAddress(char* address, char* port, char* hostname, int hostnamelen){
    struct sockaddr_in* sockaddr;
    constructSockAddr(sockaddr, address, atoi(port));
    if (getnameinfo((struct sockaddr *)sockaddr, sizeof(*sockaddr), 
            hostname, hostnamelen, NULL, 0, 0) < 0) {
        printf("failed to find server hostname\n");
        return -1;
    }
    return 0;
}