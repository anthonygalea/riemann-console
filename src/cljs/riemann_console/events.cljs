(ns riemann-console.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [ajax.core :as ajax]
   [amalloy.ring-buffer :as ring-buffer]
   [cljs.core.async :as a :refer [<! >!]]
   [cognitect.transit :as t]
   [goog.string :as string]
   [day8.re-frame.http-fx]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [riemann-console.db :as db]))

(def request-timeout 20000)

(re-frame/reg-event-db
 ::delete-confirmation
 (fn [db _]
   (-> db
       (assoc :dashboard-settings? false)
       (update :delete-confirmation? not))))

(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(re-frame/reg-event-db
 ::layout-changed
 (fn [db [_ layout]]
   (let [widgets (->> layout
                      (map (fn [{:keys [x y w h i]}]
                             [i {:x x :y y :w w :h h}]))
                      (into {}))]
     (update-in db [:dashboard :widgets] deep-merge widgets))))

(re-frame/reg-event-fx
 ::notification
 (fn [{:keys [db]} [_ notification]]
   {:db (assoc db :notification notification)
    :dispatch-later [{:ms 2000 :dispatch [::clear-notification]}]}))

(re-frame/reg-event-db
 ::clear-notification
 (fn [db [_ notification]]
   (assoc db :notification nil)))

(re-frame/reg-event-fx
 ::load-dashboards
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :http-xhrio {:method          :get
                 :uri             (str "/d")
                 :timeout         request-timeout
                 :response-format (ajax/transit-response-format)
                 :on-success      [::loading-dashboards-succeeded]
                 :on-failure      [::loading-dashboards-failed]}}))

(re-frame/reg-event-fx
 ::loading-dashboards-succeeded
 (fn [{:keys [db]} [_ result]]
   {:db (-> db
            (assoc :loading? false)
            (assoc :dashboards result))
    :dispatch [::load-dashboard (->> result (sort-by val) ffirst)]}))

(re-frame/reg-event-fx
 ::loading-dashboards-failed
 (fn [_ _]
   {:dispatch [::notification {:type :error
                               :message "Loading dashboards failed"}]}))

(re-frame/reg-event-fx
 ::load-dashboard
 (fn [{:keys [db]} [_ id]]
   {:stream {:action :remove
             :widgets (get-in db [:dashboard :widgets])}
    :db   (-> db
              (assoc :loading? true)
              (assoc :configuring-widget nil)
              (assoc :dashboard {}))
    :http-xhrio {:method          :get
                 :uri             (str "/d/" id)
                 :timeout         request-timeout
                 :response-format (ajax/transit-response-format)
                 :on-success      [::loading-dashboard-succeeded]
                 :on-failure      [::loading-dashboard-failed]}}))

(re-frame/reg-event-fx
 ::loading-dashboard-succeeded
 (fn [{:keys [db]} [_ result]]
   {:db (-> db
            (assoc :loading? false)
            (assoc :dashboard (js->clj result {:keywordize-keys? true})))
    :stream {:action :add
             :endpoint (:endpoint (js->clj result {:keywordize-keys? true}))
             :widgets (-> result
                          (js->clj {:keywordize-keys? true})
                          :widgets)}}))

(re-frame/reg-event-fx
 ::loading-dashboard-failed
 (fn [_ _]
   {:dispatch [::notification {:type :error
                               :message "Loading dashboard failed"}]}))

(re-frame/reg-event-fx
 ::dashboard-saved
 (fn [{:keys [db]} _]
   {:db (assoc db :saving? true)
    :http-xhrio {:method          :put
                 :uri             (str "/d/" (get-in db [:dashboard :id]))
                 :timeout         request-timeout
                 :params          (:dashboard db)
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/transit-response-format)
                 :on-success      [::saving-dashboard-succeeded]
                 :on-failure      [::saving-dashboard-failed]}
    :dispatch [::notification {:type :success
                               :message "Dashboard saved"}]}))

(re-frame/reg-event-db
 ::saving-dashboard-succeeded
 (fn [db _]
   (assoc db :saving? false)))

(re-frame/reg-event-fx
 ::saving-dashboard-failed
 (fn [_ _]
   {:dispatch [::notification {:type :error
                               :message "Saving dashboard failed"}]}))

(re-frame/reg-event-fx
 ::dashboard-added
 (fn [{:keys [db]}]
   {:db (assoc db :loading? true)
    :http-xhrio {:method          :post
                 :uri             "/d"
                 :timeout         request-timeout
                 :params          {}
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/transit-response-format)
                 :on-success      [::adding-dashboard-succeeded]
                 :on-failure      [::adding-dashboard-failed]}
    :stream {:action :remove
             :widgets (-> db :dashboard :widgets)}}))

