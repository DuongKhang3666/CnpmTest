@echo off
if not defined JAVA_HOME set JAVA_HOME=D:\program
set PATH=%JAVA_HOME%\bin;%PATH%
call "C:\Users\ASUS\.m2\wrapper\dists\apache-maven-3.10.0\2aaba761688b1a6ea5a46e7e5f4d5406ed7a75ee8da95e5e3079863a23461c9b\bin\mvn.cmd" %*
