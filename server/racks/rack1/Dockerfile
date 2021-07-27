FROM ubuntu:18.04
LABEL MAINTAINER Jaswanth Bommidi <mail@evilsyn.com>

RUN apt update; apt -y --autoremove full-upgrade
ARG DEBIAN_FRONTEND=noninteractive
RUN apt install vsftpd ftp -y

# RUN apt-get autoremove
# RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

COPY vsftpd.conf /etc/vsftpd.conf
COPY entrypoint.sh /root/start.sh

RUN bash -c "useradd -m racka -s /bin/bash"
RUN bash -c "echo racka:rackapassword | chpasswd"

ENTRYPOINT [ "bash", "/root/start.sh"]