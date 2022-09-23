#include <stdio.h>
#include "common.h"

void printMessage(message* msg) {
    printf("message: %i | %i | %i | %s | %s| %s |\n",msg->addrlen, msg->timelen, msg->msglen, msg->addr, msg->currtime, msg->payload);
}