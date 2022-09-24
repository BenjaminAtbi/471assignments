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

void initializeMessage(message* msg, char* addr, char* currtime, char* payload) {
    msg->addrlen = strlen(addr);
    msg->timelen = strlen(currtime);
    strncpy(msg->addr, addr, strlen(addr));
    strncpy(msg->currtime, currtime, strlen(currtime));
    strncpy(msg->payload, payload, strlen(payload));
    msg->msglen = 3*sizeof(int) + strlen(addr) + strlen(currtime) + strlen(payload);
} 

void readMessage(message* msg, char* recvbuff){
    memcpy(&msg->addrlen, &recvbuff[0], sizeof(int));
    memcpy(&msg->timelen, &recvbuff[sizeof(int)], sizeof(int));
    memcpy(&msg->msglen, &recvbuff[sizeof(int)*2], sizeof(int));
    memcpy(msg->addr,recvbuff + sizeof(int)*3, msg->addrlen);
    memcpy(msg->currtime,recvbuff + sizeof(int)*3 + msg->addrlen, msg->timelen);
    strcpy(msg->payload,recvbuff + sizeof(int)*3 + msg->addrlen + msg->timelen);
}

int writeMessage(int fd, message* msg) {
    char buff[MAXLINE];

    if(msg->msglen > MAXLINE) {
        printf("message is too large to send\n");
        return -1;
    }
    memcpy(buff, &msg->addrlen, sizeof(int));
    memcpy(buff + sizeof(int), &msg->timelen, sizeof(int));
    memcpy(buff + sizeof(int)*2, &msg->msglen, sizeof(int));
    snprintf( buff + sizeof(int)*3, MAXLINE-sizeof(int)*3, "%s%s%s", msg->addr, msg->currtime, msg->payload);
    if(write(fd, buff, msg->msglen) < 0){
        printf("error writing message to socket\n");
        return -1;
    }

    return 0;
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

