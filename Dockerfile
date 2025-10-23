FROM alpine/java:17.0.12

RUN apk --no-cache add tzdata
ENV TZ=Asia/Seoul

WORKDIR /app

COPY ./build/libs/greenlight-admin-api-0.0.1-SNAPSHOT.jar /app/greenlight-admin-api.jar

EXPOSE 28080

ENTRYPOINT ["java", "-jar", "greenlight-admin-api.jar"]