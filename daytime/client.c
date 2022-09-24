#include <netinet/in.h>
#include <errno.h>
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
#include <regex.h>    
#include "client.h"

int main(int argc, char **argv)
{
    int sockfd, n;
    char recvline[MAXLINE + 1];
    struct sockaddr_in servaddr;
    struct sockaddr_in tunneladdr;
    char server_hostname[MAXLINE];
    char tunnel_hostname[MAXLINE];
    char* serv_addr_arg;
    char* serv_port_arg;
    char* tunnel_addr_arg;
    char* tunnel_port_arg;

    if (argc == 3){
        serv_addr_arg = argv[1];
        serv_port_arg = argv[2];
    } else if (argc == 5){
        tunnel_addr_arg = argv[1];
        tunnel_port_arg = argv[2];
        serv_addr_arg = argv[3];
        serv_port_arg = argv[4];
    } else {
        printf("usage: client <Server Hostname/IP_address> <Server port_number> or client <Tunnel Hostname/IP_address> <Tunnel port_number> <Server Hostname/IP_address> <Server port_number>\n");
        exit(1);
    }

    if( parseArgs(serv_addr_arg, serv_port_arg, &servaddr, server_hostname) < 0) {
        printf("unable to find server by given hostname/address\n");
        exit(1);
    }

    if ( (sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        printf("socket error\n");
        exit(1);
    }

    //If no tunnel, ping server
    if(argc == 3) {
        if (connect(sockfd, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0) {
            printf("connect error with server\n");
            exit(1);
        }
    //If tunnel, ping server through it
    } else {

        if(parseArgs(tunnel_addr_arg, tunnel_port_arg, &tunneladdr, tunnel_hostname) < 0) {
            printf("unable to find tunnel by given hostname/address\n");
            exit(1);
        }

        if (connect(sockfd, (struct sockaddr *) &tunneladdr, sizeof(tunneladdr)) < 0) {
            printf("connect error with tunnel\n");
            exit(1);
        }

        message msg;
        char addrbuff[MAXLINE];
        char timebuff[MAXLINE];
        time_t ticks = time(NULL);
        snprintf(timebuff, MAXLINE, "%.24s", ctime(&ticks));
        inet_ntop(AF_INET, &servaddr.sin_addr, addrbuff, MAXLINE);

        initializeMessage(&msg, addrbuff, timebuff, serv_port_arg);
        if( writeMessage(sockfd, &msg) < 0) {
            printf("error writing message to tunnel\n");
            exit(1);
        } 
    }

    //pick up response from server or tunnel
    while ( (n = read(sockfd, recvline, MAXLINE)) > 0) {
        recvline[n] = 0;        /* null terminate */
        message msg;
        readMessage(&msg, recvline);
        if(argc == 3){
            printResult(&msg, &servaddr, server_hostname); 
        } else {
            printResultTunnel(&msg, &servaddr, server_hostname, &tunneladdr, tunnel_hostname, tunnel_port_arg); 
        }
    }

    if (n < 0) {
        printf("read error %i\n", errno);
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

        if(nameFromAddress(argument, port, hostname, MAXLINE) < 0){
            printf("error getting hostname\n");
            return -1;
        }

        constructSockAddr(servaddr, argument, atoi(port));
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
            printf("failed to find address\n");
            return -1;
        }
        memcpy(servaddr, addr_info_res->ai_addr, sizeof(*addr_info_res->ai_addr));
        strcpy(hostname, argument);
        freeaddrinfo(addr_info_res);

        char msgbuf[MAXLINE];
        inet_ntop(AF_INET, &servaddr->sin_addr, msgbuf, MAXLINE);
        return 0;
    }

    char msgbuf[100];
    regerror(addrmatch, &regex, msgbuf, sizeof(msgbuf));
    fprintf(stderr, "Regex match failed: %s\n", msgbuf);
    return -1;

}

void printResult(message* msg, struct sockaddr_in* servaddr, char* hostname){
    char addrbuf[MAXLINE];
    inet_ntop(AF_INET, &servaddr->sin_addr, addrbuf, MAXLINE);
    
    printf("Server Name: %s\n", hostname);
    printf("IP Address: %s\n", addrbuf);
    printf("Time: %s\n", msg->currtime);
    printf("who: %s\n", msg->payload);
}

void printResultTunnel(message* msg, struct sockaddr_in* servaddr, char* servname, struct sockaddr_in* tunneladdr, char* tunnelname, char* tunnelport){
    
    char servaddrbuf[MAXLINE];
    inet_ntop(AF_INET, &servaddr->sin_addr, servaddrbuf, MAXLINE);
    char tunneladdrbuf[MAXLINE];
    inet_ntop(AF_INET, &tunneladdr->sin_addr, tunneladdrbuf, MAXLINE);

    printf("Server Name: %s\n", servname);
    printf("IP Address: %s\n", servaddrbuf);
    printf("Time: %s\n\n", msg->currtime);
    printf("Via Tunnel: %s\n", tunnelname);
    printf("IP Address: %s\n", tunneladdrbuf);
    printf("Port Number: %s\n\n", tunnelport);
    printf("who: %s\n", msg->payload);
}