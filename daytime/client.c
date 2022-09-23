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
#include <regex.h>    
#include "client.h"

int main(int argc, char **argv)
{
    int sockfd, n;
    char recvline[MAXLINE + 1];
    struct sockaddr_in servaddr;
    char hostname[NI_MAXHOST];

    if (argc != 3) {
        printf("usage: client <Hostname/IP_address> <port_number>\n");
        exit(1);
    }

    if( parseArgs(argv[1], argv[2], &servaddr, hostname) < 0) {
        printf("unable to find server by given hostname/address\n");
        exit(1);
    }

    if ( (sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        printf("socket error\n");
        exit(1);
    }

    if (connect(sockfd, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0) {
        printf("connect error\n");
        exit(1);
    }

    while ( (n = read(sockfd, recvline, MAXLINE)) > 0) {
        recvline[n] = 0;        /* null terminate */
        message msg;
        readMessage(&msg, recvline);
        printMessage(&msg);   
    }

    if (n < 0) {
        printf("read error\n");
        exit(1);
    }

    exit(0);
}


int parseArgs(char* argument, char* port, struct sockaddr_in* servaddr, char* hostname){
    
    int addrmatch;
    regex_t regex;
    if(regcomp(&regex, "[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*", 0)){
        printf("failed to compile regex\n");
        return -1;
    }
    addrmatch = regexec(&regex, argument, 0, NULL, 0);

    //match address pattern
    if(addrmatch == 0){
        char hbuf[NI_MAXHOST];
        constructServaddr(servaddr, argument, atoi(port));
        if (getnameinfo((struct sockaddr *)servaddr, sizeof(*servaddr), 
                hbuf, sizeof(hbuf), NULL, 0, 0) < 0) {
            printf("failed to find server hostname\n");
            return -1;
        }
        memcpy(hostname, hbuf, strlen(hbuf)+1);
        printf("found server hostname: %s\n", hostname);
        return 0;
    }

    //doesn't match address pattern
    if(addrmatch == REG_NOMATCH){
        struct addrinfo* addr_info_res;
        struct addrinfo hints;

        memset(&hints, 0, sizeof(hints));
        hints.ai_family = AF_INET;
        hints.ai_socktype = SOCK_STREAM;
        
        if(getaddrinfo(argument, port, &hints, &addr_info_res) < 0) {
            printf("failed to find server address\n");
            return -1;
        }
        memcpy(servaddr, addr_info_res->ai_addr, sizeof(*addr_info_res->ai_addr));
        hostname = argument;
        freeaddrinfo(addr_info_res);

        char msgbuf[MAXLINE];
        inet_ntop(AF_INET, &servaddr->sin_addr, msgbuf, MAXLINE);
        printf("found server address: %s\n", msgbuf);
        return 0;
    }

    char msgbuf[100];
    regerror(addrmatch, &regex, msgbuf, sizeof(msgbuf));
    fprintf(stderr, "Regex match failed: %s\n", msgbuf);
    return -1;

}

void constructServaddr(struct sockaddr_in* servaddr,  char* address, int port) {
    bzero(servaddr, sizeof(*servaddr));
    servaddr->sin_family = AF_INET;
    servaddr->sin_port = htons(port);  /* daytime server */
    if (inet_pton(AF_INET, address, &(servaddr->sin_addr)) <= 0) {
        printf("inet_pton error for %s\n", address);
        exit(1);
    }
}

void readMessage(message* msg, char* recvbuff){
    msg->addrlen = recvbuff[0];
    msg->timelen = recvbuff[4];
    msg->msglen = recvbuff[8];
    memcpy(msg->addr,recvbuff + sizeof(int)*3, msg->addrlen);
    memcpy(msg->currtime,recvbuff + sizeof(int)*3 + msg->addrlen, msg->timelen);
    strcpy(msg->payload,recvbuff + sizeof(int)*3 + msg->addrlen + msg->timelen);
}