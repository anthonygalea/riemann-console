(ns riemann-console.server
  (:require [riemann-console.config :as config]
            [riemann-console.handler :refer [handler]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:refer-clojure :exclude [get])
  (:gen-class))

(defn -main [& args]
  (let [port (config/get :port 5557)]
    (run-jetty handler {:port port :join? false})))
