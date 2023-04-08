FROM amazoncorretto:8

COPY . /

ENV PROFILE=prod

RUN chmod 777 start.sh

ENTRYPOINT [ "sh", "-c", "/start.sh" ]

