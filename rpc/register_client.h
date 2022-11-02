#ifndef _REG_CLIENT_H
#define _REG_CLIENT_H

#include <grpcpp/grpcpp.h>
#include <stdint.h>

#include <iostream>
#include <memory>
#include <set>
#include <string>
#include <vector>

#include "result.h"
#include "register.grpc.pb.h"

using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using register_service::CreateRequest;
using register_service::DeleteRequest;
using register_service::GetRequest;
using register_service::MultiRegisterArray;
using register_service::ReadItemRequest;
using register_service::RegisterArray;
using register_service::RegisterService;
using register_service::Response;
using register_service::WriteItemRequest;

// RegisterServiceClient class: DON'T MODIFY
class RegisterServiceClient {
   public:
    RegisterServiceClient(std::shared_ptr<Channel> channel, std::ostream& out);
    Result* GetAllRegisterArrays();
    Result* GetRegisterArray(std::string name);
    Result* CreateRegisterArray(std::string name, uint32_t capacity);
    Result* DeleteRegisterArray(std::string name);
    Result* ReadValue(std::string name, uint32_t index);
    Result* WriteValue(std::string name, uint32_t index, uint32_t value);

   private:
    std::unique_ptr<RegisterService::Stub> stub_;
    std::ostream& out_;

    void OutputRegisterArray(::google::protobuf::internal::RepeatedPtrIterator<RegisterArray> it);
};

#endif