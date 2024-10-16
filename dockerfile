FROM openjdk:21-slim

WORKDIR /app

COPY build/libs/AxsessGard-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /etc/axsessgard
COPY ./src/main/resources/config/* /etc/axsessgard/
COPY ./src/main/resources/sample_data.txt /etc/sample_data.txt


ENV AXSG_PUBLIC_KEY=MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEzsLb427Ewa4dWp51L6ZcVrPpEYBzT6vcQaCmmR6BhmN1EoFxzFo3NiLmTh9CyonldHgI05ns8D54sn4jPnRJew==
ENV AXSG_PRIVATE_KEY=MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgncREArQ4aGEbETfG4Xnco73k3Z7nCYhDzfPUrpa5uJahRANCAATOwtvjbsTBrh1annUvplxWs+kRgHNPq9xBoKaZHoGGY3USgXHMWjc2IuZOH0LKieV0eAjTmezwPniyfiM+dEl7
ENV AXSG_ALGO_TYPE=EC
ENV AXSG_ALGO=ECDSA256
ENV AXSG_INIT_DATA=/etc/sample_data.txt

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=production

ENTRYPOINT ["java", "-jar", "app.jar"]