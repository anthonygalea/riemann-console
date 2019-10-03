(ns riemann-console.subs
  (:require
   [clojure.string :as string]
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::loading?
 (fn [db]
   (:loading? db)))

(re-frame/reg-sub
 ::saving?
 (fn [db]
   (:saving? db)))

(re-frame/reg-sub
 ::notification
 (fn [db]
   (:notification db)))

(re-frame/reg-sub
 ::configuring-widget
 (fn [db]
   (:configuring-widget db)))

(re-frame/reg-sub
 ::dashboard-settings?
 (fn [db]
   (:dashboard-settings? db)))

(re-frame/reg-sub
 ::delete-confirmation?
 (fn [db]
   (:delete-confirmation? db)))

(re-frame/reg-sub
 ::stream
 (fn [db [_ widget-id]]
   (get-in db [:streams widget-id])))

(re-frame/reg-sub
 ::streams?
 (fn [db [_]]
   (-> db :streams count pos?)))

(re-frame/reg-sub
 ::dashboards
 (fn [db]
   (:dashboards db)))

;; dashboard

(re-frame/reg-sub
 ::current-dashboard
 (fn [db]
   (get-in db [:dashboard :id])))

(re-frame/reg-sub
 ::dashboard-name
 (fn [db]
   (get-in db [:dashboard :name])))

(re-frame/reg-sub
 ::dashboard-endpoint
 (fn [db]
   (get-in db [:dashboard :endpoint])))

(re-frame/reg-sub
 ::dashboard-description
 (fn [db]
   (get-in db [:dashboard :description])))

(re-frame/reg-sub
 ::dashboard-widgets
 (fn [db]
   (get-in db [:dashboard :widgets])))

(re-frame/reg-sub
 ::widget
 (fn [db [_ id]]
   (get-in db [:dashboard :widgets id])))

(re-frame/reg-sub
 ::widget-configurer-property
 (fn [db [_ property]]
   (let [widget-id (:configuring-widget db)]
     (get-in db [:dashboard :widgets widget-id property]))))

(re-frame/reg-sub
 ::dashboard-widget-fields
 (fn [db [_ widget-id]]
   (-> db
       (get-in [:dashboard :widgets widget-id :fields])
       (string/split #","))))
