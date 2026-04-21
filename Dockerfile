FROM eclipse-temurin:17-jdk AS build
COPY . /app
WORKDIR /app
RUN ./gradlew :teavm:buildRelease --no-daemon

FROM nginx:alpine
COPY --from=build /app/teavm/build/dist/webapp/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
