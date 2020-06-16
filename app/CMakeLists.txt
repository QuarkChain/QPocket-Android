cmake_minimum_required(VERSION 3.6.0)

add_library( # Sets the name of the library.
             scrypt                        #c/cpp代码将要编译成为so库的名称，java代码加载库文件要用这个名称
             SHARED
             src/main/cpp/scrypt_jni.c      #c代码文件路径
             src/main/cpp/crypto_scrypt-nosse.c  #cpp代码文件路径 这里可以随意添加c、c++文件
             src/main/cpp/sha256.c
              )

include_directories(.)