cmake_minimum_required(VERSION 3.22.1)
project("obf")

add_library(obf SHARED obf.cpp)

find_library(log-lib log)
target_link_libraries(obf ${log-lib})
