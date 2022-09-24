#include <netinet/in.h>
#include <arpa/inet.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <time.h>
#include <stdlib.h>
#include <strings.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include "tunnel.h"

int main(int argc, char **argv)
{
    int listenfd, clientfd, servfd, n;
    char recvline[MAXLINE + 1];
    struct sockaddr_in tunneladdr;
    struct sockaddr_in serveraddr;
    struct sockaddr clientaddr;
    socklen_t clientlen;
    message incoming_msg;
    message return_msg;

    if (argc != 2) {
        printf("usage: tunnel <port_number>\n");
        exit(1);
    }

    listenfd = socket(AF_INET, SOCK_STREAM, 0);

    bzero(&tunneladdr, sizeof(tunneladdr));
    tunneladdr.sin_family = AF_INET;
    tunneladdr.sin_addr.s_addr = htonl(INADDR_ANY);
    tunneladdr.sin_port = htons(atoi(argv[1]));

    bind(listenfd, (struct sockaddr *) &tunneladdr, sizeof(tunneladdr));
    listen(listenfd, LISTENQ);

    printf("Tunnel is open on port: %i\n", ntohs(tunneladdr.sin_port));

    for ( ; ; ) {
        bzero(&clientaddr, sizeof(clientaddr));
        clientlen = sizeof(clientaddr);
        clientfd = accept(listenfd, &clientaddr, &clientlen);
        

        if((n = read(clientfd, recvline, MAXLINE)) < 0) {
            printf("read error %i\n", errno);
            exit(1);
        }
        recvline[n] = 0;        /* null terminate */
        readMessage(&incoming_msg, recvline);
        dumpMessage(&incoming_msg);

        printf("opening socket\n");
        if ( (servfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
            printf("socket error\n");
            exit(1);
        }
        
        printf("open socket\n");

        printf("|%s|%i|\n",incoming_msg.addr, atoi(incoming_msg.payload));
        constructSockAddr(&serveraddr, incoming_msg.addr, atoi(incoming_msg.payload));

        if (connect(servfd, (struct sockaddr *) &serveraddr, sizeof(serveraddr)) < 0) {
            printf("error connecting to server\n");
            exit(1);
        }

        while ( (n = read(servfd, recvline, MAXLINE)) > 0) {
            recvline[n] = 0;        /* null terminate */
            readMessage(&return_msg, recvline);
            dumpMessage(&return_msg);   
        }

        if (n < 0) {
            printf("read error %i\n", errno);
            exit(1);
        }

        if(writeMessage(clientfd, &return_msg) < 0) {
            printf("error writing message back to client\n");
            exit(1);
        } 
        close(clientfd);
    }
}