#ifndef __CLIENT_H
#define __CLIENT_H

#define MAXLINE     4096    /* max text line length */
#define LISTENQ     1024    /* 2nd argument to listen() */

int parseArgs(char* argument, char* port, struct sockaddr_in* address, char* hostname);

void constructServaddr(struct sockaddr_in* servaddr,  char* address, int port);

#endif // __CLIENT_H