(re-frame/reg-event-fx
 ::dashboard-copied
 (fn [{:keys [db]}]
   (let [dashboard (:dashboard db)]
     {:db (assoc db :loading? true)
      :http-xhrio {:method          :post
                   :uri             "/d"
                   :timeout         request-timeout
                   :params          (-> dashboard
                                        (dissoc :id)
                                        (assoc :name (str (:name dashboard) " (copy)")))
                   :format          (ajax/transit-request-format)
                   :response-format (ajax/transit-response-format)
                   :on-success      [::adding-dashboard-succeeded]
                   :on-failure      [::adding-dashboard-failed]}
      :stream {:action :remove
               :widgets (-> db :dashboard :widgets)}})))

(re-frame/reg-event-fx
 ::adding-dashboard-succeeded
 (fn [{:keys [db]} [_ result]]
   (let [dashboard (js->clj result {:keywordize-keys? true})]
     {:db (-> db
              (assoc :loading? false)
              (assoc-in [:dashboards (:id dashboard)] (:name dashboard))
              (assoc :dashboard dashboard))
      :stream {:action :add
               :endpoint (:endpoint dashboard)
               :widgets (:widgets dashboard)}
      :dispatch [::notification {:type :success
                                 :message "Dashboard added"}]})))

(re-frame/reg-event-fx
 ::adding-dashboard-failed
 (fn [_ _]
   {:dispatch [::notification {:type :error
                               :message "Adding dashboard failed"}]}))

(re-frame/reg-event-fx
 ::dashboard-endpoint-changed
 (fn [{:keys [db]} [_ new-endpoint]]
   {:db (assoc-in db [:dashboard :endpoint] new-endpoint)
    :stream {:action :add
             :endpoint new-endpoint
             :widgets (-> db :dashboard :widgets)}}))

(re-frame/reg-event-db
 ::widget-title-changed
 (fn [db [_ widget-id title]]
   (assoc-in db [:dashboard :widgets widget-id :title] title)))

(re-frame/reg-event-fx
 ::widget-query-changed
 (fn [{:keys [db]} [_ widget-id query]]
   {:db (assoc-in db [:dashboard :widgets widget-id :query] query)
    :stream {:action :add
             :endpoint (-> db :dashboard :endpoint)
             :widgets {widget-id {:query query}}}}))

(re-frame/reg-event-db
 ::widget-max-events-changed
 (fn [db [_ widget-id max-events]]
   (let [stream (get-in db [:streams widget-id])
         buffer (ring-buffer/ring-buffer max-events)]
     (-> db
         (assoc-in [:dashboard :widgets widget-id :max-events] max-events)
         (assoc-in [:streams widget-id] (into buffer (seq stream)))))))

(re-frame/reg-event-db
 ::widget-type-changed
 (fn [db [_ widget-id type]]
   (assoc-in db [:dashboard :widgets widget-id :type] (keyword type))))

(re-frame/reg-event-db
 ::widget-min-changed
 (fn [db [_ widget-id min]]
   (assoc-in db [:dashboard :widgets widget-id :min] min)))

(re-frame/reg-event-db
 ::widget-max-changed
 (fn [db [_ widget-id max]]
   (assoc-in db [:dashboard :widgets widget-id :max] max)))

(re-frame/reg-event-db
 ::widget-show-legend
 (fn [db [_ widget-id]]
   (update-in db [:dashboard :widgets widget-id :show-legend] not)))

(re-frame/reg-event-fx
 ::widget-added
 (fn [{:keys [db]} _]
   (let [id (str (random-uuid))
         widgets (-> db :dashboard :widgets)
         y (if (pos? (count widgets))
             (->> widgets vals (map #(+ (:y %) (:h %))) (apply max) (inc))
             0)
         widget {:x 0 :y y :w 12 :h 12
                 :title ""
                 :type :time-series
                 :show-legend false
                 :max-events 1000
                 :min 1
                 :max 100
                 :query "state = \"ok\""}]
     {:db (assoc-in db [:dashboard :widgets id] widget)
      :stream {:action :add
               :endpoint (-> db :dashboard :endpoint)
               :widgets {id widget}}})))

(defn update-configuring-widget-before-deleting
  "Returns a new `db` with `:configuring-widget` set to nil if the widget
  deleted was being configured, otherwise returns the same `db`."
  [db widget-id]
  (if (= widget-id (:configuring-widget db))
    (assoc db :configuring-widget nil)
    db))

(re-frame/reg-event-fx
 ::widget-deleted
 (fn [{:keys [db]} [_ widget-id]]
   {:db (-> db
            (update-configuring-widget-before-deleting widget-id)
            (update-in [:dashboard :widgets] dissoc widget-id)
            (update-in [:dashboard :streams] dissoc widget-id))
    :stream {:action :remove
             :widgets {widget-id {}}}}))

(re-frame/reg-event-db
 ::configure-widget
 (fn [db [_ widget-id]]
   (if (= widget-id (:configuring-widget db))
     (assoc db :configuring-widget nil)
     (assoc db :configuring-widget widget-id))))

(re-frame/reg-event-db
 ::widget-configurer-closed
 (fn [db _]
   (assoc db :configuring-widget nil)))

(re-frame/reg-event-db
 ::dashboard-settings?
 (fn [db _]
   (update db :dashboard-settings? not)))

(re-frame/reg-event-db
 ::dashboard-name-changed
 (fn [db [_ new-name]]
   (assoc-in db [:dashboard :name] new-name)))

(re-frame/reg-event-db
 ::dashboard-description-changed
 (fn [db [_ new-description]]
   (assoc-in db [:dashboard :description] new-description)))

(re-frame/reg-event-fx
 ::dashboard-settings-saved
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc :dashboard-settings? false)
            (assoc :loading? true))
    :http-xhrio {:method          :patch
                 :uri             (str "/d/" (get-in db [:dashboard :id]))
                 :timeout         request-timeout
                 :params          (select-keys (:dashboard db)
                                               [:name :description])
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/transit-response-format)
                 :on-success      [::saving-dashboard-settings-succeeded]
                 :on-failure      [::saving-dashboard-settings-failed]}}))

