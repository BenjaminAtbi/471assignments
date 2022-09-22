#ifndef __SERVER_H
#define __SERVER_H

#define MAXLINE     4096    /* max text line length */
#define LISTENQ     1024    /* 2nd argument to listen() */

void printInit(struct sockaddr_in servaddr);

#endif // __SERVERE_H