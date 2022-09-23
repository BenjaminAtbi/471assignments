# compiler to use
CC=gcc

# enable debug symbols and warnings
CFLAGS=-c -Wall
BUILDDIR=build

all: dir server client

dir:
	mkdir -p $(BUILDDIR)

server: $(BUILDDIR)/server.o $(BUILDDIR)/common.o
	$(CC) $(BUILDDIR)/server.o $(BUILDDIR)/common.o -o $(BUILDDIR)/server

client: $(BUILDDIR)/client.o $(BUILDDIR)/common.o
	$(CC) $(BUILDDIR)/client.o $(BUILDDIR)/common.o -o $(BUILDDIR)/client

$(BUILDDIR)/common.o: common.c 
	$(CC) $(CFLAGS) common.c -o $(BUILDDIR)/common.o

$(BUILDDIR)/server.o: server.c
	$(CC) $(CFLAGS) server.c -o $(BUILDDIR)/server.o

$(BUILDDIR)/client.o: client.c
	$(CC) $(CFLAGS) client.c -o $(BUILDDIR)/client.o

clean:
	rm -rf $(BUILDDIR)

