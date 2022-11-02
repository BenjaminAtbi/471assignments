include common.mk

rpc_client: $(OBJDIR)/register.pb.o $(OBJDIR)/register.grpc.pb.o $(OBJDIR)/register_client.o $(OBJDIR)/rpc_client.o
	$(CXX) -o $(BINDIR)/$@ $^ $(CXXFLAGS) $(CPPFLAGS) $(LDFLAGS)

rpc_server: $(OBJDIR)/register.pb.o $(OBJDIR)/register.grpc.pb.o $(OBJDIR)/register_server.o $(OBJDIR)/rpc_server.o
	$(CXX) -o $(BINDIR)/$@ $^ $(CXXFLAGS) $(CPPFLAGS) $(LDFLAGS)