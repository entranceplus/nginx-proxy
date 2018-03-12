(ns docker
  (:gen-class)
  (:require [clojure.string :as str]
            [selmer.parser :as selmer]
            [clojure.java.io :as io])
  (:import (com.spotify.docker.client DockerClient
                                      DockerClient$ListContainersParam
                                      DockerClient$ExecCreateParam
                                      DockerClient$ExecStartParameter
                                      DockerClient$EventsParam
                                      DefaultDockerClient)))

(defn new-docker []
  (.build (DefaultDockerClient/fromEnv)))

(defn list-containers [docker]
  (.listContainers docker (make-array DockerClient$ListContainersParam 0)))

(defn exec-command [docker cmd container]
  (let [exec-creation (.execCreate docker (.id container)
                                          (into-array String cmd)
                                          (into-array DockerClient$ExecCreateParam
                                            [(DockerClient$ExecCreateParam/attachStdout)
                                             (DockerClient$ExecCreateParam/attachStderr)]))]
    (-> docker
      (.execStart (.id exec-creation) (into-array DockerClient$ExecStartParameter nil))
      .readFully)))

(defn inspect-container [docker container]
  (.inspectContainer docker (.id container)))

(defn get-ip [docker container]
  (->> container
       (inspect-container docker)
       .networkSettings
       .networks
       first
       .getValue
       .ipAddress))


(defn parse-env-key [key]
  (str/lower-case (str/join "" (rest (str/split key #"_")))))

(defn parse-env-value [value]
  (let [[host port] (str/split value #":")]
    {:host host
     :port port}))

(defn parse-env [env]
  (parse-env-value (second (str/split env #"="))))


(defn get-env [docker container]
  (->> container
       (inspect-container docker)
       .config
       .env
       (filter (fn [env]
                 (str/starts-with? env "APP")))
       (map parse-env)))

;
; (def stream (-> (new-docker)
;                 (.events (into-array DockerClient$EventsParam nil))))
;
; (.next stream)

(defn read-nginx []
  (-> "nginx.conf"
      io/resource
      slurp))

(defn write-nginx [contents]
  (spit "/etc/nginx/conf.d/default.conf" contents))

(defn collect-app-data [docker container]
  (map (fn [env-data]
         (merge env-data
           {:ip (get-ip docker container)}))
    (get-env docker container)))

(defn get-nginx-container [docker]
  (->> docker
       list-containers
       (filter (fn [container]
                 (some #(str/includes? % "nginx-proxy") (.names container))))
      first))

(defn reload-nginx [docker]
  (exec-command docker '("nginx" "-s" "reload")
                        (get-nginx-container docker)))

(defn -main [& args]
  (let [docker (new-docker)
        containers (-> docker list-containers)
        apps (flatten (map #(collect-app-data docker %) containers))]
       (println "Containers found " (-> docker list-containers count))
       (println "Apps found" apps)
      (-> (read-nginx)
          (selmer/render {:apps apps})
          write-nginx)
      (reload-nginx docker)))
