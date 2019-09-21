(ns riemann-console.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [riemann-console.events :as events]
   [riemann-console.views :as views]
   [riemann-console.config :as config]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/app]
                  (.getElementById js/document "app")))

(defn init []
  (re-frame/dispatch-sync [::events/load-dashboards])
  (dev-setup)
  (mount-root))
