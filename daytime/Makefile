# compiler to use
CC=gcc

# enable debug symbols and warnings
CFLAGS=-c -Wall
BUILDDIR=build

all: dir server client tunnel

dir:
	mkdir -p $(BUILDDIR)

server: $(BUILDDIR)/server.o $(BUILDDIR)/common.o
	$(CC) $(BUILDDIR)/server.o $(BUILDDIR)/common.o -o $(BUILDDIR)/server

client: $(BUILDDIR)/client.o $(BUILDDIR)/common.o
	$(CC) $(BUILDDIR)/client.o $(BUILDDIR)/common.o -o $(BUILDDIR)/client

tunnel: $(BUILDDIR)/tunnel.o $(BUILDDIR)/common.o
	$(CC) $(BUILDDIR)/tunnel.o $(BUILDDIR)/common.o -o $(BUILDDIR)/tunnel


$(BUILDDIR)/common.o: common.c 
	$(CC) $(CFLAGS) common.c -o $(BUILDDIR)/common.o

$(BUILDDIR)/server.o: server.c
	$(CC) $(CFLAGS) server.c -o $(BUILDDIR)/server.o

$(BUILDDIR)/client.o: client.c
	$(CC) $(CFLAGS) client.c -o $(BUILDDIR)/client.o

$(BUILDDIR)/tunnel.o: tunnel.c
	$(CC) $(CFLAGS) tunnel.c -o $(BUILDDIR)/tunnel.o


clean:
	rm -rf $(BUILDDIR)

