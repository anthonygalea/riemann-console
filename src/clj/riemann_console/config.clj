(ns riemann-console.config
  (:require [duratom.core :as duratom])
  (:refer-clojure :exclude [get get-in]))

(def config-path (or (System/getenv "RIEMANN_CONSOLE_CONFIG")
                     "./riemann-console.edn"))

(def config
  (duratom/duratom
   :local-file
   :file-path config-path
   :init {:port 5557
          :default-dashboard-name "Riemann Console"
          :default-endpoint "127.0.0.1:5556"}))

(defn get
  ([key]
   (clojure.core/get @config key))
  ([key not-found]
   (clojure.core/get @config key not-found)))

(defn get-in
  ([ks]
   (clojure.core/get-in @config ks))
  ([ks not-found]
   (clojure.core/get-in @config ks not-found)))

;; dashboard config

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn- one-empty-dashboard []
  (let [id (uuid)]
    {id {:name (get :default-dashboard-name)
         :endpoint (get :default-endpoint)
         :widgets {}}}))

(defn get-dashboard-names
  []
  (when (zero? (count (get :dashboards)))
    (swap! config assoc :dashboards (one-empty-dashboard)))

  (->> (get :dashboards)
       (map (fn [[k v]]
              [k (:name v)]))
       (into {})))

(defn get-dashboard
  [id]
  (get-in [:dashboards id]))

(defn save-dashboard
  ([dashboard-config]
   (save-dashboard (uuid) dashboard-config))
  ([id dashboard-config]
   (let [new-dashboard {:name (get :default-dashboard-name)
                        :endpoint (get :default-endpoint)}
         dashboard-config (merge new-dashboard dashboard-config)]
     (swap! config assoc-in [:dashboards id] dashboard-config)
     (assoc dashboard-config :id id))))

(defn update-dashboard
  [id dashboard-config]
  (swap! config update-in [:dashboards id] merge dashboard-config))

(defn delete-dashboard
  [id]
  (swap! config update-in [:dashboards] dissoc id))
