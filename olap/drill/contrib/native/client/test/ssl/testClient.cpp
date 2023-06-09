/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <cstdlib>
#include <iostream>
#include <boost/bind.hpp>
#include <boost/asio.hpp>
#include <boost/asio/ssl.hpp>

enum { max_length = 1024 };

class client
{
    public:
        client(boost::asio::io_service& io_service, boost::asio::ssl::context& context,
                boost::asio::ip::tcp::resolver::iterator endpoint_iterator)
            : socket_(io_service, context)
        {
            boost::asio::ip::tcp::endpoint endpoint = *endpoint_iterator;
            socket_.lowest_layer().async_connect(endpoint,
                    boost::bind(&client::handle_connect, this,
                        boost::asio::placeholders::error, ++endpoint_iterator));
        }

        void handle_connect(const boost::system::error_code& error,
                boost::asio::ip::tcp::resolver::iterator endpoint_iterator)
        {
            if (!error)
            {
                socket_.async_handshake(boost::asio::ssl::stream_base::client,
                        boost::bind(&client::handle_handshake, this,
                            boost::asio::placeholders::error));
            }
            else if (endpoint_iterator != boost::asio::ip::tcp::resolver::iterator())
            {
                socket_.lowest_layer().close();
                boost::asio::ip::tcp::endpoint endpoint = *endpoint_iterator;
                socket_.lowest_layer().async_connect(endpoint,
                        boost::bind(&client::handle_connect, this,
                            boost::asio::placeholders::error, ++endpoint_iterator));
            }
            else
            {
                std::cout << "Connect failed: " << error << "\n";
            }
        }

        void handle_handshake(const boost::system::error_code& error)
        {
            if (!error)
            {
                std::cout << "Enter message: ";
                std::cin.getline(request_, max_length);
                size_t request_length = strlen(request_);

                boost::asio::async_write(socket_,
                        boost::asio::buffer(request_, request_length),
                        boost::bind(&client::handle_write, this,
                            boost::asio::placeholders::error,
                            boost::asio::placeholders::bytes_transferred));
            }
            else
            {
                std::cout << "Handshake failed: " << error << "\n";
            }
        }

        void handle_write(const boost::system::error_code& error,
                size_t bytes_transferred)
        {
            if (!error)
            {
                boost::asio::async_read(socket_,
                        boost::asio::buffer(reply_, bytes_transferred),
                        boost::bind(&client::handle_read, this,
                            boost::asio::placeholders::error,
                            boost::asio::placeholders::bytes_transferred));
            }
            else
            {
                std::cout << "Write failed: " << error << "\n";
            }
        }

        void handle_read(const boost::system::error_code& error,
                size_t bytes_transferred)
        {
            if (!error)
            {
                std::cout << "Reply: ";
                std::cout.write(reply_, bytes_transferred);
                std::cout << "\n";
            }
            else
            {
                std::cout << "Read failed: " << error << "\n";
            }
        }

    private:
        boost::asio::ssl::stream<boost::asio::ip::tcp::socket> socket_;
        char request_[max_length];
        char reply_[max_length];
};

int main(int argc, char* argv[])
{
    try
    {
        if (argc != 3)
        {
            std::cerr << "Usage: client <host> <port>\n";
            return 1;
        }

        boost::asio::io_service io_service;

        boost::asio::ip::tcp::resolver resolver(io_service);
        boost::asio::ip::tcp::resolver::query query(argv[1], argv[2]);
        boost::asio::ip::tcp::resolver::iterator iterator = resolver.resolve(query);

        boost::asio::ssl::context ctx(boost::asio::ssl::context::sslv23);
        ctx.set_verify_mode(boost::asio::ssl::context::verify_peer);
        ctx.load_verify_file("../../../test/ssl/drillTestCert.pem");

        client c(io_service, ctx, iterator);

        io_service.run();
    }
    catch (std::exception& e)
    {
        std::cerr << "Exception: " << e.what() << "\n";
    }

    return 0;
}
