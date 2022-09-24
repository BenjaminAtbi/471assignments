#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/types.h>
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
    struct sockaddr clientaddr;
    socklen_t clientlen;
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
        bzero(&clientaddr, sizeof(clientaddr));
        clientlen = sizeof(clientaddr);
        connfd = accept(listenfd, &clientaddr, &clientlen);
        ticks = time(NULL);
        
        printClient(&clientaddr, argv[1]);

        message msg; 
        char addrbuff[MAXLINE];
        char timebuff[MAXLINE];
        char payloadbuff[MAXLINE];

        snprintf(addrbuff, MAXLINE, "test");
        snprintf(timebuff, MAXLINE, "%.24s", ctime(&ticks));
        if(runWhoCmd(payloadbuff) < 0) exit(1); 

        initializeMessage(&msg, addrbuff, timebuff, payloadbuff);
        if(writeMessage(connfd, &msg) < 0) exit(1);

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

int runWhoCmd(char* output){
    FILE* fp;
    char buf[MAXLINE];
    bzero(output, MAXLINE);

    fp = popen("who", "r");
    if(fp == NULL){
        printf("error opening pipe for who command\n");
        return -1;
    }

    while(fgets(buf, MAXLINE, fp) != NULL){
        if(strlen(output) + strlen(buf) >= MAXLINE){
            printf("command output exceeds capacity");
            return -1;
        }
        strcat(output,buf);
    }
    return 0;
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

int printClient(struct sockaddr* clientaddr, char* port){
    char addrbuf[MAXLINE];
    char namebuf[MAXLINE];
    inet_ntop(AF_INET, &clientaddr->sa_data, addrbuf, MAXLINE);
    nameFromAddress(addrbuf, port, namebuf, MAXLINE);
    printf("Receiving Request\n");
    printf("Client IP Address: %s\n", addrbuf);
    printf("Client Hostname: %s\n", namebuf);
    return 0;
}