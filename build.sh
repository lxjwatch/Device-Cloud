
mvn package -U -DskipTests


VERSION="0.0.1_$(date +%s)"

docker build -f distribution/Dockerfile -t device_apps:${VERSION} ./distribution

docker images