(re-frame/reg-event-fx
 ::saving-dashboard-settings-succeeded
 (fn [{:keys [db]} _]
   {:db (let [dashboard (:dashboard db)]
          (-> db
              (assoc-in [:dashboards (:id dashboard)] (:name dashboard))
              (assoc :loading? false)))
    :dispatch [::notification {:type :success
                               :message "Dashboard settings saved"}]}))

(re-frame/reg-event-fx
 ::saving-dashboard-settings-failed
 (fn [_ _]
   {:dispatch [::notification {:type :error
                               :message "Saving dashboard settings failed"}]}))

(re-frame/reg-event-fx
 ::dashboard-deleted
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc :delete-confirmation? false)
            (assoc :loading? true))
    :http-xhrio {:method          :delete
                 :uri             (str "/d/" (get-in db [:dashboard :id]))
                 :timeout         request-timeout
                 :format          (ajax/transit-request-format)
                 :response-format (ajax/transit-response-format)
                 :on-success      [::deleting-dashboard-succeeded
                                   (-> db :dashboard :id)]
                 :on-failure      [::deleting-dashboard-failed]}}))
           
(re-frame/reg-event-fx
 ::deleting-dashboard-succeeded
 (fn [{:keys [db]} dashboard-id]
   {:db (-> db
            (update-in [:dashboards dashboard-id] dissoc dashboard-id)
            (assoc :loading? false))
    :dispatch-n [[::load-dashboards]
                 [::notification {:type :success
                                  :message "Dashboard deleted"}]]}))

(re-frame/reg-event-fx
 ::deleting-dashboard-failed
 (fn [_ _]
   {:dispatch [::notification {:type :error
                               :message "Deleting dashboard failed"}]}))

;; event streaming

(def r (t/reader :json))

(re-frame/reg-event-db
 ::riemann-event
 (fn [db [_ widget-id event]]
   (update-in db [:streams widget-id] conj (t/read r event))))

(re-frame/reg-event-db
 ::init-stream-buffer
 (fn [db [_ widget-id]]
   (let [max-events (get-in db [:dashboard :widgets widget-id :max-events])]
     (assoc-in db [:streams widget-id] (ring-buffer/ring-buffer max-events)))))

(re-frame/reg-event-db
 ::stream-removed
 (fn [db [_ widget-id]]
   (update db :streams dissoc widget-id)))

(defonce streams (reagent/atom {}))

(defn remove-stream [widget-id]
  (when-some [stream (get @streams widget-id)]
    (ws/close stream)
    (swap! streams dissoc widget-id)
    (re-frame/dispatch [::stream-removed widget-id])))

(defn add-stream [widget-id endpoint query]

  ;;make sure that a widget never has more than one stream
  (remove-stream widget-id)

  (go (let [stream (<! (ws/connect (str "ws://" endpoint
                                        "/index?subscribe=true&query="
                                        (string/urlEncode query))))]
        (when (ws/connected? stream)
          (swap! streams assoc widget-id stream)
          (re-frame/dispatch [::init-stream-buffer widget-id])
          (loop []
            (when-let [event (<! (:source stream))]
              (re-frame/dispatch [::riemann-event widget-id event])
              (recur)))))))

(re-frame/reg-fx
  :stream
  (fn [{:keys [action widgets endpoint]}]
    (doseq [widget widgets]
      (case action
        :add (add-stream (key widget) endpoint (:query (val widget)))
        :remove (remove-stream (key widget))))))
