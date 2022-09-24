#ifndef __COMMON_H
#define __COMMON_H

#define MAXLINE     4096    /* max text line length */
#define LISTENQ     1024    /* 2nd argument to listen() */

typedef struct message{
    int addrlen, timelen, msglen;
    char addr[MAXLINE];
    char currtime[MAXLINE];
    char payload[MAXLINE];
} message;

void dumpMessage(message* msg);

void constructSockAddr(struct sockaddr_in* servaddr,  char* address, int port);

int nameFromAddress(char* address, char* port, char* hostname, int hostnamelen);

#endif // __COMMON_H