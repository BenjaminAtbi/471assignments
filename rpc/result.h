#ifndef _REG_RESULT_H
#define _REG_RESULT_H

#include <grpcpp/grpcpp.h>
#include <stdint.h>

#include <iostream>
#include <memory>
#include <set>
#include <string>
#include <vector>

#include "register.grpc.pb.h"

using grpc::Status;
using register_service::Response;

// Result class: DON'T MODIFY
class Result {
    public:
    Result(const Status& status, const Response& response): status_(status), response_(response)  {}
    Status GetStatus() const {
        return status_;
    }

    Response GetResponse() const {
        return response_;
    }

    private:
    Status status_;
    Response response_;
};

#endif