(defproject lime-izard "1.0.0-SNAPSHOT"
  :description "Lime Izard Server"
  :url "http://lime-izard.herokuapp.com"
  :license {:name "Eclipse Public License v1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]
                 [environ "1.0.0"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [crypto-random "1.2.0"]
                 [yesql "0.5.2"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [slingshot "0.12.2"]
                 [liberator "0.14.1"]]
  :min-lein-version "2.0.0"
  :plugins [[environ/environ.lein "0.3.1"]]
  :hooks [environ.leiningen.hooks]
  :uberjar-name "lime-izard-standalone.jar"
  :profiles {:production {:env {:production true}}})
