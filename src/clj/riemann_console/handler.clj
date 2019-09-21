(ns riemann-console.handler
  (:require
    [compojure.core :refer [GET POST PUT DELETE PATCH defroutes]]
    [compojure.route :as route]
    [muuntaja.core :as m]
    [muuntaja.middleware :as muuntaja]
    [riemann-console.config :as config]
    [ring.util.response :as response]
    [ring.middleware.reload :as reload]
    [shadow.http.push-state :as push-state]))

(defn load-dashboards
  [request]
  (response/response (config/get-dashboard-names)))

(defn load-dashboard-config
  [id]
  (let [dashboard-config (config/get-dashboard id)]
    (when dashboard-config
      (response/response (assoc dashboard-config :id id)))))

(defn save-dashboard-config
  ([config]
   (let [dashboard (config/save-dashboard config)]
     (response/created (str "/d/" (:id dashboard)) dashboard)))
  ([id config]
   (let [existed? (config/get-dashboard id)
         dashboard (config/save-dashboard id config)]
     (if existed?
       (response/response dashboard)
       (response/created (str "/d/" id) dashboard)))))

(defn update-dashboard-config
  [id config]
  (config/update-dashboard id config)
  (load-dashboard-config id))

(defn delete-dashboard-config
  [id]
  (config/delete-dashboard id)
  {:status 204})

(defroutes routes
  (GET "/d/:id" [id] (load-dashboard-config id))
  (PUT "/d/:id" request (save-dashboard-config
                         (-> request :params :id)
                         (:body-params request)))
  (PATCH "/d/:id" request (update-dashboard-config
                           (-> request :params :id)
                           (:body-params request)))
  (DELETE "/d/:id" [id] (delete-dashboard-config id))
  (GET "/d" [] load-dashboards)
  (POST "/d" request (save-dashboard-config (:body-params request)))
  (GET "/" [] (response/resource-response "index.html" {:root "public"}))
  (route/resources "/"))

(def dev-handler (-> #'routes muuntaja/wrap-format push-state/handle reload/wrap-reload))

(def handler (-> #'routes muuntaja/wrap-format))
