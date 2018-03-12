(def project 'nginx-proxy)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"src" "resources"}
          :dependencies '[[org.clojure/clojure "1.9.0"]
                          [com.spotify/docker-client "8.11.1"]
                          [proto-repl "0.3.1"]
                          [selmer "1.11.7"]])

(task-options!
  aot {:namespace #{'docker}}
  jar {:main 'docker
       :file (str "nginx-proxy.jar")}
  pom {:project project
       :version version})

(deftask build
  []
  (comp (aot)
        (pom)
        (uber  :exclude #{#"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
                          #"(?i)^META-INF\\[^/]*\.(MF|SF|RSA|DSA)$"
                          #"(?i)^META-INF/INDEX.LIST$"
                          #"(?i)^META-INF\\INDEX.LIST$"})
        (jar)
        (target)))
