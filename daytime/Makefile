# compiler to use
CC=gcc

# enable debug symbols and warnings
CFLAGS=-c -Wall
BUILDDIR=build

all: dir server client

dir:
	mkdir $(BUILDDIR)

server: $(BUILDDIR)/server.o
	$(CC) $(BUILDDIR)/server.o -o $(BUILDDIR)/server

client: $(BUILDDIR)/client.o
	$(CC) $(BUILDDIR)/client.o -o $(BUILDDIR)/client

$(BUILDDIR)/server.o: server.c
	$(CC) $(CFLAGS) server.c -o $(BUILDDIR)/server.o

$(BUILDDIR)/client.o: client.c
	$(CC) $(CFLAGS) client.c -o $(BUILDDIR)/client.o

clean:
	rm -rf $(BUILDDIR)

