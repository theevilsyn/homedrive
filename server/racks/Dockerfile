FROM alpine:3.13
RUN apk --no-cache add vsftpd

COPY start.sh /bin/start.sh
COPY vsftpd.conf /etc/vsftpd/vsftpd.conf

EXPOSE 21 21
RUN mkdir /ftp

STOPSIGNAL SIGKILL

ENTRYPOINT ["/bin/start.sh"]