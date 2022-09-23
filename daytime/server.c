#include <netinet/in.h>
#include <sys/socket.h>
#include <netdb.h>
#include <time.h>
#include <stdlib.h>
#include <strings.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include "server.h"

int main(int argc, char **argv)
{
    int listenfd, connfd;
    struct sockaddr_in servaddr;
    time_t ticks;

    if (argc != 2) {
        printf("usage: server <port_number>\n");
        exit(1);
    }

    listenfd = socket(AF_INET, SOCK_STREAM, 0);

    bzero(&servaddr, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons(atoi(argv[1])); /* daytime server */

    bind(listenfd, (struct sockaddr *) &servaddr, sizeof(servaddr));

    listen(listenfd, LISTENQ);

    printf("Server is open on port: %i\n", ntohs(servaddr.sin_port));

    for ( ; ; ) {
        connfd = accept(listenfd, (struct sockaddr *) NULL, NULL);
        ticks = time(NULL);
        
        message msg; 
        char addrbuff[MAXLINE];
        char timebuff[MAXLINE];
        char payloadbuff[MAXLINE];

        snprintf(addrbuff, MAXLINE, "test");
        snprintf(timebuff, MAXLINE, "%.24s", ctime(&ticks));
        snprintf(payloadbuff, MAXLINE, "shit");

        initializeMessage(&msg, addrbuff, timebuff, payloadbuff);
        writeMessage(connfd, &msg);

        close(connfd);
    }
} 

void initializeMessage(message* msg, char* addr, char* currtime, char* payload) {
    msg->addrlen = strlen(addr);
    msg->timelen = strlen(currtime);
    strncpy(msg->addr, addr, strlen(addr));
    strncpy(msg->currtime, currtime, strlen(currtime));
    strncpy(msg->payload, payload, strlen(payload));
    msg->msglen = 3*sizeof(int) + strlen(addr) + strlen(currtime) + strlen(payload);
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
    
    printf("about to send.\n");
    printMessage(msg);

    write(fd, buff, msg->msglen);
    printf("Sending response: %s", buff);

    return 0;
}