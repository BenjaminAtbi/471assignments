#ifndef _REG_SERVER_H
#define _REG_SERVER_H

#include <grpcpp/ext/proto_server_reflection_plugin.h>
#include <grpcpp/grpcpp.h>
#include <grpcpp/health_check_service_interface.h>

#include <iostream>
#include <map>
#include <memory>
#include <set>
#include <string>

#include "register.grpc.pb.h"

using grpc::Server;
using grpc::ServerBuilder;
using grpc::ServerContext;
using grpc::Status;
using grpc::StatusCode;
using register_service::RegisterArray;
using register_service::RegisterService;
using register_service::Response;

// RegisterServiceImpl class: DON'T MODIFY
class RegisterServiceImpl final : public RegisterService::Service {
   public:
    RegisterServiceImpl();

    ::grpc::Status GetAllRegisterArrays(::grpc::ServerContext* context,
                                        const ::google::protobuf::Empty* request,
                                        ::register_service::Response* response);

    ::grpc::Status GetRegisterArray(::grpc::ServerContext* context,
                                    const ::register_service::GetRequest* request,
                                    ::register_service::Response* response);

    ::grpc::Status CreateRegisterArray(::grpc::ServerContext* context,
                                       const ::register_service::CreateRequest* request,
                                       ::register_service::Response* response);

    ::grpc::Status DeleteRegisterArray(::grpc::ServerContext* context,
                                       const ::register_service::DeleteRequest* request,
                                       ::register_service::Response* response);

    ::grpc::Status ReadValue(::grpc::ServerContext* context,
                             const ::register_service::ReadItemRequest* request,
                             ::register_service::Response* response);

    ::grpc::Status WriteValue(::grpc::ServerContext* context,
                              const ::register_service::WriteItemRequest* request,
                              ::register_service::Response* response);

   private:
    std::map<std::string, RegisterArray*> _registers;
    std::map<std::string, std::set<uint32_t>*> _written_indices;

    void BuildRegisterArrayResponse(::register_service::Response* response,
                               std::map<std::string, RegisterArray*>::iterator it);
};

#endif