FROM nginx:1.13
LABEL maintainer="Akash Shakdwipeea ashakdwipeea@gmail.com"

# Install wget and install/updates certificates
RUN apt-get update \
 && apt-get install -y -q --no-install-recommends \
    ca-certificates \
    wget \
    curl \
    rlwrap \
    bash \
    unzip \
 && apt-get clean \
 && rm -r /var/lib/apt/lists/*

# Configure Nginx and apply fix for very long server names
RUN echo "daemon off;" >> /etc/nginx/nginx.conf \
 && sed -i 's/worker_processes  1/worker_processes  auto/' /etc/nginx/nginx.conf

# Install Forego
# ADD https://github.com/jwilder/forego/releases/download/v0.16.1/forego /usr/local/bin/forego
# RUN chmod u+x /usr/local/bin/forego

# Install goreman
RUN wget https://github.com/mattn/goreman/releases/download/v0.0.10/goreman_linux_386.zip
RUN unzip goreman_linux_386.zip
RUN mv ./goreman /usr/local/bin/
RUN chmod u+x /usr/local/bin/goreman

# Install java

# jdk installation depends on this
RUN mkdir /usr/share/man/man1

# Install OpenJDK-8
RUN apt-get update && \
    apt-get install -y openjdk-8-jdk && \
    apt-get install -y ant && \
    apt-get clean;

# Fix certificate issues
RUN apt-get update && \
    apt-get install ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Setup JAVA_HOME -- useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64/
RUN export JAVA_HOME

COPY docker-entrypoint.sh /app/
COPY Procfile /app/
COPY target/nginx-proxy.jar /app/
WORKDIR /app/

ENV DOCKER_HOST unix:///tmp/docker.sock

VOLUME ["/etc/nginx/certs", "/etc/nginx/dhparam"]

ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD goreman start

# CMD tail -f /dev/null
