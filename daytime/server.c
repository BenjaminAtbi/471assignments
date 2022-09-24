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
    servaddr.sin_port = htons(atoi(argv[1]));

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

int printClient(struct sockaddr* clientaddr, char* port){

    char addrbuf[MAXLINE];
    char namebuf[MAXLINE];
    inet_ntop(AF_INET, &clientaddr->sa_data, addrbuf, MAXLINE);

    // if(nameFromAddress(addrbuf, port, namebuf, MAXLINE) < 0) {
    //     printf("error getting name from address\n");
    //     return -1;
    // }
    printf("Receiving Request\n");
    printf("Client IP Address: %s\n", addrbuf);
    printf("Client Hostname: %s\n", addrbuf);
    return 0;
}